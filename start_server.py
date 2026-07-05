import os
import sys
import logging
from dotenv import load_dotenv

# Configuração de Logs
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("radar-startup")

def validate_gemini_connection(api_key: str, model_name: str) -> bool:
    """
    Valida a conexão com a API do Google Generative AI realizando um ping rápido.
    """
    logger.info("Tentando validar a conexão com a API do Google Generative AI...")
    try:
        import google.generativeai as genai
        from google.api_core.exceptions import GoogleAPIError
        
        genai.configure(api_key=api_key)
        
        # Cria uma instância rápida do modelo para validação
        model = genai.GenerativeModel(model_name)
        
        # Realiza uma chamada extremamente leve (ping de 1 palavra) para testar a chave e conexão
        logger.info(f"Enviando requisição de teste para o modelo '{model_name}'...")
        response = model.generate_content("Diga apenas OK")
        
        if response and response.text:
            logger.info("==================================================")
            logger.info("   [CONEXÃO COM GEMINI API VALIDADA COM SUCESSO]  ")
            logger.info(f"   Modelo testado: {model_name}")
            logger.info(f"   Resposta recebida: {response.text.strip()}")
            logger.info("==================================================")
            return True
        else:
            logger.error("A API do Gemini respondeu, mas o corpo do texto veio vazio.")
            return False
            
    except ImportError:
        logger.error("Biblioteca 'google-generativeai' não está instalada!")
        logger.info("Por favor, execute: pip install -r requirements.txt")
        return False
    except GoogleAPIError as e:
        logger.error(f"Erro da API do Google ao conectar: {str(e)}")
        logger.error("Verifique se a sua chave GEMINI_API_KEY está ativa e correta.")
        return False
    except Exception as e:
        logger.error(f"Erro inesperado durante a validação de conexão: {str(e)}")
        return False

def main():
    logger.info("Iniciando processo de verificação do Radar Delivery AI...")
    
    # 1. Carrega o arquivo .env se ele existir
    env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")
    if os.path.exists(env_path):
        logger.info(f"Arquivo .env encontrado em: {env_path}. Carregando variáveis...")
        load_dotenv(dotenv_path=env_path)
    else:
        logger.warning("Arquivo .env não foi localizado no diretório raiz. Tentando usar variáveis do sistema...")

    # 2. Obtém a chave API e o modelo do ambiente
    gemini_key = os.environ.get("GEMINI_API_KEY")
    model_name = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")
    server_token = os.environ.get("X_API_TOKEN", "radar_central_secret_token_123")
    port = int(os.environ.get("PORT", 5000))

    # 3. Validações iniciais das chaves
    if not gemini_key or gemini_key == "MY_GEMINI_API_KEY":
        logger.error("=========================================================")
        logger.error(" [ERRO CRÍTICO] GEMINI_API_KEY NÃO ESTÁ CONFIGURADA!    ")
        logger.error("=========================================================")
        logger.error("Insira a sua chave API do Gemini no seu arquivo .env ou")
        logger.error("defina-a no painel de Secrets antes de iniciar o app.")
        logger.error("Exemplo no .env: GEMINI_API_KEY=AIzaSy...")
        logger.error("=========================================================")
        sys.exit(1)

    # 4. Validação de conectividade real
    is_valid = validate_gemini_connection(api_key=gemini_key, model_name=model_name)
    
    if not is_valid:
        logger.error("Não foi possível iniciar o serviço de forma segura.")
        logger.error("Corrija as credenciais ou sua conexão e tente novamente.")
        sys.exit(1)

    # 5. Inicializa o servidor Flask (importado dinamicamente para evitar carregar o app antes das validações)
    logger.info("Credenciais e API validadas com sucesso!")
    logger.info(f"Porta do Servidor: {port}")
    logger.info(f"Token de Segurança Ativo: {server_token[:4]}...{server_token[-4:] if len(server_token) > 8 else ''}")
    logger.info("Iniciando servidor de processamento de ofertas...")
    
    try:
        from server import app
        # Define as mesmas configurações de ambiente para o flask app rodar
        app.run(host="0.0.0.0", port=port, debug=False)
    except Exception as e:
        logger.error(f"Erro crítico ao subir o servidor Flask: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()
