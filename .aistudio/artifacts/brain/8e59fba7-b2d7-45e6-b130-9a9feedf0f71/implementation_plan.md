# 🎯 Planejamento de Design System e Arquitetura de UX — Radar Coordinator

> **Metodologia Startup de Alto Impacto**: Inspirado no modelo de Design System e Engenharia de Produto de referências em mobilidade urbana e entregas (*Uber Driver, iFood, Waze e Tesla In-Car UX*), focado em **segurança em trânsito, zero distração e leitura instantânea**.

---

## 1. Visão Geral do Produto e Decisões de Design

| Pilar | Decisão Estratégica |
|---|---|
| **Direção Estética** | **Cockpit HUD Tático Minimalista** (alta legibilidade sob sol intenso e chuva, sem poluição visual). |
| **Arquitetura de Telas** | **3 Abas Canônicas (M3 NavigationBar)**: `Radar Cockpit`, `Mesclagens & Rotas` e `Financeiro & Plano`. |
| **Prioridade de Campo de Visão** | **R$/km e Lucro Líquido em Destaque Instantâneo** com tipografia monoespacada gigante + **Modo Alto Contraste Automático**. |
| **Regra de Ouro (Segurança)** | **Regra dos 2 Segundos**: O motoboy em movimento deve absorver o valor da corrida e a recomendação (Aceitar / Recusar) em menos de 2 segundos. |

---

## 2. Design System: Cores, Tipografia e Tokens

### 2.1 Paleta Cromática Tática (OLED Dark + Modo Sol)
* **Fundo Base (OLED Pure Black)**: `#0A0A0E` (economia de bateria e redução de reflexo no capacete).
* **Superfície Elevada (Cards Táticos)**: `#14141E` com bordas sutis em `#242436`.
* **Cor de Lucro Máximo (Emerald Green)**: `#00FF88` (destaque instantâneo de R$/km excelente).
* **Cor de Ação Secundária (Cyan Tech)**: `#00D2FF` (mesclagens, rotas e corridors).
* **Cor de Alerta e Atenção (Amber Safety)**: `#F59E0B` (desvios de rota, zonas de lentidão e combustível).
* **Cor de Risco Crítico / Recusa (Danger Red)**: `#EF4444` (alertas de blitz policial e corridas com prejuízo).
* **Modo Luz Solar Direta (High Sunlight)**: Fundo `#FFFFFF`, cards `#F1F5F9`, texto preto `#0F172A` e badges em saturação ultra-alta (100% legível sob sol do meio-dia).

### 2.2 Hierarquia Tipográfica
* **Métrica Principal (R$/km e Ganho)**: 28sp a 32sp, Monospace ExtraBold (leitura periférica imediata).
* **Nome do Estabelecimento / Destino**: 18sp, Bold, truncamento inteligente com reticências.
* **Badges de Decisão da IA (ACEITAR / RECUSAR)**: 12sp, Black, uppercase com cantos arredondados (12dp) e preenchimento sólido.

---

## 3. Arquitetura de Navegação (3 Abas Principais)

### 🧭 Aba 1: Radar Cockpit (Operação em Tempo Real)
* **Hero HUD**: Velocidade atual via GPS Fused + Trava de Segurança (> 5 km/h) com feedback visual claro.
* **Oferta em Destaque**: O card da melhor oportunidade em análise (iFood, 99, Uber, Rappi) com botão gigante de Aceite manual ou por voz.
* **Banner de Alerta Crítico (Push FCM)**:
  * Alerta de Blitz Policial com sirene e rota de desvio.
  * Oferta relâmpago de entrega.
* **Barra de Telemetria de Foco**: Ativador do Copiloto por Voz e Modo Luva (botões expandidos para toque com luva de motoqueiro).

### 💎 Aba 2: Mesclagens & Rotas (Multi-App Synergy)
* **Radar de Oportunidades Combinadas**: Lista de pares de corrida com desvio angular menor que 15° no mesmo eixo viário.
* **Métricas da Dupla**: Ganho combinado total, desvio em metros (+350m) e projeção de ganho por hora (ex: R$ 54,00/h).
* **Ação Rápida de Split-Screen**: Botão de abertura simultânea e transição assistida para os dois apps parceiros (ex: iFood + 99).
* **Mapa de Rotas Integradas**: Coletas e entregas sequenciadas (Origem 1 ➔ Origem 2 ➔ Entrega 1 ➔ Entrega 2).

### 📊 Aba 3: Finanças & Plano Pro (Gestão e Sustentabilidade)
* **Ganhos do Dia**: Faturamento Bruto vs. Desconto Real de Combustível vs. **Lucro Líquido no Bolso**.
* **Progresso da Meta Diária**: Barra dinâmica com previsão de término de turno para atingir a meta (ex: R$ 250,00).
* **Gestão do Plano Pro**: Acesso aos recursos exclusivos (Multi-Stack ilimitado, Alertas de Blitz em tempo real e Assistente Neural).
* **Histórico e Exportação**: Extrato das corridas realizadas e relatório MEI simplificado.

---

## 4. Cronograma de Implementação Modular

```
[Fase 1: M3 Navigation Scaffold] ➔ [Fase 2: Cockpit HUD & Cartões Táticos] ➔ [Fase 3: Refinamento de Micro-interações]
```

### 🔹 Fase 1: Scaffold Central e Sistema de Abas (Turno 1)
1. Implementar o `Scaffold` com `NavigationBar` M3 de 3 abas (`Radar Cockpit`, `Mesclagens`, `Finanças`).
2. Mover o painel de ofertas e o HUD principal para a Aba 1.
3. Centralizar os motores de mesclagem (`DeepOrderPoolMergingEngine` e `AngularProximityKitchenMergingOptimizer`) na Aba 2.
4. Consolidar os relatórios financeiros (`RealTimeProfitEngine`, `FuelProfitCalculator` e metas) na Aba 3.

### 🔹 Fase 2: Redesenho dos Cards com Foco no R$/km e Modo Sol (Turno 2)
1. Aplicar a nova identidade tática nos cards de ofertas individuais e nos banners de push FCM.
2. Criar a alternância rápida e automática para o **Modo Sol (Luz Solar Forte)** com contraste maximizado.
3. Garantir touch targets mínimos de 48dp a 56dp nos botões de aceitar/recusar (Modo Luva).

### 🔹 Fase 3: Validação de Performance e Testes Automatizados (Turno 3)
1. Rodar `compile_applet` para assegurar build limpo.
2. Executar suíte de testes unitários com foco na navegação e nos cálculos de rentabilidade.
3. Gerar o novo pacote `.aab` atualizado para o Play Console.
