# Documentação Geral do Projeto: Radar Delivery AI

Este documento contém todos os detalhes do projeto construído até o momento. Ele foi criado para que você possa repassar para outras IAs (ou desenvolvedores) entenderem exatamente como o aplicativo funciona, sua arquitetura, tecnologias e regras de negócio. Nada foi deixado de fora.

## 1. Visão Geral do Projeto
O **Radar Delivery AI** é um aplicativo voltado para entregadores (motoboys), projetado para aumentar a segurança e a rentabilidade. O sistema intercepta (lê) ofertas de aplicativos de entrega (como iFood e Rappi), calcula se a corrida vale a pena com base em configurações definidas pelo motoboy (Km mínimo, valor mínimo, etc.) e utiliza inteligência artificial para tomar decisões.

Para evitar acidentes, o app possui uma **trava de segurança por velocidade**: se o entregador estiver em movimento rápido, a tela é bloqueada e a IA (Agente Jarvis) assume por voz.

## 2. O Agente "Jarvis" (Assistente de Voz)
- **Como foi integrado e funciona sem API extra de voz:** O Jarvis não usa uma API paga externa para falar ou ouvir. Ele foi integrado nativamente usando o **Text-To-Speech (TTS)** interno do Android (para falar) e a **Web Speech API / SpeechRecognizer** (para ouvir).
- **Inteligência (Ele aprende?):** O Jarvis em si utiliza o modelo de inteligência artificial **Google Gemini** para interpretar se uma corrida é boa ou ruim através de uma chamada de API (visão e raciocínio lógico). Ele "pensa" usando o Gemini e "fala/escuta" usando o celular. Ele avalia as regras, mas não "aprende" sozinho com o tempo a ponto de mudar sua própria programação (ele obedece estritamente os parâmetros do motoboy definidos no painel).

## 3. Arquitetura e Stack de Tecnologia

O projeto é dividido em três partes principais que se comunicam perfeitamente:

### A) Aplicativo Android (O Cliente)
- **Linguagem:** Kotlin.
- **Interface:** Jetpack Compose (Material Design 3).
- **Funcionalidades Nativas:**
  - `RadarCoordinatorService`: Serviço rodando em segundo plano (Foreground Service) que coleta localização, calcula a velocidade e invoca a Inteligência Artificial.
  - `VoiceManager` / `VoiceInputManager`: Controlam a fala e a escuta do Agente Jarvis usando recursos nativos do Android.
  - `RadarAccessibilityService`: Preparado para ler a tela dos apps de entrega (interceptar as corridas) e extrair os dados.

### B) Backend e Tempo Real (Firebase)
- **Aviso:** O Firebase **nunca sumiu**, ele está firme e forte! O projeto usa o **Firebase Firestore** para sincronizar tudo em tempo real.
- **Arquivo `firebase.js`:** Contém toda a configuração do banco de dados (Firestore) e autenticação.
- **O que ele guarda?** Perfil do motorista, Configurações de filtros de corridas (ex: R$ 2,00 por Km) e o Histórico de corridas ativas e recusadas.

### C) Painel Web do Motoboy (HTML/JS)
- **Arquivo `index.html`:** É o painel web responsivo (pode ser aberto no celular ou computador) onde o motoboy faz login, vê gráficos das corridas e altera suas configurações.
- **Sincronização Direta:** Quando uma regra é alterada no `index.html` (ex: aumentar o valor mínimo), o Firebase envia essa regra imediatamente para o app Android em segundo plano.

### D) Servidor Local de Testes
- **Arquivo `server.py`:** Um servidor Flask simples em Python. Ele serve os arquivos do Painel Web (HTML) e inclui rotas de simulação para o Gemini. No aplicativo real no Android, o Kotlin chama o Gemini diretamente ou processa internamente.

## 4. Estrutura de Diretórios Principal
- `app/src/main/java/com/example/` -> Código Kotlin do Android.
  - `MainActivity.kt`: A interface do aplicativo Android (Telas de Login, Dashboard, Configurações).
  - `service/RadarCoordinatorService.kt`: O "coração" do app. Monitora a velocidade, fala através do Jarvis e gerencia as corridas.
  - `coordinator/RadarCoordinator.kt`: Gerencia o estado global e a ponte com as preferências locais.
  - `data/FirestoreManager.kt`: Conecta o Android ao banco Firebase.
  - `data/FirebaseInitializer.kt`: Inicia o Firebase programaticamente para evitar erros de build.
  - `voice/VoiceManager.kt`: Configurações de TTS (fala em Português-BR).
  - `voice/VoiceInputManager.kt`: Configurações de reconhecimento de voz.
- `/index.html`: A Dashboard Web do Motoboy.
- `/firebase.js`: Módulo de integração web com Firebase.
- `/metadata.json` e `app/build.gradle.kts`: Configurações de construção e identidade visual do projeto.

## 5. Regras de Negócio e Fluxo de uma Corrida

1. **Captura:** O app intercepta uma nova oferta (ex: iFood toca).
2. **Avaliação da Velocidade:** O `RadarCoordinatorService` checa a velocidade GPS do motoqueiro.
3. **Decisão do Jarvis (IA):**
   - **Se o entregador estiver parado (< 10km/h):** O app envia uma notificação visual, o Jarvis avisa por áudio, e o motoqueiro pode aceitar clicando na tela ou por voz.
   - **Se o entregador estiver em movimento rápido (> 10km/h):** A tela é bloqueada (para ele não olhar pro celular). O Jarvis entra em ação: _"Nova corrida do iFood. R$ 15 para 5km. Diga ACEITAR ou RECUSAR."_
4. **Filtro Automático:** O Gemini avalia a corrida. Se o valor for abaixo do mínimo configurado, o Jarvis recusa automaticamente e avisa: _"Corrida recusada automaticamente por valor baixo."_
5. **Sincronização:** Se aceita ou rejeitada, a corrida é salva no Firebase e os gráficos do Painel Web (`index.html`) são atualizados na hora.

## 6. Como Continuar o Projeto (Para a próxima IA)

Para a próxima IA ou desenvolvedor dar prosseguimento ao projeto:
1. **O ambiente está completo.** As compilações do Gradle (`gradle :app:assembleDebug`) estão rodando sem erros.
2. **Firebase:** As credenciais e regras estão configuradas. Para ambiente de produção real, basta o desenvolvedor substituir as credenciais no painel "Secrets" pelas credenciais oficiais do Firebase do cliente.
3. **Acessibilidade (Leitura e Clique):** O `RadarAccessibilityService.kt` agora não apenas consegue ler a tela (fazer o *parse* da oferta e valores), como também possui um canal direto para receber o comando de clique automático do Jarvis e acionar fisicamente o botão "Aceitar" / "Confirmar" da interface (usando `AccessibilityNodeInfo.ACTION_CLICK`). Isso fecha o ciclo completo de "Leitura -> IA pensa -> IA clica".

Esta documentação foi gerada de forma independente pelo seu assistente para garantir total transparência e portabilidade do código fonte.
