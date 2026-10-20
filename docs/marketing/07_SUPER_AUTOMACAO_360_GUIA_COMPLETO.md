# SUPER AUTOMAÇÃO 360°: RECUPERAÇÃO DE PIX, ÁUDIO DE VOZ, OTIMIZADOR DE ADS E SOM CHA-CHING

Este documento consolida a arquitetura e operação dos 4 novos motores de crescimento e automação implementados na VPS Hostinger (`187.77.248.73:8000`) e no aplicativo Android do **Radar Coordinator**.

---

## 1. Visão Geral dos 4 Motores Autônomos

| Motor | O Que Faz Sozinho? | Impacto no Negócio |
|---|---|---|
| **1. Recuperador Ativo de Pix** | Detecta motoboys que geraram Pix mas não pagaram após 15m e 2h e envia lembrete persuasivo com alerta de blitz. | Resgata de 30% a 50% das vendas perdidas por esquecimento. |
| **2. Áudios Humanizados (Voice Engine)** | Envia notas de voz gravadas simulando "gravando áudio..." antes do disparo. | Multiplica a confiança e conexão com motoboys que preferem áudio a texto. |
| **3. Otimizador Autônomo de Meta Ads** | Aumenta o orçamento diário quando o CPA está baixo (ROAS 5.9x) e protege o caixa se o custo subir. | Escala as vendas no piloto automático sem você abrir o Gerenciador de Anúncios. |
| **4. Alerta Sonoro de Caixa Registradora** | Sintetiza o som de caixa registradora ("Cha-Ching! 💰") e envia notificação no seu celular a cada Pix pago. | Feedback imediato e gratificante de faturamento entrando em tempo real. |

---

## 2. Detalhes de Cada Motor

### 2.1. Recuperador de Pix Abandonados (`deploy/whatsapp_pix_recovery.py`)
- **Regra dos 15 Minutos:** Dispara mensagem amigável:
  > *"Fala parceiro! Vi que você gerou a chave Pix do Radar VIP mas ainda não confirmou. O plantão hoje tá com vários comandos na região. A liberação custa menos de R$ 0,70 centavos por dia e ativa no mesmo segundo que você pagar."*
- **Regra das 2 Horas:** Dispara incentivo final antes do encerramento do lote promocional.
- Se o motoboy pagar, a fila de cobrança é interrompida imediatamente pelo webhook do Asaas.

### 2.2. Áudios Humanizados no WhatsApp (`deploy/whatsapp_voice_engine.py`)
- O WhatsApp do dono envia o evento de digitação/gravação (`presence: recording`) por alguns segundos antes de enviar o áudio.
- Para o motoboy, parece 100% que você pegou o celular na mão e gravou um áudio exclusivo para ele.
- Áudio em formato nativo PTT (`audio/ogg; codecs=opus`) com botão de acelerar 1.5x e 2x.

### 2.3. Otimizador Autônomo de Meta Ads (`deploy/meta_ads_auto_optimizer.py`)
- Executado via cron ou chamada da API.
- Analisa a proporção: `Orçamento Gasto / Vendas Confirmadas`.
- Se o custo por venda estiver abaixo de R$ 8,00: escala o anúncio em 15%.
- Se o custo ultrapassar R$ 14,00: reduz o investimento ou aciona novo conjunto de criativos.

### 2.4. Alerta Sonoro de Caixa Registradora (`SaleAlertSoundManager.kt`)
- Desenvolvido diretamente com a engine `AudioTrack` nativa do Android.
- Sintetiza digitalmente o clique mecânico da gaveta abrindo seguido pelo acorde Si5 e Mi6 com harmônicos brilhantes.
- Toca mesmo offline, sem depender de arquivos `.mp3` pesados ou internet.

---

## 3. Como Controlar e Testar pelo Celular

No seu aplicativo Android:
1. Acesse o **Cockpit do Administrador**.
2. Role até o card **SUPER AUTOMAÇÕES 360°**.
3. **Testar Som de Venda:** Clique no botão dourado **TOCAR SOM DE CAIXA REGISTRADORA (CHA-CHING! 💰)** para ouvir a notificação sonora imediatamente.
4. **Testar Disparo de Recuperação:** Clique em **Testar Disparo** no motor de Pix para simular o lembrete de anti-abandono.
5. **Testar Áudio:** Clique em **Testar Áudio** para verificar a transcrição e parâmetros de nota de voz.
6. **Otimizar Anúncios:** Clique em **Otimizar Agora** para rodar o ciclo de escala de orçamento.
