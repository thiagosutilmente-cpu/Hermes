# Instruções Autônomas do Agente / Autonomous Agent Instructions

Este arquivo orienta os agentes de IA no Google AI Studio sobre como manter, desenvolver e auto-corrigir este aplicativo de forma 100% independente e segura, sabendo que o proprietário do projeto não possui conhecimentos em programação.

---

## 🎯 Diretriz Principal (Main Directive)
O usuário **não programa**. O agente de IA deve assumir total responsabilidade pelo ciclo completo de desenvolvimento:
1. **Iniciativa e Proatividade:** Identificar erros, analisar logs e propor/aplicar correções diretamente no código.
2. **Sem Bloqueios de Código:** Nunca pedir ao usuário para alterar código manualmente. O agente deve usar as ferramentas de edição (`edit_file`, `multi_edit_file`, `create_file`) para aplicar todas as mudanças.
3. **Autocorreção e Testes:** Sempre rodar `compile_applet` e validar com o linter após qualquer alteração para garantir que o aplicativo continue funcionando perfeitamente.

---

## 🛠️ Regras de Desenvolvimento (Development Rules)

### 1. Interface Web e Mobile (HTML/JS/CSS em `/index.html`)
- Este aplicativo possui uma rica interface baseada em web (`/index.html`) que serve como painel de controle e monitoramento de corridas (com mapas, ETA, gráficos e métricas).
- **Consistência de Design:** Mantenha a paleta de cores moderna (Slate escuro, azul `#3A86FF`, verde `#00F5D4` para sucesso, rosa `#FF006E` para alertas/acentos).
- **Experiência do Usuário (UX):** Todos os novos recursos visuais devem ser limpos, responsivos para dispositivos móveis e fáceis de usar por motoristas e motoboys.
- **Gráficos Recharts & React:** O componente de tendência de ganhos semanais usa React/Babel Standalone e Recharts dentro do próprio `index.html`. Mantenha a sincronização em tempo real do Firebase.

### 2. Integração com Firebase e Backend (`/firebase.js`, `/server.py`)
- O aplicativo utiliza sincronização em tempo real via Firebase Firestore para dados de corridas, ganhos e localização.
- Certifique-se de tratar falhas de rede de forma graciosa e manter o estado offline funcionando por meio de armazenamento local (`localStorage` ou Room/DB se aplicável) para evitar perda de dados.

### 3. Comunicação Clara e Direta (Communication Style)
- **Idioma:** Comunique-se em **Português** amigável, simples e livre de jargões técnicos complexos.
- **Transparência:** Explique o que foi alterado de forma funcional (ex: *"Adicionei um painel de tempo estimado de chegada (ETA) na corrida ativa"*), em vez de detalhar linhas de código modificadas.
- **Foco em Resultados:** Mostre que as alterações já foram aplicadas e testadas com sucesso.

---

## 🔄 Fluxo de Resolução de Problemas (Self-Healing Workflow)
Quando o usuário relatar um erro ou pedir uma nova funcionalidade:
1. **Analise o Estado Atual:** Leia os arquivos relevantes usando `view_file` ou busque referências com `grep`.
2. **Implemente a Solução de Ponta a Ponta:** Escreva código completo e maduro, sem marcadores de posição (`// TODO` ou `...`).
3. **Valide a Compilação:** Execute `compile_applet` e resolva eventuais erros de compilação imediatamente de forma autônoma.
4. **Entregue Funcionando:** Confirme o sucesso e explique as melhorias aplicadas.
