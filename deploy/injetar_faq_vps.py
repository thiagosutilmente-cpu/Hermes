#!/usr/bin/env python3
"""
Script de injeção direta do FAQ Tático no index.html da VPS.
Executa localmente na VPS sem depender de sincronização do Git.
"""
import os
import sys

target_files = [
    "/var/www/html/index.html",
    "/root/Hermes/deploy/index.html",
    "/root/Hermes/index.html",
    "/root/jarvis_backend/index.html"
]

faq_css = '''
    /* FAQ SECTION */
    .faq-section {
      margin-top: 60px;
      margin-bottom: 60px;
    }
    .faq-header {
      text-align: center;
      margin-bottom: 36px;
    }
    .faq-header h2 {
      font-size: 26px;
      font-weight: 800;
      color: #fff;
      margin-bottom: 8px;
    }
    .faq-header p {
      font-size: 14px;
      color: var(--text-muted);
      max-width: 600px;
      margin: 0 auto;
    }
    .faq-list {
      display: flex;
      flex-direction: column;
      gap: 14px;
      max-width: 860px;
      margin: 0 auto;
    }
    .faq-item {
      background: rgba(15, 23, 42, 0.7);
      border: 1px solid var(--border);
      border-radius: 14px;
      overflow: hidden;
      backdrop-filter: blur(12px);
      transition: all 0.3s ease;
    }
    .faq-item[open] {
      border-color: rgba(0, 255, 136, 0.4);
      box-shadow: 0 8px 24px rgba(0, 255, 136, 0.08);
      background: rgba(15, 23, 42, 0.85);
    }
    .faq-summary {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 18px 22px;
      cursor: pointer;
      user-select: none;
      list-style: none;
      font-size: 15px;
      font-weight: 700;
      color: #f1f5f9;
      gap: 12px;
      transition: color 0.2s;
    }
    .faq-summary::-webkit-details-marker { display: none; }
    .faq-summary:hover { color: var(--primary); }
    .faq-item[open] .faq-summary {
      color: var(--primary);
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    }
    .faq-title-wrap {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
    }
    .faq-badge {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 3px 9px;
      border-radius: 999px;
      font-size: 10.5px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      white-space: nowrap;
    }
    .faq-badge-trava {
      background: rgba(239, 68, 68, 0.15);
      color: #f87171;
      border: 1px solid rgba(239, 68, 68, 0.3);
    }
    .faq-badge-apk {
      background: rgba(59, 130, 246, 0.15);
      color: #60a5fa;
      border: 1px solid rgba(59, 130, 246, 0.3);
    }
    .faq-badge-jarvis {
      background: rgba(0, 255, 136, 0.15);
      color: var(--primary);
      border: 1px solid rgba(0, 255, 136, 0.3);
    }
    .faq-badge-plataforma {
      background: rgba(255, 183, 3, 0.15);
      color: var(--gold);
      border: 1px solid rgba(255, 183, 3, 0.3);
    }
    .faq-badge-compat {
      background: rgba(168, 85, 247, 0.15);
      color: #c084fc;
      border: 1px solid rgba(168, 85, 247, 0.3);
    }
    .faq-icon-chevron {
      font-size: 13px;
      color: var(--text-muted);
      transition: transform 0.3s ease;
      flex-shrink: 0;
    }
    .faq-item[open] .faq-icon-chevron {
      transform: rotate(180deg);
      color: var(--primary);
    }
    .faq-answer {
      padding: 18px 22px;
      font-size: 13.5px;
      line-height: 1.65;
      color: #cbd5e1;
    }
    .faq-answer strong { color: #fff; }
    .faq-highlight-box {
      margin-top: 12px;
      padding: 12px 14px;
      background: rgba(0, 210, 255, 0.06);
      border-left: 3px solid var(--cyan);
      border-radius: 6px;
      font-size: 12.5px;
      color: #94a3b8;
    }
    .faq-highlight-box strong { color: var(--cyan); }
'''

faq_html = '''
    <!-- SEÇÃO DE PERGUNTAS FREQUENTES (FAQ TÁTICO) -->
    <section id="faq" class="faq-section">
      <div class="faq-header">
        <div class="status-badge" style="margin-bottom: 12px;">
          <span>❓ RESPOSTAS TÉCNICAS E SEGURANÇA</span>
        </div>
        <h2>Perguntas Frequentes dos Pilotos</h2>
        <p>Tudo o que você precisa saber sobre a trava de segurança viária, distribuição do APK e o cérebro Jarvis Neural.</p>
      </div>

      <div class="faq-list">
        
        <!-- FAQ 1: TRAVA DE VELOCIDADE -->
        <details class="faq-item" open>
          <summary class="faq-summary">
            <div class="faq-title-wrap">
              <span class="faq-badge faq-badge-trava">🛡️ Segurança Viária</span>
              <span>Como funciona a Trava de Velocidade (>10 km/h) e por que ela bloqueia toques?</span>
            </div>
            <span class="faq-icon-chevron">▼</span>
          </summary>
          <div class="faq-answer">
            A <strong>Trava de Velocidade</strong> é um recurso vital de proteção à vida do motociclista. Utilizando a telemetria do <em>FusedLocationProvider</em> nativo do Android com precisão por GPS, o app monitora a sua velocidade em tempo real.
            <br><br>
            Ao ultrapassar <strong>10 km/h</strong>, o Radar Coordinator bloqueia automaticamente toques manuais na interface flutuante para que você <strong>nunca tire as mãos do guidão</strong> nem divida sua atenção na pista.
            <div class="faq-highlight-box">
              🎙️ <strong>Piloto Automático por Voz:</strong> Enquanto a trava de toque está ativa, o <strong>Jarvis Neural</strong> assume o comando sonoro e dita no fone Bluetooth ou intercomunicador do capacete: <em>"iFood: 4.8 km por R$ 19,50. Rendimento R$ 4,06 por quilômetro. Corrida Excelente!"</em>. Ao parar no semáforo (0 a 10 km/h), os comandos de toque se desbloqueiam instantaneamente.
            </div>
          </div>
        </details>

        <!-- FAQ 2: SEGURANÇA DO APP FORA DA PLAY STORE -->
        <details class="faq-item">
          <summary class="faq-summary">
            <div class="faq-title-wrap">
              <span class="faq-badge faq-badge-apk">📦 Instalação Direta</span>
              <span>É seguro instalar o APK fora da Play Store? Por que o app não está na Google Play?</span>
            </div>
            <span class="faq-icon-chevron">▼</span>
          </summary>
          <div class="faq-answer">
            <strong>Sim, 100% seguro, auditável e livre de malwares.</strong>
            <br><br>
            A Google Play Store impõe políticas que restringem fortemente aplicativos de terceiros que utilizam o <code>AccessibilityService</code> (Acessibilidade) para ler dados operacionais de apps concorrentes de mobilidade (iFood, Rappi, Uber e 99). Para manter a ferramenta <strong>100% funcional, independente e sem cortes de recursos</strong>, nós disponibilizamos o APK oficial com compilação direta do nosso servidor VPS Hostinger.
            <div class="faq-highlight-box">
              🔒 <strong>Sandbox e Privacidade:</strong> O Radar Coordinator roda dentro da sandbox de segurança padrão do Android, não exige root, não acessa contas bancárias, fotos ou conversas privadas. O instalador é assinado com certificado criptográfico oficial e verificado por hash SHA-256.
            </div>
          </div>
        </details>

        <!-- FAQ 3: INTEGRAÇÃO COM O JARVIS NEURAL -->
        <details class="faq-item">
          <summary class="faq-summary">
            <div class="faq-title-wrap">
              <span class="faq-badge faq-badge-jarvis">🧠 Jarvis Neural</span>
              <span>Como o Jarvis Neural se integra e faz a leitura instantânea das corridas?</span>
            </div>
            <span class="faq-icon-chevron">▼</span>
          </summary>
          <div class="faq-answer">
            O <strong>Jarvis Neural</strong> opera através de um pipeline de alta velocidade em C++ / Kotlin acoplado à árvore de acessibilidade do Android. Com um tempo de debounce de apenas <strong>300 milissegundos</strong>, ele captura o frame da notificação ou card de oferta no exato milissegundo em que o iFood, 99, Uber ou Rappi tocam na tela.
            <br><br>
            O algoritmo de visão computacional e texto estruturado decodifica imediatamente:
            <ul style="margin: 8px 0 8px 20px; color: #cbd5e1; font-size: 13px;">
              <li>Distância de busca até o restaurante + distância até o cliente final;</li>
              <li>Valor bruto ofertado em reais (R$);</li>
              <li>Previsão de tempo de tráfego e desvio por km;</li>
              <li>Cálculo matemático: <strong>R$ dividido pelo km total</strong>, subtraindo custos operacionais de combustível e pneu.</li>
            </ul>
            Tudo isso é processado localmente no processador do seu aparelho sem atrasar ou engasgar o app de entregas.
          </div>
        </details>

        <!-- FAQ 4: RISCO DE BLOQUEIO / BANIMENTO -->
        <details class="faq-item">
          <summary class="faq-summary">
            <div class="faq-title-wrap">
              <span class="faq-badge faq-badge-plataforma">🛡️ Antiban</span>
              <span>Existe risco de bloqueio ou advertência nas plataformas de entrega?</span>
            </div>
            <span class="faq-icon-chevron">▼</span>
          </summary>
          <div class="faq-answer">
            <strong>Nenhum risco de banimento.</strong> O Radar Coordinator opera estritamente como um <strong>assistente de acessibilidade e sobreposição visual (Overlay flutuante)</strong>, exatamente da mesma forma que os balões de rota do Waze ou Google Maps operam sobre outros apps.
            <br><br>
            O app <strong>não injeta código malicioso</strong>, não intercepta chaves criptográficas de API nem falsifica localização (sem Fake GPS). Para as plataformas, o entregador está apenas com um utilitário de leitura e voz ativado no Android.
          </div>
        </details>

        <!-- FAQ 5: OTIMIZAÇÃO DE BATERIA E XIAOMI/SAMSUNG -->
        <details class="faq-item">
          <summary class="faq-summary">
            <div class="faq-title-wrap">
              <span class="faq-badge faq-badge-compat">⚡ Bateria & Sistema</span>
              <span>Funciona em aparelhos Xiaomi, Samsung e Motorola sem descarregar a bateria?</span>
            </div>
            <span class="faq-icon-chevron">▼</span>
          </summary>
          <div class="faq-answer">
            O aplicativo foi projetado para consumo ultra-baixo de energia (menos de <strong>3% de bateria em uma jornada completa de 8 horas</strong>).
            <br><br>
            Em celulares <strong>Xiaomi (MIUI / HyperOS)</strong> e <strong>Samsung (One UI)</strong>, basta conceder permissão de <em>Início Automático</em> e marcar a bateria como <em>"Sem restrições"</em>. Assim, o sistema operacional nunca suspende o Jarvis Neural em segundo plano enquanto você pilota entre corridas.
          </div>
        </details>

      </div>
    </section>
'''

updated_count = 0
for file_path in target_files:
    if os.path.exists(file_path):
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        # Evita duplicar se já existir
        if "id=\"faq\"" in content or "faq-section" in content:
            print(f"ℹ️ {file_path} já contém a seção FAQ.")
            updated_count += 1
            continue

        # 1. Injetar link no navbar
        if '<a href="#depoimentos" class="nav-link">Prova Social</a>' in content:
            content = content.replace(
                '<a href="#depoimentos" class="nav-link">Prova Social</a>',
                '<a href="#depoimentos" class="nav-link">Prova Social</a>\n      <a href="#faq" class="nav-link">Dúvidas Frequentes</a>'
            )

        # 2. Injetar CSS
        if '</style>' in content:
            content = content.replace('</style>', f'{faq_css}\n  </style>', 1)

        # 3. Injetar HTML antes do footer
        if '<footer class="footer">' in content:
            content = content.replace('<footer class="footer">', f'{faq_html}\n    <!-- RODAPÉ LEGAL -->\n    <footer class="footer">', 1)
        elif '</main>' in content:
            content = content.replace('</main>', f'{faq_html}\n  </main>', 1)

        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)

        print(f"✅ {file_path} atualizado com sucesso!")
        updated_count += 1

print(f"🎉 Finalizado: {updated_count} arquivo(s) processados.")
