# Pesquisa Profunda: Aplicativos de Mobilidade e Entrega (iFood, Uber, 99) — Cenário 2026
## Guia de Engenharia de Heurísticas, Extração de Dados e Otimização de Lucro para Motoboys

Este documento apresenta um estudo aprofundado sobre os padrões de interface, comportamento e layout das telas de oferta dos principais aplicativos de transporte e delivery no Brasil (**iFood**, **Uber** e **99**) em **2026**. Ele serve de base técnica para a inteligência de acessibilidade (scrapes e extração via árvore de nós do Android) e para a otimização matemática do lucro de entregadores e motoboys.

---

## 1. Padrões de Telas e Campos Exibidos em 2026

Os aplicativos de entrega e transporte operam com estruturas de interface dinâmicas em 2026, projetadas para induzir decisões rápidas (geralmente com temporizadores regressivos de 10 a 20 segundos). Abaixo estão detalhados os elementos textuais e estruturais de cada plataforma que o serviço de acessibilidade do Radar monitora e processa.

### A. iFood para Entregadores (Delivery de Comida, Mercado e Farmácia)
O aplicativo do iFood utiliza um painel inferior (*Bottom Sheet*) deslizante ou cartões de rota sobrepostos em tela cheia na cor branca ou cinza-escura (modo noturno).

*   **Identificadores de Marca:** Presença do pacote `com.ifood.driver` e strings como *"Nova rota disponível!"*, *"iFood"*, ou *"iFood para Entregadores"*.
*   **Tarifa / Ganhos:** Exibido em destaque com tamanho de fonte elevado (ex: `R$ 15,40`), normalmente no topo do cartão de oferta.
*   **Dados de Coleta (Pickup):**
    *   Sinalizado pelas palavras-chave *"Coleta"*, *"Origem"* ou *"Retirada"*.
    *   Exibe o nome do estabelecimento parceiro (ex: *"McDonald's - Pinheiros"*, *"Pão de Açúcar"*) e o endereço simplificado do estabelecimento.
*   **Dados de Entrega (Delivery):**
    *   Sinalizado pelas palavras-chave *"Entrega"*, *"Destino"* ou *"Cliente"*.
    *   Exibe o bairro ou a rua aproximada do cliente (ex: *"Rua dos Pinheiros"*, *"Consolação"*). O número exato da residência e o apartamento só são liberados após o aceite.
*   **Métricas de Distância e Tempo:**
    *   **Distância Total:** Exibida no formato `X,X km` ou `X km` (ex: `5,4 km` ou `8 km`). Refere-se à soma do trajeto até o restaurante e do restaurante até o cliente.
    *   **Tempo Estimado:** Exibido como `X min` ou `X minutos` (ex: `25 min`, `35 minutos`).
*   **Rotas Encadeadas (Duplas / Triplas):**
    *   O iFood agrupa ofertas frequentes em 2026. Telas exibem strings como *"Rota dupla"*, *"Grupo de entregas"*, *"Pedido A + B"* ou *"Duas entregas"*.
*   **Estrutura de Ação (Gestos/Cliques):**
    *   **Botão de Aceite:** Um botão vermelho largo contendo *"Aceitar Rota"* ou um botão deslizante (*Slide to Accept*).
    *   **Botão de Rejeição:** Um botão menor, normalmente posicionado no canto superior direito ou inferior esquerdo, rotulado como *"Recusar"*, *"Rejeitar"* ou um ícone de "X".

### B. Uber Driver (Uber Moto e Uber Flash)
A tela de ofertas da Uber se sobrepõe ao sistema operacional usando recursos de exibição sobre outros apps. Ela consiste em um cartão flutuante preto e cinza com tipografia branca de alto contraste.

*   **Identificadores de Marca:** Pacote `com.ubercab.driver` e strings de produto como *"Uber Moto"*, *"Uber Flash"*, ou *"Flash Moto"*.
*   **Tarifa / Ganhos:** Exibido como `R$ XX,XX` (ex: `R$ 12,50` ou `R$ 8.90`).
*   **Avaliação do Cliente / Remetente:**
    *   A Uber exibe em destaque a nota de avaliação do usuário para segurança do motorista (ex: `4.9★` ou `4.85`).
*   **Métricas de Distância e Tempo:**
    *   A Uber divide claramente o trajeto em duas etapas na visualização em lista:
        1.  **Deslocamento até a coleta:** ex: `3 min (1,2 km)`.
        2.  **Viagem total estimada:** ex: `15 min (7,4 km)`.
    *   *Heurística de Captura:* O maior valor de quilometragem representa a distância total que o motoboy irá rodar.
*   **Endereços (Origem e Destino):**
    *   Identificados pelas strings de localização aproximada (ex: *"Embarque: Av. Paulista"*, *"Desembarque: Rua Pamplona"*).
*   **Estrutura de Ação (Gestos/Cliques):**
    *   **Botão de Aceite:** Toda a metade inferior do cartão flutuante funciona como área de toque de aceite (Toque para aceitar).
    *   **Botão de Rejeição:** Um botão de "X" posicionado no canto superior esquerdo para descartar imediatamente a oferta.

### C. 99 Driver (99Moto e 99Entrega)
O aplicativo de motoristas da 99 possui design moderno com cores cinza, amarelo e laranja vibrantes.

*   **Identificadores de Marca:** Pacote `com.nine9.driver` e strings de modalidade como *"99Moto"*, *"99Entrega"*, ou *"Pop"*.
*   **Tarifa / Ganhos:** Apresentado como `R$ XX,XX` com etiquetas de bônus, se aplicável (ex: `+ R$ 3,00 Dinâmico`).
*   **Métricas de Distância e Tempo:**
    *   Geralmente exibe frases como *"Passageiro a 800m"* ou *"Embarque a 1,2 km"*, seguido por *"Destino a 4,5 km"*.
    *   O aplicativo exibe a distância total da corrida no resumo inferior do cartão (ex: `5,7 km`).
*   **Endereços:**
    *   Usa os termos *"Embarque"* (coleta) e *"Desembarque"* (entrega), seguidos de nomes de ruas, avenidas ou estabelecimentos.
*   **Estrutura de Ação (Gestos/Cliques):**
    *   **Botão de Aceite:** Um botão amarelo ou laranja proeminente com o texto *"Aceitar"*.
    *   **Botão de Rejeição:** Um ícone discreto no topo da tela escrito *"Recusar"*.

---

## 2. Lógica de Acessibilidade: Como o Radar "Aprende" das Visualizações

A acessibilidade no Android extrai dados capturando a árvore estrutural da janela ativa (`AccessibilityNodeInfo`). 

### Fluxo de Parsing Inteligente
1.  **Filtragem de Pacotes:** O serviço escuta apenas eventos vindos de pacotes permitidos (`com.ifood.driver`, `com.ubercab.driver`, `com.nine9.driver`).
2.  **Busca Recursiva de Textos:** O Radar vasculha recursivamente todos os nós visíveis e ocultos extraindo propriedades de `.text` e `.contentDescription`.
3.  **Filtragem Antirrepetição:** Utiliza um hash de cache temporal (com ciclo de 15 segundos) para que o mesmo cartão de oferta não dispare múltiplas análises se o motorista interagir na tela.
4.  **Extração Regular (RegEx Avançado):**
    *   **Valor:** Busca por `R$\s*(\d+[,.]\d{2})` para capturar a tarifa exata da corrida.
    *   **Distância:** Filtra ocorrências de números seguidos de `km` ou `m`. Converte valores em metros (`m`) dividindo por 1000. Obtém a distância total elegendo o maior valor encontrado, contornando a variação de strings de aplicativos.
    *   **Tempo:** Filtra números seguidos de `min` ou `minutos` para inferir a duração aproximada do trajeto.
5.  **Heurística de Endereços:** Varre as linhas de texto em busca de prefixos urbanos (*Rua, Av, Avenida, Alameda, Travessa, Rodovia, Praça*) mapeando-os como endereços primários (coleta) e secundários (entrega).

---

## 3. Heurísticas Matemáticas para Otimização de Lucro em 2026

Para que o motoboy obtenha o maior lucro líquido possível nas suas entregas diárias, o Radar executa regras de decisão baseadas em eficiência financeira e segurança viária.

### Formulação de Lucro Líquido
O lucro real do entregador não é a tarifa bruta, mas sim a tarifa subtraída dos custos operacionais da moto e o tempo investido.

$$\text{Lucro Estimado} = \text{Tarifa Bruta} - (\text{Distância Total} \times \text{Custo por KM}) - (\text{Tempo Estimado} \times \text{Custo de Oportunidade})$$

Em 2026, estimam-se os seguintes parâmetros médios para motocicletas de entrega (150cc a 250cc) no Brasil:
*   **Custo por KM Rodado (Combustível + Pneu + Óleo + Depreciação):** R\$ 0,55 a R\$ 0,75 por km.
*   **Valor Mínimo Viável para Aceite:**
    *   *Recomendado:* No mínimo **R\$ 2,00 por km rodado**.
    *   *Excelente:* Acima de **R\$ 3,00 por km rodado**.

### Lógica Algorítmica do Decisor (Local e Cloud)
O algoritmo do Radar processa as seguintes tomadas de decisão sequenciais:

```
                            [ NOVA OFERTA LIDA ]
                                     │
                     Valor Bruto < Min_Configurado?
                    ┌────────────────┴────────────────┐
                   SIM                               NÃO
                    │                                 │
            [ AUTO-REJEITAR ]                 Calcular R$ / KM:
            (Poupa desgaste)             (Valor Bruto / Distância Total)
                                                      │
                                           R$/KM < Min_Por_KM_Configurado?
                                          ┌───────────┴───────────┐
                                         SIM                     NÃO
                                          │                       │
                                  [ AUTO-REJEITAR ]       Filtrar Zonas de Risco
                                  (Prejuízo no KM)        (Checar nomes de bairros)
                                                                  │
                                                        Está na Zona de Perigo?
                                                       ┌──────────┴──────────┐
                                                      SIM                   NÃO
                                                       │                     │
                                               [ RECOMENDAR RECUSA ]     Velocidade > Limite?
                                               (Foco em Segurança)      ┌────┴────┐
                                                                       SIM       NÃO
                                                                        │         │
                                                                   [ BLOQUEAR ] [ AUTO-ACEITAR /
                                                                   (Segurança)   SUGERIR VOZ ]
```

### Análise de Rotas Encadeadas (Chained Deliveries)
Se o motoboy já estiver em uma entrega e receber uma oferta secundária de "desvio", o app analisa se a quilometragem adicional (*detourDistance*) mantém a média mínima de ganhos por quilômetro configurada pelo motorista, evitando o desvio para áreas de perigo geográfico.

---

## 4. Conclusão e Práticas Profissionais para o Desenvolvedor

A implementação contida no **Radar Delivery AI** une leitura rápida de tela via acessibilidade (com consumo desprezível de bateria) e inteligência analítica local ou via Gemini API (para análise visual contextual de trânsito e mapas). 

Ao combinar **leitura de texto local robusta** (que provê cálculos instantâneos mesmo sem internet) e **análise multimodal**, garantimos que o entregador receba alertas de voz e decisões automáticas de forma confiável e em frações de segundo, proporcionando o máximo de lucro por hora de trabalho nas ruas brasileiras em 2026.
