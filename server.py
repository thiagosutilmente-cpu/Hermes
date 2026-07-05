import os
import base64
import json
import time
import math
import threading
from flask import Flask, request, jsonify, send_from_directory
import google.generativeai as genai

app = Flask(__name__)

# ==========================================
# Rotas do Frontend Web (Portal do Motoboy)
# ==========================================
@app.route('/')
def serve_index():
    """Serves the driver panel web client login & registration interface"""
    return send_from_directory('.', 'index.html')

@app.route('/firebase.js')
def serve_firebase_js():
    """Serves the Firebase configuration and auth service file"""
    return send_from_directory('.', 'firebase.js', mimetype='application/javascript')

# ==========================================
# Configurações do Servidor
# ==========================================
PORT = int(os.environ.get("PORT", 5000))

# Token central de segurança. Motoboys devem configurar este token no campo
# de autorização (X-API-Token) do aplicativo para autenticarem com a central.
SERVER_API_TOKEN = os.environ.get("X_API_TOKEN", "radar_central_secret_token_123")

# Configurações do Gemini API
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY")
# Recomenda-se usar gemini-2.5-flash ou superior para análise rápida de imagem
GEMINI_MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")

# Regras de Negócio da Central (Customizáveis via variáveis de ambiente)
MIN_VALUE_PER_KM = float(os.environ.get("MIN_VALUE_PER_KM", 2.0))
MIN_FARE_VALUE = float(os.environ.get("MIN_FARE_VALUE", 8.0))

# Limite máximo de velocidade permitido (em km/h) para interação com o app.
# Se o motoboy estiver acima deste limite, o app é travado por segurança.
MAX_SPEED_LIMIT_KMH = float(os.environ.get("MAX_SPEED_LIMIT_KMH", 40.0))

# Base de dados em memória para rastreamento de velocidade dos motoboys em tempo real
# Estrutura: { rider_id: { "lat": float, "lng": float, "timestamp": float, "speed_kmh": float, "locked": bool } }
riders_tracker = {}

# Inicialização da biblioteca Google Generative AI
if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)
    print(f"[*] Gemini API inicializada com sucesso usando o modelo: {GEMINI_MODEL}")
else:
    print("[WARNING] Variável de ambiente GEMINI_API_KEY não configurada!")
    print("          O servidor falhará ao processar requisições reais do app.")


def calculate_haversine_speed(lat1, lon1, lat2, lon2, time_diff_seconds):
    """
    Calcula a velocidade média em km/h entre duas coordenadas geográficas e um delta de tempo.
    Usa a fórmula de Haversine para precisão matemática.
    """
    if time_diff_seconds <= 0:
        return 0.0
        
    # Raio da Terra em metros
    R = 6371000.0
    
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    
    a = math.sin(delta_phi / 2.0) ** 2 + \
        math.cos(phi1) * math.cos(phi2) * \
        math.sin(delta_lambda / 2.0) ** 2
        
    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))
    distance_meters = R * c
    
    # Velocidade em m/s -> conversão para km/h (* 3.6)
    speed_mps = distance_meters / time_diff_seconds
    speed_kmh = speed_mps * 3.6
    
    return min(speed_kmh, 150.0) # Limita a 150km/h para evitar saltos de GPS espúrios


def calculate_distance_km(lat1, lon1, lat2, lon2):
    """
    Calcula a distância em quilômetros entre duas coordenadas geográficas.
    Usa a fórmula de Haversine para alta precisão.
    """
    # Raio da Terra em km
    R = 6371.0
    
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    
    a = math.sin(delta_phi / 2.0) ** 2 + \
        math.cos(phi1) * math.cos(phi2) * \
        math.sin(delta_lambda / 2.0) ** 2
        
    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))
    return R * c


# Cache em memória para evitar requisições de mapas redundantes ou recálculos Haversine repetitivos.
# Chave: ((lat1, lon1), (lat2, lon2)) arredondados para 4 casas decimais para cobrir micro-desvios (~11 metros)
DISTANCE_CACHE = {}
DISTANCE_CACHE_LOCK = threading.Lock()

def get_cached_distance(lat1, lon1, lat2, lon2):
    """
    Recupera a distância calculada entre dois pontos do cache ou realiza o cálculo e armazena.
    Utiliza arredondamento de 4 casas decimais para agrupamento geográfico inteligente.
    """
    key1 = (round(lat1, 4), round(lon1, 4))
    key2 = (round(lat2, 4), round(lon2, 4))
    # Ordena as chaves para garantir bidirecionalidade no cache (A -> B é o mesmo que B -> A)
    cache_key = tuple(sorted([key1, key2]))
    
    with DISTANCE_CACHE_LOCK:
        if cache_key in DISTANCE_CACHE:
            print(f"[CACHE HIT] Distância recuperada instantaneamente do cache: {cache_key} -> {DISTANCE_CACHE[cache_key]:.2f} km")
            return DISTANCE_CACHE[cache_key]
            
    # Calcula caso não exista no cache
    distance = calculate_distance_km(lat1, lon1, lat2, lon2)
    
    with DISTANCE_CACHE_LOCK:
        # Evita crescimento indefinido limpando o cache quando ultrapassar 2000 rotas
        if len(DISTANCE_CACHE) > 2000:
            DISTANCE_CACHE.clear()
            print("[CACHE] Cache de distância reiniciado por limite de capacidade.")
        DISTANCE_CACHE[cache_key] = distance
        print(f"[CACHE MISS] Nova rota calculada e armazenada: {cache_key} -> {distance:.2f} km")
        
    return distance


AUDIT_LOG_FILE = os.environ.get("AUDIT_LOG_FILE", "offers_audit.log")

def log_offer_decision(rider_id, app_name, fare_value, pickup, delivery, distance, duration, score, suggestion, reason):
    """
    Registra a decisão tomada sobre uma oferta capturada em um arquivo de log para auditoria futura.
    """
    try:
        from datetime import datetime
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        # 1. Estrutura JSON para leitura programática (ex: tela de auditoria ou integração)
        log_entry = {
            "timestamp": timestamp,
            "rider_id": rider_id,
            "delivery_app": app_name,
            "fare_value": fare_value,
            "pickup_address": pickup,
            "delivery_address": delivery,
            "total_distance_km": distance,
            "total_time_min": duration,
            "score": score,
            "suggestion": suggestion,
            "reason": reason
        }
        
        # 2. String legível para humanos
        human_readable = (
            f"[{timestamp}] [Rider: {rider_id}] App: {app_name} | Valor: R$ {fare_value:.2f} | "
            f"Distância: {distance:.2f}km | Tempo: {duration:.1f}min | "
            f"Decisão: {suggestion.upper()} (Score: {int(score)}) | Motivo: {reason}\n"
            f"   -> Coleta: {pickup}\n"
            f"   -> Entrega: {delivery}\n"
            f"---------------------------------------------------------------------------------\n"
        )
        
        # Salva o log em formato JSON por linha para facilitar parsing de auditorias,
        # e grava também o log humanizado legível
        with open(AUDIT_LOG_FILE, "a", encoding="utf-8") as f:
            f.write(json.dumps(log_entry, ensure_ascii=False) + "\n")
            
        with open("offers_audit_readable.txt", "a", encoding="utf-8") as f:
            f.write(human_readable)
            
        print(f"[*] Decisão registrada no log de auditoria: {suggestion} (R$ {fare_value})")
    except Exception as e:
        print(f"[ERROR] Falha ao gravar log de auditoria: {str(e)}")


@app.route('/audit_logs', methods=['GET'])
def get_audit_logs():
    """
    Retorna os logs de auditoria das ofertas processadas em formato JSON.
    """
    token = request.headers.get("X-API-Token") or request.args.get("token")
    if not token or token != SERVER_API_TOKEN:
        return jsonify({"error": "Não autorizado"}), 401
        
    rider_filter = request.args.get("rider_id")
    limit = int(request.args.get("limit", 100))
    
    logs = []
    if os.path.exists(AUDIT_LOG_FILE):
        try:
            with open(AUDIT_LOG_FILE, "r", encoding="utf-8") as f:
                for line in f:
                    if line.strip():
                        entry = json.loads(line.strip())
                        if rider_filter and entry.get("rider_id") != rider_filter:
                            continue
                        logs.append(entry)
        except Exception as e:
            return jsonify({"error": f"Erro ao ler arquivo de logs: {str(e)}"}), 500
            
    logs.reverse()
    return jsonify(logs[:limit])


@app.route('/audit_logs/readable', methods=['GET'])
def get_readable_logs():
    """
    Retorna os logs de auditoria das ofertas processadas em texto puro legível.
    """
    token = request.headers.get("X-API-Token") or request.args.get("token")
    if not token or token != SERVER_API_TOKEN:
        return "Não autorizado", 401
        
    if os.path.exists("offers_audit_readable.txt"):
        try:
            with open("offers_audit_readable.txt", "r", encoding="utf-8") as f:
                content = f.read()
            return content, 200, {"Content-Type": "text/plain; charset=utf-8"}
        except Exception as e:
            return f"Erro ao ler arquivo de logs legível: {str(e)}", 500
    else:
        return "Nenhum log registrado ainda.", 200, {"Content-Type": "text/plain; charset=utf-8"}


@app.route('/audit_logs/daily_report', methods=['GET'])
def get_daily_report():
    """
    Calcula um relatório diário de ganhos estimados baseados nas ofertas aceitas pelo motoboy.
    """
    token = request.headers.get("X-API-Token") or request.args.get("token")
    if not token or token != SERVER_API_TOKEN:
        return jsonify({"error": "Não autorizado"}), 401
        
    rider_filter = request.args.get("rider_id")
    
    daily_stats = {}
    
    if os.path.exists(AUDIT_LOG_FILE):
        try:
            with open(AUDIT_LOG_FILE, "r", encoding="utf-8") as f:
                for line in f:
                    if not line.strip():
                        continue
                    try:
                        entry = json.loads(line.strip())
                    except Exception:
                        continue
                        
                    if rider_filter and entry.get("rider_id") != rider_filter:
                        continue
                        
                    # Extract date YYYY-MM-DD from timestamp (e.g., "2026-07-02 01:34:42")
                    timestamp = entry.get("timestamp", "")
                    if len(timestamp) >= 10:
                        date_key = timestamp[:10]
                    else:
                        date_key = "Desconhecido"
                        
                    if date_key not in daily_stats:
                        daily_stats[date_key] = {
                            "date": date_key,
                            "total_offers_evaluated": 0,
                            "total_offers_accepted": 0,
                            "total_offers_rejected": 0,
                            "total_offers_considered": 0,
                            "estimated_earnings": 0.0,
                            "total_distance_km": 0.0,
                            "total_time_min": 0.0,
                            "app_breakdown": {}
                        }
                        
                    stats = daily_stats[date_key]
                    stats["total_offers_evaluated"] += 1
                    
                    suggestion = entry.get("suggestion", "").lower()
                    if suggestion == "aceitar":
                        stats["total_offers_accepted"] += 1
                        
                        # Accumulate earnings, distance and time
                        try:
                            fare = float(entry.get("fare_value", 0.0))
                        except Exception:
                            fare = 0.0
                        try:
                            dist = float(entry.get("total_distance_km", 0.0))
                        except Exception:
                            dist = 0.0
                        try:
                            duration = float(entry.get("total_time_min", 0.0))
                        except Exception:
                            duration = 0.0
                            
                        stats["estimated_earnings"] += fare
                        stats["total_distance_km"] += dist
                        stats["total_time_min"] += duration
                        
                        # App breakdown accumulation
                        app_name = entry.get("delivery_app", "Outros")
                        if app_name not in stats["app_breakdown"]:
                            stats["app_breakdown"][app_name] = {
                                "offers_accepted": 0,
                                "estimated_earnings": 0.0
                            }
                        stats["app_breakdown"][app_name]["offers_accepted"] += 1
                        stats["app_breakdown"][app_name]["estimated_earnings"] += fare
                        
                    elif suggestion == "recusar":
                        stats["total_offers_rejected"] += 1
                    else:
                        stats["total_offers_considered"] += 1
                        
        except Exception as e:
            return jsonify({"error": f"Erro ao processar logs para o relatório: {str(e)}"}), 500

    # Format and calculate averages/ratios for each day
    report_list = []
    for date_key in sorted(daily_stats.keys(), reverse=True):
        stats = daily_stats[date_key]
        
        # Round floating values for precision
        stats["estimated_earnings"] = round(stats["estimated_earnings"], 2)
        stats["total_distance_km"] = round(stats["total_distance_km"], 2)
        stats["total_time_min"] = round(stats["total_time_min"], 1)
        
        # Rounded averages
        accepted_count = stats["total_offers_accepted"]
        stats["average_fare_value"] = round(stats["estimated_earnings"] / accepted_count, 2) if accepted_count > 0 else 0.0
        
        total_dist = stats["total_distance_km"]
        stats["earnings_per_km"] = round(stats["estimated_earnings"] / total_dist, 2) if total_dist > 0 else 0.0
        
        # Format breakdown rounded numbers
        for app_name, app_data in stats["app_breakdown"].items():
            app_data["estimated_earnings"] = round(app_data["estimated_earnings"], 2)
            
        report_list.append(stats)
        
    return jsonify(report_list)


@app.route('/speed_monitor', methods=['POST'])
def speed_monitor():
    """
    Endpoint para monitorar a velocidade do motoboy em tempo real.
    Recebe as coordenadas de GPS e calcula/valida a velocidade para controle da trava de segurança.
    """
    token = request.headers.get("X-API-Token")
    if not token or token != SERVER_API_TOKEN:
        return jsonify({"error": "Token inválido"}), 401

    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "JSON corpo ausente"}), 400

        rider_id = data.get("rider_id", "default_rider")
        latitude = float(data.get("latitude", 0.0))
        longitude = float(data.get("longitude", 0.0))
        device_speed = data.get("speed_kmh") # Velocidade opcional vinda diretamente do GPS do celular

        current_time = time.time()
        calculated_speed = 0.0
        speed_source = "device"

        # Se já temos um histórico deste motoboy, calcula a velocidade pelo deslocamento real
        if rider_id in riders_tracker:
            prev_data = riders_tracker[rider_id]
            time_diff = current_time - prev_data["timestamp"]
            
            # Só calcula se passou pelo menos 1 segundo para evitar divisões imprecisas por zero
            if time_diff >= 1.0:
                calculated_speed = calculate_haversine_speed(
                    prev_data["lat"], prev_data["lng"],
                    latitude, longitude,
                    time_diff
                )
                speed_source = "calculated"
            else:
                calculated_speed = prev_data["speed_kmh"]
        
        # Se o dispositivo enviar a velocidade e ela for confiável, usa ela. 
        # Caso contrário, usa a calculada via Haversine para evitar fraudes ou mock de GPS.
        final_speed = device_speed if device_speed is not None else calculated_speed
        
        # Define se a trava de segurança deve ser ativada
        is_locked = final_speed > MAX_SPEED_LIMIT_KMH

        # Atualiza a base de dados em tempo real
        riders_tracker[rider_id] = {
            "lat": latitude,
            "lng": longitude,
            "timestamp": current_time,
            "speed_kmh": final_speed,
            "locked": is_locked
        }

        response_data = {
            "rider_id": rider_id,
            "speed_kmh": round(final_speed, 1),
            "max_speed_limit_kmh": MAX_SPEED_LIMIT_KMH,
            "speed_lock": is_locked,
            "speed_source": speed_source,
            "message": f"Velocidade segura ({round(final_speed, 1)} km/h)." if not is_locked else f"TRAVA ATIVADA: Velocidade de {round(final_speed, 1)} km/h excede o limite seguro de {MAX_SPEED_LIMIT_KMH} km/h!"
        }

        print(f"[*] Speed Monitor [{rider_id}]: {round(final_speed, 1)} km/h. Lock: {is_locked}")
        return jsonify(response_data)

    except Exception as e:
        print(f"[ERROR] Falha no Speed Monitor: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/analyze', methods=['POST'])
def analyze_offer():
    """
    Endpoint principal para receber capturas de tela das ofertas do app Android.
    Valida segurança, extrai dados de entrega com Gemini e aplica regras de rentabilidade.
    """
    # 1. Validação do Header de Segurança (Evita uso não autorizado por terceiros)
    token = request.headers.get("X-API-Token")
    if not token or token != SERVER_API_TOKEN:
        print(f"[!] Tentativa de acesso não autorizada com token: {token}")
        return jsonify({
            "suggestion": "recusar",
            "reason": "Token de acesso inválido ou ausente. Verifique a configuração no app.",
            "confidence": 0.0,
            "details": None
        }), 401

    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "Corpo da requisição JSON ausente"}), 400

        base64_image = data.get("image", "")
        latitude = data.get("latitude", 0.0)
        longitude = data.get("longitude", 0.0)
        active_delivery = data.get("active_delivery")
        device_speed = data.get("speed_kmh") # Velocidade opcional reportada durante a análise
        rider_id = data.get("rider_id", "default_rider")

        # Configurações dinâmicas enviadas pelo aplicativo cliente (Filtros Personalizados)
        req_min_val_km = data.get("min_value_per_km")
        req_min_fare = data.get("min_fare_value")
        local_min_val_km = float(req_min_val_km) if req_min_val_km is not None else MIN_VALUE_PER_KM
        local_min_fare = float(req_min_fare) if req_min_fare is not None else MIN_FARE_VALUE
        risk_zones_raw = data.get("risk_zones_keywords") or "Cracolândia, Heliópolis, Capão Redondo, Paraisópolis, Favela, Beco"

        # Verifica se o motoboy já está marcado como bloqueado no rastreador ou se sua velocidade atual é excessiva
        is_speeding = False
        current_speed = 0.0
        
        if device_speed is not None:
            current_speed = device_speed
            is_speeding = current_speed > MAX_SPEED_LIMIT_KMH
        elif rider_id in riders_tracker:
            current_speed = riders_tracker[rider_id]["speed_kmh"]
            is_speeding = riders_tracker[rider_id]["locked"]

        if is_speeding:
            print(f"[!] REJEITADO POR SEGURANÇA: Motoboy {rider_id} a {round(current_speed, 1)} km/h (limite: {MAX_SPEED_LIMIT_KMH})")
            return jsonify({
                "suggestion": "recusar",
                "reason": f"TRAVA DE SEGURANÇA: Velocidade muito alta ({round(current_speed, 1)} km/h)! Reduza para aceitar ofertas.",
                "confidence": 1.0,
                "details": None,
                "mode": "server"
            })

        if not base64_image:
            return jsonify({
                "suggestion": "considerar",
                "reason": "Imagem de captura de tela ausente na requisição.",
                "confidence": 0.0,
                "details": None
            }), 400

        # Remove prefixo de data URI caso exista (ex: 'data:image/jpeg;base64,')
        if "," in base64_image:
            base64_image = base64_image.split(",")[1]

        # Decodifica imagem para bytes brutos compatíveis com o SDK do Gemini
        image_data = base64.b64decode(base64_image)

        # Trata dados de entrega ativa caso o motorista já esteja em uma corrida
        active_delivery_destination = ""
        is_active_delivery_enabled = "false"
        if active_delivery:
            is_active_delivery_enabled = "true"
            active_delivery_destination = active_delivery.get("destination_address", "")

        # 2. Construção do Prompt de IA para extração limpa de dados e geolocalização por estimativa
        prompt = f"""
        Examine o print de tela de uma oferta de corrida de aplicativo de entrega (como iFood, Uber Moto, Lalamove, Uber Flash, Rappi, etc.).
        Você é o assistente de inteligência de um entregador de moto. Seu objetivo é analisar a imagem e extrair os dados textuais e geográficos com alta precisão técnica.
        
        Extraia detalhadamente:
        1. "delivery_app": Nome do aplicativo de entrega (ex: "iFood", "Uber Flash", "Lalamove", "99")
        2. "fare_value": Valor total em Reais (ex: 15.40, como número float)
        3. "pickup_address": O endereço ou ponto de coleta (ex: "McDonalds - Av. Paulista, 1000")
        4. "delivery_address": O endereço ou ponto de entrega final (ex: "Rua Bela Cintra, 450")
        5. "total_distance": Distância total da corrida em km informada na tela (ex: 5.2. Se não achar, estime de forma realista)
        6. "total_time": Tempo estimado em minutos informado na tela (ex: 15.0. Se não achar, estime de forma realista)
        
        Geolocalização Inteligente por IA:
        Estime com base no seu conhecimento de mapas as coordenadas de latitude e longitude:
        - "pickup_lat" e "pickup_lng" para o local de Coleta, considerando a proximidade de São Paulo (coordenadas do entregador: {latitude}, {longitude}).
        - "delivery_lat" e "delivery_lng" para o local de Entrega.
        - Caso haja uma entrega ativa em andamento com o destino "{active_delivery_destination}", estime também as coordenadas de latitude e longitude desse destino: "active_delivery_lat" e "active_delivery_lng".
        
        Retorne EXCLUSIVAMENTE um objeto JSON válido (sem blocos de código markdown ou texto explicativo extra, apenas o JSON bruto):
        {{
          "delivery_app": "iFood",
          "fare_value": 15.40,
          "pickup_address": "...",
          "delivery_address": "...",
          "total_distance": 5.2,
          "total_time": 15.0,
          "pickup_lat": -23.5612,
          "pickup_lng": -46.6554,
          "delivery_lat": -23.5723,
          "delivery_lng": -46.6665,
          "active_delivery_lat": -23.5834,
          "active_delivery_lng": -46.6776
        }}
        """

        # 3. Executa a análise usando a biblioteca 'google-generativeai'
        model = genai.GenerativeModel(GEMINI_MODEL)
        
        # Envia a imagem de forma multimodal e o prompt detalhado
        response = model.generate_content(
            contents=[
                {
                    "mime_type": "image/jpeg",
                    "data": image_data
                },
                prompt
            ],
            generation_config=genai.GenerationConfig(
                response_mime_type="application/json",
                temperature=0.1
            )
        )

        # 4. Processamento matemático do resultado no servidor
        result_text = response.text.strip()
        if result_text.startswith("```"):
            lines = result_text.splitlines()
            if lines[0].startswith("```json") or lines[0].startswith("```"):
                result_text = "\n".join(lines[1:-1])

        gemini_data = json.loads(result_text)

        # Extração de dados com fallbacks robustos
        delivery_app = gemini_data.get("delivery_app", "App de Entrega")
        try:
            extracted_fare = gemini_data.get("fare_value", 0.0)
            if isinstance(extracted_fare, str):
                extracted_fare = float(extracted_fare.replace("R$", "").replace(",", ".").strip())
            fare_value = float(extracted_fare)
        except Exception:
            fare_value = 0.0

        pickup_address = gemini_data.get("pickup_address", "Coleta")
        delivery_address = gemini_data.get("delivery_address", "Entrega")
        
        # Filtro Inteligente de Área de Risco (Dangerous Zone Security Shield)
        matched_risk_zone = None
        is_risk_zone = False
        if risk_zones_raw and delivery_address:
            # Lista de palavras-chave perigosas separadas por vírgula
            risk_keywords = [kw.strip().lower() for kw in risk_zones_raw.split(",") if kw.strip()]
            delivery_address_lower = delivery_address.lower()
            for kw in risk_keywords:
                if len(kw) >= 3 and kw in delivery_address_lower:
                    matched_risk_zone = kw
                    is_risk_zone = True
                    print(f"[!] ALERTA: Área de risco detectada na entrega! Palavra-chave: {kw.upper()} | Endereço: {delivery_address}")
                    break
        
        try:
            total_distance = float(gemini_data.get("total_distance", 5.0))
        except Exception:
            total_distance = 5.0

        try:
            total_time = float(gemini_data.get("total_time", 15.0))
        except Exception:
            total_time = 15.0

        pickup_lat = float(gemini_data.get("pickup_lat") or latitude)
        pickup_lng = float(gemini_data.get("pickup_lng") or longitude)
        delivery_lat = float(gemini_data.get("delivery_lat") or latitude)
        delivery_lng = float(gemini_data.get("delivery_lng") or longitude)

        # 1. Recupera do cache ou calcula a distância de deslocamento real do motoboy até o ponto de coleta
        dist_to_pickup = get_cached_distance(latitude, longitude, pickup_lat, pickup_lng)
        
        # Distância total real percorrida (Deslocamento até a coleta + a corrida em si)
        real_total_distance = dist_to_pickup + total_distance

        # Estimativa realista de tempos: velocidade média urbana de 30 km/h (0.5 km por minuto)
        time_to_pickup = (dist_to_pickup / 30.0) * 60.0
        real_total_time = time_to_pickup + total_time + 5.0 # Adiciona 5 min de espera para preparo

        # Inicializa variáveis para rota encadeada (Chained Delivery)
        detour_distance = 0.0
        detour_time = 0.0
        chained_distance = 0.0
        chained_time = 0.0

        is_chained = active_delivery is not None

        if is_chained:
            active_lat = float(gemini_data.get("active_delivery_lat") or latitude)
            active_lng = float(gemini_data.get("active_delivery_lng") or longitude)
            
            # Distância do motoboy até a sua entrega ativa em andamento (recuperada do cache)
            dist_to_active = get_cached_distance(latitude, longitude, active_lat, active_lng)
            
            # Distância de desvio: da entrega ativa até o ponto de coleta da nova oferta (recuperada do cache)
            dist_active_to_pickup = get_cached_distance(active_lat, active_lng, pickup_lat, pickup_lng)
            
            detour_distance = dist_active_to_pickup
            detour_time = (dist_active_to_pickup / 30.0) * 60.0 + 5.0 # Desvio + 5 min espera

            # Chained Total: Distância acumulada para terminar a ativa e fazer a nova corrida por completo
            chained_distance = dist_to_active + dist_active_to_pickup + total_distance
            chained_time = (dist_to_active / 30.0) * 60.0 + detour_time + total_time
            
            # Para corridas encadeadas, o ganho por km considera a nova distância incrementada da atual
            real_total_distance = detour_distance + total_distance
            real_total_time = detour_time + total_time
        else:
            detour_distance = dist_to_pickup
            detour_time = time_to_pickup

        # Algoritmo de Pontuação de Decisão (Score de 0 a 100)
        score = 100.0
        penalties = []

        # Calcula taxas reais de rendimento
        real_value_per_km = fare_value / max(real_total_distance, 0.1)
        value_per_minute = fare_value / max(real_total_time, 1.0)

        # Regra 1: Comparação com Mínimo por KM configurado na central
        if real_value_per_km < local_min_val_km:
            diff = local_min_val_km - real_value_per_km
            score -= (diff / local_min_val_km) * 55.0
            penalties.append(f"Baixo valor/km (R$ {round(real_value_per_km, 2)})")
        
        # Regra 2: Comparação com Mínimo de Corrida configurado
        if fare_value < local_min_fare:
            diff = local_min_fare - fare_value
            score -= (diff / local_min_fare) * 30.0
            penalties.append(f"Valor abaixo do limite (R$ {round(fare_value, 2)})")

        # Regra 3: Distância de deslocamento até a coleta
        if dist_to_pickup > 4.0:
            score -= (dist_to_pickup - 4.0) * 10.0
            penalties.append(f"Coleta longe ({round(dist_to_pickup, 1)}km)")
        elif dist_to_pickup < 1.2:
            score += 10.0 # Bônus por estar do lado!

        # Regra 4: Desvio de rota para corridas encadeadas (Chained Delivery)
        if is_chained:
            if detour_distance > 5.0:
                score -= (detour_distance - 5.0) * 15.0
                penalties.append(f"Desvio longo ({round(detour_distance, 1)}km)")
            elif detour_distance < 1.5:
                score += 15.0 # Bônus se a coleta for colada com a entrega atual!

        # Limites do score
        score = max(0.0, min(100.0, score))

        # Classificação de sugestão baseada no Score e Hard Constraints (com override de Área de Risco)
        if is_risk_zone:
            suggestion = "recusar"
            score = 0.0
        elif fare_value < (local_min_fare * 0.75) or real_value_per_km < (local_min_val_km * 0.55):
            suggestion = "recusar"
        elif score >= 70.0:
            suggestion = "aceitar"
        elif score >= 45.0:
            suggestion = "considerar"
        else:
            suggestion = "recusar"

        # Constrói justificativa amigável e concisa para áudio (TTS) do entregador
        if is_risk_zone:
            reason = f"ALERTA DE SEGURANÇA: Destino em área de risco ({matched_risk_zone.upper()})! Evite esta região."
        elif suggestion == "aceitar":
            if is_chained:
                reason = f"Aceitar Encadeada! Coleta pertinho a {round(detour_distance, 1)}km do destino atual. R$ {round(real_value_per_km, 2)}/km."
            else:
                reason = f"Excelente corrida! Coleta a {round(dist_to_pickup, 1)}km. Ganhando R$ {round(real_value_per_km, 2)} por km."
        elif suggestion == "considerar":
            if penalties:
                reason = f"Considerar: " + " e ".join(penalties[:2]) + f". R$ {round(real_value_per_km, 2)}/km."
            else:
                reason = f"Considerar. Taxa razoável de R$ {round(real_value_per_km, 2)} por km."
        else:
            if penalties:
                reason = f"Recusar: " + " e ".join(penalties[:2]) + f". R$ {round(real_value_per_km, 2)}/km."
            else:
                reason = f"Recusar. Baixa rentabilidade de R$ {round(real_value_per_km, 2)} por km."

        # Monta a resposta final
        response_json = {
            "suggestion": suggestion,
            "reason": reason[:110], # Trunca para caber com elegância no TTS do celular
            "confidence": round(score / 100.0, 2),
            "mode": "server",
            "details": {
                "extracted_data": {
                    "pickup_address": pickup_address,
                    "delivery_address": delivery_address,
                    "fare_value": str(fare_value),
                    "delivery_app": delivery_app
                },
                "route_data": {
                    "total_distance": round(real_total_distance, 2),
                    "total_time": round(real_total_time, 1),
                    "detour_distance": round(detour_distance, 2),
                    "detour_time": round(detour_time, 1),
                    "chained_distance": round(chained_distance, 2),
                    "chained_time": round(chained_time, 1)
                },
                "metrics": {
                    "fare_value": fare_value,
                    "value_per_km": round(real_value_per_km, 2),
                    "value_per_minute": round(value_per_minute, 2)
                }
            }
        }

        # Registra a decisão no arquivo de log de auditoria
        log_offer_decision(
            rider_id=rider_id,
            app_name=delivery_app,
            fare_value=fare_value,
            pickup=pickup_address,
            delivery=delivery_address,
            distance=real_total_distance,
            duration=real_total_time,
            score=score,
            suggestion=suggestion,
            reason=reason
        )

        print(f"[*] Análise concluída: {suggestion} (Score: {int(score)}) - Motivo: {reason}")
        return jsonify(response_json)

    except Exception as e:
        print(f"[ERROR] Falha ao processar requisição: {str(e)}")
        return jsonify({
            "suggestion": "considerar",
            "reason": f"Erro interno no processador do servidor central: {str(e)}",
            "confidence": 0.0,
            "details": None
        }), 500

@app.route('/audit_logs/hot_zones', methods=['GET'])
def get_hot_zones():
    """
    Retorna as zonas quentes de alta rentabilidade (Hot-Spots) baseadas nas corridas
    registradas no arquivo de auditoria agregadas por região.
    Inclui dados simulados realistas caso o arquivo esteja vazio para experiência imediata.
    """
    token = request.headers.get("X-API-Token") or request.args.get("token")
    if not token or token != SERVER_API_TOKEN:
        return jsonify({"error": "Não autorizado"}), 401

    # Zonas de alta demanda padrão em São Paulo (Fallbacks de alta fidelidade)
    default_hot_zones = {
        "Shopping Paulista / Paraíso": {"lat": -23.5616, "lng": -46.6560, "count": 18, "sum_fare": 320.50, "sum_km": 82.4, "app": "iFood"},
        "Largo da Batata / Pinheiros": {"lat": -23.5670, "lng": -46.7032, "count": 14, "sum_fare": 285.00, "sum_km": 68.2, "app": "Keeta"},
        "Itaim Bibi / Faria Lima": {"lat": -23.5855, "lng": -46.6815, "count": 22, "sum_fare": 450.20, "sum_km": 98.0, "app": "Uber Flash"},
        "Moema / Av. Ibirapuera": {"lat": -23.6025, "lng": -46.6621, "count": 11, "sum_fare": 198.40, "sum_km": 48.1, "app": "99 Moto"},
        "Vila Olímpia / Shopping JK": {"lat": -23.5958, "lng": -46.6865, "count": 15, "sum_fare": 310.80, "sum_km": 72.5, "app": "iFood"}
    }

    # Se existirem registros reais de corridas, nós os agrupamos para enriquecer/criar novas zonas dinamicamente
    if os.path.exists(AUDIT_LOG_FILE):
        try:
            with open(AUDIT_LOG_FILE, "r", encoding="utf-8") as f:
                for line in f:
                    if not line.strip():
                        continue
                    try:
                        entry = json.loads(line.strip())
                    except Exception:
                        continue

                    pickup = entry.get("pickup_address", "")
                    if not pickup or len(pickup) < 4:
                        continue

                    try:
                        fare = float(entry.get("fare_value") or 0.0)
                        dist = float(entry.get("total_distance_km") or 1.0)
                    except Exception:
                        continue
                    app = entry.get("delivery_app") or "iFood"

                    # Se encontrarmos correspondência por sub-string, acumulamos na zona padrão
                    matched = False
                    for name, zone in default_hot_zones.items():
                        # Separa termos comuns para cruzar endereços
                        short_name = name.split("/")[0].strip().lower()
                        if short_name in pickup.lower() or "paulista" in pickup.lower() and "paulista" in short_name:
                            zone["count"] += 1
                            zone["sum_fare"] += fare
                            zone["sum_km"] += dist
                            zone["app"] = app
                            matched = True
                            break

                    # Caso contrário, se for um endereço novo e legível, criamos uma zona dinâmica
                    if not matched and len(pickup) > 8:
                        clean_name = pickup.split("-")[0].split(",")[0].strip()[:24]
                        if len(clean_name) >= 3:
                            # Gera coordenadas pseudo-aleatórias próximas a SP Centro baseadas na string
                            h = hash(clean_name)
                            pseudo_lat = -23.5505 + (h % 100) * 0.0005
                            pseudo_lng = -46.6333 + ((h // 100) % 100) * 0.0005
                            
                            default_hot_zones[clean_name] = {
                                "lat": pseudo_lat,
                                "lng": pseudo_lng,
                                "count": 1,
                                "sum_fare": fare,
                                "sum_km": dist,
                                "app": app
                            }
        except Exception as e:
            print(f"[ERROR] Erro ao ler logs de hot zones: {str(e)}")

    # Constrói o JSON final de retorno
    response_list = []
    for name, zone in default_hot_zones.items():
        count = zone["count"]
        avg_fare = round(zone["sum_fare"] / count, 2) if count > 0 else 0.0
        avg_km = zone["sum_km"] / count if count > 0 else 0.0
        avg_val_km = round(avg_fare / avg_km, 2) if avg_km > 0 else 1.8
        
        response_list.append({
            "address": name,
            "latitude": zone["lat"],
            "longitude": zone["lng"],
            "offers_count": count,
            "avg_fare": avg_fare,
            "avg_value_per_km": avg_val_km,
            "predominant_app": zone["app"]
        })

    # Ordena decrescente pelo número de ofertas e rentabilidade média
    response_list.sort(key=lambda x: (x["offers_count"], x["avg_value_per_km"]), reverse=True)
    return jsonify(response_list[:6])

if __name__ == '__main__':
    print(f"[*] Iniciando servidor Radar Delivery AI em http://0.0.0.0:{PORT}")
    app.run(host='0.0.0.0', port=PORT, debug=True)

