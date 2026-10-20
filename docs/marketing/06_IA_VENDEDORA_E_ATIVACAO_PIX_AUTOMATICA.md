# PILOTO AUTOMÁTICO 100%: IA VENDEDORA NO WHATSAPP + LIBERAÇÃO INSTANTÂNEA ASAAS PIX

Este manual descreve a arquitetura e a operação do sistema de vendas mais avançado e autônomo do **Radar Coordinator**, operando diretamente na VPS Hostinger (`187.77.248.73:8000`).

---

## 1. O que torna este sistema 100% autônomo e surpreendente?

Em sistemas tradicionais de vendas pelo WhatsApp:
- O dono precisa ficar com o celular na mão respondendo dúvidas repetitivas.
- Quando o cliente pede o Pix, o dono precisa gerar a chave manualmente.
- O cliente manda o comprovante e espera horas até o dono conferir e liberar o aplicativo.

**No Radar Coordinator com IA Autônoma:**
1. **O anúncio do Meta Ads entrega o motoboy diretamente no seu WhatsApp pessoal.**
2. **A IA no servidor responde em 1 a 2 segundos:** fala na linguagem direta do motoboy, quebra objeções de preço ("menos de R$ 0,70/dia contra multas de R$ 293+"), explica o funcionamento (alertas sonoros no fone com tela apagada).
3. **A IA gera a chave Pix Asaas Copia e Cola na hora** dentro da conversa.
4. **O motoboy paga no banco dele:** o Asaas notifica a VPS em milissegundos via Webhook (`/api/webhooks/asaas-pix`).
5. **A conta é liberada imediatamente:** o sistema cadastra a licença VIP no banco de dados e dispara automaticamente o recibo com o link do APK e boas-vindas pelo seu WhatsApp pessoal.
6. **Zero intervenção humana:** você pode estar dormindo, trabalhando ou viajando — as vendas acontecem e o dinheiro cai na sua conta Asaas.

---

## 2. Diagrama do Fluxo 100% Autônomo

```
[ Anúncio Meta Ads (Face/Instagram) ]
                 │
                 ▼
[ Motoboy clica em "Conversar no WhatsApp" ]
                 │
                 ▼
[ VPS Hostinger: Evolution API escuta a mensagem ]
                 │
                 ▼
[ Agente de IA (deploy/whatsapp_ai_sales_agent.py) ]
  ├── 1. Identifica dúvidas e objeções do motoboy
  ├── 2. Responde com argumentos persuasivos e comprovados
  └── 3. Envia Chave Pix Copia e Cola Asaas (R$ 19,90)
                 │
                 ▼
[ Motoboy realiza o Pix no app do banco ]
                 │
                 ▼ (Webhook instantâneo em milissegundos)
[ VPS Hostinger: deploy/asaas_pix_auto_activation.py ]
  ├── 1. Valida liquidação do Pix no Asaas
  ├── 2. Grava status VIP_ACTIVE no SQLite (jarvis_whatsapp_leads.db)
  └── 3. Dispara mensagem comemorativa VIP + Link do APK no WhatsApp
                 │
                 ▼
[ App Radar Coordinator no celular do Dono ]
  └── Exibe venda concluída no Cockpit com tag: "100% AUTÔNOMO"
```

---

## 3. As Objeções que a IA quebra Sozinha

| Dúvida / Objeção do Motoboy | Como a IA Responde e Convence |
|---|---|
| **"Quanto custa? É caro?"** | *"Custa apenas R$ 19,90 por mês — dá menos de R$ 0,70 centavos por dia! Uma multa de comando passa de R$ 293 fora os pontos na CNH. Na primeira blitz evitada o app já se pagou pro ano todo!"* |
| **"Funciona com celular no bolso e tela apagada?"** | *"Sim! Toca alerta no fone de ouvido bluetooth e vibra forte com a tela 100% apagada, sem gastar bateria nem esquentar o celular no guidão."* |
| **"Como funciona? Tem radar móvel?"** | *"A comunidade de motoboys reporta viaturas e radares móveis em 1 toque na tela, sincronizando com todos da região em tempo real."* |
| **"Manda o Pix" / "Quero testar"** | Envia a chave Pix oficial Copia e Cola do Asaas e explica que a liberação é imediata assim que pagar. |

---

## 4. Como Configurar o Webhook no Painel do Asaas

Para que o Asaas avise a sua VPS no mesmo segundo em que o motoboy pagar o Pix:

1. Acesse sua conta no **Asaas** (pelo navegador do celular ou computador).
2. Vá em **Configurações da Conta** > **Integrações** > **Webhooks** (ou acesse a aba Webhook para Cobranças).
3. Preencha os campos:
   - **URL do Webhook:** `http://187.77.248.73:8000/api/webhooks/asaas-pix`
   - **Versão da API:** `v3`
   - **Eventos selecionados:** Marque `PAYMENT_RECEIVED` e `PAYMENT_CONFIRMED`.
   - **Status:** Ativo.
4. Clique em **Salvar**.
5. Pronto! Cada Pix pago disparará a liberação no mesmo milissegundo.

---

## 5. Como Monitorar e Simular pelo Celular no Cockpit

No seu aplicativo Android:
1. Abra a tela de **Cockpit do Administrador / Desenvolvedor**.
2. Role até o card: **IA VENDEDORA 100% AUTÔNOMA**.
3. **Métricas:** Veja em tempo real quantas vendas foram concluídas sem você encostar no celular.
4. **Simulador ao Vivo:** Digite qualquer pergunta ou clique nos botões de teste ("Quanto custa?", "Funciona no bolso?") para ver na hora como a IA responderia no WhatsApp.
5. **Feed de Vendas:** Acompanhe cada motoboy que foi ativado com sucesso.

---

## 6. Comandos Úteis na VPS Hostinger

Para verificar os logs da IA em tempo real na VPS:
```bash
# Ver mensagens sendo negociadas pela IA no WhatsApp
tail -f /var/log/jarvis_ai_sales.log

# Ver confirmações de pagamento e liberações imediatas de Pix
tail -f /var/log/jarvis_asaas_pix.log
```
