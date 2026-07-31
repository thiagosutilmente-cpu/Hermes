# 🗃️ PROJETO RADAR COORDINATOR - ESPECIFICAÇÃO DE ENGENHARIA DO SISTEMA
## DOCUMENTO DE SPREAD & ANÁLISE COMPLETA (BLACK BOX ENGINE)

Este documento descreve detalhadamente a arquitetura do sistema, modelos de dados, componentes nativos do Android, lógica de controle por voz, e os algoritmos preditivos de otimização de rotas para entregadores multi-app. Foi projetado para ser 100% legível por Modelos de Linguagem de IA (LLMs) para permitir análise, expansão e depuração imediata sem perda de contexto.

---

## 1. 🏗️ MAPA ARQUITETURAL & FLUXO DE DADOS REATIVO

O sistema funciona em uma **Tríade Reativa Bidirecional** que interliga o dispositivo Android físico à nuvem e a um painel de controle Web de forma assíncrona.

```
       [ Painel Web: index.html ] (React Standalone / Recharts)
                  ▲
                  │  Sincronização de Estados / Sliders (Leitura/Escrita)
                  ▼
   [ Firebase Firestore (Realtime DB) ]
                  ▲
                  │  Telemetria / Pings / Estado de Voz / Resultados (Leitura/Escrita)
                  ▼
 [ Aplicativo Android Nativo (Kotlin) ]
       ├─ RadarCoordinatorService.kt (Orquestrador)
       ├─ RadarAccessibilityService.kt (Leitura de Telas / OCR / Cliques)
       └─ VoiceInputManager.kt (Controle de Voz Jarvis Passivo)
```

### Camadas do Sistema:
1. **Frontend / Painel Web (`index.html`):** Uma interface visual de alta fidelidade rodando em React, contendo widgets de telemetria, mapas interativos, e sliders de ajuste fino para as regras de negócio. Sincroniza diretamente com o Firestore.
2. **Banco de Dados Cloud (Firebase Firestore):** Atua como o barramento de sincronização global e centralizador de dados entre o painel e o APK nativo.
3. **Serviços Nativos Android (Kotlin/Compose):** Serviços de acessibilidade e segundo plano que rodam no celular do entregador, capturando telas, simulando cliques e processando comandos por voz locais via APIs locais.

---

## 2. ⚙️ SCHEMA DO FIRESTORE (SINCRONIZAÇÃO OBRIGATÓRIA)

Os dados no documento de configurações do motorista (`RadarSettings`) no Firestore obedecem estritamente à seguinte estrutura de dados:

| Campo no Firestore | Tipo Kotlin | Tipo JS (index.html) | Descrição do Parâmetro |
| :--- | :--- | :--- | :--- |
| `chainDeliveriesMode` | `Boolean` | `boolean` | Ativação do modo de entregas encadeadas |
| `isGhostSequenceEnabled` | `Boolean` | `boolean` | Liga/Desliga a IA de Roteamento Sequência Fantasma |
| `ghostSequenceAggressiveness`| `String` | `string` | Nível: `"CONSERVADOR"`, `"EQUILIBRADO"`, `"AGRESSIVO"` |
| `ghostSequenceTrafficWeight` | `Double` | `number` (0 a 1) | Peso do tráfego na rota |
| `ghostSequenceLatencyWeight` | `Double` | `number` (0 a 1) | Peso da latência na rota |
| `systemHealthScore` | `Int` | `number` (0-100) | Pontuação de integridade calculada pelo Android |
| `activeAnomalies` | `List<String>` | `array` | Lista de problemas pendentes (ex: `GPS_OFFLINE`, `GHOST_IDLE`) |
| `voiceOnlyMode` | `Boolean` | `boolean` | Ativação do modo Jarvis interativo |
| `jarvisVoiceState` | `String` | `string` | Estado de voz: `IDLE`, `LISTENING_WAKEWORD`, `LISTENING_COMMAND`, `PROCESSING`, `SPEAKING` |
| `jarvisRecognizedText` | `String` | `string` | Último texto decodificado do comando do motorista |

---

## 3. 🛡️ SERVIÇOS NATIVOS DO ANDROID

### A. `RadarCoordinatorService.kt`
- **Função:** Central de controle em segundo plano. Coordena o fluxo de ativação dos serviços, responde a eventos do sistema, gerencia o ciclo de vida da localização por satélite e calcula a telemetria que alimenta o Firestore.
- **Intercepção de Eventos:** Filtra os pacotes de ofertas recebidos e despacha notificações para a tela do motorista usando injeções personalizadas.

### B. `RadarAccessibilityService.kt`
- **Função:** Motor de automação por interface gráfica. Lê os textos exibidos na tela de aplicativos de entrega de terceiros (Uber, iFood, Rappi) usando a API de Acessibilidade do Android.
- **OCR Espacial & Cliques Semânticos:** Varre a árvore de nós visuais para extrair informações críticas, como valores de tarifas (R$), distâncias (km) e endereços de coleta/entrega. Permite realizar cliques automáticos simulando toques físicos em locais específicos com base na análise espacial de elementos.
- **Suporte a Split-Screen:** Monitora se o motorista está com duas telas abertas simultaneamente, ajustando as coordenadas de clique conforme a proporção de cada janela.

### C. `VoiceInputManager.kt` & `VoiceManager.kt`
- **Escuta Passiva Ativa (Modo Jarvis):** Utiliza reconhecimento de voz nativo do Android para detectar palavras de ativação ("Jarvis" ou "Ok Google").
- **Fila de TTS (Text-To-Speech):** Gerencia a fala do Jarvis de forma limpa. Quando o Jarvis fala uma nova oferta (`isOfferAnnouncement`), o sistema ajusta `shouldAutoWakeOnTtsFinish = true` para que, assim que ele termine de falar, o reconhecimento de voz reative automaticamente, permitindo que o motorista responda "aceitar" ou "recusar" sem tocar na tela.

---

## 4. ⚡ ALGORITMOS E LOGÍSTICAS DE DECISÃO

### A. IA Ghost Sequence (Otimização de Custos)
A fórmula matemática proprietária para o cálculo do custo de viagem ajustado por tráfego, agressividade e latência é:

$$	ext{Custo} = 	ext{Distância} 	imes \left(1.0 + (	ext{FatorTráfego} 	imes 	ext{FatorAgressividade} 	imes 	ext{ghostSequenceTrafficWeight}) + (	ext{ghostSequenceLatencyWeight} 	imes 0.2)ight)$$

Onde:
- **Distância:** Em quilômetros (km).
- **FatorTráfego:** Calculado em tempo real com base nos congestionamentos locais (0.0 a 1.0).
- **FatorAgressividade:** Mapeado conforme o perfil de direção configurado:
  - `"CONSERVADOR"` = 0.5
  - `"EQUILIBRADO"` = 1.0
  - `"AGRESSIVO"` = 1.8

### B. Monitoramento de Integridade (System Health Pulse)
O serviço Android roda um loop em segundo plano a cada **30 segundos** avaliando a saúde geral do app:
- **Pontuação Inicial:** 100 pontos.
- **Reduções de Saúde:**
  - `-30 pts` se o sinal de GPS for nulo (`GPS_OFFLINE`).
  - `-15 pts` se a localização estiver sem atualizar por mais de 60 segundos (`GPS_STALE`).
  - `-10 pts` se o atraso de tráfego atual for superior a 15 minutos (`TRAFFIC_CONGESTION`).
  - `-5 pts` se a IA Ghost Sequence estiver ativada mas não houver rotas a otimizar (`GHOST_IDLE`).
As anomalias detectadas são empurradas ao Firestore instantaneamente para exibição no Dashboard Web.

---

## 5. 🖥️ MULTIPLEXAÇÃO NEURAL (4 TELAS SIMULTÂNEAS)

Para permitir a simulação de multi-aplicativos em paralelo, o frontend (`index.html`) e o aplicativo Android suportam o modo **Multiplexação**. No painel Web, a tela se divide em uma grade 2x2 contendo 4 instâncias virtuais emuladas em tempo real:

1. **VM_01: UBER_DRV (Canal Azul)** - Monitora chamadas e hooks da Uber.
2. **VM_02: IFOOD_LOG (Canal Vermelho)** - Intercepta conexões de restaurantes do iFood.
3. **VM_03: G_MAPS_SYS (Canal Verde)** - Fornece telemetria de trânsito em tempo real via satélite.
4. **VM_04: RAPPI_LOG (Canal Amarelo)** - Simula scraping de supermercados em segundo plano.

Esta matriz é totalmente integrada às configurações de sincronização do Firestore, de modo que alterações de estado se refletem simultaneamente nas visualizações nativas Android e na UI Web.

---

## 6. 📝 PROMPTS DE INGESTÃO PARA OUTRA IA

Caso queira que outra inteligência artificial analise, expanda ou corrija o projeto, copie e envie o seguinte prompt padrão:

> **PROMPT DE INGESTÃO DO PROJETO:**
> "Olá! Você está recebendo o arquivo de especificações completas do 'Projeto Radar Coordinator'. Este é um sistema inteligente de alta performance para entregadores multi-app que interliga um painel Web em React, um banco de dados em tempo real no Firebase Firestore e um aplicativo nativo em Kotlin/Android de forma reativa.
> 
> **Suas Diretrizes:**
> 1. Respeite as regras de Caixa Preta estabelecidas em `AGENTS.md`. Toda alteração numérica ou de tipo vinda do Firestore precisa de cast defensivo (`as? Number` em Kotlin, e checagens seguras em JS).
> 2. Mantenha a sincronização bidirecional de estados de voz (como `jarvisVoiceState`) e parâmetros de Roteamento Fantasma (`ghostSequenceTrafficWeight`, etc.).
> 3. Analise a arquitetura de acessibilidade e o controle por voz com auto-wake ativo ao final de notificações de voz.
> 
> Como você proporia otimizar a lógica de cálculo de rotas e o OCR semântico do aplicativo?"

---
