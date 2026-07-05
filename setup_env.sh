#!/bin/bash

# Cores para saída elegante no terminal
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # Sem cor
BLUE='\033[0;34m'
BOLD='\033[1m'
CYAN='\033[0;36m'

ENV_FILE=".env"
TEMP_FILE=".env.tmp"

clear
echo -e "${BLUE}====================================================${NC}"
echo -e "${BOLD}${GREEN}     🚀 ASSISTENTE DE CONFIGURAÇÃO DO RADAR AI      ${NC}"
echo -e "${BLUE}====================================================${NC}"

# Função para mascarar a chave para exibição segura
get_masked_key() {
    local key="$1"
    if [ -z "$key" ]; then
        echo -e "${RED}[Não configurada]${NC}"
    elif [ ${#key} -gt 12 ]; then
        echo -e "${GREEN}${key:0:6}...${key: -6}${NC}"
    else
        echo -e "${YELLOW}********${NC}"
    fi
}

# Função para obter o valor de uma variável do .env de forma limpa
get_env_val() {
    local var_name="$1"
    if [ -f "$ENV_FILE" ]; then
        grep -E "^${var_name}=" "$ENV_FILE" | cut -d= -f2- | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^["'\'' ]*//' -e 's/["'\'' ]*$//'
    fi
}

# Garante que o arquivo .env exista (copiando do .env.example ou criando padrão)
ensure_env_exists() {
    if [ ! -f "$ENV_FILE" ]; then
        if [ -f ".env.example" ]; then
            echo -e "${YELLOW}📂 Criando arquivo .env a partir de .env.example...${NC}"
            cp .env.example "$ENV_FILE"
        else
            echo -e "${YELLOW}📂 Criando um novo arquivo .env com valores padrão...${NC}"
            cat <<EOF > "$ENV_FILE"
PORT=5000
GEMINI_API_KEY=
X_API_TOKEN=radar_central_secret_token_123
MIN_VALUE_PER_KM=2.0
MIN_FARE_VALUE=8.0
MAX_SPEED_LIMIT_KMH=40.0
EOF
        fi
    fi
}

# Salva uma variável no .env substituindo ou adicionando
save_to_env() {
    local key="$1"
    local value="$2"
    ensure_env_exists
    
    local found=false
    > "$TEMP_FILE"

    while IFS= read -r line || [ -n "$line" ]; do
        if [[ "$line" =~ ^${key}= ]]; then
            echo "${key}=${value}" >> "$TEMP_FILE"
            found=true
        else
            echo "$line" >> "$TEMP_FILE"
        fi
    done < "$ENV_FILE"

    if [ "$found" = false ]; then
        echo "${key}=${value}" >> "$TEMP_FILE"
    fi

    mv "$TEMP_FILE" "$ENV_FILE"
}

# Testa a conexão da chave atual via chamada Python simplificada ao Gemini API
test_gemini_connection() {
    local key=$(get_env_val "GEMINI_API_KEY")
    if [ -z "$key" ] || [ "$key" = "MY_GEMINI_API_KEY" ]; then
        echo -e "${RED}❌ Erro: Nenhuma chave configurada para testar.${NC}"
        return 1
    fi

    echo -e "${CYAN}🔄 Testando conectividade real com a API do Gemini...${NC}"
    
    # Verifica se python3 está disponível
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}❌ Python3 não está instalado no sistema.${NC}"
        return 1
    fi

    # Executa o script python inline para testar
    python3 -c "
import os, sys
try:
    import google.generativeai as genai
    genai.configure(api_key='${key}')
    model = genai.GenerativeModel('gemini-2.5-flash')
    response = model.generate_content('Diga OK')
    if response and response.text:
        print('SUCCESS::' + response.text.strip())
    else:
        print('EMPTY')
except Exception as e:
    print('ERROR::' + str(e))
" > .conn_test_res 2>&1

    local res=$(cat .conn_test_res)
    rm -f .conn_test_res

    if [[ "$res" == SUCCESS::* ]]; then
        local reply=${res#SUCCESS::}
        echo -e "${GREEN}✔ Conexão estabelecida com SUCESSO!${NC}"
        echo -e "   -> Resposta do Gemini: ${BOLD}\"$reply\"${NC}"
        return 0
    else
        echo -e "${RED}❌ Falha de Conexão com o Gemini!${NC}"
        echo -e "${YELLOW}Detalhes do erro:${NC}"
        echo -e "$res"
        echo -e "\n${YELLOW}Dica: Verifique se a sua chave está ativa e sem restrições de IP/região.${NC}"
        return 1
    fi
}

# Configura a chave API do Gemini solicitando entrada
configure_key() {
    ensure_env_exists
    local current_key=$(get_env_val "GEMINI_API_KEY")
    
    if [ -n "$current_key" ] && [ "$current_key" != "MY_GEMINI_API_KEY" ]; then
        echo -e "${YELLOW}Uma chave já está configurada: $(get_masked_key "$current_key")${NC}"
        echo -n "Deseja substituí-la por uma nova? (s/N): "
        read -r choice
        if [[ ! "$choice" =~ ^[sS]$ ]]; then
            echo -e "${CYAN}Mantendo chave atual.${NC}"
            return 0
        fi
    fi

    echo -e "\n${YELLOW}Por favor, digite ou cole a sua chave API do Gemini:${NC}"
    echo -n "> "
    read -r API_KEY

    # Higienização da chave (remove espaços e aspas extras)
    API_KEY=$(echo "$API_KEY" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^["'\'' ]*//' -e 's/["'\'' ]*$//')

    if [ -z "$API_KEY" ]; then
        echo -e "${RED}❌ Erro: Chave inválida ou vazia! Operação cancelada.${NC}"
        return 1
    fi

    save_to_env "GEMINI_API_KEY" "$API_KEY"
    echo -e "${GREEN}✔ Chave salva no arquivo .env com sucesso!${NC}"
    
    # Pergunta se quer testar na hora
    echo -n "Deseja testar a nova chave agora? (S/n): "
    read -r test_choice
    if [[ "$test_choice" =~ ^[nN]$ ]]; then
        return 0
    fi
    test_gemini_connection
}

# Verifica se dependências estão instaladas no ambiente Python
check_dependencies() {
    echo -e "${CYAN}🔍 Verificando ambiente Python e dependências...${NC}"
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}❌ Python3 não está instalado! Instale-o primeiro.${NC}"
        return 1
    fi
    
    # Verifica se estamos em um virtual environment (venv)
    if [ -n "$VIRTUAL_ENV" ]; then
        echo -e "${GREEN}✔ Ambiente Virtual Ativo: $VIRTUAL_ENV${NC}"
    else
        echo -e "${YELLOW}⚠ Nenhum ambiente virtual (venv) está ativo.${NC}"
        if [ -d "venv" ]; then
            echo -e "   -> Nota: Existe uma pasta 'venv' neste diretório. Você pode ativá-la com: ${BOLD}source venv/bin/activate${NC}"
        fi
    fi

    # Testa import de pacotes críticos
    python3 -c "
missing = []
try: import flask
except ImportError: missing.append('flask')
try: import google.generativeai
except ImportError: missing.append('google-generativeai')
try: import dotenv
except ImportError: missing.append('python-dotenv')
if missing:
    print('MISSING::' + ','.join(missing))
else:
    print('OK')
" > .dep_check 2>&1

    local dep_res=$(cat .dep_check)
    rm -f .dep_check

    if [ "$dep_res" = "OK" ]; then
        echo -e "${GREEN}✔ Todas as dependências (Flask, Gemini API, Dotenv) estão instaladas!${NC}"
    else
        local missing_pkgs=${dep_res#MISSING::}
        echo -e "${RED}❌ Dependências ausentes: $missing_pkgs${NC}"
        echo -n "Deseja instalar as dependências necessárias do 'requirements.txt' agora? (S/n): "
        read -r install_choice
        if [[ ! "$install_choice" =~ ^[nN]$ ]]; then
            echo -e "${CYAN}Installing dependencies...${NC}"
            pip install -r requirements.txt
        fi
    fi
}

# Exibe as configurações atuais mascaradas de forma elegante
show_status() {
    ensure_env_exists
    local key=$(get_env_val "GEMINI_API_KEY")
    local port=$(get_env_val "PORT")
    local token=$(get_env_val "X_API_TOKEN")
    local limit=$(get_env_val "MAX_SPEED_LIMIT_KMH")

    echo -e "\n${BLUE}--- Configurações Ativas (.env) ---${NC}"
    echo -e "🔑 ${BOLD}GEMINI_API_KEY:${NC}      $(get_masked_key "$key")"
    echo -e "🔌 ${BOLD}Porta do Servidor:${NC}    ${CYAN}${port:-5000}${NC}"
    echo -e "🔒 ${BOLD}X_API_TOKEN:${NC}          $(get_masked_key "$token")"
    echo -e "⚡ ${BOLD}Limite de Velocidade:${NC} ${YELLOW}${limit:-40.0} km/h${NC}"
    echo -e "${BLUE}----------------------------------${NC}\n"
}

# Loop do Menu principal
while true; do
    echo -e "\n${BOLD}Selecione uma opção:${NC}"
    echo -e "  [${GREEN}1${NC}] Configurar ou Atualizar a chave do Gemini API"
    echo -e "  [${GREEN}2${NC}] Realizar Teste de Diagnóstico de Conexão com o Gemini"
    echo -e "  [${GREEN}3${NC}] Verificar / Instalar Dependências de Sistema"
    echo -e "  [${GREEN}4${NC}] Visualizar Configurações do .env"
    echo -e "  [${GREEN}5${NC}] Sair"
    echo -n "Escolha (1-5): "
    read -r option

    case "$option" in
        1)
            configure_key
            ;;
        2)
            test_gemini_connection
            ;;
        3)
            check_dependencies
            ;;
        4)
            show_status
            ;;
        5)
            echo -e "\n${GREEN}Boas simulações! Até mais! 👋${NC}\n"
            exit 0
            ;;
        *)
            echo -e "${RED}Opção inválida! Selecione uma opção de 1 a 5.${NC}"
            ;;
    esac
    echo -e "\nPressione [ENTER] para continuar..."
    read -r
    clear
    echo -e "${BLUE}====================================================${NC}"
    echo -e "${BOLD}${GREEN}     🚀 ASSISTENTE DE CONFIGURAÇÃO DO RADAR AI      ${NC}"
    echo -e "${BLUE}====================================================${NC}"
done
