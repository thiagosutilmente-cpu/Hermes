# -*- coding: utf-8 -*-
# DOCKER COMPOSE:
# version: '3.8'
# services:
#   radar:
#     build: .
#     ports:
#       - "5000:5000"
#     volumes:
#       - ./data:/app/data
#     restart: always

"""
Radar Coordinator — Jarvis Neural Cockpit para Entregadores Brasileiros
Aplicação Web Fullstack autocontida em arquivo único (Flask + SQLite + SPA Vanilla HTML5/CSS3/JS)
"""

import os
import sys
import json
import sqlite3
import datetime
import threading
import urllib.parse
from http.server import HTTPServer, ThreadingHTTPServer, BaseHTTPRequestHandler

# Suporte opcional a Flask se instalado; fallback automático para servidor padrão Python (Zero dependências externas)
try:
    from flask import Flask, request as flask_request, jsonify as flask_jsonify, Response as FlaskResponse
    FLASK_AVAILABLE = True
except ImportError:
    FLASK_AVAILABLE = False

# ==============================================================================
# 1. BACKEND & BANCO DE DADOS LOCAL SQLITE
# ==============================================================================

DATABASE_FILE = "radar_cockpit.db"

def get_db():
    conn = sqlite3.connect(DATABASE_FILE)
    conn.row_factory = sqlite3.Row
    return conn

def init_database():
    """Inicializa as 4 tabelas obrigatórias e popula dados mockados no startup."""
    conn = get_db()
    cur = conn.cursor()

    # Tabela 1: users (id, name, email, phone, plan, created_at)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            email TEXT NOT NULL,
            phone TEXT,
            plan TEXT DEFAULT 'pro',
            created_at TEXT NOT NULL
        )
    """)

    # Tabela 2: stacks (id, apps, restaurant, total_value, distance_km, time_min, status, created_at)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS stacks (
            id TEXT PRIMARY KEY,
            apps TEXT NOT NULL,
            restaurant TEXT NOT NULL,
            total_value REAL NOT NULL,
            distance_km REAL NOT NULL,
            time_min INTEGER NOT NULL,
            status TEXT DEFAULT 'pending',
            created_at TEXT NOT NULL
        )
    """)

    # Tabela 3: earnings (id, user_id, amount, date, app_source, km_driven)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS earnings (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id TEXT NOT NULL,
            amount REAL NOT NULL,
            date TEXT NOT NULL,
            app_source TEXT NOT NULL,
            km_driven REAL NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users(id)
        )
    """)

    # Tabela 4: health_logs (id, score, gps_accuracy, latency_ms, temperature, created_at)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS health_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            score INTEGER NOT NULL,
            gps_accuracy REAL NOT NULL,
            latency_ms INTEGER NOT NULL,
            temperature REAL NOT NULL,
            created_at TEXT NOT NULL
        )
    """)
    conn.commit()

    # Criação do usuário padrão se não existir
    cur.execute("SELECT id FROM users WHERE id = 'usr_thiago_01'")
    if not cur.fetchone():
        cur.execute("""
            INSERT INTO users (id, name, email, phone, plan, created_at)
            VALUES ('usr_thiago_01', 'Thiago Sutil', 'thiagosutilmente@gmail.com', '(11) 98765-4321', 'pro', datetime('now'))
        """)
        conn.commit()

    # Criação dos 8 Stacks Mockados no startup
    cur.execute("SELECT COUNT(*) as count FROM stacks")
    if cur.fetchone()["count"] == 0:
        now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        mock_stacks = [
            ("stk_01", "iFood + Rappi", "Burger King Paulista & Pizza Hut Jardins", 33.00, 4.2, 18, "pending", now_str),
            ("stk_02", "iFood", "McDonald's Henrique Schaumann", 15.00, 2.8, 12, "pending", now_str),
            ("stk_03", "Rappi", "Starbucks Frei Caneca", 18.00, 3.1, 14, "pending", now_str),
            ("stk_04", "99Food + Uber", "Habib's Rebouças & Subway Pinheiros", 27.50, 3.9, 19, "pending", now_str),
            ("stk_05", "iFood", "Outback Center 3", 22.00, 3.2, 16, "pending", now_str),
            ("stk_06", "Rappi + iFood", "Madero Vila Olímpia & Bacio di Latte", 36.00, 4.8, 21, "pending", now_str),
            ("stk_07", "Uber", "Bullguer Jardins", 19.50, 2.5, 11, "pending", now_str),
            ("stk_08", "iFood + 99", "Sukiya Liberdade & Ragazzo Aclimação", 31.00, 4.5, 20, "pending", now_str)
        ]
        cur.executemany("""
            INSERT INTO stacks (id, apps, restaurant, total_value, distance_km, time_min, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, mock_stacks)
        conn.commit()

    # Criação de 30 Dias de Earnings Mockados
    cur.execute("SELECT COUNT(*) as count FROM earnings")
    if cur.fetchone()["count"] == 0:
        base_date = datetime.date.today()
        earnings_rows = []
        apps_cycle = ["iFood", "Rappi", "Uber", "99", "iFood + Rappi"]
        for day_back in range(30, -1, -1):
            day_dt = base_date - datetime.timedelta(days=day_back)
            day_str = day_dt.strftime("%Y-%m-%d")
            # 4 corridas diárias simulando entregador dedicado
            runs = [
                (24.50, 3.8, apps_cycle[day_back % 5]),
                (33.00, 4.2, "iFood + Rappi"),
                (18.00, 2.9, apps_cycle[(day_back + 1) % 5]),
                (28.50, 4.0, apps_cycle[(day_back + 2) % 5])
            ]
            if day_back == 0:
                # Dia de hoje totalizando exatamente R$ 284,50
                runs = [
                    (33.00, 4.2, "iFood + Rappi"),
                    (54.00, 7.8, "iFood + Rappi"),
                    (42.50, 5.5, "Rappi"),
                    (65.00, 8.2, "99 + Uber"),
                    (90.00, 12.5, "iFood + Rappi")
                ]
            for amt, km, src in runs:
                earnings_rows.append(("usr_thiago_01", amt, day_str, src, km))

        cur.executemany("""
            INSERT INTO earnings (user_id, amount, date, app_source, km_driven)
            VALUES (?, ?, ?, ?, ?)
        """, earnings_rows)
        conn.commit()

    # Inserção de Health Log Inicial (score 94, gps 4.2, latency 12, temp 28)
    cur.execute("SELECT COUNT(*) as count FROM health_logs")
    if cur.fetchone()["count"] == 0:
        cur.execute("""
            INSERT INTO health_logs (score, gps_accuracy, latency_ms, temperature, created_at)
            VALUES (94, 4.2, 12, 28.0, datetime('now'))
        """)
        conn.commit()

    conn.close()

# ==============================================================================
# FLASK APP SETUP & API REST (COM FALLBACK AUTOMÁTICO PARA HTTP.SERVER PADRÃO)
# ==============================================================================

def get_stacks_data(status="pending"):
    conn = get_db()
    if status == "all":
        rows = conn.execute("SELECT * FROM stacks ORDER BY created_at DESC").fetchall()
    else:
        rows = conn.execute("SELECT * FROM stacks WHERE status = ? ORDER BY total_value DESC", (status,)).fetchall()
    conn.close()
    return [dict(r) for r in rows]

def accept_stack_data(stack_id):
    if not stack_id:
        return {"error": "Parâmetro stack_id é obrigatório"}, 400
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM stacks WHERE id = ?", (stack_id,))
    stack = cur.fetchone()
    if not stack:
        conn.close()
        return {"error": "Stack não encontrado"}, 404
    cur.execute("UPDATE stacks SET status = 'accepted' WHERE id = ?", (stack_id,))
    today_str = datetime.date.today().strftime("%Y-%m-%d")
    cur.execute("""
        INSERT INTO earnings (user_id, amount, date, app_source, km_driven)
        VALUES ('usr_thiago_01', ?, ?, ?, ?)
    """, (stack["total_value"], today_str, stack["apps"], stack["distance_km"]))
    conn.commit()
    conn.close()
    return {
        "success": True,
        "message": f"Stack {stack_id} aceito com sucesso!",
        "stack_id": stack_id,
        "amount": stack["total_value"]
    }, 200

def decline_stack_data(stack_id):
    if not stack_id:
        return {"error": "Parâmetro stack_id é obrigatório"}, 400
    conn = get_db()
    conn.execute("UPDATE stacks SET status = 'declined' WHERE id = ?", (stack_id,))
    conn.commit()
    conn.close()
    return {"success": True, "message": f"Stack {stack_id} recusado com sucesso"}, 200

def get_earnings_data():
    conn = get_db()
    today = datetime.date.today()
    today_str = today.strftime("%Y-%m-%d")
    week_ago = (today - datetime.timedelta(days=7)).strftime("%Y-%m-%d")
    month_ago = (today - datetime.timedelta(days=30)).strftime("%Y-%m-%d")

    r_today = conn.execute("""
        SELECT COALESCE(SUM(amount), 0) as total, COALESCE(SUM(km_driven), 0) as total_km
        FROM earnings WHERE date = ?
    """, (today_str,)).fetchone()

    r_week = conn.execute("""
        SELECT COALESCE(SUM(amount), 0) as total, COALESCE(SUM(km_driven), 0) as total_km
        FROM earnings WHERE date >= ?
    """, (week_ago,)).fetchone()

    r_month = conn.execute("""
        SELECT COALESCE(SUM(amount), 0) as total, COALESCE(SUM(km_driven), 0) as total_km
        FROM earnings WHERE date >= ?
    """, (month_ago,)).fetchone()

    daily_rows = conn.execute("""
        SELECT date, SUM(amount) as daily_total, SUM(km_driven) as daily_km
        FROM earnings WHERE date >= ?
        GROUP BY date ORDER BY date ASC
    """, (week_ago,)).fetchall()
    conn.close()

    today_val = round(float(r_today["total"]) if r_today["total"] > 0 else 284.50, 2)
    today_km = round(float(r_today["total_km"]) if r_today["total_km"] > 0 else 38.2, 1)
    week_val = round(float(r_week["total"]) if r_week["total"] > 0 else 1420.80, 2)
    week_km = round(float(r_week["total_km"]) if r_week["total_km"] > 0 else 184.0, 1)
    month_val = round(float(r_month["total"]) if r_month["total"] > 0 else 5680.00, 2)
    month_km = round(float(r_month["total_km"]) if r_month["total_km"] > 0 else 760.5, 1)
    profit = round(today_val * 0.77, 2)

    chart_7d = []
    for r in daily_rows:
        dt = datetime.datetime.strptime(r["date"], "%Y-%m-%d")
        chart_7d.append({
            "date": r["date"],
            "short_date": dt.strftime("%d/%m"),
            "amount": round(float(r["daily_total"]), 2),
            "km": round(float(r["daily_km"]), 1)
        })

    return {
        "today": today_val,
        "todayKm": today_km,
        "week": week_val,
        "weekKm": week_km,
        "month": month_val,
        "monthKm": month_km,
        "profit": profit,
        "totalKm": today_km,
        "chart_7d": chart_7d
    }

def get_health_data():
    conn = get_db()
    row = conn.execute("SELECT * FROM health_logs ORDER BY id DESC LIMIT 1").fetchone()
    conn.close()
    if row:
        return dict(row)
    return {
        "score": 94,
        "gps_accuracy": 4.2,
        "latency_ms": 12,
        "temperature": 28.0,
        "created_at": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }

def evaluate_decision_data(data):
    try:
        val = float(data.get("value", 0))
        dist = float(data.get("distance", 1))
        app_name = data.get("app", "iFood")
        user_id = data.get("user_id", "usr_thiago_01")
    except (ValueError, TypeError):
        return {"error": "Parâmetros 'value' e 'distance' numéricos são obrigatórios"}, 400

    if dist <= 0:
        dist = 0.5

    gain_per_km = round(val / dist, 2)
    if gain_per_km >= 5.0:
        decision = "accept"
        confidence = 0.95
        reason = "Ganho/km acima da média"
    elif gain_per_km >= 3.5 and dist <= 4.0:
        decision = "accept"
        confidence = 0.78
        reason = "Distância curta compensa"
    elif dist > 6.0:
        decision = "decline"
        confidence = 0.88
        reason = "Distância excessiva"
    else:
        decision = "decline"
        confidence = 0.65
        reason = "Ganho/km abaixo do ideal"

    return {
        "decision": decision,
        "confidence": confidence,
        "reason": reason,
        "gain_per_km": gain_per_km,
        "value": val,
        "distance": dist,
        "app": app_name
    }, 200

def get_manifest_json():
    return json.dumps({
        "name": "Radar Coordinator — Jarvis Neural Cockpit",
        "short_name": "Radar Jarvis",
        "start_url": "/#dashboard",
        "display": "standalone",
        "background_color": "#0a0a0f",
        "theme_color": "#0a0a0f",
        "icons": [
            {
                "src": "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><circle cx='50' cy='50' r='45' fill='%23111118' stroke='%2300ff88' stroke-width='6'/><circle cx='50' cy='50' r='14' fill='%2300ff88'/></svg>",
                "sizes": "192x192 512x512",
                "type": "image/svg+xml"
            }
        ]
    })

def get_sw_js():
    return """
    self.addEventListener('install', e => self.skipWaiting());
    self.addEventListener('activate', e => e.waitUntil(clients.claim()));
    self.addEventListener('fetch', e => e.respondWith(fetch(e.request).catch(() => caches.match(e.request))));
    """

if FLASK_AVAILABLE:
    app = Flask(__name__)
    @app.after_request
    def add_cors(resp):
        resp.headers["Access-Control-Allow-Origin"] = "*"
        resp.headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
        resp.headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization"
        resp.headers.pop("X-Frame-Options", None)
        return resp
    @app.route("/api/stacks", methods=["GET"])
    def f_stacks(): return flask_jsonify(get_stacks_data(flask_request.args.get("status", "pending")))
    @app.route("/api/stacks/accept", methods=["POST"])
    def f_acc():
        res, code = accept_stack_data((flask_request.get_json(silent=True) or {}).get("stack_id"))
        return flask_jsonify(res), code
    @app.route("/api/stacks/decline", methods=["POST"])
    def f_dec():
        res, code = decline_stack_data((flask_request.get_json(silent=True) or {}).get("stack_id"))
        return flask_jsonify(res), code
    @app.route("/api/earnings", methods=["GET"])
    def f_earn(): return flask_jsonify(get_earnings_data())
    @app.route("/api/health", methods=["GET"])
    def f_hlth(): return flask_jsonify(get_health_data())
    @app.route("/api/decision", methods=["POST"])
    def f_dcs():
        res, code = evaluate_decision_data(flask_request.get_json(silent=True) or {})
        return flask_jsonify(res), code
    @app.route("/manifest.json", methods=["GET"])
    def f_mnf(): return FlaskResponse(get_manifest_json(), mimetype="application/json")
    @app.route("/sw.js", methods=["GET"])
    def f_sw(): return FlaskResponse(get_sw_js(), mimetype="application/javascript")
    @app.route("/", methods=["GET"])
    def f_idx(): return HTML_CONTENT

class RadarHTTPHandler(BaseHTTPRequestHandler):
    def send_cors_headers(self, content_type="application/json"):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Content-Type", content_type)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_cors_headers()
        self.end_headers()

    def do_HEAD(self):
        self.send_response(200)
        self.send_cors_headers("text/html; charset=utf-8")
        self.end_headers()

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        qs = urllib.parse.parse_qs(parsed.query)

        if path == "/" or path == "/index.html":
            body = HTML_CONTENT.encode("utf-8")
            self.send_response(200)
            self.send_cors_headers("text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/manifest.json":
            body = get_manifest_json().encode("utf-8")
            self.send_response(200)
            self.send_cors_headers("application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/sw.js":
            body = get_sw_js().encode("utf-8")
            self.send_response(200)
            self.send_cors_headers("application/javascript")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/api/stacks":
            status = qs.get("status", ["pending"])[0]
            body = json.dumps(get_stacks_data(status)).encode("utf-8")
            self.send_response(200)
            self.send_cors_headers("application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/api/earnings":
            body = json.dumps(get_earnings_data()).encode("utf-8")
            self.send_response(200)
            self.send_cors_headers("application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/api/health":
            body = json.dumps(get_health_data()).encode("utf-8")
            self.send_response(200)
            self.send_cors_headers("application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/api/user":
            body = json.dumps({"id": "usr_thiago_01", "name": "Thiago Sutil", "email": "thiagosutilmente@gmail.com", "plan": "pro"}).encode("utf-8")
            self.send_response(200)
            self.send_cors_headers("application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        else:
            self.send_response(404)
            self.send_cors_headers()
            self.end_headers()
            self.wfile.write(b'{"error": "Not found"}')

    def do_POST(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        content_len = int(self.headers.get("Content-Length", 0))
        post_body = self.rfile.read(content_len) if content_len > 0 else b"{}"
        try:
            req_data = json.loads(post_body.decode("utf-8")) if post_body else {}
        except Exception:
            req_data = {}

        if path == "/api/stacks/accept":
            res, code = accept_stack_data(req_data.get("stack_id"))
            body = json.dumps(res).encode("utf-8")
            self.send_response(code)
            self.send_cors_headers("application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/api/stacks/decline":
            res, code = decline_stack_data(req_data.get("stack_id"))
            body = json.dumps(res).encode("utf-8")
            self.send_response(code)
            self.send_cors_headers("application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/api/decision":
            res, code = evaluate_decision_data(req_data)
            body = json.dumps(res).encode("utf-8")
            self.send_response(code)
            self.send_cors_headers("application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/api/user/plan":
            plan = req_data.get("plan", "pro")
            conn = get_db()
            conn.execute("UPDATE users SET plan = ? WHERE id = 'usr_thiago_01'", (plan,))
            conn.commit()
            conn.close()
            body = json.dumps({"success": True, "plan": plan}).encode("utf-8")
            self.send_response(200)
            self.send_cors_headers("application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        else:
            self.send_response(404)
            self.send_cors_headers()
            self.end_headers()
            self.wfile.write(b'{"error": "Not found"}')

    def log_message(self, format, *args):
        # Silencia logs repetitivos para melhor performance
        return

HTML_CONTENT = """<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="theme-color" content="#0a0a0f">
  <title>Radar Coordinator — Jarvis Neural Cockpit</title>
  <link rel="manifest" href="/manifest.json">
  <style>
    :root {
      --bg: #0a0a0f;
      --surface: #111118;
      --surface-card: rgba(22, 22, 32, 0.9);
      --surface-border: rgba(255, 255, 255, 0.08);
      --primary: #00ff88;
      --primary-glow: rgba(0, 255, 136, 0.4);
      --text: #f0f3f8;
      --text-muted: #8e95a5;
      --ifood: #ea1d2c;
      --rappi: #ff441f;
      --uber: #ffffff;
      --99: #f7c200;
      --danger: #ff4757;
      --font-stack: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      -webkit-tap-highlight-color: transparent;
      user-select: none;
    }

    body {
      background-color: var(--bg);
      color: var(--text);
      font-family: var(--font-stack);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      overflow-x: hidden;
      font-variant-numeric: tabular-nums;
    }

    .tabular { font-variant-numeric: tabular-nums; }
    .neon-text { color: var(--primary); text-shadow: 0 0 10px var(--primary-glow); }
    .glass {
      background: rgba(255, 255, 255, 0.05);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
      border: 1px solid var(--surface-border);
      border-radius: 16px;
    }

    .view-section {
      display: none !important;
      width: 100%;
      max-width: 900px;
      margin: 0 auto;
      padding: 16px;
      animation: fadeIn 0.3s ease-out;
    }
    .view-section.active {
      display: block !important;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(4px); }
      to { opacity: 1; transform: translateY(0); }
    }

    /* Animações CSS solicitadas */
    /* 1. Nós: box-shadow pulse 3s */
    @keyframes nodePulse {
      0% { box-shadow: 0 0 0 0 rgba(0, 255, 136, 0.7); }
      70% { box-shadow: 0 0 0 12px rgba(0, 255, 136, 0); }
      100% { box-shadow: 0 0 0 0 rgba(0, 255, 136, 0); }
    }
    /* 2. Fantasma: translateY float 3s */
    @keyframes ghostFloat {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-8px); }
    }
    /* 3. Barra ghost: width 0->83% 2.5s */
    @keyframes ghostBarGrow {
      from { width: 0%; }
      to { width: 83%; }
    }
    /* 4. Cards: slideIn translateX(30px)->0 0.5s */
    @keyframes slideInCard {
      from { transform: translateX(30px); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
    /* 5. Anel saúde: scale pulse 2.5s */
    @keyframes healthPulse {
      0% { transform: scale(1); }
      50% { transform: scale(1.08); filter: drop-shadow(0 0 6px var(--primary)); }
      100% { transform: scale(1); }
    }

    /* Top Bar */
    .top-bar {
      position: sticky;
      top: 0;
      z-index: 100;
      background: rgba(10, 10, 15, 0.95);
      backdrop-filter: blur(14px);
      border-bottom: 1px solid var(--surface-border);
      padding: 12px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .brand-box {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
    }
    .brand-logo {
      font-size: 24px;
      animation: nodePulse 3s infinite;
      border-radius: 50%;
    }
    .brand-name {
      font-size: 15px;
      font-weight: 800;
      color: #ffffff;
      letter-spacing: 0.5px;
    }
    .top-earning-box {
      text-align: right;
      cursor: pointer;
    }
    .top-earning-val {
      font-size: 20px;
      font-weight: 900;
      color: var(--primary);
    }
    .top-earning-lbl {
      font-size: 10px;
      color: var(--text-muted);
      text-transform: uppercase;
    }

    /* Status Bar: GPS 4.2m, Firebase Sync, 4 Apps */
    .status-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 8px;
      padding: 10px 14px;
      margin: 12px 0 16px;
      border-radius: 12px;
      background: rgba(17, 17, 24, 0.9);
      border: 1px solid var(--surface-border);
    }
    .status-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 11px;
      color: var(--text-muted);
    }
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--primary);
      box-shadow: 0 0 6px var(--primary);
      animation: nodePulse 2s infinite;
    }
    .apps-badges {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .app-dot {
      font-size: 9px;
      font-weight: 800;
      padding: 2px 7px;
      border-radius: 6px;
      text-transform: uppercase;
    }
    .app-ifood { background: rgba(234, 29, 44, 0.25); color: var(--ifood); border: 1px solid var(--ifood); }
    .app-rappi { background: rgba(255, 68, 31, 0.25); color: var(--rappi); border: 1px solid var(--rappi); }
    .app-uber { background: rgba(255, 255, 255, 0.15); color: var(--uber); border: 1px solid var(--uber); }
    .app-99 { background: rgba(247, 194, 0, 0.25); color: var(--99); border: 1px solid var(--99); }

    /* Constellation Map Radar */
    .constellation-map {
      position: relative;
      height: 250px;
      border-radius: 18px;
      overflow: hidden;
      margin-bottom: 18px;
      border: 1px solid rgba(0, 255, 136, 0.25);
      background: radial-gradient(circle at center, #151824 0%, #0a0a0f 85%);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .map-grid {
      position: absolute;
      width: 100%;
      height: 100%;
      top: 0;
      left: 0;
      pointer-events: none;
      background-image: radial-gradient(circle, rgba(0, 255, 136, 0.08) 1px, transparent 1px);
      background-size: 24px 24px;
    }
    .radar-sweep-line {
      position: absolute;
      width: 230px;
      height: 230px;
      border-radius: 50%;
      border: 1px dashed rgba(0, 255, 136, 0.3);
      animation: rotateSweep 7s linear infinite;
    }
    @keyframes rotateSweep {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
    .const-node {
      position: absolute;
      display: flex;
      flex-direction: column;
      align-items: center;
      cursor: pointer;
      transition: transform 0.2s;
    }
    .const-node:hover { transform: scale(1.18); z-index: 25; }
    .const-node-icon {
      font-size: 20px;
      background: rgba(17, 17, 24, 0.95);
      border-radius: 50%;
      padding: 6px;
      border: 1.5px solid var(--surface-border);
    }
    .const-node.pilot .const-node-icon {
      border-color: var(--primary);
      animation: nodePulse 2.5s infinite;
    }
    .const-node-lbl {
      font-size: 9px;
      font-weight: 700;
      background: rgba(10, 10, 15, 0.85);
      padding: 2px 6px;
      border-radius: 4px;
      margin-top: 3px;
      border: 0.5px solid var(--surface-border);
      white-space: nowrap;
    }

    /* Ghost Sequence */
    .ghost-card {
      background: linear-gradient(135deg, rgba(20, 24, 38, 0.92) 0%, rgba(12, 14, 22, 0.95) 100%);
      border: 1px solid rgba(0, 255, 136, 0.3);
      border-radius: 16px;
      padding: 14px 18px;
      margin-bottom: 18px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      box-shadow: 0 4px 20px rgba(0, 255, 136, 0.08);
    }
    .ghost-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .ghost-tag {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      font-weight: 800;
      color: #ffffff;
    }
    .ghost-symbol {
      font-size: 22px;
      animation: ghostFloat 3s ease-in-out infinite;
    }
    .ghost-stat {
      font-size: 12px;
      font-weight: 700;
      color: var(--primary);
    }
    .ghost-bar-track {
      width: 100%;
      height: 8px;
      background: rgba(255, 255, 255, 0.08);
      border-radius: 100px;
      overflow: hidden;
    }
    .ghost-bar-fill {
      height: 100%;
      width: 83%;
      background: linear-gradient(90deg, #00ff88, #00d2ff);
      border-radius: 100px;
      animation: ghostBarGrow 2.5s ease-out;
      box-shadow: 0 0 10px rgba(0, 255, 136, 0.6);
    }
    .ghost-text {
      font-size: 11px;
      color: var(--text-muted);
      line-height: 1.4;
    }

    /* Stack Cards (3 obrigatórios no Dashboard) */
    .stack-card {
      background: var(--surface-card);
      border: 1px solid var(--surface-border);
      border-radius: 16px;
      padding: 16px;
      margin-bottom: 14px;
      animation: slideInCard 0.5s ease-out;
      transition: all 0.25s;
    }
    .stack-card.multi {
      border-color: rgba(0, 255, 136, 0.35);
      background: linear-gradient(180deg, rgba(24, 28, 42, 0.95) 0%, rgba(15, 17, 26, 0.95) 100%);
    }
    .stack-card.accepted {
      border-color: var(--primary) !important;
      box-shadow: 0 0 24px rgba(0, 255, 136, 0.4) !important;
    }
    .stack-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 10px;
    }
    .stack-price {
      font-size: 24px;
      font-weight: 900;
      color: var(--primary);
    }
    .stack-gain-tag {
      font-size: 12px;
      font-weight: 700;
      color: #00ff88;
      background: rgba(0, 255, 136, 0.12);
      padding: 3px 8px;
      border-radius: 6px;
      display: inline-block;
    }
    .stack-route-display {
      background: rgba(0, 0, 0, 0.3);
      border-radius: 10px;
      padding: 10px 12px;
      margin: 12px 0;
      font-size: 11px;
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
    }
    .route-arrow { color: var(--primary); font-weight: 900; }
    .stack-btn-row {
      display: flex;
      gap: 10px;
      margin-top: 14px;
    }
    .btn {
      flex: 1;
      padding: 12px;
      border-radius: 12px;
      font-size: 13px;
      font-weight: 700;
      border: none;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      transition: all 0.2s;
    }
    .btn-green {
      background: var(--primary);
      color: #0a0a0f;
      box-shadow: 0 4px 14px rgba(0, 255, 136, 0.25);
    }
    .btn-green:hover { background: #1aff96; transform: translateY(-2px); }
    .btn-red {
      background: rgba(255, 71, 87, 0.12);
      color: var(--danger);
      border: 1px solid rgba(255, 71, 87, 0.3);
    }
    .btn-red:hover { background: rgba(255, 71, 87, 0.25); }

    /* Bottom Bar HUD */
    .bottom-hud {
      position: sticky;
      bottom: 0;
      z-index: 100;
      background: rgba(10, 10, 15, 0.95);
      backdrop-filter: blur(16px);
      border-top: 1px solid var(--surface-border);
      padding: 10px 16px 14px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
    }
    .health-badge {
      display: flex;
      align-items: center;
      gap: 8px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--surface-border);
      padding: 6px 10px;
      border-radius: 100px;
      cursor: pointer;
    }
    .health-ring-svg {
      width: 28px;
      height: 28px;
      animation: healthPulse 2.5s infinite;
    }
    .health-text {
      font-size: 13px;
      font-weight: 800;
      color: var(--primary);
    }
    .hud-sensors {
      display: flex;
      gap: 10px;
      font-size: 10px;
      color: var(--text-muted);
    }
    .hud-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .hud-btn {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--surface-border);
      color: var(--text);
      font-size: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.2s;
    }
    .hud-btn:hover, .hud-btn.active {
      background: var(--primary);
      color: #0a0a0f;
      border-color: var(--primary);
    }
    .btn-route-start {
      padding: 8px 14px;
      border-radius: 10px;
      background: linear-gradient(135deg, #00ff88, #00c6ff);
      color: #0a0a0f;
      font-size: 12px;
      font-weight: 800;
      border: none;
      cursor: pointer;
    }

    /* Gráficos CSS */
    .css-chart-wrap {
      display: flex;
      align-items: flex-end;
      gap: 10px;
      height: 140px;
      padding: 16px 0 6px;
      border-bottom: 1px solid var(--surface-border);
      margin-bottom: 14px;
    }
    .css-chart-bar {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      height: 100%;
      justify-content: flex-end;
      gap: 4px;
    }
    .css-bar-fill {
      width: 100%;
      max-width: 28px;
      background: linear-gradient(180deg, #00ff88 0%, rgba(0, 255, 136, 0.25) 100%);
      border-radius: 6px 6px 0 0;
      min-height: 8px;
      transition: height 0.6s ease;
    }
    .css-bar-lbl { font-size: 9px; color: var(--text-muted); }
    .css-bar-val { font-size: 9px; font-weight: 700; color: #ffffff; }

    /* Navegação Auxiliar */
    .nav-pills {
      display: flex;
      gap: 6px;
      overflow-x: auto;
      padding: 6px 0 14px;
      scrollbar-width: none;
    }
    .nav-pills::-webkit-scrollbar { display: none; }
    .nav-pill {
      padding: 8px 14px;
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--surface-border);
      border-radius: 100px;
      font-size: 12px;
      font-weight: 700;
      color: var(--text-muted);
      cursor: pointer;
      text-decoration: none;
      white-space: nowrap;
    }
    .nav-pill.active {
      background: var(--primary);
      color: #0a0a0f;
      border-color: var(--primary);
    }

    /* Cards de Planos */
    .plan-box {
      background: var(--surface-card);
      border: 1px solid var(--surface-border);
      border-radius: 18px;
      padding: 22px;
      margin-bottom: 16px;
      position: relative;
    }
    .plan-box.pro {
      border: 2px solid #ffd700;
      box-shadow: 0 0 24px rgba(255, 215, 0, 0.2);
    }
    .gold-badge {
      position: absolute;
      top: -12px;
      right: 20px;
      background: #ffd700;
      color: #0a0a0f;
      font-size: 10px;
      font-weight: 900;
      padding: 3px 10px;
      border-radius: 100px;
    }

    /* Toast de Voz Jarvis */
    #voice-toast {
      position: fixed;
      top: 60px;
      left: 50%;
      transform: translateX(-50%);
      background: rgba(10, 10, 15, 0.95);
      border: 1px solid var(--primary);
      color: #ffffff;
      padding: 8px 16px;
      border-radius: 100px;
      font-size: 12px;
      font-weight: 700;
      display: none;
      z-index: 200;
      box-shadow: 0 4px 20px rgba(0, 255, 136, 0.4);
    }
  </style>
</head>
<body>

  <!-- Toast Flutuante de Voz Jarvis -->
  <div id="voice-toast">🎙️ <span id="voice-toast-msg"></span></div>

  <!-- Top Bar -->
  <header class="top-bar">
    <div class="brand-box" onclick="location.hash='#dashboard'">
      <div class="brand-logo">🎯</div>
      <div>
        <div class="brand-name">RADAR COORDINATOR</div>
        <div style="font-size: 10px; color: var(--primary); font-weight: 700;">JARVIS NEURAL COCKPIT</div>
      </div>
    </div>
    <div class="top-earning-box" onclick="location.hash='#analytics'">
      <div class="top-earning-val tabular" id="top-ganho">R$ 284,50</div>
      <div class="top-earning-lbl">Ganhos de Hoje</div>
    </div>
  </header>

  <!-- #SPLASH (Logo 🎯 animada por 2s) -->
  <section id="splash" class="view-section">
    <div style="min-height: 75vh; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center;">
      <div class="brand-logo" style="font-size: 68px; margin-bottom: 20px;">🎯</div>
      <h1 style="font-size: 26px; font-weight: 900; color: #ffffff; margin-bottom: 6px;">RADAR COORDINATOR</h1>
      <p style="color: var(--primary); font-size: 14px; font-weight: 700; margin-bottom: 24px;">Jarvis Neural Cockpit</p>
      <button class="btn btn-green" style="max-width: 240px;" onclick="location.hash='#dashboard'">
        Entrar no Cockpit Agora ➔
      </button>
    </div>
  </section>

  <!-- #ONBOARDING (3 slides, só 1ª vez, salva no localStorage) -->
  <section id="onboarding" class="view-section">
    <div class="glass" style="padding: 24px; text-align: center; margin-top: 24px;">
      <div id="onboard-content">
        <div style="font-size: 52px; margin-bottom: 16px;">📦</div>
        <h2 style="font-size: 20px; font-weight: 800; color: #ffffff; margin-bottom: 8px;">Multi-App Stacking</h2>
        <p style="font-size: 13px; color: var(--text-muted); line-height: 1.5; margin-bottom: 24px;">
          Agrupe pedidos de iFood, Rappi, Uber e 99 na mesma rota sincronizada e aumente seu faturamento em até 70%.
        </p>
      </div>
      <div style="display: flex; justify-content: center; gap: 8px; margin-bottom: 20px;">
        <span class="status-dot"></span>
        <span class="status-dot" style="opacity: 0.3;"></span>
        <span class="status-dot" style="opacity: 0.3;"></span>
      </div>
      <div style="display: flex; gap: 10px;">
        <button class="btn btn-red" onclick="finishOnboarding()">Pular</button>
        <button class="btn btn-green" id="onboard-btn-next" onclick="nextOnboardSlide()">Avançar</button>
      </div>
    </div>
  </section>

  <!-- #AUTH (Login/Cadastro simulado) -->
  <section id="auth" class="view-section">
    <div class="glass" style="padding: 24px; max-width: 380px; margin: 30px auto;">
      <h2 style="font-size: 18px; font-weight: 800; color: #ffffff; margin-bottom: 6px;">Login do Piloto</h2>
      <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 16px;">Conecte seu terminal às contas dos aplicativos.</p>
      <div style="margin-bottom: 12px;">
        <label style="font-size: 11px; color: var(--text-muted); display: block; margin-bottom: 4px;">Nome</label>
        <input type="text" id="auth-name" class="glass" style="width: 100%; padding: 10px; color: #fff; font-size: 13px;" value="Thiago Sutil">
      </div>
      <div style="margin-bottom: 18px;">
        <label style="font-size: 11px; color: var(--text-muted); display: block; margin-bottom: 4px;">Email / WhatsApp</label>
        <input type="text" id="auth-email" class="glass" style="width: 100%; padding: 10px; color: #fff; font-size: 13px;" value="thiagosutilmente@gmail.com">
      </div>
      <button class="btn btn-green" style="width: 100%;" onclick="submitAuth()">Conectar Cockpit</button>
    </div>
  </section>

  <!-- #DASHBOARD (Cockpit Principal) -->
  <section id="dashboard" class="view-section">
    
    <!-- Navegação -->
    <div class="nav-pills">
      <a href="#dashboard" class="nav-pill active">Cockpit</a>
      <a href="#stacks" class="nav-pill">Stacks (8)</a>
      <a href="#analytics" class="nav-pill">Analytics</a>
      <a href="#subscription" class="nav-pill">Plano Pro</a>
      <a href="#settings" class="nav-pill">Ajustes</a>
      <a href="#admin" class="nav-pill">Admin</a>
    </div>

    <!-- Status: GPS 4.2m, Firebase Sync, 4 Apps (bolinhas pulsantes) -->
    <div class="status-row">
      <div class="status-badge">
        <span class="status-dot"></span>
        <span>GPS: <strong style="color:#ffffff;">4.2m</strong></span>
      </div>
      <div class="status-badge">
        <span>🔥 Firebase Sync: <strong style="color:var(--primary);">Ativo</strong></span>
      </div>
      <div class="apps-badges">
        <span class="app-dot app-ifood">iFood</span>
        <span class="app-dot app-rappi">Rappi</span>
        <span class="app-dot app-uber">Uber</span>
        <span class="app-dot app-99">99</span>
      </div>
    </div>

    <!-- Constellation Map: nós absolutos (🏍️ você, 🍔 BK, 🍕 PH, 🏠, 🏢, ☕ Starbucks) -->
    <div class="constellation-map">
      <div class="map-grid"></div>
      <div class="radar-sweep-line"></div>

      <!-- Nó: Você -->
      <div class="const-node pilot" style="top: 48%; left: 50%; transform: translate(-50%, -50%);">
        <div class="const-node-icon">🏍️</div>
        <div class="const-node-lbl" style="color: var(--primary);">VOCÊ</div>
      </div>

      <!-- Nós: BK, PH, Residência, Edifício, Starbucks -->
      <div class="const-node" style="top: 24%; left: 24%;" onclick="speak('Burger King Paulista. Coleta pronta.')">
        <div class="const-node-icon">🍔</div>
        <div class="const-node-lbl">BK Paulista</div>
      </div>
      <div class="const-node" style="top: 26%; left: 74%;" onclick="speak('Pizza Hut Jardins. Pedido embalado.')">
        <div class="const-node-icon">🍕</div>
        <div class="const-node-lbl">Pizza Hut</div>
      </div>
      <div class="const-node" style="top: 72%; left: 26%;" onclick="speak('Residência Apto 84.')">
        <div class="const-node-icon">🏠</div>
        <div class="const-node-lbl">Residência</div>
      </div>
      <div class="const-node" style="top: 70%; left: 76%;" onclick="speak('Edifício Comercial Faria Lima.')">
        <div class="const-node-icon">🏢</div>
        <div class="const-node-lbl">Edifício</div>
      </div>
      <div class="const-node" style="top: 12%; left: 50%;" onclick="speak('Starbucks Frei Caneca.')">
        <div class="const-node-icon">☕</div>
        <div class="const-node-lbl">Starbucks</div>
      </div>
    </div>

    <!-- Ghost Sequence: overlay com 👻, barra 83%, "83% chance de stack em 3 min" -->
    <div class="ghost-card">
      <div class="ghost-head">
        <div class="ghost-tag">
          <span class="ghost-symbol">👻</span>
          <span>GHOST SEQUENCE ATIVA</span>
        </div>
        <span class="ghost-stat">83% Chance</span>
      </div>
      <div class="ghost-bar-track">
        <div class="ghost-bar-fill"></div>
      </div>
      <div class="ghost-text">
        83% chance de stack em 3 min no raio de 800m. Sugestão: prossiga até a Av. Paulista.
      </div>
    </div>

    <!-- Calculadora Inteligente POST /api/decision -->
    <div class="glass" style="padding: 14px; margin-bottom: 18px;">
      <div style="font-size: 13px; font-weight: 700; color: var(--primary); margin-bottom: 10px;">
        🧠 Avaliador Neural Jarvis (/api/decision)
      </div>
      <div style="display: grid; grid-template-columns: 1fr 1fr auto; gap: 8px;">
        <input type="number" id="dec-val" class="glass" style="padding: 8px 10px; color: #fff; font-size: 12px;" placeholder="Valor (R$)" value="33.00">
        <input type="number" id="dec-km" class="glass" style="padding: 8px 10px; color: #fff; font-size: 12px;" placeholder="Distância (km)" value="4.2">
        <button class="btn btn-green" style="padding: 8px 14px; font-size: 11px;" onclick="testDecision()">Testar</button>
      </div>
      <div id="dec-res" style="display:none; margin-top: 10px; padding: 8px 10px; border-radius: 8px; font-size: 12px;"></div>
    </div>

    <!-- Stack Cards (3 cards pedidos especificamente):
         1. Multi-app iFood+Rappi = R$33, 4.2km, R$7.86/km, rota ●BK→●PH→🏠→🏢, botões ✅❌
         2. iFood solo R$15
         3. Rappi solo R$18 -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
      <h3 style="font-size: 14px; font-weight: 800; color: #ffffff;">Oportunidades em Destaque</h3>
      <a href="#stacks" style="font-size: 11px; color: var(--primary); text-decoration: none; font-weight: 700;">Ver todos ➔</a>
    </div>

    <div id="dash-stacks-container">
      <!-- Card 1: Multi-app iFood+Rappi -->
      <div class="stack-card multi" id="card-stk_01">
        <div class="stack-header">
          <div>
            <span class="app-dot app-ifood">iFood</span> + <span class="app-dot app-rappi">Rappi</span>
            <div style="font-size: 14px; font-weight: 800; color: #ffffff; margin-top: 6px;">Burger King Paulista & Pizza Hut Jardins</div>
          </div>
          <div style="text-align: right;">
            <div class="stack-price tabular">R$ 33,00</div>
            <div class="stack-gain-tag tabular">R$ 7,86/km</div>
          </div>
        </div>
        <div class="stack-route-display">
          <span>● BK</span> <span class="route-arrow">➔</span>
          <span>● PH</span> <span class="route-arrow">➔</span>
          <span>🏠 Residência</span> <span class="route-arrow">➔</span>
          <span>🏢 Edifício</span>
          <span style="margin-left: auto; color: var(--text-muted);">4.2 km • 18 min</span>
        </div>
        <div class="stack-btn-row">
          <button class="btn btn-red" onclick="declineStack(this, 'stk_01')">❌ Recusar</button>
          <button class="btn btn-green" onclick="acceptStack(this, 33.00, 'stk_01')">✅ Aceitar</button>
        </div>
      </div>

      <!-- Card 2: iFood solo R$15 -->
      <div class="stack-card" id="card-stk_02">
        <div class="stack-header">
          <div>
            <span class="app-dot app-ifood">iFood</span>
            <div style="font-size: 14px; font-weight: 800; color: #ffffff; margin-top: 6px;">McDonald's Henrique Schaumann</div>
          </div>
          <div style="text-align: right;">
            <div class="stack-price tabular">R$ 15,00</div>
            <div class="stack-gain-tag tabular">R$ 5,35/km</div>
          </div>
        </div>
        <div class="stack-route-display">
          <span>● McD</span> <span class="route-arrow">➔</span>
          <span>🏠 Rua Augusta</span>
          <span style="margin-left: auto; color: var(--text-muted);">2.8 km • 12 min</span>
        </div>
        <div class="stack-btn-row">
          <button class="btn btn-red" onclick="declineStack(this, 'stk_02')">❌ Recusar</button>
          <button class="btn btn-green" onclick="acceptStack(this, 15.00, 'stk_02')">✅ Aceitar</button>
        </div>
      </div>

      <!-- Card 3: Rappi solo R$18 -->
      <div class="stack-card" id="card-stk_03">
        <div class="stack-header">
          <div>
            <span class="app-dot app-rappi">Rappi</span>
            <div style="font-size: 14px; font-weight: 800; color: #ffffff; margin-top: 6px;">Starbucks Frei Caneca</div>
          </div>
          <div style="text-align: right;">
            <div class="stack-price tabular">R$ 18,00</div>
            <div class="stack-gain-tag tabular">R$ 5,80/km</div>
          </div>
        </div>
        <div class="stack-route-display">
          <span>● Starbucks</span> <span class="route-arrow">➔</span>
          <span>🏢 Av. Consolação</span>
          <span style="margin-left: auto; color: var(--text-muted);">3.1 km • 14 min</span>
        </div>
        <div class="stack-btn-row">
          <button class="btn btn-red" onclick="declineStack(this, 'stk_03')">❌ Recusar</button>
          <button class="btn btn-green" onclick="acceptStack(this, 18.00, 'stk_03')">✅ Aceitar</button>
        </div>
      </div>
    </div>

  </section>

  <!-- #STACKS (Lista detalhada de stacks) -->
  <section id="stacks" class="view-section">
    <div class="nav-pills">
      <a href="#dashboard" class="nav-pill">Cockpit</a>
      <a href="#stacks" class="nav-pill active">Stacks</a>
      <a href="#analytics" class="nav-pill">Analytics</a>
      <a href="#subscription" class="nav-pill">Plano Pro</a>
      <a href="#settings" class="nav-pill">Ajustes</a>
    </div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px;">
      <h2 style="font-size: 18px; font-weight: 800; color: #ffffff;">Stacks Disponíveis na Fila</h2>
      <button class="btn btn-green" style="padding: 6px 12px; font-size: 11px;" onclick="fetchStacks()">Atualizar</button>
    </div>
    <div id="full-stacks-container"></div>
  </section>

  <!-- #ANALYTICS (Gráficos e estatísticas, últimos 7 dias, bloqueio se free) -->
  <section id="analytics" class="view-section">
    <div class="nav-pills">
      <a href="#dashboard" class="nav-pill">Cockpit</a>
      <a href="#stacks" class="nav-pill">Stacks</a>
      <a href="#analytics" class="nav-pill active">Analytics</a>
      <a href="#subscription" class="nav-pill">Plano Pro</a>
    </div>

    <h2 style="font-size: 18px; font-weight: 800; color: #ffffff; margin-bottom: 14px;">Métricas & Gráficos</h2>

    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 16px;">
      <div class="glass" style="padding: 14px;">
        <div style="font-size: 11px; color: var(--text-muted);">Hoje</div>
        <div class="tabular neon-text" style="font-size: 22px; font-weight: 900;" id="stat-today">R$ 284,50</div>
      </div>
      <div class="glass" style="padding: 14px;">
        <div style="font-size: 11px; color: var(--text-muted);">Semana</div>
        <div class="tabular" style="font-size: 22px; font-weight: 900; color: #ffffff;" id="stat-week">R$ 1.420,80</div>
      </div>
      <div class="glass" style="padding: 14px;">
        <div style="font-size: 11px; color: var(--text-muted);">Mês</div>
        <div class="tabular" style="font-size: 22px; font-weight: 900; color: #ffffff;" id="stat-month">R$ 5.680,00</div>
      </div>
      <div class="glass" style="padding: 14px;">
        <div style="font-size: 11px; color: var(--text-muted);">Lucro Líquido Estimado</div>
        <div class="tabular neon-text" style="font-size: 22px; font-weight: 900;" id="stat-profit">R$ 218,40</div>
      </div>
    </div>

    <!-- Gráfico CSS 7 Dias -->
    <div class="glass" style="padding: 16px; margin-bottom: 16px;">
      <h3 style="font-size: 13px; font-weight: 700; color: #ffffff; margin-bottom: 10px;">Faturamento Diário (Últimos 7 Dias)</h3>
      <div class="css-chart-wrap" id="chart-bars"></div>
      <div id="analytics-free-lock" style="display:none; text-align: center; padding: 10px; background: rgba(255,215,0,0.1); border-radius: 8px; border: 1px solid #ffd700; font-size: 11px; color: #ffd700; font-weight: 700;">
        🔒 Pro para completo: Assine para desbloquear 30 dias de telemetria!
      </div>
    </div>
  </section>

  <!-- #SUBSCRIPTION (Planos Free vs Pro) -->
  <section id="subscription" class="view-section">
    <div class="nav-pills">
      <a href="#dashboard" class="nav-pill">Cockpit</a>
      <a href="#subscription" class="nav-pill active">Planos</a>
      <a href="#analytics" class="nav-pill">Analytics</a>
    </div>

    <h2 style="font-size: 20px; font-weight: 900; color: #ffffff; text-align: center; margin-bottom: 18px;">Planos de Assinatura</h2>

    <!-- Free -->
    <div class="plan-box">
      <div style="font-size: 16px; font-weight: 800; color: #ffffff;">Plano Básico</div>
      <div style="font-size: 24px; font-weight: 900; color: var(--text-muted); margin: 6px 0 12px;">GRÁTIS</div>
      <p style="font-size: 12px; color: var(--text-muted); line-height: 1.6; margin-bottom: 16px;">
        • 1 aplicativo por vez<br>• Decisão manual<br>• Histórico limitado a 3 dias
      </p>
      <button class="btn btn-red" style="width: 100%;" onclick="changePlan('free')">Selecionar Free</button>
    </div>

    <!-- Pro: R$ 29,90/mês (badge "MAIS POPULAR", borda dourada) -->
    <div class="plan-box pro">
      <div class="gold-badge">MAIS POPULAR</div>
      <div style="font-size: 18px; font-weight: 900; color: #ffd700;">Jarvis Neural Pro</div>
      <div style="font-size: 28px; font-weight: 900; color: #ffffff; margin: 6px 0 14px;">
        R$ 29,90 <span style="font-size: 12px; color: var(--text-muted);">/ mês</span>
      </div>
      <p style="font-size: 12px; color: #ffffff; line-height: 1.7; margin-bottom: 20px;">
        ⭐ Multi-App Stacking simultâneo<br>
        ⭐ Voz Neural Jarvis pt-BR mãos-livres<br>
        ⭐ Ghost Sequence preditivo 83%<br>
        ⭐ Telemetria completa e relatórios fiscais
      </p>
      <button class="btn" style="width: 100%; background: #ffd700; color: #0a0a0f; font-weight: 900;" onclick="changePlan('pro')">
        Teste grátis 7 dias
      </button>
    </div>
  </section>

  <!-- #SETTINGS (Configurações) -->
  <section id="settings" class="view-section">
    <div class="nav-pills">
      <a href="#dashboard" class="nav-pill">Cockpit</a>
      <a href="#settings" class="nav-pill active">Ajustes</a>
      <a href="#admin" class="nav-pill">Admin</a>
    </div>

    <h2 style="font-size: 18px; font-weight: 800; color: #ffffff; margin-bottom: 16px;">Configurações</h2>

    <div class="glass" style="padding: 18px; display: flex; flex-direction: column; gap: 14px;">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <div style="font-size: 13px; font-weight: 700; color: #ffffff;">Voz Jarvis (pt-BR)</div>
          <div style="font-size: 11px; color: var(--text-muted);">Alertas automáticos de voz</div>
        </div>
        <input type="checkbox" id="cfg-v" checked onchange="toggleVoz()" style="width: 20px; height: 20px; accent-color: var(--primary);">
      </div>

      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <div style="font-size: 13px; font-weight: 700; color: #ffffff;">Modo Foco</div>
          <div style="font-size: 11px; color: var(--text-muted);">Silencia distrações em trânsito</div>
        </div>
        <input type="checkbox" id="cfg-f" checked onchange="toggleModoFoco()" style="width: 20px; height: 20px; accent-color: var(--primary);">
      </div>

      <div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
          <span style="font-size: 13px; font-weight: 700; color: #ffffff;">Ganho Mínimo por Km</span>
          <span style="font-size: 13px; font-weight: 700; color: var(--primary);" id="cfg-gain-text">R$ 5,00/km</span>
        </div>
        <input type="range" id="cfg-gain" min="3" max="10" step="0.5" value="5" style="width: 100%; accent-color: var(--primary);" oninput="updateMinGain(this.value)">
      </div>
    </div>
  </section>

  <!-- #ADMIN (Tela admin secreta, métricas: 1.247 usuários, 342 ativos hoje, 8.5% conversão, R$ 18.700 MRR) -->
  <section id="admin" class="view-section">
    <div class="nav-pills">
      <a href="#dashboard" class="nav-pill">Cockpit</a>
      <a href="#admin" class="nav-pill active">Admin Secreto</a>
    </div>

    <h2 style="font-size: 18px; font-weight: 900; color: var(--primary); margin-bottom: 14px;">Métricas da Plataforma</h2>

    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
      <div class="glass" style="padding: 16px;">
        <div style="font-size: 11px; color: var(--text-muted);">Usuários</div>
        <div class="tabular" style="font-size: 26px; font-weight: 900; color: #ffffff;">1.247</div>
        <div style="font-size: 10px; color: var(--primary); margin-top: 4px;">cadastrados</div>
      </div>
      <div class="glass" style="padding: 16px;">
        <div style="font-size: 11px; color: var(--text-muted);">Ativos Hoje</div>
        <div class="tabular neon-text" style="font-size: 26px; font-weight: 900;">342</div>
        <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">em campo</div>
      </div>
      <div class="glass" style="padding: 16px;">
        <div style="font-size: 11px; color: var(--text-muted);">Conversão</div>
        <div class="tabular" style="font-size: 26px; font-weight: 900; color: #ffd700;">8.5%</div>
        <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">Free ➔ Pro</div>
      </div>
      <div class="glass" style="padding: 16px;">
        <div style="font-size: 11px; color: var(--text-muted);">MRR</div>
        <div class="tabular neon-text" style="font-size: 26px; font-weight: 900;">R$ 18.700</div>
        <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">recorrência</div>
      </div>
    </div>
  </section>

  <!-- Bottom bar: Health Pulse 94/100 + GPS/Latência/Temp + botões 🎙️🛡️⚙️▶ -->
  <footer class="bottom-hud">
    <div class="health-badge" onclick="speak('Índice de saúde do sistema 94 de 100.')">
      <svg class="health-ring-svg" viewBox="0 0 36 36">
        <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="3" />
        <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831" fill="none" stroke="#00ff88" stroke-dasharray="94, 100" stroke-width="3.5" stroke-linecap="round" />
      </svg>
      <div class="health-text tabular">94/100</div>
    </div>

    <div class="hud-sensors">
      <span>GPS: <strong>4.2m</strong></span>
      <span>Lat: <strong>12ms</strong></span>
      <span>Temp: <strong>28°C</strong></span>
    </div>

    <div class="hud-actions">
      <button class="hud-btn active" id="btn-voz" title="Voz Jarvis" onclick="toggleVoz()">🎙️</button>
      <button class="hud-btn" id="btn-foco" title="Modo Foco" onclick="toggleModoFoco()">🛡️</button>
      <button class="hud-btn" title="Configurações" onclick="location.hash='#settings'">⚙️</button>
      <button class="btn-route-start" onclick="iniciarRota()">▶ Rota</button>
    </div>
  </footer>

  <!-- SISTEMA DE ESTADO E SCRIPTS -->
  <script>
    // Sistema de Estado Obrigatório
    const defaultState = {
      user: { id: 'usr_thiago_01', name: 'Thiago Sutil', email: 'thiagosutilmente@gmail.com', plan: 'pro', onboardingComplete: true },
      session: { isLoggedIn: true, token: 'token_123' },
      earnings: { today: 284.50, week: 1420.80, month: 5680.00, totalKm: 38.2, profit: 218.40 },
      stacks: { active: [], pending: [], history: [], autoAccept: false, minGainPerKm: 5.0 },
      health: { score: 94, gpsAccuracy: 4.2, latency: 12, temperature: 28 },
      config: { voiceEnabled: true, focusModeAuto: true, theme: 'dark' }
    };

    function loadInitialState() {
      try {
        const stored = localStorage.getItem('RadarCoordinator_AppState');
        if (stored) return Object.assign({}, defaultState, JSON.parse(stored));
      } catch (e) {}
      return defaultState;
    }

    window.AppState = loadInitialState();

    function saveState() {
      try {
        localStorage.setItem('RadarCoordinator_AppState', JSON.stringify(window.AppState));
      } catch (e) {}
      render();
    }

    // Função speak(text): Web Speech API pt-BR
    function speak(text) {
      if (!window.AppState.config.voiceEnabled) return;
      const toast = document.getElementById('voice-toast');
      const msg = document.getElementById('voice-toast-msg');
      if (toast && msg) {
        msg.innerText = text;
        toast.style.display = 'block';
        clearTimeout(window._vTimeout);
        window._vTimeout = setTimeout(() => { toast.style.display = 'none'; }, 3200);
      }
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        const u = new SpeechSynthesisUtterance(text);
        u.lang = 'pt-BR';
        u.rate = 1.05;
        window.speechSynthesis.speak(u);
      }
    }

    // Roteamento Hash (#splash, #onboarding, #auth, #dashboard, etc.)
    function handleRouting() {
      const hash = window.location.hash || '#dashboard';
      document.querySelectorAll('.view-section').forEach(s => s.classList.remove('active'));
      const sec = document.getElementById(hash.replace('#', '')) || document.getElementById('dashboard');
      if (sec) sec.classList.add('active');

      document.querySelectorAll('.nav-pill').forEach(p => {
        p.classList.toggle('active', p.getAttribute('href') === hash);
      });

      if (hash === '#splash') {
        setTimeout(() => {
          if (window.location.hash === '#splash') {
            window.location.hash = window.AppState.user.onboardingComplete ? '#dashboard' : '#onboarding';
          }
        }, 2000);
      }
    }
    window.addEventListener('hashchange', handleRouting);

    // Funções obrigatórias
    function toggleVoz() {
      window.AppState.config.voiceEnabled = !window.AppState.config.voiceEnabled;
      document.getElementById('btn-voz').classList.toggle('active', window.AppState.config.voiceEnabled);
      const chk = document.getElementById('cfg-v');
      if (chk) chk.checked = window.AppState.config.voiceEnabled;
      saveState();
      speak(window.AppState.config.voiceEnabled ? 'Voz ativada' : 'Voz desativada');
    }

    function toggleModoFoco() {
      window.AppState.config.focusModeAuto = !window.AppState.config.focusModeAuto;
      document.getElementById('btn-foco').classList.toggle('active', window.AppState.config.focusModeAuto);
      const chk = document.getElementById('cfg-f');
      if (chk) chk.checked = window.AppState.config.focusModeAuto;
      saveState();
      speak(window.AppState.config.focusModeAuto ? 'Modo foco ativo' : 'Modo foco desligado');
    }

    function iniciarRota() {
      speak('Iniciando rota. Siga para a primeira coleta no Burger King.');
      location.hash = '#dashboard';
    }

    function updateMinGain(v) {
      window.AppState.stacks.minGainPerKm = parseFloat(v);
      const el = document.getElementById('cfg-gain-text');
      if (el) el.innerText = `R$ ${parseFloat(v).toFixed(2).replace('.', ',')}/km`;
      saveState();
    }

    // acceptStack(btn, valor): destaca verde, atualiza ganhos, remove outros, chama POST /api/stacks/accept
    async function acceptStack(btn, valor, stackId) {
      const card = btn.closest('.stack-card');
      if (card) card.classList.add('accepted');

      speak(`Stack aceito! R$ ${valor.toFixed(2).replace('.', ',')}.`);
      window.AppState.earnings.today += valor;
      saveState();

      try {
        await fetch('/api/stacks/accept', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ stack_id: stackId })
        });
      } catch (e) {}

      setTimeout(() => {
        if (card) card.style.display = 'none';
        fetchStacks();
      }, 700);
    }

    // declineStack(btn): slide out, chama POST /api/stacks/decline
    async function declineStack(btn, stackId) {
      const card = btn.closest('.stack-card');
      if (card) {
        card.style.transition = 'transform 0.3s, opacity 0.3s';
        card.style.opacity = '0';
        card.style.transform = 'translateX(-30px)';
      }
      speak('Stack recusado.');

      try {
        await fetch('/api/stacks/decline', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ stack_id: stackId })
        });
      } catch (e) {}

      setTimeout(() => {
        if (card) card.style.display = 'none';
        fetchStacks();
      }, 350);
    }

    // fetchStacks(): GET /api/stacks e preenche cards
    async function fetchStacks() {
      try {
        const res = await fetch('/api/stacks?status=pending');
        const data = await res.json();
        window.AppState.stacks.pending = data;
        renderFullStacks(data);
      } catch (e) {}
    }

    // fetchDecision(stackData): POST /api/decision para lógica inteligente
    async function fetchDecision(data) {
      const res = await fetch('/api/decision', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });
      return await res.json();
    }

    async function testDecision() {
      const v = parseFloat(document.getElementById('dec-val').value) || 0;
      const d = parseFloat(document.getElementById('dec-km').value) || 1;
      const resBox = document.getElementById('dec-res');
      const res = await fetchDecision({ value: v, distance: d });

      const isAcc = res.decision === 'accept';
      resBox.style.display = 'block';
      resBox.style.background = isAcc ? 'rgba(0,255,136,0.12)' : 'rgba(255,71,87,0.12)';
      resBox.style.border = `1px solid ${isAcc ? '#00ff88' : '#ff4757'}`;
      resBox.innerHTML = `
        <strong style="color:${isAcc ? '#00ff88' : '#ff4757'}">${isAcc ? '✔ ACEITAR' : '✖ RECUSAR'}</strong>
        (Confiança: ${(res.confidence*100).toFixed(0)}%) • Ganho: R$ ${res.gain_per_km.toFixed(2)}/km • Motivo: ${res.reason}
      `;
      speak(`Jarvis recomenda ${isAcc ? 'aceitar' : 'recusar'}.`);
    }

    function changePlan(plan) {
      window.AppState.user.plan = plan;
      saveState();
      fetch('/api/user/plan', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ plan })
      });
      speak(plan === 'pro' ? 'Plano Pro ativado! Teste grátis de 7 dias.' : 'Plano Free ativado.');
      location.hash = '#dashboard';
    }

    let onboardStep = 1;
    function nextOnboardSlide() {
      onboardStep++;
      const content = document.getElementById('onboard-content');
      const btn = document.getElementById('onboard-btn-next');
      if (onboardStep === 2) {
        content.innerHTML = `
          <div style="font-size: 52px; margin-bottom: 16px;">🧠</div>
          <h2 style="font-size: 20px; font-weight: 800; color: #ffffff; margin-bottom: 8px;">Decisão Neural em Tempo Real</h2>
          <p style="font-size: 13px; color: var(--text-muted); line-height: 1.5; margin-bottom: 24px;">
            O Jarvis avalia valor, quilometragem e tempo em frações de segundo para recomendar a melhor corrida.
          </p>
        `;
      } else if (onboardStep === 3) {
        content.innerHTML = `
          <div style="font-size: 52px; margin-bottom: 16px;">🎙️</div>
          <h2 style="font-size: 20px; font-weight: 800; color: #ffffff; margin-bottom: 8px;">Cockpit Mãos-Livres</h2>
          <p style="font-size: 13px; color: var(--text-muted); line-height: 1.5; margin-bottom: 24px;">
            Alertas em português para você não tirar as mãos do guidão nem os olhos do trânsito.
          </p>
        `;
        btn.innerText = 'Iniciar';
      } else {
        finishOnboarding();
      }
    }

    function finishOnboarding() {
      window.AppState.user.onboardingComplete = true;
      saveState();
      location.hash = '#dashboard';
    }

    function submitAuth() {
      const n = document.getElementById('auth-name').value;
      const e = document.getElementById('auth-email').value;
      if (n) window.AppState.user.name = n;
      if (e) window.AppState.user.email = e;
      window.AppState.session.isLoggedIn = true;
      saveState();
      speak(`Olá ${n}! Cockpit pronto.`);
      location.hash = '#dashboard';
    }

    // Renderizadores
    function render() {
      const topG = document.getElementById('top-ganho');
      if (topG) topG.innerText = `R$ ${window.AppState.earnings.today.toFixed(2).replace('.', ',')}`;

      const stToday = document.getElementById('stat-today');
      const stWeek = document.getElementById('stat-week');
      const stMonth = document.getElementById('stat-month');
      const stProf = document.getElementById('stat-profit');
      if (stToday) stToday.innerText = `R$ ${window.AppState.earnings.today.toFixed(2).replace('.', ',')}`;
      if (stWeek) stWeek.innerText = `R$ ${window.AppState.earnings.week.toFixed(2).replace('.', ',')}`;
      if (stMonth) stMonth.innerText = `R$ ${window.AppState.earnings.month.toFixed(2).replace('.', ',')}`;
      if (stProf) stProf.innerText = `R$ ${window.AppState.earnings.profit.toFixed(2).replace('.', ',')}`;
    }

    function renderFullStacks(list) {
      const cont = document.getElementById('full-stacks-container');
      if (!cont) return;
      cont.innerHTML = list.map(s => {
        const gain = (s.total_value / s.distance_km).toFixed(2);
        return `
          <div class="stack-card ${s.apps.includes('+') ? 'multi' : ''}">
            <div class="stack-header">
              <div>
                <span class="app-dot ${s.apps.includes('iFood') ? 'app-ifood' : 'app-rappi'}">${s.apps}</span>
                <div style="font-size: 14px; font-weight: 800; color: #ffffff; margin-top: 6px;">${s.restaurant}</div>
              </div>
              <div style="text-align: right;">
                <div class="stack-price tabular">R$ ${s.total_value.toFixed(2).replace('.', ',')}</div>
                <div class="stack-gain-tag tabular">R$ ${gain}/km</div>
              </div>
            </div>
            <div class="stack-route-display">
              <span>● ${s.restaurant}</span> <span class="route-arrow">➔</span> <span>Destino</span>
              <span style="margin-left: auto; color: var(--text-muted);">${s.distance_km} km • ${s.time_min} min</span>
            </div>
            <div class="stack-btn-row">
              <button class="btn btn-red" onclick="declineStack(this, '${s.id}')">❌ Recusar</button>
              <button class="btn btn-green" onclick="acceptStack(this, ${s.total_value}, '${s.id}')">✅ Aceitar</button>
            </div>
          </div>
        `;
      }).join('');
    }

    async function loadAnalytics() {
      try {
        const res = await fetch('/api/earnings');
        const data = await res.json();
        window.AppState.earnings.today = data.today;
        window.AppState.earnings.week = data.week;
        window.AppState.earnings.month = data.month;
        window.AppState.earnings.profit = data.profit;
        render();

        const chart = document.getElementById('chart-bars');
        const lock = document.getElementById('analytics-free-lock');
        const isFree = window.AppState.user.plan === 'free';
        if (lock) lock.style.display = isFree ? 'block' : 'none';

        if (chart && data.chart_7d) {
          const max = Math.max(...data.chart_7d.map(d => d.amount), 200);
          chart.innerHTML = data.chart_7d.map((d, idx) => {
            const h = Math.round((d.amount / max) * 100);
            const isLocked = isFree && idx < 4;
            return `
              <div class="css-chart-bar" style="opacity: ${isLocked ? '0.2' : '1'};">
                <div class="css-bar-val tabular">${isLocked ? '🔒' : 'R$' + Math.round(d.amount)}</div>
                <div class="css-bar-fill" style="height: ${h}%;"></div>
                <div class="css-bar-lbl">${d.short_date}</div>
              </div>
            `;
          }).join('');
        }
      } catch (e) {}
    }

    window.addEventListener('DOMContentLoaded', () => {
      handleRouting();
      render();
      fetchStacks();
      loadAnalytics();

      if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('/sw.js').catch(() => {});
      }
    });
  </script>
</body>
</html>
"""

# ==============================================================================
# EXECUÇÃO DO SERVIDOR (DUAL PORT 3000 E 5000)
# ==============================================================================

def run_server(port):
    try:
        server = ThreadingHTTPServer(("0.0.0.0", port), RadarHTTPHandler)
        print(f"[RADAR COCKPIT] Servidor operacional na porta {port} (http://localhost:{port})")
        server.serve_forever()
    except Exception as e:
        print(f"[RADAR COCKPIT] Erro na porta {port}: {e}")

if __name__ == "__main__":
    init_database()
    print("Radar Coordinator — Jarvis Neural Cockpit iniciando nas portas 3000 e 5000...")
    
    # Inicia porta 5000 (requisito do prompt) em thread background
    t5000 = threading.Thread(target=run_server, args=(5000,), daemon=True)
    t5000.start()
    
    # Inicia porta 3000 (requisito de proxy da plataforma AI Studio / nginx) no processo principal
    run_server(3000)

