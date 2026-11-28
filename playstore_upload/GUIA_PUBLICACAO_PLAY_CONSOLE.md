# Guia Completo de Publicação — Google Play Console (Radar Coordinator)

Este guia reúne tudo o que você precisa copiar, colar e enviar no **Google Play Console** para aprovar o **Radar Coordinator** (`com.aistudio.radardelivery.jkxwpa` • `v3.1.0` • `versionCode 30`).

---

## 1. Arquivos Prontos para Upload

| Item | Onde está no projeto | Especificação |
|---|---|---|
| **Pacote do App (AAB)** | `app/build/outputs/bundle/release/app-release.aab` *(ou gere via menu Configurações > Exportar AAB no AI Studio)* | Android App Bundle v3.1.0 (`targetSdk 36`, `versionCode 30`) |
| **Ícone da Loja** | `playstore_upload/app_icon_512x512.png` | `512 × 512 px` PNG RGBA |
| **Imagem em Destaque (Banner)** | `playstore_upload/feature_graphic_1024x500.png` | `1024 × 500 px` PNG 24-bit |
| **Capturas de Tela (Screenshots)** | *Capturar 2 a 8 telas no celular* | Proporção 16:9 ou 9:16 (mín. 320px, máx. 3840px) |

---

## 2. Ficha Principal da Loja (Presença na Loja)

- **Nome do App (27 / 30 caracteres):**
  ```text
  Radar Coordinator: Entregas
  ```
- **Descrição Breve (80 / 80 caracteres):**
  ```text
  Copiloto e calculadora de lucro por km em tempo real para entregadores e motoboys.
  ```
- **Descrição Completa:**
  ```text
  O Radar Coordinator é o copiloto inteligente projetado para entregadores, motoboys e motoristas de aplicativo que precisam de agilidade, rentabilidade e segurança no trânsito.

  Chega de perder tempo ou arriscar sua atenção tentando calcular de cabeça se uma corrida vale a pena enquanto pilota!

  PRINCIPAIS RECURSOS:

  • MESCLAGENS DE ENTREGA E ROTAS COMPLEMENTARES:
  Analise em tempo real se uma nova entrega combina com a sua rota atual (desvio em km, tempo extra e ganho adicional) para multiplicar seus ganhos sem sair do caminho.

  • CÁLCULO INSTANTÂNEO DE LUCRO POR KM:
  Assim que uma notificação de entrega chega, o Radar Coordinator calcula instantaneamente o ganho por quilômetro rodado (R$/km) e o lucro líquido estimado, mostrando se a corrida compensa.

  • COPILOTO POR VOZ (MÃOS LIVRES):
  Ouça o valor da corrida, destino e taxa de ganho diretamente no fone ou comunicador de capacete sem precisar desviar o olhar da pista.

  • HUD FLUTUANTE DISCRETO (CONTROLE DO MOTORISTA):
  Painel compacto que exibe o resumo da oferta na tela, mantendo sempre a decisão final de aceitar ou recusar nas mãos do entregador.

  • RADAR DE SEGURANÇA VIÁRIA:
  Alertas preventivos de zonas de atenção, velocidade e regiões de maior demanda durante o turno ativo de trabalho.

  • HISTÓRICO E GESTÃO FINANCEIRA:
  Acompanhe seu faturamento diário, meta do dia, distância percorrida e custo estimado de combustível da moto.

  PRIVACIDADE E TRANSPARÊNCIA:
  O Radar Coordinator não vende suas informações nem compartilha seu histórico de rotas com terceiros.
  ```
- **Categoria:** Produtividade *ou* Mapas e navegação
- **E-mail de Contato:** `thiagosutilmente@gmail.com`

---

## 3. Produtos de Assinatura (Monetizar > Produtos > Assinaturas)

O aplicativo está configurado em **Modo Play Store** (`IS_PLAY_STORE_COMPLIANT_MODE = true`), usando exclusivamente o **Google Play Billing** dentro do app. Crie exatamente estes 2 IDs de assinatura no Play Console antes de liberar para teste/produção:

1. **ID do Produto:** `radar_pro_mensal`
   - Plano base mensal (sugestão: R$ 99,90/mês ou teste grátis de 7 dias)
2. **ID do Produto:** `radar_pro_anual`
   - Plano base anual (sugestão: R$ 650,00/ano ou teste grátis de 7 dias)

---

## 4. Conteúdo do App & Declarações Obrigatórias

### A. Política de Privacidade
- Use a página `privacy.html` incluída no repositório.
- **Dica importante:** o Google Play exige que o link da Política de Privacidade abra publicamente via **HTTPS** (`https://...`). Antes de enviar para revisão, publique o arquivo `privacy.html` no seu domínio com SSL ou gratuitamente em HTTPS (ex.: GitHub Pages / Firebase Hosting).

### B. Acesso ao App
- Marque: **"Algumas funcionalidades têm acesso restrito"** *apenas se exigir login/assinatura*, ou forneça instruções de teste informando que os modos de avaliação/simulação do radar funcionam imediatamente ao abrir o app (sem conta obrigatória para navegar nas telas iniciais).

### C. Anúncios
- Marque: **Não, meu app não contém anúncios**.

### D. Classificação Indicativa & Público-Alvo
- Público-alvo: **18 anos ou mais** (profissionais de entrega / condutores).

### E. Segurança dos Dados (Data Safety)
- **Coleta ou compartilha dados de usuário?** Marque **Sim** (coleta para funcionamento do app).
- **Todos os dados são criptografados em trânsito?**
  - **ATENÇÃO:** só marque **Sim** depois que sua VPS estiver respondendo em `https://...` e o `AndroidManifest.xml` estiver com `usesCleartextTraffic="false"`. Enquanto houver tráfego `http://`, marque **Não** para evitar reprovação automática na checagem de rede.
- **Os usuários podem solicitar a exclusão dos dados?** Marque **Sim** (solicitação via `thiagosutilmente@gmail.com`).
- **Tipos de dados coletados (uso exclusivo no app, sem compartilhamento para terceiros):**
  - **Localização (aproximada e precisa):** Finalidade *Funcionalidade do app* (cálculo de distância/velocidade e zonas de demanda durante turno ativo).
  - **Informações pessoais (nome/e-mail/telefone — opcional):** Apenas se o usuário criar conta/sincronizar licença.
  - **Desempenho do app (registros de falhas):** Finalidade *Análise/Diagnóstico*.

### F. Justificativas de Permissões e Serviços em Primeiro Plano
O projeto **não** usa `AccessibilityService`, **não** usa `ACCESS_BACKGROUND_LOCATION` e **não** usa `REQUEST_INSTALL_PACKAGES`. Caso o Console peça explicação para os itens abaixo, cole os textos:

1. **Serviços em Primeiro Plano (`location`, `microphone`, `dataSync`):**
   > O aplicativo executa um serviço em primeiro plano iniciado explicitamente pelo entregador ao ligar seu turno de trabalho ("Iniciar Turno"), exibindo notificação persistente contínua. A localização em primeiro plano calcula distância até a coleta, velocidade atual e proximidade de polos de demanda; o microfone processa comandos de voz mãos-livres solicitados pelo motorista; e a sincronização atualiza a telemetria do turno ativo.
2. **Acesso às Notificações (`NotificationListenerService`):**
   > Permissão ativada opcionalmente pelo usuário para ler notificações de novas corridas recebidas de plataformas de entrega, calcular em tempo real o ganho líquido por quilômetro (R$/km) e anunciar o resumo por voz no capacete.
3. **Exibir sobre outros apps (`SYSTEM_ALERT_WINDOW`):**
   > Exibe um card flutuante compacto com o valor por km calculado e oportunidade de mesclagem sobre a tela, permitindo que o entregador decida aceitar ou recusar a corrida com segurança sem alternar entre aplicativos.

---

## 5. Passo a Passo Final de Envio (Teste Fechado -> Produção)

1. Suba o arquivo `.aab` na faixa **Teste fechado** (em contas pessoais criadas após nov/2023, o Google Play exige **12 testadores inscritos por 14 dias contínuos** antes de liberar Produção).
2. Configure os dois produtos de assinatura (`radar_pro_mensal` e `radar_pro_anual`) e publique as alterações da faixa de Teste Fechado.
3. Adicione os e-mails dos testadores (inclusive o seu para testar a compra real em modo sandbox sem cobrança) e envie o link de participação.
4. Após cumprir os 14 dias de teste fechado, clique em **Solicitar acesso à produção** e promova a versão validada.
