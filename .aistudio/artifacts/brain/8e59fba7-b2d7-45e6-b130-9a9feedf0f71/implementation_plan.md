# Plano de Implementação: Resolução de Conflitos e Cancelamentos em Mesclagem Multi-Apps

## 1. Problema Operacional e Solução Arquitetural

Em operações com múltiplos aplicativos abertos simultaneamente, ocorrem dois cenários críticos de conflito:

```
CENÁRIO 1: RACE CONDITION (Outro motoboy aceitou primeiro)
+--------------------------------------------------------------------------+
|  1. App detecta sinergia iFood + 99 Food.                                |
|  2. Motoboy aceita o iFood (R$ 24,50).                                    |
|  3. Ao tentar aceitar o 99 Food, a oferta já expirou ou foi aceita.      |
|                                                                          |
|  >> PROTOCOLO DE MITIGAÇÃO:                                              |
|     - Desmonte atômico e silencioso da mesclagem (sem pânico).           |
|     - Preserva a corrida do iFood ativa e confirmada.                    |
|     - Recalcula a rota individual (remove os 800m de desvio da 99).      |
|     - Alerta de voz no fone: "Oferta 99 expirada. Mantendo apenas iFood  |
|       com rota recalculada de 3,5 km."                                   |
+--------------------------------------------------------------------------+

CENÁRIO 2: CANCELAMENTO EM TRÂNSITO (Cliente/Restaurante cancelou)
+--------------------------------------------------------------------------+
|  1. Motoboy está a caminho da coleta ou entrega das duas corridas.       |
|  2. O cliente do pedido B cancela a entrega.                             |
|                                                                          |
|  >> PROTOCOLO DE MITIGAÇÃO (COM CONFIRMAÇÃO MANUAL):                     |
|     - O sistema NÃO altera o GPS bruscamente sem aviso.                 |
|     - Dispara aviso sonoro de atenção imediato.                          |
|     - Abre MODAL DE CONFIRMAÇÃO TÁTIL GIGANTE na tela:                   |
|       "🚨 Cancelamento 99 Food detectado. Taxa de deslocamento: R$ 7,50"|
|       [ CONFIRMAR E RECALCULAR GPS ] vs [ MANTER ROTA ATUAL ]           |
|     - Ao confirmar, redireciona o GPS apenas para o destino remanescente.|
+--------------------------------------------------------------------------+
```

---

## 2. Componentes da Solução

### Componente 1: Motor de Resolução de Conflitos (`MultiAppConflictResolutionEngine.kt`)
- `handleOfferLostRaceCondition(lostOfferId, remainingOffer)`: Desfaz o agrupamento de pedidos, restaura as métricas individuais e aciona a síntese de voz informativa.
- `handleInTransitCancellation(cancelledOffer, activeOffer)`: Registra a perda, calcula a compensação financeira estimada (taxa de deslocamento) e suspende a alteração de rota até a confirmação manual do motoboy.

### Componente 2: Diálogo Interativo de Cancelamento (`InTransitCancellationDialog.kt`)
- Janela flutuante/modal de alta visibilidade e alto contraste.
- Exibe o app que cancelou, o valor estimado a reivindicar de taxa de deslocamento e o botão gigante para recalcular o destino no GPS.

### Componente 3: Painel de Simulação e Controle de Conflitos (`ConflictResolutionCard.kt`)
- Painel integrado no cockpit para que o entregador teste e entenda na prática como o app se comporta em cada situação.
- Botões de simulação:
  - "Simular Perda da 2ª Oferta (Outro Motoboy Aceitou)"
  - "Simular Cancelamento de Pedido em Trânsito"

---

## 3. Roteiro de Implementação

1. **`MultiAppConflictResolutionEngine.kt`**: Implementação das regras de desmonte de stack e compensação financeira.
2. **`InTransitCancellationDialog.kt`**: Modal interativo com botões táteis de confirmação antes de redirecionar o GPS.
3. **`ConflictResolutionCard.kt`**: Card de cockpit com telemetria de incidentes e simuladores práticos.
4. **Integração no `MainActivity.kt`**: Conexão aos eventos de cancelamento do `AppNotificationListenerService`.
5. **Compilação e Verificação**: Execução do `compile_applet`.
