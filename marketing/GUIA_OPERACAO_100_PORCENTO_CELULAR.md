# 📱 Guia de Operação 100% pelo Celular (Zero Computador)
## Jarvis Radar Motoboy — Máquina de Vendas & Marketing Autônoma com Claude

Você **não precisa de notebook nem computador**. O seu servidor VPS na Hostinger (`187.77.248.73`) já está configurado para trabalhar como seu funcionário digital 24 horas por dia. O seu smartphone Android será o seu painel de controle e faturamento.

---

## 🎯 1. Os 3 Aplicativos Gratuitos para Baixar no seu Celular

Abra a **Google Play Store** no seu celular e instale:
1. **Termius** ou **JuiceSSH** (Para acessar o servidor em 1 toque e colar comandos).
2. **Asaas** (Para acompanhar as notificações de PIX caindo e transferir o lucro).
3. **Gerenciador de Anúncios da Meta (Meta Ads)** (Para acompanhar o alcance dos anúncios no Instagram/Facebook).

---

## 🔑 2. Pegando suas Chaves 100% pelo Navegador do Celular

### Passo 2.1: Chave do Claude (Anthropic)
1. No Chrome do celular, acesse: **[console.anthropic.com](https://console.anthropic.com/)**
2. Crie sua conta com Google e faça uma recarga inicial de **$5 USD** (aprox. R$ 28).
3. Vá no menu lateral ➔ **API Keys** ➔ **Create Key**.
4. Copie a chave gerada (começa com `sk-ant-...`).

### Passo 2.2: Chave do Asaas (Recebimento PIX R$ 25/semana)
1. Abra o app ou site do **Asaas**.
2. Vá em **Configurações da Conta** ➔ **Integrações** ➔ **Gerar Chave de API**.
3. Copie a chave gerada.

---

## ⚡ 3. Ativando o Agente no Servidor pelo Celular (Apenas 1 Vez)

1. Abra o app **Termius** no seu celular.
2. Adicione uma nova conexão:
   - **Host / IP:** `187.77.248.73`
   - **Usuário:** `root`
   - **Senha:** *(A senha da sua VPS Hostinger)*
   - **Porta:** `22`
3. Toque para conectar. Na tela preta do terminal, copie e cole este comando único:

```bash
cd /app/applet 2>/dev/null || cd /root/applet || cd /var/www/radar
export ANTHROPIC_API_KEY="SUA_CHAVE_CLAUDE_AQUI"
export ASAAS_API_KEY="SUA_CHAVE_ASAAS_AQUI"
export ADMIN_WHATSAPP_PHONE="5511SEUNUMERO"
bash deploy_autonomous_marketer.sh
```

4. Pronto! O Agente do Claude já entra em execução contínua em segundo plano e você pode fechar o Termius. O servidor **não dorme e não gasta a bateria do seu celular**.

---

## 🤖 4. Como o Claude Trabalha Sozinho por Você 24 Horas

| Momento | O que o Claude faz automaticamente |
| :--- | :--- |
| **08:00 (Manhã)** | 1. Analisa as métricas do dia anterior.<br>2. Gera **5 novos anúncios** e 3 roteiros virais para TikTok/Reels.<br>3. Envia o **Relatório Matinal** direto no seu WhatsApp pessoal com faturamento, novos clientes e gasto diário. |
| **Durante o Dia** | 1. Fica de plantão no WhatsApp respondendo os motoboys interessados em menos de 5 segundos.<br>2. Quebra objeções com gírias reais da pista de SP.<br>3. Gera o QR Code PIX de R$ 25 do Asaas assim que o motoboy decide comprar. |
| **Na Confirmação do PIX** | O sistema recebe o alerta do Asaas e **envia o link do APK e a licença VIP** instantaneamente no zap do comprador. |
| **A cada 30 min** | Audita os custos no Meta Ads. Se algum anúncio estiver com clique caro (acima de R$ 1,50), o Claude pausa o anúncio na hora para proteger seus R$ 100 de capital. |

---

## 💰 5. Sua Rotina Diária no Celular (5 Minutos por Dia)

1. **Às 08:00:** Você acorda e olha a mensagem do Claude no seu WhatsApp com o resumo do dia.
2. **No almoço:** Abre o app do **Asaas** para ver as notificações de PIX de R$ 25 recebidos.
3. **No fim do dia:** Se quiser, grava a tela do celular com o Jarvis funcionando por 20 segundos e posta no TikTok/Reels usando o roteiro que o Claude gerou.

---

## 🛡️ 6. Travas de Segurança Financeira (Garantia de Não Prejuízo)
- **Teto Rígido:** O Claude está programado para **nunca gastar mais de R$ 15,00 por dia** em anúncios.
- **Auto-Financiamento:** Bastam **4 vendas de R$ 25** na semana para recuperar 100% dos seus R$ 100 iniciais. A partir da 5ª venda, todo o dinheiro é lucro líquido caindo na sua conta bancária via Asaas.
