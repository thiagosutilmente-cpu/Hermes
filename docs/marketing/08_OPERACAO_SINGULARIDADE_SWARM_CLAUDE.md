# OPERAÇÃO SINGULARIDADE - R$ 3 MILHÕES EM 1 ANO
## ENXAME DE 10 SUB-AGENTES AUTÔNOMOS + MIND-0 (CGO)

Este documento é o playbook oficial e manual de execução da **Operação Singularidade**, projetado para rodar em malha fechada (Closed-Loop System) tanto no seu **PC via Claude / Python** quanto **24/7 na sua VPS Hostinger (187.77.248.73)**.

---

### 1. Parâmetros Matemáticos da Operação
- **Meta Anual:** R$ 3.000.000,00 (Três Milhões de Reais)
- **Meta Mensal:** R$ 250.000,00
- **Meta Diária:** R$ 8.333,33 (~278 assinaturas/dia a R$ 29,90 ou ~167 a R$ 49,90)
- **CPA Máximo Permitido:** R$ 12,00 (40% do primeiro pagamento)
- **ROAS Mínimo Global:** 2.50x
- **LTV Alvo:** 6 a 8 meses de retenção por motoboy

---

### 2. A Enxame dos 10 Sub-Agentes Especializados

| Agente | Codinome | Missão Central | Ação Chave |
|---|---|---|---|
| **01** | **SCOUT-1** | Radar de Rua & Scraper 24/7 | Detecta chuva forte, preço de combustível e tarifas dinâmicas de apps. |
| **02** | **MEDIA-2** | Engenheiro Viral | Escreve de 5 a 10 roteiros de vídeos curtos (15-30s) baseados na dor da pista. |
| **03** | **STUDIO-3** | Produtor Multimídia | Conecta ElevenLabs e legendas neon amarelas/pretas de alta retenção. |
| **04** | **ADS-4** | Media Buyer Meta/TikTok | Escala orçamentos (+20% se CPA < 20% do teto) e corta se CPA > 1.5x limite. |
| **05** | **SOCIAL-5** | Publisher Multicanal | Posta 4x ao dia (08:00, 11:30, 15:30, 19:30) e dispara links via DM automática. |
| **06** | **CLOSER-6** | Vendedor WhatsApp por Voz | Atua em checkouts abandonados em < 3min, enviando áudio humanizado no Zap. |
| **07** | **LTV-7** | Churn Killer & Indicação | Programa "Indique 3 Parceiros e Rode de Graça" + Upsell anual à vista (R$ 199). |
| **08** | **FINANCE-8** | Auditor de Margem | Trava de R$ 8.333/dia, cálculo de ROAS e liberação de 40% do lucro para reinvestir. |
| **09** | **DEV-9** | Self-Healing da VPS | Monitora webhooks Asaas, tokens de API e portas 8000/8080 sem travar nada. |
| **10** | **EVO-10** | Auto-Evolução às 23:59 | Analisa o dia, reescreve system prompts e deixa o sistema 10% mais agressivo. |

---

### 3. Como Executar no seu PC (com Claude ou Python CLI)

Você pode rodar todo o orquestrador autônomo diretamente no seu computador pessoal:

#### Passo 1: Configurar as credenciais (.env)
Copie o template de variáveis de ambiente:
```bash
cp deploy/.env.singularidade.example deploy/.env
```
Preencha suas chaves:
- `ANTHROPIC_API_KEY`: Chave da Anthropic para o Claude atuar como cérebro central.
- `META_ACCESS_TOKEN` & `META_AD_ACCOUNT_ID`: Token da Graph API do Meta Ads.
- `WHATSAPP_API_URL` & `WHATSAPP_API_KEY`: Endereço da Evolution API na sua VPS.
- `ELEVENLABS_API_KEY`: Chave da ElevenLabs para voz hiper-realista.
- `ASAAS_API_KEY`: Chave de API do Asaas para confirmação instantânea do Pix.

#### Passo 2: Executar o ciclo do MIND-0
No terminal do seu PC:
```bash
python3 deploy/operacao_singularidade_swarm.py
```
O script executará todos os 10 agentes sequencialmente, gravando os dados no banco SQLite local `deploy/operacao_singularidade.db` e gerando o relatório diário.

---

### 4. Como Executar 24/7 na VPS Hostinger

Na sua VPS Hostinger (`187.77.248.73`), o enxame já está integrado aos serviços FastAPI:

#### Execução via Cronjob Diário (Autonomia 100%)
Adicione no `crontab -e` da VPS:
```cron
# Diagnóstico matinal às 08:00
0 8 * * * cd /root/app && python3 deploy/operacao_singularidade_swarm.py >> /var/log/singularidade.log 2>&1

# Otimização do almoço às 12:00
0 12 * * * cd /root/app && python3 deploy/operacao_singularidade_swarm.py >> /var/log/singularidade.log 2>&1

# Otimização noturna às 18:00
0 18 * * * cd /root/app && python3 deploy/operacao_singularidade_swarm.py >> /var/log/singularidade.log 2>&1

# Ciclo de Auto-Evolução EVO-10 às 23:59
59 23 * * * cd /root/app && python3 deploy/operacao_singularidade_swarm.py >> /var/log/singularidade.log 2>&1
```

#### Execução via API REST
Você também pode disparar e consultar o estado do enxame via chamadas HTTP:
- **Consultar progresso da meta:** `GET http://187.77.248.73:8000/api/singularidade/status`
- **Disparar ciclo imediato:** `POST http://187.77.248.73:8000/api/singularidade/cycle/run`

---

### 5. Os 3 Primeiros Roteiros de Vídeos Orgânicos (15-30s)

#### Roteiro 1 (22s) - "A Chuva que Quebra o Celular vs O App que Salva a Semana"
- **Hook (0-3s):** *"Irmão da pista, se teu celular molhar hoje nesse temporal, quanto custa o conserto?"*
- **Cena visual:** Cortador de corredor na chuva, gotas escorrendo pelo guidão e celular no suporte tremendo.
- **Desenvolvimento (4-16s):** *"Quem roda na chuva sabe que 1 gota no conector pode tirar 3 dias de diária. Com o Radar Ativo no guidão, você ouve tudo no fone, não precisa olhar pra tela e não toma multa de radar escondido."*
- **CTA (17-22s):** *"Comenta 'CHUVA' aqui que a IA te manda o link de teste agora na DM."*

#### Roteiro 2 (26s) - "A Matemática do Pneu e da Gasolina no Fim do Mês"
- **Hook (0-3s):** *"Você trabalhou 12 horas hoje pra deixar metade da féria no posto de gasolina?"*
- **Cena visual:** Close na bomba de combustível subindo o valor e expressão cansada no capacete.
- **Desenvolvimento (4-18s):** *"A conta é simples: se você economizar 1 litro por dia evitando corrida furada e rota com trânsito travado, sobra R$ 190 limpo a mais na sua mão no final do mês. Mais do que paga a assinatura anual inteira."*
- **CTA (19-26s):** *"Comenta 'MOTO' pra liberar 7 dias sem pagar nada direto no seu WhatsApp."*

#### Roteiro 3 (19s) - "Radar Escondido na Madrugada: A Multa que Comeu seu Aluguel"
- **Hook (0-3s):** *"Já tomou aquela multa covarde de 50km/h atrás da árvore?"*
- **Cena visual:** Visão noturna de moto em avenida deserta, flash de radar e notificação vermelha.
- **Desenvolvimento (4-14s):** *"Uma multa de R$ 130 joga 2 dias de corrida no lixo. O nosso radar avisa no fone 500 metros antes com voz alta. Você não toma susto e não perde pontos na CNH."*
- **CTA (15-19s):** *"Comenta 'RADAR' que eu te mando o acesso liberado no WhatsApp."*

---

### 6. Os 3 Primeiros Anúncios de Tráfego Pago Estruturados (Ads-4)

#### Anúncio 01 - Meta Ads (Instagram Reels & Stories)
- **Nome da Campanha:** `CMP-PAGO-01-RADAR-CHUVA-PIX`
- **Público:** Homens 19-45 anos, interesses: Motociclismo, iFood para Entregadores, Uber Eats, 99Moto, Gasolina. Raio das principais capitais (SP, RJ, BH, Curitiba).
- **Criativo:** Vídeo POV piloto na chuva com áudio ElevenLabs acelerado.
- **Texto Principal:**
  *"Chuva forte na pista? Não queima teu celular nem toma multa de bobeira. Alerta no fone de ouvido 500m antes do radar. Assinatura liberada na hora via Pix por R$ 29,90. Clica e testa hoje."*
- **Botão CTA:** "Enviar mensagem no WhatsApp" (Direto para o CLOSER-6 fechar).

#### Anúncio 02 - Meta Ads (Feed Facebook / Comunidades de Motoboys)
- **Nome da Campanha:** `CMP-PAGO-02-ECONOMIA-FIM-DO-MES`
- **Público:** Entregadores e motoboys cadastrados em grupos de entrega e motofrete.
- **Criativo:** Imagem estática de alto contraste (fundo escuro, letras amarelas neon):
  *Tabela comparativa: Sem Radar (-R$ 130 de multa, -R$ 190 de gasolina gasta) vs Com Radar (+R$ 320 no bolso limpo).*
- **Texto Principal:**
  *"Quanto sobrou da sua féria ontem? Pare de trabalhar para pagar radar e posto. Instale agora e rode com quem protege seu bolso."*
- **Botão CTA:** "Obter Oferta"

#### Anúncio 03 - TikTok Ads (Spark Ads / Tráfego Noturno)
- **Nome da Campanha:** `CMP-PAGO-03-TIKTOK-MADRUGADA-RADAR`
- **Público:** 18-35 anos, horários de pico noturno (18h às 23h30).
- **Criativo:** Vídeo estilo vlog gravado na moto com corte seco e som de notificação de Pix.
- **Texto:** *"Nunca mais tomei susto de radar depois que liguei isso aqui. Salva o aluguel do motoca."*
- **Botão CTA:** "Comprar Agora" (Checkout Pix Asaas com ativação em 5 segundos).
