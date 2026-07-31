# Instruções Autônomas do Agente / Autonomous Agent Instructions
# 🗃️ CAIXA PRETA & MEMÓRIA GERAL DO SISTEMA (BLACK BOX ENGINE)

Este arquivo orienta os agentes de IA no Google AI Studio sobre como manter, desenvolver e auto-corrigir este aplicativo de forma 100% independente e segura, sabendo que o proprietário do projeto não possui conhecimentos em programação. Ele serve como a **Caixa Preta do Sistema**, documentando todas as regras fundamentais, schemas e relações de sincronização para evitar bugs em atualizações futuras.

---

## 🎯 Diretriz Principal (Main Directive)
O usuário **não programa**. O agente de IA deve assumir total responsabilidade pelo ciclo completo de desenvolvimento:
1. **Iniciativa e Proatividade:** Identificar erros, analisar logs e propor/aplicar correções diretamente no código.
2. **Sem Bloqueios de Código:** Nunca pedir ao usuário para alterar código manualmente. O agente deve usar as ferramentas de edição (`edit_file`, `multi_edit_file`, `create_file`) para aplicar todas as mudanças.
3. **Autocorreção e Testes:** Sempre rodar `compile_applet` e validar com o linter após qualquer alteração para garantir que o aplicativo continue funcionando perfeitamente.

---

## 🏗️ Mapa de Arquitetura e Relação de Arquivos (Architecture Map)

O sistema opera de forma reativa e bidirecional através de **três camadas críticas**:
1. **Frontend / Painel Web (`index.html`):** Roda em React/Babel Standalone, oferecendo dashboards e widgets de controle (ex: ajuste fino da Ghost Sequence e gráficos Recharts em tempo real). Ele lê/escreve as configurações no Firestore.
2. **Banco de Dados em Tempo Real (Firebase Firestore):** Sincroniza em tempo real as coordenadas, ofertas aceitas, estado dos apps parceiros e os parâmetros finos salvos pelo motorista.
3. **Serviço Android Nativo (`RadarCoordinatorService.kt`, `FirestoreManager.kt`, `GhostRouteOptimizer.kt`):** Coleta localização via GPS de alta precisão, processa o cálculo de rotas usando fatores pesados de tráfego, latência e reordena as entregas pendentes em tempo de execução.

### 📁 Principais Arquivos e suas Funções:
- `/index.html`: Interface visual rica, monitoramento de mapa, sliders de ajuste fino e painel de telemetria em tempo real (Recharts).
- `/app/src/main/java/com/example/coordinator/RadarCoordinator.kt`: Centralizador de estados de decisão de rotas, telemetria de tráfego e responsável por rodar o diagnóstico de saúde do sistema (`performDiagnostic`).
- `/app/src/main/java/com/example/data/FirestoreManager.kt`: Gerenciador de sincronização do banco de dados Firebase. Sempre que um campo for adicionado ou alterado, ele DEVE ser mapeado aqui nos métodos de leitura (`as? Double`, `as? Number`) e escrita (`"campo" to valor`).
- `/app/src/main/java/com/example/util/GhostRouteOptimizer.kt`: Algoritmo preditivo de otimização de rotas que reduz o custo de viagem ponderando tráfego, agressividade e pesos finos (`trafficWeight`, `latencyWeight`).

---

## ⚙️ Schema Técnico do Firebase (Sincronização Obrigatória)

Ao ler ou atualizar as configurações (`RadarSettings`), os seguintes campos do Firestore **precisam ser mapeados de forma estrita em todos os arquivos sincronizados**:

| Campo no Firestore | Tipo Kotlin | Tipo JS (index.html) | Descrição do Parâmetro |
| :--- | :--- | :--- | :--- |
| `chainDeliveriesMode` | `Boolean` | `boolean` | Ativação do modo de entregas encadeadas |
| `isGhostSequenceEnabled`| `Boolean` | `boolean` | Ligar/desligar IA Ghost Sequence |
| `ghostSequenceAggressiveness`| `String` | `string` | Nível: `"CONSERVADOR"`, `"EQUILIBRADO"`, `"AGRESSIVO"` |
| `ghostSequenceTrafficWeight` | `Double` | `number` (0 a 1) | Peso do fator tráfego no custo da rota (Ajuste fino) |
| `ghostSequenceLatencyWeight` | `Double` | `number` (0 a 1) | Peso de reatividade em milissegundos (Ajuste fino) |
| `systemHealthScore` | `Int` | `number` (0-100) | Pontuação de integridade calculada pelo sistema Android |
| `activeAnomalies` | `List<String>` | `array` | Lista de problemas pendentes (ex: `GPS_OFFLINE`, `GHOST_IDLE`) |
| `voiceOnlyMode` | `Boolean`| `boolean` | Modo Jarvis interativo de escuta passiva |

---

## ⚡ Regras de Negócio e Lógica Imutável

### 1. IA Ghost Sequence (Roteamento Inteligente)
- **Custo da Rota:** O custo para percorrer uma rota é calculado como: `distância * (1.0 + (fator_trafego * fator_agressividade * trafficWeight) + (latencyWeight * 0.2))`.
- **Valores Padrão:** Tráfego padrão = 50% (`0.5`), Latência padrão = 30% (`0.3`).
- **Conservador:** Menor peso a desvios agressivos.
- **Agressivo:** Procura atalhos complexos para evitar trânsito mesmo que aumente a latência de rota local.

### 2. Monitoramento Ativo (System Health Pulse)
- Executa a cada **30 segundos** de forma contínua em segundo plano no Android (`RadarCoordinator.kt`).
- Reduz a pontuação de saúde do APK sob condições anômalas:
  - `-30 pts` se o GPS for nulo (`GPS_OFFLINE`).
  - `-15 pts` se a localização estiver sem atualização há mais de 1 minuto (`GPS_STALE`).
  - `-10 pts` se o atraso de trânsito atual for maior que 15 minutos (`TRAFFIC_CONGESTION`).
  - `-5 pts` se o Ghost estiver habilitado mas sem paradas ativas para calcular (`GHOST_IDLE`).
- Se houver anomalias, elas são empurradas ao Firestore para exibição em tempo real no dashboard do motorista.

---

## 🔄 Fluxo de Desenvolvimento Seguro (Self-Healing Workflow)

Quando for solicitado qualquer ajuste ou novas frentes no código:
1. **Validação de Tipos de Dados:** Lembre-se que campos numéricos vindos do Firestore podem ser instanciados como `Long`, `Double` ou `Float` dependendo do JSON serializado. No Kotlin, sempre faça cast defensivo usando `(data["campo"] as? Number)?.toDouble() ?: padrao` ou `?.toInt()`. Isso evita a maior causa de travamentos (ClassCastExceptions) no APK final.
2. **Código Sem Pontas Soltas:** É estritamente proibido inserir comentários com `// TODO` ou trechos de código incompletos. Se editar um método, certifique-se de fechar todos os colchetes, declarar as importações corretas e manter a estabilidade original de outras variáveis não relacionadas.
3. **Prevenção de Loops de Interface:** No frontend (`index.html`), garanta que os listeners de mudança (como `oninput` ou sliders) evitem disparos infinitos redundantes para o Firestore. Use sincronização baseada em estados reais locais antes de subir as atualizações.
4. **Verificação Obrigatória:**
   - Execute `compile_applet` para assegurar que a compilação esteja 100% livre de erros de sintaxe ou referências perdidas antes de dar a tarefa por concluída.

