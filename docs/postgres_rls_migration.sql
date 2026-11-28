-- ============================================================================
-- RADAR COORDINATOR — MIGRAÇÃO POSTGRESQL / SUPABASE COM ROW LEVEL SECURITY
-- Modelo Zero-Trust: isolamento estrito por auth.uid() + idempotência atômica
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. PERFIS DE USUÁRIO & FILTROS DE RENTABILIDADE
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT,
    phone TEXT,
    daily_goal_brl NUMERIC(10,2) NOT NULL DEFAULT 250.00 CHECK (daily_goal_brl >= 0 AND daily_goal_brl <= 50000),
    min_gain_per_km NUMERIC(8,2) NOT NULL DEFAULT 2.00 CHECK (min_gain_per_km >= 0 AND min_gain_per_km <= 500),
    min_offer_value NUMERIC(10,2) NOT NULL DEFAULT 8.00 CHECK (min_offer_value >= 0 AND min_offer_value <= 5000),
    referral_code TEXT UNIQUE NOT NULL DEFAULT UPPER(SUBSTRING(ENCODE(gen_random_bytes(5), 'hex') FROM 1 FOR 8)),
    referred_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. ASSINATURAS & LICENÇAS (GERENCIADO EXCLUSIVAMENTE PELO BACKEND / WEBHOOKS)
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    provider TEXT NOT NULL CHECK (provider IN ('GOOGLE_PLAY', 'ASAAS_PIX', 'STRIPE')),
    external_customer_id TEXT,
    external_subscription_id TEXT,
    plan_tier TEXT NOT NULL CHECK (plan_tier IN ('FREE', 'PRO_WEEKLY', 'PRO_MONTHLY', 'PRO_ANNUAL')),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'OVERDUE', 'CANCELLED', 'EXPIRED')),
    price_brl NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    current_period_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_status_end
    ON public.subscriptions (status, current_period_end);

-- 3. HISTÓRICO DE CORRIDAS E MESCLAGENS DO ENTREGADOR
CREATE TABLE IF NOT EXISTS public.delivery_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    app_source TEXT NOT NULL CHECK (char_length(app_source) BETWEEN 1 AND 40),
    restaurant TEXT NOT NULL CHECK (char_length(restaurant) BETWEEN 1 AND 120),
    payout_brl NUMERIC(10,2) NOT NULL CHECK (payout_brl >= 0 AND payout_brl <= 10000),
    distance_km NUMERIC(8,2) NOT NULL CHECK (distance_km >= 0 AND distance_km <= 1000),
    fuel_cost_brl NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    merged BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_delivery_logs_user_created
    ON public.delivery_logs (user_id, created_at DESC);

-- 4. CONTROLE DE COTAS E CUSTO DE APIs EXTERNAS (IA / ROTAS)
CREATE TABLE IF NOT EXISTS public.usage_ledger (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    feature TEXT NOT NULL,
    units INTEGER NOT NULL DEFAULT 1 CHECK (units > 0),
    estimated_cost_brl NUMERIC(10,4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_usage_ledger_user_day
    ON public.usage_ledger (user_id, created_at DESC);

-- 5. EVENTOS DE WEBHOOK IDEMPOTENTES (SEM ACESSO DO CLIENTE)
CREATE TABLE IF NOT EXISTS public.webhook_events (
    event_id TEXT PRIMARY KEY,
    provider TEXT NOT NULL,
    event_type TEXT NOT NULL,
    customer_id TEXT,
    amount_brl NUMERIC(10,2),
    payload_hash TEXT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. TRILHA DE AUDITORIA IMUTÁVEL
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id UUID,
    action TEXT NOT NULL,
    target_table TEXT NOT NULL,
    target_id TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_created
    ON public.audit_logs (actor_id, created_at DESC);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) OBRIGATÓRIO EM TODAS AS TABELAS
-- ============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.delivery_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.usage_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.webhook_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- Profiles: usuário lê/atualiza apenas o próprio perfil (campos financeiros sensíveis ficam fora desta tabela)
CREATE POLICY "profiles_select_own" ON public.profiles
    FOR SELECT USING (auth.uid() = id);
CREATE POLICY "profiles_insert_own" ON public.profiles
    FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "profiles_update_own" ON public.profiles
    FOR UPDATE USING (auth.uid() = id) WITH CHECK (auth.uid() = id);
CREATE POLICY "profiles_delete_own" ON public.profiles
    FOR DELETE USING (auth.uid() = id);

-- Subscriptions: cliente só PODE LER o próprio status (jamais inserir/alterar plano pelo app!)
CREATE POLICY "subscriptions_select_own" ON public.subscriptions
    FOR SELECT USING (auth.uid() = user_id);

-- Delivery logs: usuário gerencia apenas suas próprias entregas
CREATE POLICY "delivery_logs_select_own" ON public.delivery_logs
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "delivery_logs_insert_own" ON public.delivery_logs
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "delivery_logs_delete_own" ON public.delivery_logs
    FOR DELETE USING (auth.uid() = user_id);

-- Usage ledger: leitura apenas do próprio consumo; escrita exclusiva via backend/service_role
CREATE POLICY "usage_ledger_select_own" ON public.usage_ledger
    FOR SELECT USING (auth.uid() = user_id);

-- Webhook events & Audit logs: bloqueados para qualquer cliente (apenas service_role no backend)
CREATE POLICY "webhook_events_deny_all_clients" ON public.webhook_events
    FOR ALL USING (FALSE);
CREATE POLICY "audit_logs_deny_all_clients" ON public.audit_logs
    FOR ALL USING (FALSE);

-- ============================================================================
-- FUNÇÃO TRANSACIONAL ATÔMICA PARA WEBHOOK DE PAGAMENTO (SELECT FOR UPDATE)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.apply_payment_webhook_atomic(
    p_event_id TEXT,
    p_provider TEXT,
    p_event_type TEXT,
    p_user_id UUID,
    p_plan_tier TEXT,
    p_amount_brl NUMERIC,
    p_days_valid INTEGER,
    p_payload_hash TEXT
) RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_sub_id UUID;
BEGIN
    -- Idempotência estrita: se event_id já existir, ignora silenciosamente
    INSERT INTO public.webhook_events (event_id, provider, event_type, customer_id, amount_brl, payload_hash)
    VALUES (p_event_id, p_provider, p_event_type, p_user_id::TEXT, p_amount_brl, p_payload_hash)
    ON CONFLICT (event_id) DO NOTHING;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    -- Lock pessimista na assinatura do usuário para evitar race condition
    SELECT id INTO v_sub_id
    FROM public.subscriptions
    WHERE user_id = p_user_id
    FOR UPDATE;

    IF v_sub_id IS NULL THEN
        INSERT INTO public.subscriptions (user_id, provider, plan_tier, status, price_brl, current_period_end)
        VALUES (p_user_id, p_provider, p_plan_tier, 'ACTIVE', p_amount_brl, NOW() + make_interval(days => p_days_valid));
    ELSE
        UPDATE public.subscriptions
        SET provider = p_provider,
            plan_tier = p_plan_tier,
            status = 'ACTIVE',
            price_brl = p_amount_brl,
            current_period_end = GREATEST(COALESCE(current_period_end, NOW()), NOW()) + make_interval(days => p_days_valid),
            updated_at = NOW()
        WHERE id = v_sub_id;
    END IF;

    INSERT INTO public.audit_logs (actor_id, action, target_table, target_id, metadata)
    VALUES (p_user_id, 'SUBSCRIPTION_ACTIVATED', 'subscriptions', p_user_id::TEXT, jsonb_build_object('event_id', p_event_id, 'plan', p_plan_tier));

    RETURN TRUE;
END;
$$;
