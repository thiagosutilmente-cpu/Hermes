# 🔑 Como Conectar sua Conta do Meta Ads na VPS Hostinger (Guia Rápido)

> **Passo a Passo Simplificado para o Dono do Negócio**  
> *Configure suas credenciais uma única vez e deixe a VPS Hostinger gerenciar e otimizar as campanhas 24 horas por dia de forma 100% automática, sem precisar do seu computador ou notebook ligado.*

---

## 🚀 1. Visão Geral da Automação 24/7

Quando configurado:
* A **VPS Hostinger (`187.77.248.73:8000`)** cria e mantém ativa a campanha no Facebook e Instagram.
* Segmenta automaticamente entregadores de aplicativos nas capitais de maior movimento (São Paulo, Rio de Janeiro, Belo Horizonte e Curitiba).
* Monitora cliques, impressões e custo por clique (CPC) a cada hora.
* Você controla tudo (pausar, ativar, ajustar orçamento) **direto pelo celular** dentro do painel restrito do app Radar Coordinator (PIN `1984`).

---

## 🛠️ 2. Como Obter seu Token de Acesso Permanente no Meta (5 Minutos)

Para que a VPS possa criar os anúncios no seu Facebook/Instagram Ads de forma oficial:

### Passo 1: Acessar o Gerenciador de Negócios do Meta
1. Acesse no navegador: [business.facebook.com/settings](https://business.facebook.com/settings).
2. No menu lateral esquerdo, vá em **Usuários** > **Usuários do Sistema**.
3. Clique em **Adicionar**.
   * Nome: `VPS Hostinger Jarvis`
   * Função: `Administrador`
4. Clique em **Criar usuário do sistema**.

### Passo 2: Dar Permissão na Conta de Anúncios
1. Clique no usuário do sistema criado (`VPS Hostinger Jarvis`).
2. Clique no botão **Atribuir ativos**.
3. Selecione **Contas de anúncios** > Selecione a sua conta de anúncios > Ative **Controle total (Gerenciar campanhas)**.
4. Clique em **Salvar alterações**.

### Passo 3: Gerar o Token de Acesso Permanente
1. Com o usuário selecionado, clique no botão **Gerar novo token**.
2. Selecione o seu aplicativo do Meta.
3. Defina a validade como **Permanente (Nunca expira)**.
4. Marque as seguintes permissões essenciais:
   * ✅ `ads_management`
   * ✅ `ads_read`
   * ✅ `business_management`
5. Clique em **Gerar token**.
6. **Copie e salve esse token** (ele começa com `EAA...`). Esse é o seu `META_ACCESS_TOKEN`.

### Passo 4: Localizar o ID da sua Conta de Anúncios
1. No menu lateral esquerdo do Gerenciador de Negócios, clique em **Contas de anúncios**.
2. Copie o número da conta (exemplo: `act_123456789012345`). Esse é o seu `META_AD_ACCOUNT_ID`.

---

## 🌐 3. Inserindo as Credenciais na VPS Hostinger

Acesse o terminal da sua VPS Hostinger (ou edite o arquivo `.env` da aplicação):

```bash
# Acesse o diretório da aplicação na VPS
cd /root/jarvis/deploy

# Abra o arquivo de variáveis de ambiente
nano .env
```

Adicione ou atualize as seguintes linhas:
```env
META_ACCESS_TOKEN=EAABcd...coloque_seu_token_aqui...
META_AD_ACCOUNT_ID=act_123456789012345
```

Salve o arquivo (`Ctrl + O`, `Enter`, `Ctrl + X`) e reinicie o backend:
```bash
docker compose restart
# ou se estiver rodando via uvicorn/systemd:
systemctl restart jarvis_backend
```

---

## 📱 4. Como Ligar e Controlar pelo Celular

1. Abra o app **Radar Coordinator** no seu celular Android.
2. Toque 5 vezes na versão no rodapé (`v2.4.0`) para abrir o diálogo de PIN.
3. Digite o PIN `1984` e confirme.
4. No card **AUTOMAÇÃO 24H META ADS**:
   * Toque em **ATIVAR 24H** para iniciar as veiculações.
   * Selecione o orçamento diário desejado (**R$ 15, R$ 20, R$ 30 ou R$ 50/dia**).
   * Acompanhe os cliques, impressões e custo por clique (CPC) em tempo real.
5. Você pode fechar o app e desligar seu computador: a VPS continuará rodando os anúncios e otimizando o público ininterruptamente!
