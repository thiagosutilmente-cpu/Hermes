# 💳 Guia Prático: Acompanhamento de Vendas Asaas Direto no Celular

> **Manual de Operação Financeira Móvel para o Proprietário**  
> *Como receber notificações de Pix na hora, acompanhar cobranças e gerenciar transferências usando exclusivamente o seu próprio smartphone.*

---

## 📲 1. Por que usar o Aplicativo Oficial do Asaas no Celular?

Você **não precisa** ficar na frente do computador ou abrir a VPS para ver quem pagou a assinatura do Radar Coordinator:
* O Asaas possui aplicativo oficial gratuito para **Android** (Google Play Store) e **iOS** (Apple App Store).
* Toda vez que um motoboy pagar o Pix de R$ 4,90 ou o plano semanal/mensal, o aplicativo do Asaas apita no seu celular com notificação push imediata:  
  * *"Pix recebido: R$ 4,90 de João Silva"*
* O split de pagamentos cai automaticamente no seu saldo disponível.

---

## 🚀 2. Passo a Passo de Configuração no Celular

### Passo 1: Baixar o Aplicativo
1. Abra a **Google Play Store** no seu Android (ou App Store no iPhone).
2. Pesquise por **Asaas** (desenvolvedor: *Asaas Gestão Financeira Instituição de Pagamento S.A.*).
3. Toque em **Instalar** e abra o app.

### Passo 2: Fazer Login na sua Conta
1. Digite seu e-mail e senha cadastrados no Asaas (onde você gerou a API Key de produção).
2. Se solicitar autenticação de dois fatores (2FA), confirme pelo SMS ou app autenticador.

### Passo 3: Ativar Notificações Push Instantâneas
1. No menu inferior do app Asaas, toque no seu perfil (**Configurações** / Ícone de Engrenagem).
2. Selecione **Notificações**.
3. Ative as opções:
   * ✅ **Cobrança Recebida (Pix)**
   * ✅ **Transferência Concluída**
   * ✅ **Alerta de Saldo**
4. Nas configurações do Android do seu aparelho:
   * Vá em *Configurações do Android > Aplicativos > Asaas > Notificações > Permitir todas*.
   * Certifique-se de que a economia de bateria do Android não suspenda o app Asaas em segundo plano.

---

## 💰 3. Como Funciona o Fluxo Automático de Vendas

```
┌─────────────────────────┐
│   Motoboy no App        │
│   (Toca em 'Pagar Pix') │
└────────────┬────────────┘
             │ Gera QR Code Pix Dinâmico de R$ 4,90
             ▼
┌─────────────────────────┐
│   Banco Central / Asaas │
│   (Motoboy paga no banco│
│    dele: Nubank, Inter) │
└────────────┬────────────┘
             │
             ├─────────────────────────────────────────┐
             │ Confirmação Instantânea                 │ Webhook automático
             ▼                                         ▼
┌─────────────────────────┐               ┌─────────────────────────┐
│  SEU CELULAR (App Asaas)│               │  VPS (187.77.248.73)    │
│  Toca Notificação Push: │               │  Libera na hora o acesso│
│  "Pix recebido: R$ 4,90"│               │  Pro no app do motoboy  │
└─────────────────────────┘               └─────────────────────────┘
```

---

## 🏦 4. Como Sacar o Dinheiro pelo Celular

1. Abra o app do Asaas no seu celular.
2. Na tela inicial você verá o **Saldo Disponível**.
3. Toque em **Transferir** ou **Pix**.
4. Digite a sua chave Pix pessoal (seu CPF, e-mail ou celular).
5. O dinheiro cai na sua conta bancária corrente na mesma hora (TED ou Pix).

---

## ⚙️ 5. Como Inserir sua API Key na VPS (Quando for ligar em Produção)

Para que a VPS gere as cobranças vinculadas à sua conta Asaas:
1. No site do Asaas (pelo computador ou navegador do celular):
   * Vá em *Menu > Configurações da Conta > Integrações > Gerar Chave de API*.
2. Copie a chave que começa com `$aact_...`.
3. Abra o arquivo `.env` na VPS ou adicione no Secrets:
   ```env
   ASAAS_API_KEY=$aact_sua_chave_real_aqui
   ASAAS_ENVIRONMENT=production
   ```
4. Pronto! O sistema cuidará de gerar as cobranças, emitir os QR Codes e liberar as assinaturas de forma 100% autônoma.
