# 📱 Plano de Implementação: Simulação de Corridas Reais com Mesclagem & Tela Dividida

## 🎯 Objetivo
Permitir que o condutor/motoboy simule e vivencie em tempo real no app o fluxo completo de **mesclagem profunda de corridas (iFood + 99Food + Rappi)** e a **divisão prática de tela (Split-Screen 50/50)** para uso no guidão da moto, com visualização gráfica de rotas compatíveis, desvio Haversine e acionamento em 1 toque.

---

## 🛠️ Etapas de Execução

### Etapa 1: Simulador Visual Interativo de Tela Dividida (Split Screen Live Canvas)
- Implementar componente visual interativo **`DualSplitWindowSimulatorCard`**:
  - **Metade Superior (50%):** Mockup da janela do App Primário (Ex: iFood - Coleta Madero / Entrega Consolação - R$ 19,50).
  - **Metade Inferior (50%):** Mockup da janela do App Secundário (Ex: 99Food - Coleta Outback / Entrega Consolação - R$ 21,00).
  - **Divisor Central & Floating Tactical HUD:** Mini-HUD central flutuante sobreposto exibindo o combo consolidado:
    - Ganho Bruto Total: **R$ 40,50**
    - Desvio Geodésico Haversine: **+280m • +1.4 min extras (🟢 Mínimo)**
    - Lucro Líquido: **R$ 38,20** (descontado combustível)

### Etapa 2: Integração do Acionamento Real de Split Screen
- Atualizar a ação de aceite no `DeepOrderPoolCockpitCard` e no novo simulador para disparar:
  - `SplitScreenDualRadarCoordinator.getInstance(context).launchSplitScreenPair(...)`
  - Utilizar a Accessibility API (`performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)`) para repartir a tela real do dispositivo Android entre os dois apps parceiros instalados.
  - Alerta por voz via TTS: *"Iniciando combo de iFood mais 99Food em tela dividida. Primeira coleta a 300 metros."*

### Etapa 3: Cenários Práticos de Teste em 1 Toque
- Adicionar seletores de cenários de trânsito real no Cockpit:
  1. **💎 Combo Diamante (Paulista -> Consolação):** Corredor direto, desvio de 280m, ganho de R$ 40,50.
  2. **🟡 Combo Transversal (Jardins -> Itaim):** Ruas paralelas, desvio de 650m, ganho de R$ 34,00.
  3. **🛑 Descarte de Conflito Geodésico:** Tentativa de corrida em contra-fluxo barrada com aviso sonoro para evitar que a comida esfrie.

---

## 🔍 Critérios de Sucesso e Verificação
1. **Compilação 100% OK:** O projeto deve compilar sem erros através do `compile_applet`.
2. **Interatividade no Emulador:** O motoboy pode clicar em qualquer combo para visualizar a tela dividida simulada e clicar no botão para acionar a divisão nativa.
3. **Métricas Claras:** R$ por hora, desvio em metros e minutos extras calculados pelo `HaversineGeoDeviationCalculator`.
