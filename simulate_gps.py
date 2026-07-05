import os
import sys
import time
import requests
from dotenv import load_dotenv

# Configurações de Cores ANSI para o Terminal
RESET = "\033[0m"
BOLD = "\033[1m"
GREEN = "\033[32m"
RED = "\033[31m"
YELLOW = "\033[33m"
CYAN = "\033[36m"

def main():
    print(f"{BOLD}{CYAN}==================================================")
    print("      SIMULADOR DE GPS E TRAVA DE VELOCIDADE      ")
    print(f"=================================================={RESET}")

    # Carrega variáveis de ambiente
    env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")
    if os.path.exists(env_path):
        load_dotenv(dotenv_path=env_path)

    # Configurações de conexão
    port = int(os.environ.get("PORT", 5000))
    server_url = f"http://localhost:{port}/speed_monitor"
    token = os.environ.get("X_API_TOKEN", "radar_central_secret_token_123")

    print(f"[*] Conectando ao Servidor: {server_url}")
    print(f"[*] Token de Acesso: {token[:4]}...{token[-4:] if len(token) > 8 else ''}")

    # Coordenadas simuladas de um trajeto na Av. Paulista, São Paulo
    # latitude, longitude, speed_kmh, descricao
    simulated_steps = [
        (-23.5615, -46.6560, 0.0, "Motoboy parado no sinal da Alameda Campinas"),
        (-23.5620, -46.6550, 15.0, "Arrancando suavemente com a moto"),
        (-23.5628, -46.6540, 28.0, "Acelerando na via expressa"),
        (-23.5635, -46.6530, 39.5, "Quase no limite seguro da central (40 km/h)"),
        (-23.5645, -46.6520, 48.0, "Acelerou demais! Excedeu limite seguro"),
        (-23.5655, -46.6510, 52.5, "Alta velocidade constante"),
        (-23.5662, -46.6500, 35.0, "Freando ao ver o sinal vermelho à frente"),
        (-23.5670, -46.6490, 12.0, "Sinalizando para encostar"),
        (-23.5675, -46.6485, 0.0, "Totalmente parado. Pronto para interações")
    ]

    headers = {
        "Content-Type": "application/json",
        "X-API-Token": token
    }

    print(f"\n[*] Iniciando simulação de trajeto em tempo real (9 etapas)...\n")

    for idx, (lat, lng, speed, desc) in enumerate(simulated_steps, 1):
        print(f"{BOLD}[Passo {idx}/9] {desc}{RESET}")
        print(f"    Coordenadas: Lat {lat}, Lng {lng} | Velocidade: {speed} km/h")

        payload = {
            "rider_id": "motoboy_thiago_01",
            "latitude": lat,
            "longitude": lng,
            "speed_kmh": speed
        }

        try:
            # Envia a telemetria do GPS para o servidor Flask central
            response = requests.post(server_url, json=payload, headers=headers)
            
            if response.status_code == 200:
                res_json = response.json()
                speed_ret = res_json.get("speed_kmh", 0.0)
                is_locked = res_json.get("speed_lock", False)
                msg = res_json.get("message", "")

                status_tag = f"{BOLD}{RED}[!!! BLOQUEADO !!!]{RESET}" if is_locked else f"{BOLD}{GREEN}[✓ LIBERADO]{RESET}"
                color_speed = RED if is_locked else GREEN

                print(f"    Servidor respondeu: {status_tag}")
                print(f"    Velocidade Registrada: {color_speed}{speed_ret} km/h{RESET} (Limite: {res_json.get('max_speed_limit_kmh')} km/h)")
                print(f"    Status: {msg}")
            elif response.status_code == 401:
                print(f"    {RED}[ERRO 401] Falha de autenticação. Verifique o X-API-Token.{RESET}")
            else:
                print(f"    {RED}[ERRO {response.status_code}] {response.text}{RESET}")

        except requests.exceptions.ConnectionError:
            print(f"    {YELLOW}[AVISO] O servidor local não está em execução nesta porta ({port}).")
            print(f"            Para ver a simulação ativa, certifique-se de que o backend Flask está rodando.{RESET}")
            break
        except Exception as e:
            print(f"    {RED}[ERRO INESPERADO] {str(e)}{RESET}")
            break

        print("-" * 50)
        time.sleep(2) # Intervalo simulado entre leituras do GPS

    print(f"\n{BOLD}{CYAN}==================================================")
    print("           FIM DA SIMULAÇÃO DE TELEMETRIA         ")
    print(f"=================================================={RESET}")

if __name__ == "__main__":
    main()
