# Especificação Técnica & Arquitetura SaaS — Radar Coordinator

## 1. Proposta de Valor & Loop de Retenção (Hook Model)
- **Dor Principal:** Entregadores e motoboys perdem dinheiro em corridas abaixo do custo real de combustível/tempo ou sofrem acidentes tentando calcular R$/km e rotas duplas enquanto pilotam.
- **Aha! Moment (< 30 segundos, sem barreira de cadastro longo):** Ao abrir o app e ativar o turno ou testar uma oferta simulada, o motorista vê o **HUD flutuante + voz no capacete** anunciando imediatamente `R$/km líquido`, `desvio Haversine` para mesclagem e recomendação `ACEITAR / RECUSAR`.
- **Loop de Retenção (Hook Model):**
  - *Gatilho Interno:* Insegurança financeira ao receber nova entrega no trânsito.
  - *Gatilho Externo:* Notificação recebida do iFood/99/Uber/Rappi + Resumo de Meta Diária no turno ativo.
  - *Ação:* Olhar rápido no HUD ou comando de voz mãos-livres (1 toque ou zero toque).
  - *Recompensa Variável:* Ganho extra imediato em corridas mescladas e lucro líquido preservado ao bloquear corridas ruins.
  - *Investimento:* Histórico financeiro diário, calibração de combustível e metas sincronizadas na nuvem (criam custo de troca orgânico).

---

## 2. Arquitetura & Stack de Baixo Custo / Alta Escala
- **Cliente Mobile (Atual no Projeto):** Android Nativo em **Kotlin + Jetpack Compose + Material 3**, cache local offline com **Room**, autenticação **Google Sign-In via Credential Manager**, nuvem em **Cloud Firestore (com Zero-Trust Rules)** e cobrança na loja via **Google Play Billing v9.1.0**.
- **Backend API & Automações:** **FastAPI (Python)** em VPS Hostinger + Nginx + SSL, protegido por rate limiter, `usage_ledger` de cotas de IA e painel administrativo isolado via túnel SSH (`127.0.0.1:8443`).
- **Camada de Escala Futura (PostgreSQL / Supabase):** Migração pronta em `docs/postgres_rls_migration.sql` com **Row Level Security (RLS)** ativo em 100% das tabelas e função transacional atômica (`SELECT FOR UPDATE`).
- **Pagamentos:** **Google Play Billing** dentro do `.aab` publicado na Play Store (`radar_pro_mensal`, `radar_pro_anual`) e **Asaas PIX/Split** protegido por webhook verificado para operação web/externa.

---

## 3. Matriz de Blindagem (10 Falhas Críticas & Gargalos Resolvidos)

| # | Risco / Gargalo | Como Está Blindado no Radar Coordinator |
|---|---|---|
| 1 | **Race condition em webhooks de pagamento** | Tabela `processed_payments` (SQLite) e `apply_payment_webhook_atomic` com `SELECT FOR UPDATE` + `webhook_events` PK no PostgreSQL (`docs/postgres_rls_migration.sql`). |
| 2 | **Manipulação de webhook Asaas/Billing** | Verificação timing-safe de `asaas-access-token` (`ASAAS_WEBHOOK_SECRET`) + validação oficial de compra via `BillingClient` no servidor. |
| 3 | **Confiança cega no cliente (Zero-Trust)** | Preços, expiração e status de assinatura checados no servidor; confirmação simulada de PIX removida e rotas de billing isoladas por `IS_PLAY_STORE_COMPLIANT_MODE`. |
| 4 | **Vazamento de segredos no APK/HTML/JS** | Zero senhas ou chaves mestres no APK (`AdminSecurityManager` usa sessão efêmera validada no servidor com `scrypt`); Painel Admin isolado fora da URL pública. |
| 5 | **Abuso de IA e explosão de custo de API** | Tabela `usage_ledger` por `user_id` com teto diário (30 grátis / 500 PRO) + `/api/v1/generate-enxame` travado com `verify_admin_token`. |
| 6 | **Brute-force / Spam em rotas públicas** | Rate limiter deslizante (`check_rate_limit`) no login admin, ativação de chaves, exclusão de conta e análise de corridas (HTTP 429). |
| 7 | **Modo offline e perda de telemetria no trânsito** | Banco local **Room** (`cached_radar_offers` / `cached_delivery_routes`) + fila `ConcurrentLinkedQueue` em `RadarBackendSyncManager` + cálculo Haversine local sem depender de internet. |
| 8 | **Acesso cruzado entre usuários (IDOR)** | Firestore Rules (`isOwner(userId)`), PostgreSQL RLS (`auth.uid() = user_id`) e `/api/v1/account/delete` exigindo `Bearer token` verificado do titular ou Admin. |
| 9 | **Duplo clique em pagamentos / ações críticas** | Estados de carregamento (`isDeletingAccount`, `isGeneratingPix`, `PlayBillingPurchaseState.Loading`) desabilitam o botão imediatamente durante a transação. |
| 10 | **Backup e observabilidade 24/7** | Endpoint `/health`, tabela `crash_reports` com painel de diagnóstico, alertas automáticos no `/api/admin/metrics` e recomendação de cron diário de snapshot SQLite/pg_dump. |

---

## 4. Design System & Mapa de Telas Implementados
- **Estética & Ergonomia:** Tema OLED de alto contraste (`DarkBg`, `NeonGreen`, `FintechCyan`, `FintechGold`), alvos táteis >= `48.dp`, uso com uma mão na moto e suporte a Light/Dark.
- **Estados Obrigatórios:** `CloudUiState` (`Loading`, `Success`, `Error`), empty states com CTA, modo offline resiliente (Room + Haversine local).
- **Onboarding & Conversão:** Tour guiado em 3 passos (`GuidedOnboardingTour.kt`), Paywall ancorado (`FintechPlansSelectionScreen.kt` e `StartupFintechCheckoutScreen.kt`) e Growth Loop nativo (`ViralMarketingShareManager.kt` + `ReferralViralShareCard.kt`).
- **Mapa de Telas:**
  1. **Home / Cockpit (`RadarMainScreen` / `StartupDeliveryCockpitScreen`):** Lucro do dia, velocidade, radar de hot-spots e status de turno.
  2. **Ação Principal (`OfferListScreen` / HUD Flutuante):** Análise em tempo real, R$/km líquido e desvio de mesclagem.
  3. **Histórico & Relatórios (`DeliveryHistoryStatementCard` / `AnalyticsDashboardSheet` / `MeiTaxReportCard`):** Extrato de corridas, economia de combustível e relatório MEI.
  4. **Assinatura (`FintechPlansSelectionScreen` / `StartupFintechCheckoutScreen`):** Fluxo 100% Google Play Billing na versão da loja.
  5. **Configurações, Privacidade & Admin (`FilterSettingsScreen` / `AdminDeveloperCockpitScreen`):** Calibração de metas, lab de diagnóstico e botão nativo **"Excluir Minha Conta e Dados"**.

---

## 5. Conformidade Play Store, LGPD & Lançamento (Primeiros 100 Pagantes)
1. **Exclusão de Conta e Dados:** Botão nativo em **Configurações > Conta, Privacidade & LGPD > Excluir Minha Conta e Dados** (`AccountDeletionManager.kt`) + endpoint autenticado `/api/v1/account/delete` + instrução explícita em `privacy.html`.
2. **Checklist Pré-Produção:**
   - Habilitar SSL/HTTPS na VPS (`deploy/enable_https_ssl.sh`) e então mudar `usesCleartextTraffic="false"` no `AndroidManifest.xml`.
   - Publicar `privacy.html` em URL HTTPS.
   - Subir `app/build/outputs/bundle/release/app-release.aab`, cadastrar `radar_pro_mensal` e `radar_pro_anual` no Play Console e cumprir **Teste Fechado** (12 testadores por 14 dias).
3. **Validação dos Primeiros 100 Pagantes:**
   - Recrutar 15 motoboys reais da sua praça no Teste Fechado medindo economia real de km e taxa de conversão no `GET /api/admin/metrics` (`conversion_rate`, `mrr_estimate`, `ai_cost_30d`, `churn_count`).
   - Ativar cron de backup diário do banco na VPS e alerta de monitoramento no `/health`.
