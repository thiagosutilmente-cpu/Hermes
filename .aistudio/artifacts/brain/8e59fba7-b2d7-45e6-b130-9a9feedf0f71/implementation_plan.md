# Plano de Implementação: FuelManager & HapticFeedbackEngine (Cockpit Tático do Piloto)

Este plano detalha a arquitetura e implementação das classes **`FuelManager`** e **`HapticFeedbackEngine`**, atendendo com máxima fidelidade às respostas do entregador: calibração para **suporte rígido no guidão da moto** e assinaturas táteis exclusivas para **alta rentabilidade**, **corrida longa** e **rejeição automática**, além do cálculo de consumo urbano com compensação aerodinâmica e de tráfego para o **modo chuva**.

---

## 1. Módulo de Combustível Tático: `FuelManager`

### 1.1 Responsabilidades
* **Modelos Brasileiros Reais:** Catálogo com motos predominantes no delivery (Honda CG 160 Fan/Titan/Cargo, Biz 125, Pop 110i, Yamaha Factor 150, Fazer 250, Honda CB 300F Twister, Scooters PCX/NMax e modo Personalizado).
* **Consumo em Ciclo Urbano:** Considera o ciclo severo de entregas brasileiras (paradas em semáforos, arrancadas em 1ª/2ª marchas, corredor com variação contínua de acelerador).
* **Alternância 'Modo Seco' vs 'Modo Chuva':**
  * **Resistência Aerodinâmica Adicional:** Uso de capa de chuva e conjunto impermeável que expande a área frontal ($C_d \times A$) do piloto, gerando turbulência e arrasto extra.
  * **Resistência de Rolamento e Tráfego Lento:** Lâmina d'água no asfalto, tempo prolongado em marcha lenta (*idle*) e frenagens contínuas reduzem a autonomia por litro entre 15% e 22%.
  * **Cálculo de Custo Real por KM e Margem Líquida:** Atualização automática do custo por quilômetro rodado e threshold mínimo sugerido para aceitar corridas.

### 1.2 Integração
* Conectar `FuelManager` ao painel de cálculo financeiro (`FuelProfitCalculator`) e expor alternador de "Modo Chuva / Pista Molhada" no HUD do Cockpit Tático.

---

## 2. Motor Tátil de Guidão: `HapticFeedbackEngine`

### 2.1 Calibração para Suporte Rígido no Guidão
Em um suporte rígido montado no guidão da moto, o smartphone sofre constantes microvibrações geradas pelo motor monocilíndrico (especialmente entre 4.000 e 6.500 RPM) e pelas irregularidades do asfalto.
Para que o entregador sinta e reconheça a notificação **sem precisar olhar para a tela**:
* Utilização de `VibrationEffect.createWaveform` com amplitude máxima (`255`) e modulação de frequência de onda.
* Frequências de ataque rápidas (tempos curtos de resposta) e contrastes de silêncio para romper a vibração parasita mecânica da moto.

### 2.2 Assinaturas Táteis Exclusivas por Categoria de Oferta
1. **🏆 Alta Rentabilidade (Golden Offer / R$ 4,00+ por km):**
   * *Padrão:* Tríplice pulso cadenciado em crescendo com amplitude máxima (`255`).
   * *Sensação física:* Batida triunfal e afirmativa ("Ta-Ta-TUM!"), transmitindo ganho expressivo de imediato.
2. **🛣️ Corrida Longa (> 10 km ou percurso intermunicipal):**
   * *Padrão:* Pulso longo sustentado (350ms) seguido de eco rítmico estendido.
   * *Sensação física:* Onda suave contínua ("Vruuuum..."), identificando trajeto extenso.
3. **⛔ Rejeição Automática (Corrida Lixo / Prejuízo / Tarifa Baixa):**
   * *Padrão:* Duplo corte seco e incisivo de altíssima amplitude com intervalo curto (80ms - 40ms pausa - 80ms).
   * *Sensação física:* Dois estalos secos ("Tuc-Tuc"), informando que o Jarvis eliminou a oferta tóxica.
4. **⚡ Oferta Padrão / Aceitável:**
   * *Padrão:* Vibração dupla equilibrada (100ms vibra, 50ms pausa, 100ms vibra).
5. **⚠️ Alerta de Risco / Área Crítica:**
   * *Padrão:* Tremor rápido e intermitente de emergência.

### 2.3 Integração no Fluxo de Notificações
* Interceptar a chegada de ofertas no `AppNotificationListenerService` e no `JarvisNeuralCockpitEngine`.
* Acionar `HapticFeedbackEngine.vibrateForOffer(offer)` instantaneamente no recebimento da notificação dos apps (iFood, Rappi, Uber, 99Food), permitindo tomada de decisão tátil cega.

---

## 3. Plano de Etapas de Execução

1. **Revisar e consolidar `FuelManager.kt`:**
   * Validar catálogo completo de modelos e fórmulas de arrasto aerodinâmico e tráfego molhado.
2. **Criar `HapticFeedbackEngine.kt`:**
   * Implementar `enum class OfferHapticCategory` (HIGH_PROFIT, LONG_DISTANCE, AUTO_REJECTED, STANDARD, RISK_ALERT).
   * Implementar métodos de vibração para guidão de moto com suporte para Android 8.0+ (Oreo, API 26+) até Android 15 (API 35) com fallback seguro.
   * Implementar `classifyAndVibrate(offerPayout, distanceKm, isAutoRejected)`.
3. **Atualizar `AppNotificationListenerService.kt` e `MainActivity.kt`:**
   * Disparar a assinatura tátil exata assim que o Jarvis processar a notificação flutuante.
4. **Adicionar Controles Interativos no HUD:**
   * Permitir testar e sentir as assinaturas táteis no app.
   * Alternar o Modo Chuva com um toque no cockpit.
5. **Compilação e Verificação:**
   * Rodar `compile_applet` para assegurar build íntegro e sem regressões.
