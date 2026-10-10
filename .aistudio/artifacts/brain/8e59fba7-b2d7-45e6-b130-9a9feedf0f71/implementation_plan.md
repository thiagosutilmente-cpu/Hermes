# Filtro Inteligente de Raio de Alcance de Restaurantes (Radar Delivery)

Este plano estabelece a adição do **Seletor de Raio de Alcance Máximo de Restaurantes** nas configurações do aplicativo (`FilterSettingsScreen.kt`) e a integração completa com o motor de filtragem em tempo real (`RealTimeOfferFilter.kt`).

---

## 1. Atualização do Motor de Filtro & Notificações (`RealTimeOfferFilter.kt`)
- Garantir que `matchesNotification(offer: RadarOffer)` também respeite `isGeoRadiusFilterActive` e `maxPickupRadiusKm`, ignorando notificações de restaurantes além do raio configurado.
- Atualizar a avaliação de atividade do filtro (`isActive`) para refletir o status do raio de alcance.

---

## 2. Interface no Painel de Configurações (`FilterSettingsScreen.kt`)
- **Novos Estados Reativos:**
  - `isGeoRadiusFilterActive`: toggle para ativar ou desativar o limite de distância de coleta.
  - `maxPickupRadiusKm`: valor numérico em km (padrão inicial calibrado em 10.0 km).
- **Card Dedicado de Raio de Coleta:**
  - Título: *"📍 RAIO DE ALCANCE MÁXIMO DE RESTAURANTES"*
  - Descrição: *"Filtro inteligente via GPS do piloto. O radar ignora automaticamente chamadas de restaurantes localizados além da distância configurada."*
  - **Switch Liga/Desliga:** Permite desativar temporariamente o filtro para corridas em áreas metropolitanas abertas.
  - **Chips de Acesso Rápido:** `3 km`, `5 km`, `10 km`, `15 km` e `20 km` com destaque neon/cyberpunk.
  - **Slider Contínuo:** Ajuste fino de 1.0 km a 25.0 km com botões de incremento/decremento unitário (`−` e `+`).
  - **Telemetria de Cobertura:** Exibição da área de cobertura em km² e estimativa de tempo de deslocamento até a coleta.
- **Persistência ao Salvar:**
  - Repassar `isGeoRadiusFilterActive` e `maxPickupRadiusKm` para o callback `onSaveCriteria` e persistir via `FilterPreferencesManager`.

---

## 3. Validação e Compilação
- Compilar o app via `compile_applet`.
- Atualizar o build do APK.
- Verificar a integridade das tags de teste (`switch_geo_radius_filter`, `slider_max_pickup_radius`, `chip_radius_3km`, etc.).
