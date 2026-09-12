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
import socket
from http.server import HTTPServer, ThreadingHTTPServer, BaseHTTPRequestHandler

# Suporte opcional a Flask se instalado; fallback automático para servidor padrão Python (Zero dependências externas)
try:
    from flask import Flask, request as flask_request, jsonify as flask_jsonify, Response as FlaskResponse, send_file as flask_send_file
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

    # Tabela 5: offer_failure_logs (id, offer_id, app_name, restaurant, value, distance_km, error_code, reason, created_at)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS offer_failure_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            offer_id TEXT,
            app_name TEXT NOT NULL,
            restaurant TEXT NOT NULL,
            value REAL NOT NULL,
            distance_km REAL NOT NULL,
            error_code TEXT NOT NULL,
            reason TEXT NOT NULL,
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

    # Inserção de Logs Iniciais de Falha para Testes de Campo / Depuração
    cur.execute("SELECT COUNT(*) as count FROM offer_failure_logs")
    if cur.fetchone()["count"] == 0:
        seed_failures = [
            ("stk_err_101", "Uber Eats", "Pizzaria Baggio Pinheiros", 0.0, 0.0, "ERR_INVALID_DISTANCE", "Payload com distância zerada ou corrompida no broadcast", datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")),
            ("stk_err_102", "99 Food", "Habib's Rebouças", 15.0, 4.5, "ERR_NETWORK_TIMEOUT", "Timeout de resposta de 4500ms durante handshake com servidor parceiro", (datetime.datetime.now() - datetime.timedelta(minutes=18)).strftime("%Y-%m-%d %H:%M:%S")),
            ("stk_err_103", "iFood", "Outback Center 3", 32.0, 6.2, "ERR_OCR_PARSE_EXCEPTION", "Inconsistência nos caracteres de moeda e valor bruto capturados", (datetime.datetime.now() - datetime.timedelta(minutes=45)).strftime("%Y-%m-%d %H:%M:%S"))
        ]
        cur.executemany("""
            INSERT INTO offer_failure_logs (offer_id, app_name, restaurant, value, distance_km, error_code, reason, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, seed_failures)
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
        SELECT date, SUM(amount) as daily_total, SUM(km_driven) as daily_km, COUNT(*) as delivery_count
        FROM earnings WHERE date >= ?
        GROUP BY date ORDER BY date ASC
    """, (week_ago,)).fetchall()

    daily_30_rows = conn.execute("""
        SELECT date, SUM(amount) as daily_total, SUM(km_driven) as daily_km, COUNT(*) as delivery_count
        FROM earnings WHERE date >= ?
        GROUP BY date ORDER BY date ASC
    """, (month_ago,)).fetchall()

    app_rows = conn.execute("""
        SELECT app_source, COUNT(*) as count, SUM(amount) as total_amount, SUM(km_driven) as total_km
        FROM earnings
        GROUP BY app_source ORDER BY total_amount DESC
    """).fetchall()

    recent_accepted_rows = conn.execute("""
        SELECT id, amount, date, app_source, km_driven
        FROM earnings ORDER BY id DESC LIMIT 12
    """).fetchall()

    conn.close()

    today_val = round(float(r_today["total"]) if r_today["total"] > 0 else 284.50, 2)
    today_km = round(float(r_today["total_km"]) if r_today["total_km"] > 0 else 38.2, 1)
    week_val = round(float(r_week["total"]) if r_week["total"] > 0 else 1420.80, 2)
    week_km = round(float(r_week["total_km"]) if r_week["total_km"] > 0 else 184.0, 1)
    month_val = round(float(r_month["total"]) if r_month["total"] > 0 else 5680.00, 2)
    month_km = round(float(r_month["total_km"]) if r_month["total_km"] > 0 else 760.5, 1)
    profit = round(today_val * 0.803, 2)
    fuel_cost_today = round(today_val * 0.197, 2)

    days_map = ["Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"]
    daily_dict = {r["date"]: {"amount": float(r["daily_total"]), "km": float(r["daily_km"]), "count": int(r["delivery_count"])} for r in daily_rows}
    daily_30_dict = {r["date"]: {"amount": float(r["daily_total"]), "km": float(r["daily_km"]), "count": int(r["delivery_count"])} for r in daily_30_rows}

    chart_7d = []
    mock_amts = [215.00, 198.50, 254.00, 288.00, 362.50, 410.00, today_val]
    for i in range(6, -1, -1):
        d = today - datetime.timedelta(days=i)
        d_str = d.strftime("%Y-%m-%d")
        record = daily_dict.get(d_str)
        if record and record["amount"] > 0:
            amt = round(record["amount"], 2)
            km = round(record["km"], 1)
            count = record["count"]
        else:
            amt = mock_amts[6 - i]
            km = round(amt / 5.2, 1)
            count = max(6, int(amt / 25))
        
        day_name = days_map[d.weekday()]
        is_today = (i == 0)
        net_prof = round(amt * 0.814, 2)
        fuel_c = round(amt * 0.186, 2)
        gain_per_km = round(amt / km, 2) if km > 0 else 5.20

        chart_7d.append({
            "date": d_str,
            "short_date": "Hoje" if is_today else d.strftime("%d/%m"),
            "day_name": day_name,
            "amount": amt,
            "km": km,
            "net_profit": net_prof,
            "fuel_cost": fuel_c,
            "count": count,
            "gain_per_km": gain_per_km,
            "is_today": is_today
        })

    max_day_val = max(d["amount"] for d in chart_7d) if chart_7d else 400.0
    for d in chart_7d:
        d["is_best_day"] = (d["amount"] == max_day_val and max_day_val > 0)

    # 30 Dias para Visão Completa Pro
    chart_30d = []
    for i in range(29, -1, -1):
        d = today - datetime.timedelta(days=i)
        d_str = d.strftime("%Y-%m-%d")
        record = daily_30_dict.get(d_str)
        if record and record["amount"] > 0:
            amt = round(record["amount"], 2)
            km = round(record["km"], 1)
            count = record["count"]
        else:
            amt = round(195.0 + ((i * 13) % 185) + ((i % 4) * 28.0), 2)
            km = round(amt / 5.15, 1)
            count = max(5, int(amt / 27))
        is_today = (i == 0)
        net_prof = round(amt * 0.814, 2)
        fuel_c = round(amt * 0.186, 2)
        gain_per_km = round(amt / km, 2) if km > 0 else 5.20
        chart_30d.append({
            "date": d_str,
            "short_date": "Hoje" if is_today else d.strftime("%d/%m"),
            "day_name": days_map[d.weekday()],
            "amount": amt,
            "km": km,
            "net_profit": net_prof,
            "fuel_cost": fuel_c,
            "count": count,
            "gain_per_km": gain_per_km,
            "is_today": is_today
        })

    calculated_week_gross = round(sum(d["amount"] for d in chart_7d), 2)
    calculated_week_net = round(sum(d["net_profit"] for d in chart_7d), 2)
    calculated_week_km = round(sum(d["km"] for d in chart_7d), 1)
    calculated_week_fuel = round(sum(d["fuel_cost"] for d in chart_7d), 2)
    calculated_week_deliveries = sum(d["count"] for d in chart_7d)
    calculated_week_daily_avg = round(calculated_week_gross / 7.0, 2)

    # Histórico Semanal das Últimas 4 Semanas
    weekly_history = [
        {
            "week_id": "w1",
            "name": "Semana -3",
            "range": (today - datetime.timedelta(days=27)).strftime("%d/%m") + " - " + (today - datetime.timedelta(days=21)).strftime("%d/%m"),
            "gross": 1820.50,
            "net": 1482.00,
            "fuel": 338.50,
            "km": 372.0,
            "count": 72,
            "goal": 2200.00,
            "goal_pct": 82.8,
            "avg_gain_km": 4.89,
            "ticket_avg": 25.28
        },
        {
            "week_id": "w2",
            "name": "Semana -2",
            "range": (today - datetime.timedelta(days=20)).strftime("%d/%m") + " - " + (today - datetime.timedelta(days=14)).strftime("%d/%m"),
            "gross": 2040.00,
            "net": 1660.50,
            "fuel": 379.50,
            "km": 402.0,
            "count": 80,
            "goal": 2200.00,
            "goal_pct": 92.7,
            "avg_gain_km": 5.07,
            "ticket_avg": 25.50
        },
        {
            "week_id": "w3",
            "name": "Semana Passada",
            "range": (today - datetime.timedelta(days=13)).strftime("%d/%m") + " - " + (today - datetime.timedelta(days=7)).strftime("%d/%m"),
            "gross": 2280.00,
            "net": 1856.00,
            "fuel": 424.00,
            "km": 435.0,
            "count": 88,
            "goal": 2200.00,
            "goal_pct": 103.6,
            "avg_gain_km": 5.24,
            "ticket_avg": 25.91
        },
        {
            "week_id": "w4",
            "name": "Semana Atual",
            "range": (today - datetime.timedelta(days=6)).strftime("%d/%m") + " - Hoje",
            "gross": calculated_week_gross,
            "net": calculated_week_net,
            "fuel": calculated_week_fuel,
            "km": calculated_week_km,
            "count": calculated_week_deliveries,
            "goal": 2200.00,
            "goal_pct": round((calculated_week_gross / 2200.00) * 100, 1),
            "avg_gain_km": round(calculated_week_gross / calculated_week_km, 2) if calculated_week_km > 0 else 5.15,
            "ticket_avg": round(calculated_week_gross / calculated_week_deliveries, 2) if calculated_week_deliveries > 0 else 26.50
        }
    ]

    # Distribuição por Aplicativo Parceiro
    app_colors = {
        "iFood": "#ea1d2c",
        "Rappi": "#ff441f",
        "Uber": "#ffffff",
        "Uber Direct": "#ffffff",
        "99": "#f7c200",
        "99Food": "#f7c200",
        "iFood + Rappi": "#00ff88",
        "Multi-Stack": "#00ff88"
    }

    total_app_sum = sum(float(r["total_amount"]) for r in app_rows) if app_rows else 0.0
    app_distribution = []
    if app_rows and total_app_sum > 0:
        for r in app_rows:
            amt = round(float(r["total_amount"]), 2)
            src = str(r["app_source"])
            color = app_colors.get(src, "#00d2ff")
            app_distribution.append({
                "name": src,
                "value": amt,
                "count": int(r["count"]),
                "km": round(float(r["total_km"]), 1),
                "pct": round((amt / total_app_sum) * 100, 1),
                "color": color
            })
    else:
        app_distribution = [
            {"name": "iFood", "value": 980.50, "count": 42, "km": 182.0, "pct": 48.7, "color": "#ea1d2c"},
            {"name": "Rappi", "value": 465.00, "count": 19, "km": 88.5, "pct": 23.1, "color": "#ff441f"},
            {"name": "Multi-Stack", "value": 310.00, "count": 9, "km": 42.0, "pct": 15.4, "color": "#00ff88"},
            {"name": "99Food", "value": 257.00, "count": 11, "km": 51.5, "pct": 12.8, "color": "#f7c200"}
        ]

    # Corridas Aceitas Recentes
    recent_accepted = []
    for r in recent_accepted_rows:
        amt = float(r["amount"])
        km = float(r["km_driven"])
        recent_accepted.append({
            "id": r["id"],
            "amount": amt,
            "net_profit": round(amt * 0.814, 2),
            "fuel_cost": round(amt * 0.186, 2),
            "date": r["date"],
            "app_source": r["app_source"],
            "km_driven": km,
            "gain_per_km": round(amt / km, 2) if km > 0 else 5.20
        })

    return {
        "today": today_val,
        "todayKm": today_km,
        "todayFuel": fuel_cost_today,
        "profit": profit,
        "totalKm": today_km,
        "week": calculated_week_gross,
        "weekNet": calculated_week_net,
        "weekFuel": calculated_week_fuel,
        "weekKm": calculated_week_km,
        "weekDeliveries": calculated_week_deliveries,
        "weekDailyAvg": calculated_week_daily_avg,
        "month": month_val,
        "monthKm": month_km,
        "dailyGoal": 350.00,
        "weeklyGoal": 2200.00,
        "chart_7d": chart_7d,
        "chart_30d": chart_30d,
        "weekly_history": weekly_history,
        "app_distribution": app_distribution,
        "recent_accepted": recent_accepted
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

def get_failures_data(limit=50):
    conn = get_db()
    rows = conn.execute("SELECT * FROM offer_failure_logs ORDER BY id DESC LIMIT ?", (limit,)).fetchall()
    conn.close()
    return [dict(r) for r in rows]

def record_failure_data(data):
    app_name = data.get("app_name", "Desconhecido")
    restaurant = data.get("restaurant", "Não identificado")
    val = float(data.get("value", 0.0))
    dist = float(data.get("distance_km", 0.0))
    error_code = data.get("error_code", "ERR_PROCESSING_GENERIC")
    reason = data.get("reason", "Erro de processamento da oferta em campo")
    offer_id = data.get("offer_id", f"err_{int(datetime.datetime.now().timestamp())}")
    now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        INSERT INTO offer_failure_logs (offer_id, app_name, restaurant, value, distance_km, error_code, reason, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, (offer_id, app_name, restaurant, val, dist, error_code, reason, now_str))
    conn.commit()
    inserted_id = cur.lastrowid
    conn.close()

    return {
        "success": True,
        "message": "Falha de processamento registrada com sucesso",
        "log_id": inserted_id,
        "error_code": error_code
    }, 201

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
    @app.route("/api/failures", methods=["GET"])
    def f_failures_get():
        limit = int(flask_request.args.get("limit", 50))
        return flask_jsonify(get_failures_data(limit))
    @app.route("/api/failures", methods=["POST"])
    def f_failures_post():
        data = flask_request.get_json(silent=True) or {}
        res, code = record_failure_data(data)
        return flask_jsonify(res), code
    @app.route("/api/decision", methods=["POST"])
    def f_dcs():
        res, code = evaluate_decision_data(flask_request.get_json(silent=True) or {})
        return flask_jsonify(res), code
    @app.route("/api/analytics", methods=["POST"])
    def f_analytics():
        data = flask_request.get_json(silent=True) or {}
        event_name = data.get("event_name", "unknown")
        params = data.get("params", {})
        print(f"[FIREBASE ANALYTICS WEB] Event: {event_name} | {params}")
        return flask_jsonify({"success": True, "event": event_name, "status": "recorded"}), 200
    @app.route("/manifest.json", methods=["GET"])
    def f_mnf(): return FlaskResponse(get_manifest_json(), mimetype="application/json")
    @app.route("/sw.js", methods=["GET"])
    def f_sw(): return FlaskResponse(get_sw_js(), mimetype="application/javascript")
    @app.route("/download/apk", methods=["GET"])
    @app.route("/api/download-apk", methods=["GET"])
    def f_apk():
        apk_paths = [
            file_path for file_path in [
                os.path.abspath(".build-outputs/app-debug.apk"),
                os.path.abspath("app/build/outputs/apk/debug/app-debug.apk"),
                os.path.abspath("build/outputs/apk/debug/app-debug.apk")
            ] if os.path.exists(file_path)
        ]
        if apk_paths:
            return flask_send_file(apk_paths[0], as_attachment=True, download_name="RadarCoordinator.apk", mimetype="application/vnd.android.package-archive")
        return flask_jsonify({"error": "APK não encontrado"}), 404
    @app.route("/", methods=["GET"])
    def f_idx(): return HTML_CONTENT

class RadarHTTPHandler(BaseHTTPRequestHandler):
    def handle(self):
        try:
            super().handle()
        except (BrokenPipeError, ConnectionResetError, socket.error):
            pass

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
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        if path in ("/download/apk", "/api/download-apk"):
            self.send_response(200)
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", 'attachment; filename="RadarCoordinator.apk"')
            self.end_headers()
        else:
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
            try:
                self.wfile.write(body)
            except (BrokenPipeError, ConnectionResetError, socket.error):
                pass
        elif path == "/manifest.json":
            body = get_manifest_json().encode("utf-8")
            self.send_response(200)
            self.send_cors_headers("application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            try:
                self.wfile.write(body)
            except (BrokenPipeError, ConnectionResetError, socket.error):
                pass
        elif path == "/sw.js":
            body = get_sw_js().encode("utf-8")
            self.send_response(200)
            self.send_cors_headers("application/javascript")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            try:
                self.wfile.write(body)
            except (BrokenPipeError, ConnectionResetError, socket.error):
                pass
        elif path == "/download/apk" or path == "/api/download-apk":
            apk_paths = [
                file_path for file_path in [
                    os.path.abspath(".build-outputs/app-debug.apk"),
                    os.path.abspath("app/build/outputs/apk/debug/app-debug.apk"),
                    os.path.abspath("build/outputs/apk/debug/app-debug.apk")
                ] if os.path.exists(file_path)
            ]
            if apk_paths:
                file_size = os.path.getsize(apk_paths[0])
                self.send_response(200)
                self.send_header("Access-Control-Allow-Origin", "*")
                self.send_header("Content-Type", "application/vnd.android.package-archive")
                self.send_header("Content-Disposition", 'attachment; filename="RadarCoordinator.apk"')
                self.send_header("Content-Length", str(file_size))
                self.end_headers()
                try:
                    with open(apk_paths[0], "rb") as f:
                        while True:
                            chunk = f.read(65536)
                            if not chunk:
                                break
                            self.wfile.write(chunk)
                except (BrokenPipeError, ConnectionResetError, socket.error):
                    pass
            else:
                self.send_response(404)
                self.send_cors_headers("application/json")
                self.end_headers()
                self.wfile.write(b'{"error": "APK not found"}')
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
        elif path == "/api/failures":
            limit = int(qs.get("limit", [50])[0])
            body = json.dumps(get_failures_data(limit)).encode("utf-8")
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
        elif path == "/api/failures":
            res, code = record_failure_data(req_data)
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
        elif path == "/api/analytics":
            event_name = req_data.get("event_name", "unknown")
            body = json.dumps({"success": True, "event": event_name, "status": "recorded"}).encode("utf-8")
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
  <!-- React 18 + ReactDOM + Recharts para Visualização Avançada de Dados -->
  <script src="https://cdnjs.cloudflare.com/ajax/libs/react/18.2.0/umd/react.production.min.js"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/react-dom/18.2.0/umd/react-dom.production.min.js"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/prop-types/15.8.1/prop-types.min.js"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/recharts/2.12.7/Recharts.min.js"></script>
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
    .btn-maps {
      background: rgba(0, 210, 255, 0.15);
      color: #00d2ff;
      border: 1px solid rgba(0, 210, 255, 0.4);
      box-shadow: 0 4px 12px rgba(0, 210, 255, 0.15);
    }
    .btn-maps:hover { background: rgba(0, 210, 255, 0.28); }
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
    .hud-btn.listening {
      background: #ff4757 !important;
      color: #ffffff !important;
      border-color: #ff4757 !important;
      box-shadow: 0 0 16px rgba(255, 71, 87, 0.8) !important;
      animation: voiceListenPulse 1.2s infinite ease-in-out;
    }
    @keyframes voiceListenPulse {
      0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(255, 71, 87, 0.7); }
      70% { transform: scale(1.12); box-shadow: 0 0 0 10px rgba(255, 71, 87, 0); }
      100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(255, 71, 87, 0); }
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
      <a href="#dashboard" class="nav-pill active">🎯 Cockpit</a>
      <a href="#stacks" class="nav-pill">📦 Stacks (8)</a>
      <a href="#analytics" class="nav-pill">📊 Analytics</a>
      <a href="#subscription" class="nav-pill">⭐ Plano Pro</a>
      <a href="#settings" class="nav-pill">⚙️ Ajustes</a>
      <a href="#admin" class="nav-pill">🔒 Admin</a>
      <a href="/download/apk" class="nav-pill" style="background: rgba(0, 255, 136, 0.15); color: #00ff88; border-color: #00ff88;" download>📲 Baixar APK (.apk)</a>
    </div>

    <!-- Banner de Download do APK Nativo Android -->
    <div style="background: linear-gradient(90deg, rgba(0, 255, 136, 0.12) 0%, rgba(17, 17, 24, 0.95) 100%); border: 1px solid rgba(0, 255, 136, 0.35); border-radius: 14px; padding: 12px 16px; margin-bottom: 14px; display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap;">
      <div style="display: flex; align-items: center; gap: 10px;">
        <div style="font-size: 26px;">📱</div>
        <div>
          <div style="font-size: 13px; font-weight: 800; color: #00ff88;">APK Nativo Android Pronto para Instalação</div>
          <div style="font-size: 11px; color: var(--text-muted);">Radar Coordinator com Reconhecimento de Voz nativo (SpeechRecognizer) e Google Maps.</div>
        </div>
      </div>
      <a href="/download/apk" class="btn btn-green" style="flex: 0 0 auto; padding: 9px 18px; font-size: 12px; text-decoration: none; border-radius: 10px;" download>
        ⬇️ Baixar APK Direto
      </a>
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

    <!-- Card de Telemetria de Velocidade e Trava de Segurança em Movimento (Android Location API) -->
    <div class="glass" style="padding: 14px 16px; margin-bottom: 14px; border: 1.5px solid var(--surface-border); border-radius: 16px;" id="speed-telemetry-box">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
        <div style="display: flex; align-items: center; gap: 8px;">
          <span style="font-size: 18px;">🏍️</span>
          <div>
            <div style="font-size: 12px; font-weight: 800; color: #ffffff;">TELEMETRIA DE VELOCIDADE (GPS ANDROID)</div>
            <div style="font-size: 10px; color: var(--text-muted);">API de Localização em Tempo Real • Trava Automática > 10 km/h</div>
          </div>
        </div>
        <div id="speed-lock-badge">
          <span style="color:#00ff88; font-weight:800; font-size:11px;">🛡️ TOQUE LIVRE (<= 10 km/h)</span>
        </div>
      </div>

      <div style="display: flex; justify-content: space-between; align-items: center; background: rgba(0,0,0,0.35); padding: 10px 14px; border-radius: 12px; margin-bottom: 10px;">
        <div style="display: flex; align-items: baseline; gap: 6px;">
          <span id="dash-speed-display" style="font-size: 32px; font-weight: 900; color: var(--primary);" class="tabular">0</span>
          <span style="font-size: 12px; font-weight: 700; color: var(--text-muted);">km/h</span>
        </div>
        <div style="text-align: right;">
          <div style="font-size: 11px; font-weight: 700; color: #fff;">Limite de Segurança: 10 km/h</div>
          <div style="font-size: 10px; color: var(--text-muted);" id="speed-movement-status">🟢 Moto Parada</div>
        </div>
      </div>

      <!-- Trava de Segurança Banner -->
      <div id="speed-safety-lock-banner" style="display:none; background: rgba(255, 71, 87, 0.15); border: 1.5px solid #ff4757; border-radius: 10px; padding: 10px; margin-bottom: 10px; text-align: center;">
        <div style="font-size: 13px; font-weight: 800; color: #ff4757;">🚨 TRAVA DE SEGURANÇA ATIVADA (> 10 KM/H)</div>
        <div style="font-size: 11px; color: #ffccd0; margin-top: 2px;">Veículo em movimento detectado pelo GPS! Interface desativada para evitar acidentes. Use comandos de áudio no viva-voz:</div>
        <div style="font-size: 10px; color: var(--primary); margin-top: 4px; font-weight: 700;">
          🎙️ "Aceitar" | "Recusar" | "Filtro Chuva" | "Tiro Curto" | "Máximo Lucro" | "Resetar Filtros"
        </div>
        <div style="margin-top: 8px; display: flex; justify-content: center; gap: 6px; flex-wrap: wrap;">
          <button class="btn" style="flex: initial; padding: 4px 10px; font-size: 10px; background: rgba(0, 255, 136, 0.2); color: #00ff88; border: 1px solid #00ff88;" onclick="triggerVoiceCommand('aceitar')">🗣️ "Aceitar"</button>
          <button class="btn" style="flex: initial; padding: 4px 10px; font-size: 10px; background: rgba(255, 71, 87, 0.25); color: #ff4757; border: 1px solid #ff4757;" onclick="triggerVoiceCommand('recusar')">🗣️ "Recusar"</button>
          <button class="btn" style="flex: initial; padding: 4px 8px; font-size: 10px; background: rgba(0, 210, 255, 0.2); color: #00d2ff; border: 1px solid #00d2ff;" onclick="handleVoiceCommand('filtro chuva')">🌧️ "Chuva"</button>
          <button class="btn" style="flex: initial; padding: 4px 8px; font-size: 10px; background: rgba(255, 184, 0, 0.2); color: #ffb800; border: 1px solid #ffb800;" onclick="handleVoiceCommand('tiro curto')">⚡ "Curto"</button>
          <button class="btn" style="flex: initial; padding: 4px 8px; font-size: 10px; background: rgba(157, 78, 221, 0.2); color: #c77dff; border: 1px solid #9d4edd;" onclick="handleVoiceCommand('resetar filtros')">🔄 "Resetar"</button>
          <button class="btn" id="btn-mic-safety" style="flex: initial; padding: 4px 10px; font-size: 10px; background: rgba(255, 255, 255, 0.1); color: #fff; border: 1px solid rgba(255,255,255,0.2);" onclick="toggleVoiceRecognition()">🎙️ Falar Agora</button>
        </div>
      </div>

      <!-- Controles de Teste / Simulação de Velocidade -->
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 6px;">
        <span style="font-size: 10px; font-weight: 700; color: var(--text-muted);">Testar Velocidade:</span>
        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
          <button class="btn" style="padding: 4px 8px; font-size: 10px; background: rgba(255,255,255,0.08);" onclick="updateSpeed(0, 'Simulado')">0 km/h</button>
          <button class="btn" style="padding: 4px 8px; font-size: 10px; background: rgba(0, 255, 136, 0.15); color: #00ff88; border: 1px solid #00ff88;" onclick="updateSpeed(8, 'Simulado')">8 km/h (Livre)</button>
          <button class="btn" style="padding: 4px 8px; font-size: 10px; background: rgba(255, 71, 87, 0.2); color: #ff4757; border: 1px solid #ff4757;" onclick="updateSpeed(15, 'Simulado')">15 km/h (Trava)</button>
          <button class="btn" style="padding: 4px 8px; font-size: 10px; background: rgba(255, 71, 87, 0.25); color: #ff4757; border: 1px solid #ff4757;" onclick="updateSpeed(35, 'Simulado')">35 km/h (Trânsito)</button>
          <button class="btn" style="padding: 4px 8px; font-size: 10px; background: rgba(0, 255, 136, 0.15); color: #00ff88; border: 1px solid #00ff88;" onclick="initGeoLocationTracking()">🛰️ GPS Real</button>
        </div>
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
    <div class="ghost-card" style="cursor: pointer;" onclick="triggerGhostSweep()">
      <div class="ghost-head">
        <div class="ghost-tag">
          <span class="ghost-symbol">👻</span>
          <span>GHOST SEQUENCE ATIVA</span>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          <span class="ghost-stat">83% Chance</span>
          <span style="background: rgba(0, 255, 136, 0.2); color: var(--primary); font-size: 9px; font-weight: 800; padding: 2px 6px; border-radius: 4px; border: 1px solid var(--primary);">VARRER ⚡</span>
        </div>
      </div>
      <div class="ghost-bar-track">
        <div class="ghost-bar-fill"></div>
      </div>
      <div class="ghost-text">
        83% chance de stack em 3 min no raio de 800m. Toque para varrer o corredor viário por entregas mescladas.
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

    <!-- PAINEL DE VISUALIZAÇÃO DE GANHOS DIÁRIOS E SEMANAIS (RADAR AI COCKPIT) -->
    <div class="glass" style="padding: 16px; margin-bottom: 18px; border: 1.5px solid rgba(0, 255, 136, 0.4); border-radius: 18px;" id="dash-financial-panel">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <div style="display: flex; align-items: center; gap: 8px;">
          <span style="font-size: 20px;">💰</span>
          <div>
            <div style="font-size: 13px; font-weight: 900; color: #ffffff;">PAINEL DE DESEMPENHO FINANCEIRO</div>
            <div style="font-size: 10px; color: var(--text-muted);">Acompanhamento em tempo real • Radar AI</div>
          </div>
        </div>
        <div style="background: rgba(0, 255, 136, 0.15); border: 1px solid rgba(0, 255, 136, 0.4); border-radius: 6px; padding: 3px 8px; font-size: 9px; font-weight: 900; color: var(--primary);">
          ⚡ CONSOLIDADO
        </div>
      </div>

      <!-- Seletor de Período: Diário vs Semanal -->
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 6px; background: rgba(0,0,0,0.5); padding: 4px; border-radius: 10px; margin-bottom: 14px; border: 1px solid var(--surface-border);">
        <button id="btn-period-daily" class="btn" style="padding: 8px; font-size: 11px; font-weight: 900; background: var(--primary); color: #0a0a0f; border-radius: 8px; transition: all 0.2s;" onclick="switchFinancialPeriod('daily')">
          📅 Ganhos Diários
        </button>
        <button id="btn-period-weekly" class="btn" style="padding: 8px; font-size: 11px; font-weight: 800; background: transparent; color: var(--text-muted); border-radius: 8px; transition: all 0.2s;" onclick="switchFinancialPeriod('weekly')">
          📆 Ganhos Semanais
        </button>
      </div>

      <!-- CONTEÚDO VISÃO DIÁRIA -->
      <div id="financial-view-daily">
        <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--surface-border); border-radius: 14px; padding: 14px; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <div style="font-size: 10px; font-weight: 800; color: var(--text-muted); letter-spacing: 0.5px;">LUCRO LÍQUIDO DE HOJE (NO BOLSO)</div>
            <div class="tabular neon-text" style="font-size: 28px; font-weight: 900; margin: 2px 0;" id="fin-daily-net">R$ 228,40</div>
            <div style="font-size: 10px; color: #a1a1aa;" id="fin-daily-margin">Margem líquida de 80.3% das entregas</div>
          </div>
          <div style="text-align: right;">
            <div style="font-size: 10px; font-weight: 800; color: var(--text-muted);">FATURAMENTO BRUTO</div>
            <div class="tabular" style="font-size: 18px; font-weight: 800; color: #ffffff;" id="fin-daily-gross">R$ 284,50</div>
            <div style="font-size: 10px; color: #ff4757; font-weight: 700; margin-top: 3px;" id="fin-daily-fuel">Combustível: - R$ 56,10</div>
          </div>
        </div>

        <!-- Grid de Eficiência do Dia -->
        <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; margin-bottom: 12px;">
          <div class="glass" style="padding: 8px; text-align: center;">
            <div style="font-size: 9px; color: var(--text-muted); font-weight: 800;">MÉDIA R$/KM</div>
            <div class="tabular neon-text" style="font-size: 14px; font-weight: 900;" id="fin-daily-avg-km">R$ 7,45/km</div>
            <div style="font-size: 8px; color: var(--text-muted);">🌟 Nível Ouro</div>
          </div>
          <div class="glass" style="padding: 8px; text-align: center;">
            <div style="font-size: 9px; color: var(--text-muted); font-weight: 800;">SURPLUS RADAR</div>
            <div class="tabular" style="font-size: 14px; font-weight: 900; color: #ffd700;" id="fin-daily-surplus">+ R$ 162,26</div>
            <div style="font-size: 8px; color: var(--text-muted);">vs R$ 3,20 rua</div>
          </div>
          <div class="glass" style="padding: 8px; text-align: center;">
            <div style="font-size: 9px; color: var(--text-muted); font-weight: 800;">TICKET MÉDIO</div>
            <div class="tabular" style="font-size: 14px; font-weight: 900; color: #ffffff;" id="fin-daily-ticket">R$ 31,61</div>
            <div style="font-size: 8px; color: var(--text-muted);" id="fin-daily-runs">9 corridas</div>
          </div>
        </div>

        <!-- Meta Diária com Barra de Progresso -->
        <div style="background: rgba(0,0,0,0.4); border-radius: 12px; padding: 12px; margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; font-size: 11px; font-weight: 800; margin-bottom: 6px;">
            <span style="color: #ffffff;">META DIÁRIA (R$ 350,00)</span>
            <span id="fin-daily-goal-pct" style="color: var(--primary);">81.3%</span>
          </div>
          <div style="height: 8px; background: rgba(255,255,255,0.08); border-radius: 4px; overflow: hidden; margin-bottom: 6px;">
            <div id="fin-daily-goal-bar" style="height: 100%; width: 81.3%; background: var(--primary); border-radius: 4px; transition: width 0.6s;"></div>
          </div>
          <div style="display: flex; justify-content: space-between; font-size: 10px; color: var(--text-muted);">
            <span id="fin-daily-goal-remain">Faltam R$ 65,50 para bater o dia</span>
            <span id="fin-daily-km-driven">Rodagem: 38.2 km</span>
          </div>
        </div>
      </div>

      <!-- CONTEÚDO VISÃO SEMANAL -->
      <div id="financial-view-weekly" style="display: none;">
        <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--surface-border); border-radius: 14px; padding: 14px; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <div style="font-size: 10px; font-weight: 800; color: var(--text-muted); letter-spacing: 0.5px;">LUCRO LÍQUIDO DA SEMANA (7 DIAS)</div>
            <div class="tabular neon-text" style="font-size: 28px; font-weight: 900; margin: 2px 0;" id="fin-weekly-net">R$ 1.637,50</div>
            <div style="font-size: 10px; color: #a1a1aa;" id="fin-weekly-margin">Margem acumulada de 81.4% no período</div>
          </div>
          <div style="text-align: right;">
            <div style="font-size: 10px; font-weight: 800; color: var(--text-muted);">TOTAL BRUTO SEMANAL</div>
            <div class="tabular" style="font-size: 18px; font-weight: 800; color: #ffffff;" id="fin-weekly-gross">R$ 2.012,50</div>
            <div style="font-size: 10px; color: #ff4757; font-weight: 700; margin-top: 3px;" id="fin-weekly-fuel">Combustível: - R$ 375,00</div>
          </div>
        </div>

        <!-- Métricas Globais da Semana -->
        <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; margin-bottom: 12px;">
          <div class="glass" style="padding: 8px; text-align: center;">
            <div style="font-size: 9px; color: var(--text-muted); font-weight: 800;">MÉDIA DIÁRIA</div>
            <div class="tabular" style="font-size: 14px; font-weight: 900; color: #ffffff;" id="fin-weekly-avg-day">R$ 287,50</div>
            <div style="font-size: 8px; color: var(--text-muted);">Por dia trabalhado</div>
          </div>
          <div class="glass" style="padding: 8px; text-align: center;">
            <div style="font-size: 9px; color: var(--text-muted); font-weight: 800;">KM SEMANAL</div>
            <div class="tabular" style="font-size: 14px; font-weight: 900; color: #38bdf8;" id="fin-weekly-total-km">409 km</div>
            <div style="font-size: 8px; color: var(--text-muted);">Média R$ 4,92/km</div>
          </div>
          <div class="glass" style="padding: 8px; text-align: center;">
            <div style="font-size: 9px; color: var(--text-muted); font-weight: 800;">TOTAL CORRIDAS</div>
            <div class="tabular" style="font-size: 14px; font-weight: 900; color: #ffd700;" id="fin-weekly-total-runs">81</div>
            <div style="font-size: 8px; color: var(--text-muted);">Concluídas na semana</div>
          </div>
        </div>

        <!-- Meta Semanal com Barra de Progresso -->
        <div style="background: rgba(0,0,0,0.4); border-radius: 12px; padding: 12px; margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; font-size: 11px; font-weight: 800; margin-bottom: 6px;">
            <span style="color: #ffffff;">META SEMANAL (R$ 2.200,00)</span>
            <span id="fin-weekly-goal-pct" style="color: #ffd700;">91.5%</span>
          </div>
          <div style="height: 8px; background: rgba(255,255,255,0.08); border-radius: 4px; overflow: hidden; margin-bottom: 6px;">
            <div id="fin-weekly-goal-bar" style="height: 100%; width: 91.5%; background: linear-gradient(90deg, #ffd700, #00ff88); border-radius: 4px; transition: width 0.6s;"></div>
          </div>
          <div style="display: flex; justify-content: space-between; font-size: 10px; color: var(--text-muted);">
            <span id="fin-weekly-goal-remain">Faltam R$ 187,50 para bater a semana</span>
            <span style="color: var(--primary); font-weight: 700;">Ritmo: +14% vs média</span>
          </div>
        </div>

        <!-- Gráfico Interativo de Barras Semanal -->
        <div style="background: rgba(255,255,255,0.02); border: 1px solid var(--surface-border); border-radius: 14px; padding: 12px; margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
            <div style="font-size: 11px; font-weight: 800; color: #ffffff;">FATURAMENTO DOS 7 DIAS (SEMANA)</div>
            <div style="font-size: 9px; color: var(--text-muted);">Toque na barra para inspecionar</div>
          </div>
          
          <div id="weekly-bars-container" style="display: flex; justify-content: space-between; align-items: flex-end; height: 115px; gap: 4px; padding-bottom: 4px;">
            <!-- Barras preenchidas dinamicamente via JS -->
          </div>

          <!-- Card de Inspeção do Dia Selecionado -->
          <div id="weekly-inspected-day-card" style="background: rgba(0,0,0,0.5); border: 1px solid var(--primary); border-radius: 10px; padding: 8px 12px; margin-top: 8px; display: flex; justify-content: space-between; align-items: center;">
            <div>
              <span id="inspected-day-title" style="font-size: 11px; font-weight: 900; color: #ffffff;">Dom (Hoje)</span>
              <span id="inspected-day-tag" style="background: rgba(0,255,136,0.2); color: var(--primary); font-size: 8px; font-weight: 800; padding: 2px 4px; border-radius: 4px; margin-left: 6px;">HOJE</span>
              <div id="inspected-day-sub" style="font-size: 10px; color: var(--text-muted); margin-top: 2px;">9 entregas • 38.2 km rodados</div>
            </div>
            <div style="text-align: right;">
              <div id="inspected-day-gross" style="font-size: 12px; font-weight: 800; color: #ffffff;">R$ 284,50 Bruto</div>
              <div id="inspected-day-net" style="font-size: 11px; font-weight: 900; color: var(--primary);">R$ 228,40 Líquido</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Faturamento por Aplicativo Parceiro (Accordion Expansível) -->
      <div style="border-top: 1px solid var(--surface-border); padding-top: 10px; margin-top: 6px;">
        <div style="display: flex; justify-content: space-between; align-items: center; cursor: pointer;" onclick="toggleFinancialAppBreakdown()">
          <span style="font-size: 11px; font-weight: 800; color: var(--primary);" id="fin-apps-toggle-lbl">Ver faturamento por app (Hoje)</span>
          <span id="fin-apps-toggle-icon" style="color: var(--primary); font-size: 12px;">▼</span>
        </div>
        <div id="fin-apps-breakdown" style="display: none; margin-top: 10px; flex-direction: column; gap: 6px;">
          <!-- Itens preenchidos dinamicamente via JS -->
        </div>
      </div>

      <!-- Botões de Ação do Painel -->
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 14px;">
        <button class="btn btn-green" style="padding: 9px; font-size: 11px; font-weight: 800;" onclick="exportFinancialReport()">
          📤 Relatório <span id="btn-export-period-lbl">Hoje</span>
        </button>
        <button class="btn" style="padding: 9px; font-size: 11px; font-weight: 800; background: rgba(255,255,255,0.06); color: var(--text-muted); border: 1px solid var(--surface-border);" onclick="resetFinancialTurn()">
          🔄 Novo Turno
        </button>
      </div>
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
      <!-- Card 1: Multi-app iFood+Rappi (Mesclada) -->
      <div class="stack-card multi" id="card-stk_01">
        <div class="stack-header">
          <div>
            <span class="app-dot app-ifood">iFood</span> + <span class="app-dot app-rappi">Rappi</span>
            <span style="background: rgba(0, 255, 136, 0.2); color: var(--primary); font-size: 10px; font-weight: 800; padding: 2px 6px; border-radius: 4px; margin-left: 6px; border: 1px solid var(--primary);">✨ MESCLADA</span>
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
          <button class="btn" style="background: rgba(255, 215, 0, 0.15); color: #ffd700; border: 1px solid #ffd700;" onclick="readOfferAloud('iFood e Rappi', 'Burger King e Pizza Hut', 33.0, 4.2, 7.86, 18)">🔊 Ouvir</button>
          <button class="btn btn-maps" onclick="openMapsRoute('Burger King Avenida Paulista, Sao Paulo', 'Pizza Hut Alameda Santos, Sao Paulo', 'Edificio Paulista Corporate, Sao Paulo', 'stk_01')">🗺️ Maps Rota</button>
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
          <button class="btn" style="background: rgba(255, 215, 0, 0.15); color: #ffd700; border: 1px solid #ffd700;" onclick="readOfferAloud('iFood', 'McDonalds Henrique Schaumann', 15.0, 2.8, 5.35, 12)">🔊 Ouvir</button>
          <button class="btn btn-maps" onclick="openMapsRoute('McDonalds Henrique Schaumann, Sao Paulo', null, 'Rua Augusta 1500, Sao Paulo', 'stk_02')">🗺️ Maps</button>
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
          <button class="btn" style="background: rgba(255, 215, 0, 0.15); color: #ffd700; border: 1px solid #ffd700;" onclick="readOfferAloud('Rappi', 'Starbucks Frei Caneca', 18.0, 3.1, 5.80, 14)">🔊 Ouvir</button>
          <button class="btn btn-maps" onclick="openMapsRoute('Starbucks Shopping Frei Caneca, Sao Paulo', null, 'Avenida Consolacao 2000, Sao Paulo', 'stk_03')">🗺️ Maps</button>
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
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
      <h2 style="font-size: 18px; font-weight: 800; color: #ffffff;">Stacks Disponíveis na Fila</h2>
      <button class="btn btn-green" style="padding: 6px 12px; font-size: 11px;" onclick="fetchStacks()">Atualizar</button>
    </div>

    <!-- Painel de Filtragem Rápida no Topo das Stacks -->
    <div class="glass" style="padding: 14px; margin-bottom: 14px; border: 1px solid rgba(0, 255, 136, 0.3);">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
        <span style="font-size: 11px; font-weight: 800; color: var(--primary); letter-spacing: 0.8px;">🎚️ FILTRAGEM ATIVA</span>
        <span id="stacks-filter-badge" style="font-size: 11px; font-weight: 700; color: var(--text-muted);">Calculando...</span>
      </div>

      <!-- Presets Rápidos de Estratégia -->
      <div style="display: flex; gap: 6px; overflow-x: auto; padding-bottom: 8px; margin-bottom: 10px;">
        <button class="btn" style="padding: 5px 10px; font-size: 10px; background: rgba(255,255,255,0.06); color: #fff; border: 1px solid var(--surface-border);" onclick="applyStrategyPreset(0, 10, 0)">⚡ Padrão</button>
        <button class="btn" style="padding: 5px 10px; font-size: 10px; background: rgba(0,255,136,0.12); color: var(--primary); border: 1px solid var(--primary);" onclick="applyStrategyPreset(18, 5, 4.5)">🎯 Rentável</button>
        <button class="btn" style="padding: 5px 10px; font-size: 10px; background: rgba(255,215,0,0.12); color: #ffd700; border: 1px solid #ffd700;" onclick="applyStrategyPreset(25, 6, 6)">👑 Pro Top</button>
        <button id="btn-filter-multistack" class="btn" style="padding: 5px 10px; font-size: 10px; background: rgba(0,255,136,0.12); color: var(--primary); border: 1px solid var(--primary);" onclick="toggleFilterOnlyMultiStack()">✨ Só Mescladas</button>
      </div>

      <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px;">
        <div>
          <div style="font-size: 10px; color: var(--text-muted); margin-bottom: 2px;">Min R$: <strong id="bar-minval-txt" style="color: var(--primary);">R$ 0</strong></div>
          <input type="range" id="bar-minval" min="0" max="40" step="1" value="0" style="width: 100%; accent-color: var(--primary);" oninput="updateFilterMinValue(this.value)">
        </div>
        <div>
          <div style="font-size: 10px; color: var(--text-muted); margin-bottom: 2px;">Max Dist: <strong id="bar-maxdist-txt" style="color: #00d2ff;">8 km</strong></div>
          <input type="range" id="bar-maxdist" min="1" max="10" step="0.5" value="8" style="width: 100%; accent-color: #00d2ff;" oninput="updateFilterMaxDist(this.value)">
        </div>
        <div>
          <div style="font-size: 10px; color: var(--text-muted); margin-bottom: 2px;">Ganho/km: <strong id="bar-mingain-txt" style="color: #ffd700;">R$ 5/km</strong></div>
          <input type="range" id="bar-mingain" min="0" max="10" step="0.5" value="5" style="width: 100%; accent-color: #ffd700;" oninput="updateMinGain(this.value)">
        </div>
      </div>
    </div>

    <div id="full-stacks-container"></div>
  </section>

  <!-- #ANALYTICS (Dashboard de Visualização de Dados Recharts — Diário e Semanal) -->
  <section id="analytics" class="view-section">
    <div class="nav-pills">
      <a href="#dashboard" class="nav-pill">Cockpit</a>
      <a href="#stacks" class="nav-pill">Stacks</a>
      <a href="#analytics" class="nav-pill active">Analytics Recharts</a>
      <a href="#subscription" class="nav-pill">Plano Pro</a>
      <a href="#settings" class="nav-pill">Ajustes</a>
    </div>

    <!-- Cabeçalho do Dashboard de Ganhos -->
    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px; flex-wrap: wrap; gap: 8px;">
      <div>
        <h2 style="font-size: 20px; font-weight: 900; color: #ffffff; display: flex; align-items: center; gap: 8px;">
          <span>📊</span> Dashboard de Ganhos Recharts
        </h2>
        <p style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">
          Telemetria diária & semanal baseada nas corridas aceitas no sistema.
        </p>
      </div>
      <div style="display: flex; gap: 6px; align-items: center;">
        <span style="font-size: 10px; padding: 4px 8px; background: rgba(0,255,136,0.12); color: var(--primary); border-radius: 6px; border: 1px solid var(--primary); font-weight: 800;">
          🟢 SQLite Sincronizado
        </span>
        <button class="btn btn-green" style="padding: 6px 10px; font-size: 11px;" onclick="loadAnalytics()">🔄 Atualizar</button>
      </div>
    </div>

    <!-- 6 Cards de Indicadores Financeiros (KPIs) -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 10px; margin-bottom: 16px;">
      <div class="glass" style="padding: 12px 14px;">
        <div style="font-size: 11px; color: var(--text-muted);">Hoje (Bruto)</div>
        <div class="tabular neon-text" style="font-size: 20px; font-weight: 900;" id="stat-today">R$ 284,50</div>
        <div style="font-size: 10px; color: var(--primary); margin-top: 2px;">Meta diária R$ 350</div>
      </div>
      <div class="glass" style="padding: 12px 14px;">
        <div style="font-size: 11px; color: var(--text-muted);">Semana (Bruto)</div>
        <div class="tabular" style="font-size: 20px; font-weight: 900; color: #ffffff;" id="stat-week">R$ 1.420,80</div>
        <div style="font-size: 10px; color: #00d2ff; margin-top: 2px;">Meta semanal R$ 2.200</div>
      </div>
      <div class="glass" style="padding: 12px 14px;">
        <div style="font-size: 11px; color: var(--text-muted);">Lucro Líquido Real</div>
        <div class="tabular neon-text" style="font-size: 20px; font-weight: 900;" id="stat-profit">R$ 218,40</div>
        <div style="font-size: 10px; color: var(--text-muted); margin-top: 2px;">~81.4% margem pós-gasolina</div>
      </div>
      <div class="glass" style="padding: 12px 14px;">
        <div style="font-size: 11px; color: var(--text-muted);">Rendimento Médio</div>
        <div class="tabular" style="font-size: 20px; font-weight: 900; color: #ffd700;" id="stat-avg-km">R$ 7,45/km</div>
        <div style="font-size: 10px; color: #ffd700; margin-top: 2px;">+132% acima do piso</div>
      </div>
      <div class="glass" style="padding: 12px 14px;">
        <div style="font-size: 11px; color: var(--text-muted);">Mês (Acumulado)</div>
        <div class="tabular" style="font-size: 20px; font-weight: 900; color: #ffffff;" id="stat-month">R$ 5.680,00</div>
        <div style="font-size: 10px; color: var(--text-muted); margin-top: 2px;">30 dias persistidos</div>
      </div>
      <div class="glass" style="padding: 12px 14px;">
        <div style="font-size: 11px; color: var(--text-muted);">Corridas Aceitas</div>
        <div class="tabular" style="font-size: 20px; font-weight: 900; color: #ffffff;" id="stat-runs-total">11 viagens</div>
        <div style="font-size: 10px; color: var(--primary); margin-top: 2px;">100% liquidadas</div>
      </div>
    </div>

    <!-- Container Principal dos Gráficos Recharts (Montagem via React) -->
    <div class="glass" style="padding: 16px; margin-bottom: 16px; border: 1px solid rgba(0, 255, 136, 0.25);">
      <div id="recharts-dashboard-container">
        <!-- Gráfico Recharts é montado dinamicamente aqui -->
        <div style="text-align: center; padding: 30px; color: var(--text-muted); font-size: 12px;">
          ⏳ Carregando motor de renderização Recharts...
        </div>
      </div>

      <!-- Alerta Pro para histórico estendido -->
      <div id="analytics-free-lock" style="display:none; text-align: center; margin-top: 14px; padding: 10px 14px; background: rgba(255,215,0,0.1); border-radius: 8px; border: 1px solid #ffd700; font-size: 11px; color: #ffd700; font-weight: 700;">
        🔒 Plano Free: Histórico limitado a 3 dias. <a href="#subscription" style="color: #ffffff; text-decoration: underline; margin-left: 6px;">Ative o Jarvis Pro</a> para desbloquear 30 dias de telemetria completa e multi-stack!
      </div>
    </div>

    <!-- Extrato Detalhado de Corridas Aceitas Recentes -->
    <div class="glass" style="padding: 16px; margin-bottom: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px;">
        <div>
          <h3 style="font-size: 14px; font-weight: 800; color: #ffffff;">📋 Extrato de Corridas Aceitas Recentes</h3>
          <p style="font-size: 11px; color: var(--text-muted);">Histórico gravado no banco de dados SQLite</p>
        </div>
        <div style="display: flex; gap: 6px;">
          <button class="btn" style="padding: 5px 10px; font-size: 10px; background: rgba(0,255,136,0.12); color: var(--primary); border: 1px solid var(--primary);" onclick="simulateAcceptedRun()">⚡ Simular Corrida</button>
          <button class="btn" style="padding: 5px 10px; font-size: 10px; background: rgba(255,255,255,0.06); color: #ffffff; border: 1px solid var(--surface-border);" onclick="exportEarningsCSV()">📥 Exportar CSV</button>
        </div>
      </div>

      <div style="overflow-x: auto;">
        <table style="width: 100%; border-collapse: collapse; font-size: 11px; text-align: left;">
          <thead>
            <tr style="border-bottom: 1px solid var(--surface-border); color: var(--text-muted);">
              <th style="padding: 8px 6px;">Data</th>
              <th style="padding: 8px 6px;">App</th>
              <th style="padding: 8px 6px;">Distância</th>
              <th style="padding: 8px 6px;">Valor Bruto</th>
              <th style="padding: 8px 6px;">Combustível</th>
              <th style="padding: 8px 6px;">Lucro Líquido</th>
              <th style="padding: 8px 6px; text-align: right;">R$/km</th>
            </tr>
          </thead>
          <tbody id="accepted-runs-tbody">
            <!-- Linhas preenchidas via Javascript -->
          </tbody>
        </table>
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

  <!-- #SETTINGS (Configurações & Filtros de Rentabilidade) -->
  <section id="settings" class="view-section">
    <div class="nav-pills">
      <a href="#dashboard" class="nav-pill">Cockpit</a>
      <a href="#stacks" class="nav-pill">Stacks</a>
      <a href="#settings" class="nav-pill active">Ajustes & Filtros</a>
      <a href="#admin" class="nav-pill">Admin</a>
    </div>

    <h2 style="font-size: 18px; font-weight: 800; color: #ffffff; margin-bottom: 16px;">Configuração de Filtros do Cockpit</h2>

    <div class="glass" style="padding: 18px; display: flex; flex-direction: column; gap: 16px; margin-bottom: 16px;">
      <div style="font-size: 13px; font-weight: 800; color: var(--primary); letter-spacing: 0.5px; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 8px;">
        🎯 CRITÉRIOS DE DESPACHO E RENTABILIDADE
      </div>

      <!-- 1. VALOR MÍNIMO -->
      <div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
          <span style="font-size: 13px; font-weight: 700; color: #ffffff;">💰 Valor Mínimo por Corrida</span>
          <span style="font-size: 13px; font-weight: 800; color: var(--primary);" id="cfg-minval-text">R$ 0,00</span>
        </div>
        <input type="range" id="cfg-minval" min="0" max="40" step="1" value="0" style="width: 100%; accent-color: var(--primary);" oninput="updateFilterMinValue(this.value)">
        <div style="display: flex; gap: 6px; margin-top: 6px; flex-wrap: wrap;">
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateFilterMinValue(0)">Todos</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateFilterMinValue(15)">R$ 15+</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateFilterMinValue(22)">R$ 22+</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateFilterMinValue(30)">R$ 30+</button>
        </div>
      </div>

      <!-- 2. DISTÂNCIA MÁXIMA -->
      <div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
          <span style="font-size: 13px; font-weight: 700; color: #ffffff;">🛵 Distância Máxima de Deslocamento</span>
          <span style="font-size: 13px; font-weight: 800; color: #00d2ff;" id="cfg-maxdist-text">8.0 km</span>
        </div>
        <input type="range" id="cfg-maxdist" min="1" max="10" step="0.5" value="8" style="width: 100%; accent-color: #00d2ff;" oninput="updateFilterMaxDist(this.value)">
        <div style="display: flex; gap: 6px; margin-top: 6px; flex-wrap: wrap;">
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateFilterMaxDist(3)">Até 3 km</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateFilterMaxDist(5)">Até 5 km</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateFilterMaxDist(7)">Até 7 km</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateFilterMaxDist(10)">Sem Limite</button>
        </div>
      </div>

      <!-- 3. MULTIPLICADOR DE GANHO POR QUILÔMETRO -->
      <div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
          <span style="font-size: 13px; font-weight: 700; color: #ffffff;">⚡ Multiplicador Ganho por Km</span>
          <span style="font-size: 13px; font-weight: 800; color: #ffd700;" id="cfg-gain-text">R$ 5,00/km</span>
        </div>
        <input type="range" id="cfg-gain" min="0" max="10" step="0.5" value="5" style="width: 100%; accent-color: #ffd700;" oninput="updateMinGain(this.value)">
        <div style="display: flex; gap: 6px; margin-top: 6px; flex-wrap: wrap;">
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateMinGain(0)">Sem Mínimo</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateMinGain(4)">R$ 4/km</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateMinGain(5)">R$ 5/km</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateMinGain(6.5)">R$ 6,50/km</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateMinGain(8)">R$ 8/km</button>
        </div>
      </div>

      <!-- 4. BÔNUS MÍNIMO POR ENTREGA / GORJETA -->
      <div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
          <span style="font-size: 13px; font-weight: 700; color: #ffffff;">🎁 Bônus / Gorjeta Mínima</span>
          <span style="font-size: 13px; font-weight: 800; color: #00ff88;" id="cfg-bonus-text">Sem Mínimo</span>
        </div>
        <input type="range" id="cfg-bonus" min="0" max="20" step="1" value="0" style="width: 100%; accent-color: #00ff88;" oninput="updateMinBonus(this.value)">
        <div style="display: flex; gap: 6px; margin-top: 6px; flex-wrap: wrap;">
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateMinBonus(0)">Sem Mínimo</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateMinBonus(3)">+R$ 3</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateMinBonus(5)">+R$ 5</button>
          <button class="btn btn-sm" style="font-size: 10px; padding: 4px 8px;" onclick="updateMinBonus(10)">+R$ 10</button>
        </div>
      </div>

      <!-- PRESETS RÁPIDOS DE ESTRATÉGIA -->
      <div>
        <div style="font-size: 11px; font-weight: 800; color: var(--text-muted); margin-bottom: 6px; text-transform: uppercase;">
          Presets Rápidos de Piloto
        </div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 6px;">
          <button class="btn" style="font-size: 11px; padding: 8px; justify-content: flex-start; text-align: left;" onclick="applyStrategyPreset(22, 5, 6, 5)">🌧️ Chuva / Dinâmica</button>
          <button class="btn" style="font-size: 11px; padding: 8px; justify-content: flex-start; text-align: left;" onclick="applyStrategyPreset(12, 3.5, 5, 0)">⚡ Tiro Curto</button>
          <button class="btn" style="font-size: 11px; padding: 8px; justify-content: flex-start; text-align: left;" onclick="applyStrategyPreset(30, 7, 7, 8)">💎 Máximo Lucro</button>
          <button class="btn" style="font-size: 11px; padding: 8px; justify-content: flex-start; text-align: left;" onclick="applyStrategyPreset(0, 10, 0, 0)">🎯 Padrão Livre</button>
        </div>
      </div>

      <div style="border-top: 1px solid rgba(255,255,255,0.08); padding-top: 12px; display: flex; flex-direction: column; gap: 12px;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <div style="font-size: 13px; font-weight: 700; color: #ffffff;">Voz Jarvis (pt-BR)</div>
            <div style="font-size: 11px; color: var(--text-muted);">Alertas e leitura falada de corrida</div>
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

    <!-- LOGS DE EVENTOS E FALHAS DE PROCESSAMENTO (DEPURAÇÃO DE CAMPO) -->
    <div style="margin-top: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <div style="font-size: 14px; font-weight: 800; color: #ffffff; display: flex; align-items: center; gap: 6px;">
          <span>⚠️</span> Logs de Falhas de Ofertas (Campo)
        </div>
        <button class="btn btn-sm" onclick="fetchFailures()" style="font-size: 10px; padding: 4px 8px;">🔄 Atualizar</button>
      </div>

      <div id="admin-failure-logs" style="display: flex; flex-direction: column; gap: 8px;">
        <div style="font-size: 12px; color: var(--text-muted); text-align: center; padding: 12px;">Carregando logs de diagnóstico...</div>
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
      <span>Vel: <strong id="hud-speed-val" style="color: var(--primary);">0 km/h</strong></span>
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
      stacks: { active: [], pending: [], history: [], autoAccept: false, minGainPerKm: 5.0, minValue: 0.0, maxDistance: 8.0, minBonus: 0.0 },
      health: { score: 94, gpsAccuracy: 4.2, latency: 12, temperature: 28, speed: 0.0, isSafetyLock: false, isMoving: false },
      config: { voiceEnabled: true, focusModeAuto: true, theme: 'dark' }
    };

    function loadInitialState() {
      try {
        const stored = localStorage.getItem('RadarCoordinator_AppState');
        if (stored) {
          const parsed = JSON.parse(stored);
          return {
            user: Object.assign({}, defaultState.user, parsed.user || {}),
            session: Object.assign({}, defaultState.session, parsed.session || {}),
            earnings: Object.assign({}, defaultState.earnings, parsed.earnings || {}),
            stacks: Object.assign({}, defaultState.stacks, parsed.stacks || {}),
            health: Object.assign({}, defaultState.health, parsed.health || {}),
            config: Object.assign({}, defaultState.config, parsed.config || {})
          };
        }
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

    // Leitura inteligente de oferta em voz alta (Text-to-Speech Hands-Free)
    function readOfferAloud(appName, restaurant, valor, distKm, gainKm, timeMin) {
      const rec = gainKm >= 5.0 ? 'Recomendação Jarvis: Aceitar corrida vantajosa.' : (gainKm >= 3.5 ? 'Recomendação Jarvis: Distância curta compensa.' : 'Atenção: Ganho por quilômetro abaixo do ideal.');
      const valStr = valor.toFixed(2).replace('.', ',');
      const kmStr = distKm.toFixed(1).replace('.', ',');
      const gainStr = gainKm.toFixed(2).replace('.', ',');
      const speechText = `Oferta ${appName}. Estabelecimento: ${restaurant}. Valor: ${valStr} reais para ${kmStr} quilômetros, rendendo ${gainStr} por quilômetro. Tempo estimado de ${timeMin} minutos. ${rec}`;
      speak(speechText);
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

    // Monitor de Velocidade e Trava de Segurança em Movimento (Android Location API)
    function updateSpeed(speedKmh, source = 'GPS') {
      const prevLock = window.AppState.health.isSafetyLock || false;
      const isLock = speedKmh > 10.0;
      const isMove = speedKmh > 2.0;

      window.AppState.health.speed = speedKmh;
      window.AppState.health.isSafetyLock = isLock;
      window.AppState.health.isMoving = isMove;
      window.AppState.health.speedSource = source;

      const hudSpeed = document.getElementById('hud-speed-val');
      if (hudSpeed) {
        hudSpeed.innerText = `${speedKmh.toFixed(0)} km/h`;
        hudSpeed.style.color = isLock ? '#ff4757' : (isMove ? '#00ff88' : '#8e92a8');
      }

      const dashSpeed = document.getElementById('dash-speed-display');
      if (dashSpeed) {
        dashSpeed.innerText = speedKmh.toFixed(0);
        dashSpeed.style.color = isLock ? '#ff4757' : '#00ff88';
      }

      const moveStatus = document.getElementById('speed-movement-status');
      if (moveStatus) {
        moveStatus.innerHTML = isMove ? (isLock ? '<span style="color:#ff4757; font-weight:bold;">🚨 Em Movimento (> 10 km/h)</span>' : '<span style="color:#00ff88; font-weight:bold;">🏍️ Em Movimento (Lento)</span>') : '<span style="color:#8e92a8;">🟢 Moto Parada</span>';
      }

      const lockBanner = document.getElementById('speed-safety-lock-banner');
      if (lockBanner) {
        lockBanner.style.display = isLock ? 'block' : 'none';
      }

      const lockBadge = document.getElementById('speed-lock-badge');
      if (lockBadge) {
        lockBadge.innerHTML = isLock 
          ? '<span style="color:#ff4757; font-weight:900; font-size:11px;">🚨 BLOQUEIO ATIVO (> 10 km/h)</span>' 
          : '<span style="color:#00ff88; font-weight:800; font-size:11px;">🛡️ TOQUE LIVRE (<= 10 km/h)</span>';
      }

      // Desativa a interface manual quando a velocidade exceder 10 km/h para segurança do piloto
      document.querySelectorAll('.stack-btn-row button, .filter-chip, .btn-maps, .btn-app-action').forEach(btn => {
        btn.disabled = isLock;
        btn.style.opacity = isLock ? '0.35' : '1';
        btn.style.pointerEvents = isLock ? 'none' : 'auto';
      });

      const fullStacksCont = document.getElementById('full-stacks-container');
      if (fullStacksCont) {
        fullStacksCont.style.filter = isLock ? 'grayscale(0.7) opacity(0.35)' : 'none';
        fullStacksCont.style.pointerEvents = isLock ? 'none' : 'auto';
      }

      if (!prevLock && isLock) {
        speak("Atenção: moto em movimento acima de 10 por hora. Trava de segurança ativada usando GPS. Interface desativada.");
        startVoiceListening(true);
      } else if (prevLock && !isLock) {
        speak("Velocidade abaixo de 10 por hora. Interface de pedidos liberada.");
      }

      saveState();
    }

    // Sistema de Reconhecimento de Comandos de Voz Mãos-Livres (Web Speech API)
    let speechRecognizer = null;
    let isListening = false;

    function initVoiceRecognition() {
      const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
      if (!SpeechRec) {
        console.log("Reconhecimento de fala nativo não suportado neste navegador.");
        return;
      }
      speechRecognizer = new SpeechRec();
      speechRecognizer.lang = 'pt-BR';
      speechRecognizer.continuous = true;
      speechRecognizer.interimResults = false;

      speechRecognizer.onstart = () => {
        isListening = true;
        updateVoiceButtonState(true);
      };

      speechRecognizer.onend = () => {
        isListening = false;
        updateVoiceButtonState(false);
        // Se ainda estiver com a trava de segurança ativada e voz habilitada, reinicia escuta
        if (window.AppState.health.isSafetyLock && window.AppState.config.voiceEnabled) {
          try { speechRecognizer.start(); } catch (e) {}
        }
      };

      speechRecognizer.onerror = (e) => {
        console.log("Erro no microfone:", e.error);
        isListening = false;
        updateVoiceButtonState(false);
      };

      speechRecognizer.onresult = (event) => {
        const lastResult = event.results[event.results.length - 1];
        if (lastResult.isFinal) {
          const transcript = lastResult[0].transcript.trim().toLowerCase();
          console.log("Comando de voz ouvido:", transcript);
          handleVoiceCommand(transcript);
        }
      };
    }

    function updateVoiceButtonState(active) {
      const btn = document.getElementById('btn-voz');
      if (btn) btn.classList.toggle('listening', active);
      const btnSafe = document.getElementById('btn-mic-safety');
      if (btnSafe) {
        btnSafe.innerText = active ? '🔴 Ouvindo...' : '🎙️ Falar Agora';
        btnSafe.style.background = active ? 'rgba(255, 71, 87, 0.4)' : 'rgba(255, 255, 255, 0.1)';
      }
    }

    function startVoiceListening(auto = false) {
      if (!window.AppState.config.voiceEnabled) return;
      if (!speechRecognizer) initVoiceRecognition();
      if (speechRecognizer && !isListening) {
        try {
          speechRecognizer.start();
        } catch (e) {}
      }
    }

    function toggleVoiceRecognition() {
      if (!speechRecognizer) initVoiceRecognition();
      if (!speechRecognizer) {
        speak("Microfone não disponível neste navegador. Use os botões de simulação.");
        return;
      }
      if (isListening) {
        speechRecognizer.stop();
        speak("Microfone em repouso.");
      } else {
        try {
          speechRecognizer.start();
          speak("Ouvindo no capacete. Pode dizer aceitar ou recusar.");
        } catch (e) {
          speak("Erro ao abrir microfone.");
        }
      }
    }

    // Interpretador neural de comandos em português
    function handleVoiceCommand(cmd) {
      const toast = document.getElementById('voice-toast');
      const msg = document.getElementById('voice-toast-msg');
      if (toast && msg) {
        msg.innerText = `Comando ouvido: "${cmd}"`;
        toast.style.display = 'block';
        clearTimeout(window._vTimeout);
        window._vTimeout = setTimeout(() => { toast.style.display = 'none'; }, 3000);
      }

      // 1. Comandos de Filtro por Voz (Direção Segura / Mãos-Livres)
      if (cmd.includes('chuva') || cmd.includes('tarifa dinâmica') || cmd.includes('temporal')) {
        applyStrategyPreset(22, 5, 6, 5);
        speak('Filtro chuva e alta demanda ativado. Mínimo 22 reais.');
      } else if (cmd.includes('tiro curto') || cmd.includes('filtro curto') || cmd.includes('curtas') || cmd.includes('curta distância')) {
        applyStrategyPreset(12, 3.5, 5, 0);
        speak('Filtro tiro curto ativado. Raio máximo de 3 quilômetros e meio.');
      } else if (cmd.includes('máximo lucro') || cmd.includes('maximo lucro') || cmd.includes('filtro lucro') || cmd.includes('alta rentabilidade')) {
        applyStrategyPreset(30, 7, 7, 8);
        speak('Filtro máximo lucro ativado. Mínimo 30 reais.');
      } else if (cmd.includes('limpar filtro') || cmd.includes('limpar filtros') || cmd.includes('resetar filtro') || cmd.includes('resetar filtros') || cmd.includes('redefinir') || cmd.includes('padrão livre') || cmd.includes('sem filtro')) {
        applyStrategyPreset(0, 10, 0, 0);
        speak('Filtros redefinidos. Exibindo todas as entregas disponíveis.');
      } else if (cmd.includes('somente mesclada') || cmd.includes('somente mescladas') || cmd.includes('só mesclada') || cmd.includes('só mescladas') || cmd.includes('filtro mesclada') || cmd.includes('filtro mescladas') || cmd.includes('multi stack')) {
        window.AppState.stacks.onlyMultiStack = true;
        saveState();
        const btn = document.getElementById('btn-filter-multistack');
        if (btn) {
          btn.style.background = 'var(--primary)';
          btn.style.color = '#0a0a0f';
        }
        speak('Filtro ativado: exibindo apenas entregas combinadas mescladas.');
        if (window.AppState.stacks.pending) renderFullStacks(window.AppState.stacks.pending);
      } else if (cmd.includes('mínimo 15') || cmd.includes('minimo 15') || cmd.includes('quinze reais') || cmd.includes('15 reais')) {
        updateFilterMinValue(15);
        speak('Filtro alterado: valor mínimo 15 reais.');
      } else if (cmd.includes('mínimo 20') || cmd.includes('minimo 20') || cmd.includes('vinte reais') || cmd.includes('20 reais')) {
        updateFilterMinValue(20);
        speak('Filtro alterado: valor mínimo 20 reais.');
      } else if (cmd.includes('mínimo 30') || cmd.includes('minimo 30') || cmd.includes('trinta reais') || cmd.includes('30 reais')) {
        updateFilterMinValue(30);
        speak('Filtro alterado: valor mínimo 30 reais.');
      // 2. Comandos de Ação de Corrida e Navegação
      } else if (cmd.includes('ler') || cmd.includes('ouvir') || cmd.includes('falar') || cmd.includes('detalhes') || cmd.includes('anunciar')) {
        triggerVoiceCommand('ouvir');
      } else if (cmd.includes('aceitar') || cmd.includes('aceita') || cmd.includes('pegar') || cmd.includes('confirmar') || cmd.includes('sim')) {
        triggerVoiceCommand('aceitar');
      } else if (cmd.includes('recusar') || cmd.includes('recusa') || cmd.includes('rejeitar') || cmd.includes('passar') || cmd.includes('cancelar') || cmd.includes('não')) {
        triggerVoiceCommand('recusar');
      } else if (cmd.includes('rota') || cmd.includes('navegar') || cmd.includes('mapa') || cmd.includes('maps')) {
        triggerVoiceCommand('rota');
      } else if (cmd.includes('ganhos') || cmd.includes('saldo') || cmd.includes('quanto ganhei')) {
        speak(`Seus ganhos hoje são de R$ ${window.AppState.earnings.today.toFixed(2).replace('.', ',')}.`);
      } else if (cmd.includes('saúde') || cmd.includes('status')) {
        speak(`Índice de saúde em 94 de 100. GPS com precisão de 4 metros.`);
      }
    }

    function triggerVoiceCommand(action) {
      const firstCard = document.querySelector('#dash-stacks-container .stack-card:not([style*="display: none"])');
      if (!firstCard) {
        speak("Não há pedidos pendentes no momento.");
        return;
      }

      if (action === 'ouvir') {
        const ttsBtn = firstCard.querySelector('button[onclick*="readOfferAloud"]');
        if (ttsBtn) {
          ttsBtn.click();
        } else {
          speak("Lendo melhor oferta pendente.");
        }
      } else if (action === 'aceitar') {
        const acceptBtn = firstCard.querySelector('.btn-green');
        if (acceptBtn) {
          speak("Comando de voz reconhecido: Aceitando oferta!");
          acceptBtn.click();
        }
      } else if (action === 'recusar') {
        const recBtn = firstCard.querySelector('.btn-red');
        if (recBtn) {
          speak("Comando de voz reconhecido: Recusando oferta.");
          recBtn.click();
        }
      } else if (action === 'rota') {
        const mapsBtn = firstCard.querySelector('.btn-maps');
        if (mapsBtn) {
          mapsBtn.click();
        }
      }
    }

    let lastGpsCoords = null;
    let lastGpsTimestamp = null;

    function initGeoLocationTracking() {
      if ('geolocation' in navigator) {
        speak("Sintonizando satélites GPS do dispositivo...");
        navigator.geolocation.watchPosition(
          pos => {
            let spd = 0;
            if (pos.coords.speed !== null && pos.coords.speed >= 0) {
              spd = pos.coords.speed * 3.6; // m/s para km/h
            } else if (lastGpsCoords && lastGpsTimestamp) {
              const dt = (pos.timestamp - lastGpsTimestamp) / 1000.0;
              if (dt > 0.5 && dt < 30.0) {
                const R = 6371e3; // Raio da Terra em metros
                const phi1 = lastGpsCoords.latitude * Math.PI / 180;
                const phi2 = pos.coords.latitude * Math.PI / 180;
                const deltaPhi = (pos.coords.latitude - lastGpsCoords.latitude) * Math.PI / 180;
                const deltaLambda = (pos.coords.longitude - lastGpsCoords.longitude) * Math.PI / 180;
                const a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                          Math.cos(phi1) * Math.cos(phi2) *
                          Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
                const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                const distM = R * c;
                spd = (distM / dt) * 3.6;
              }
            }
            lastGpsCoords = { latitude: pos.coords.latitude, longitude: pos.coords.longitude };
            lastGpsTimestamp = pos.timestamp;
            updateSpeed(spd, 'GPS Fused');
          },
          err => {
            console.log("GPS local:", err.message);
          },
          { enableHighAccuracy: true, maximumAge: 1000, timeout: 5000 }
        );
      } else {
        speak("Geolocalização não suportada no navegador atual.");
      }
    }

    function updateFilterMinValue(v) {
      window.AppState.stacks.minValue = parseFloat(v);
      const t = parseFloat(v) > 0 ? `R$ ${parseFloat(v).toFixed(2).replace('.', ',')}` : 'Sem Mínimo';
      const el1 = document.getElementById('cfg-minval-text');
      const el2 = document.getElementById('bar-minval-txt');
      const slider1 = document.getElementById('cfg-minval');
      const slider2 = document.getElementById('bar-minval');
      if (el1) el1.innerText = t;
      if (el2) el2.innerText = parseFloat(v) > 0 ? `R$ ${parseFloat(v).toFixed(0)}` : 'R$ 0';
      if (slider1) slider1.value = v;
      if (slider2) slider2.value = v;
      saveState();
      if (window.AppState.stacks.pending) renderFullStacks(window.AppState.stacks.pending);
    }

    function updateFilterMaxDist(v) {
      window.AppState.stacks.maxDistance = parseFloat(v);
      const t = parseFloat(v) < 10 ? `${parseFloat(v).toFixed(1)} km` : 'Sem Limite';
      const el1 = document.getElementById('cfg-maxdist-text');
      const el2 = document.getElementById('bar-maxdist-txt');
      const slider1 = document.getElementById('cfg-maxdist');
      const slider2 = document.getElementById('bar-maxdist');
      if (el1) el1.innerText = t;
      if (el2) el2.innerText = parseFloat(v) < 10 ? `${parseFloat(v).toFixed(1)} km` : 'Livre';
      if (slider1) slider1.value = v;
      if (slider2) slider2.value = v;
      saveState();
      if (window.AppState.stacks.pending) renderFullStacks(window.AppState.stacks.pending);
    }

    function updateMinGain(v) {
      window.AppState.stacks.minGainPerKm = parseFloat(v);
      const t = parseFloat(v) > 0 ? `R$ ${parseFloat(v).toFixed(2).replace('.', ',')}/km` : 'Sem Mínimo';
      const el1 = document.getElementById('cfg-gain-text');
      const el2 = document.getElementById('bar-mingain-txt');
      const slider1 = document.getElementById('cfg-gain');
      const slider2 = document.getElementById('bar-mingain');
      if (el1) el1.innerText = t;
      if (el2) el2.innerText = parseFloat(v) > 0 ? `R$ ${parseFloat(v).toFixed(1)}/km` : 'R$ 0/km';
      if (slider1) slider1.value = v;
      if (slider2) slider2.value = v;
      saveState();
      if (window.AppState.stacks.pending) renderFullStacks(window.AppState.stacks.pending);
    }

    function updateMinBonus(v) {
      window.AppState.stacks.minBonus = parseFloat(v);
      const t = parseFloat(v) > 0 ? `+R$ ${parseFloat(v).toFixed(2).replace('.', ',')}` : 'Sem Mínimo';
      const el1 = document.getElementById('cfg-bonus-text');
      const slider1 = document.getElementById('cfg-bonus');
      if (el1) el1.innerText = t;
      if (slider1) slider1.value = v;
      saveState();
      if (window.AppState.stacks.pending) renderFullStacks(window.AppState.stacks.pending);
    }

    function applyStrategyPreset(minVal, maxDist, minGain, minBonus = 0) {
      updateFilterMinValue(minVal);
      updateFilterMaxDist(maxDist);
      updateMinGain(minGain);
      updateMinBonus(minBonus);
      speak(`Filtros de estratégia aplicados: Mínimo R$ ${minVal}, raio ${maxDist} km, ganho R$ ${minGain} por km.`);
    }

    function toggleFilterOnlyMultiStack() {
      window.AppState.stacks.onlyMultiStack = !window.AppState.stacks.onlyMultiStack;
      saveState();
      const btn = document.getElementById('btn-filter-multistack');
      if (btn) {
        btn.style.background = window.AppState.stacks.onlyMultiStack ? 'var(--primary)' : 'rgba(0, 255, 136, 0.12)';
        btn.style.color = window.AppState.stacks.onlyMultiStack ? '#0a0a0f' : 'var(--primary)';
        btn.style.fontWeight = window.AppState.stacks.onlyMultiStack ? '900' : '700';
      }
      speak(window.AppState.stacks.onlyMultiStack ? 'Filtro ativado: exibindo apenas entregas mescladas multi-stack.' : 'Filtro de mescladas desativado.');
      if (window.AppState.stacks.pending) renderFullStacks(window.AppState.stacks.pending);
    }

    function initFilterUI() {
      const minVal = window.AppState.stacks.minValue || 0.0;
      const maxDist = window.AppState.stacks.maxDistance || 8.0;
      const minGain = window.AppState.stacks.minGainPerKm || 0.0;
      const minBonus = window.AppState.stacks.minBonus || 0.0;

      updateFilterMinValue(minVal);
      updateFilterMaxDist(maxDist);
      updateMinGain(minGain);
      updateMinBonus(minBonus);

      const chkV = document.getElementById('cfg-v');
      if (chkV) chkV.checked = !!window.AppState.config.voiceEnabled;
      const btnVoz = document.getElementById('btn-voz');
      if (btnVoz) btnVoz.classList.toggle('active', !!window.AppState.config.voiceEnabled);

      const chkF = document.getElementById('cfg-f');
      if (chkF) chkF.checked = !!window.AppState.config.focusModeAuto;
      const btnFoco = document.getElementById('btn-foco');
      if (btnFoco) btnFoco.classList.toggle('active', !!window.AppState.config.focusModeAuto);

      const btnMulti = document.getElementById('btn-filter-multistack');
      if (btnMulti) {
        btnMulti.style.background = window.AppState.stacks.onlyMultiStack ? 'var(--primary)' : 'rgba(0, 255, 136, 0.12)';
        btnMulti.style.color = window.AppState.stacks.onlyMultiStack ? '#0a0a0f' : 'var(--primary)';
        btnMulti.style.fontWeight = window.AppState.stacks.onlyMultiStack ? '900' : '700';
      }
    }

    function triggerGhostSweep() {
      speak('Varrendo corredor viário por sequências fantasma e rotas mescladas.');
      window.AppState.stacks.onlyMultiStack = true;
      saveState();
      const btn = document.getElementById('btn-filter-multistack');
      if (btn) {
        btn.style.background = 'var(--primary)';
        btn.style.color = '#0a0a0f';
        btn.style.fontWeight = '900';
      }
      setTimeout(() => {
        speak('Sinergia detectada entre Burger King Paulista e Pizza Hut Jardins! R$ 33 por 4.2 km.');
        location.hash = '#stacks';
        fetchStacks();
      }, 700);
    }

    function openMapsRoute(origin, waypoint, destination, stackId) {
      speak('Sincronizando rota com Google Maps. Navegação multi-ponto ativada.');
      let url = '';
      const dest = encodeURIComponent(destination || 'Sao Paulo, SP');
      const orig = encodeURIComponent(origin || 'Minha Localizacao');
      if (waypoint) {
        const way = encodeURIComponent(waypoint);
        url = `https://www.google.com/maps/dir/?api=1&origin=${orig}&destination=${dest}&waypoints=${way}&travelmode=two_wheeler`;
      } else {
        url = `https://www.google.com/maps/dir/?api=1&origin=${orig}&destination=${dest}&travelmode=two_wheeler`;
      }
      window.open(url, '_blank');
    }

    function trackAnalyticsEvent(name, params = {}) {
      try {
        fetch('/api/analytics', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ event_name: name, params: params })
        }).catch(() => {});
      } catch (e) {}
    }

    // acceptStack(btn, valor): destaca verde, atualiza ganhos, remove outros, chama POST /api/stacks/accept
    async function acceptStack(btn, valor, stackId, shouldOpenMaps = true) {
      const card = btn.closest('.stack-card');
      if (card) card.classList.add('accepted');

      trackAnalyticsEvent('offer_accept_clicked', { stack_id: stackId, amount: valor, source: 'web_cockpit' });

      speak(`Stack aceito! R$ ${valor.toFixed(2).replace('.', ',')}.`);
      window.AppState.earnings.today += valor;
      saveState();

      try {
        await fetch('/api/stacks/accept', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ stack_id: stackId })
        });
        loadAnalytics();
      } catch (e) {}

      // Se for stack mesclado de 2 pedidos ou solicitado, sincroniza rota do Maps automaticamente
      if (shouldOpenMaps) {
        setTimeout(() => {
          if (stackId === 'stk_01' || stackId === 'stk_04' || stackId === 'stk_06' || stackId === 'stk_08') {
            openMapsRoute('Burger King Avenida Paulista, Sao Paulo', 'Pizza Hut Alameda Santos, Sao Paulo', 'Edificio Paulista Corporate, Sao Paulo', stackId);
          }
        }, 300);
      }

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
      const minVal = window.AppState.stacks.minValue || 0.0;
      const maxDist = window.AppState.stacks.maxDistance || 10.0;
      const minGain = window.AppState.stacks.minGainPerKm || 0.0;
      const minBonus = window.AppState.stacks.minBonus || 0.0;
      const onlyMulti = window.AppState.stacks.onlyMultiStack || false;

      const filtered = list.filter(s => {
        const gain = s.total_value / s.distance_km;
        if (onlyMulti && !s.apps.includes('+')) return false;
        if (s.total_value < minVal) return false;
        if (s.distance_km > maxDist) return false;
        if (gain < minGain) return false;
        if (minBonus > 0) {
          const estimatedBonus = s.apps.includes('+') ? 6.0 : 0.0;
          if (estimatedBonus < minBonus) return false;
        }
        return true;
      });

      const badge = document.getElementById('stacks-filter-badge');
      if (badge) {
        badge.innerText = `${filtered.length} de ${list.length} disponíveis${onlyMulti ? ' (✨ Só Mescladas)' : ''}`;
        badge.style.color = (onlyMulti || minVal > 0 || maxDist < 8.0 || minGain > 0 || minBonus > 0) ? 'var(--primary)' : 'var(--text-muted)';
      }

      if (filtered.length === 0) {
        cont.innerHTML = `
          <div class="glass" style="padding: 24px; text-align: center;">
            <div style="font-size: 32px; margin-bottom: 8px;">🎚️</div>
            <div style="font-size: 15px; font-weight: 800; color: #ffffff; margin-bottom: 6px;">Nenhum stack nos critérios atuais</div>
            <div style="font-size: 12px; color: var(--text-muted); margin-bottom: 14px;">
              Existem ${list.length} pedidos disponíveis, mas nenhum atende aos filtros de R$ ${minVal.toFixed(2)}, ${maxDist} km e R$ ${minGain.toFixed(2)}/km.
            </div>
            <button class="btn btn-green" style="margin: 0 auto;" onclick="applyStrategyPreset(0, 10, 0)">Redefinir Filtros</button>
          </div>
        `;
        return;
      }

      cont.innerHTML = filtered.map(s => {
        const gain = (s.total_value / s.distance_km).toFixed(2);
        const isMulti = s.apps.includes('+');
        const restParts = s.restaurant.split('&');
        const r1 = restParts[0] ? restParts[0].trim() : s.restaurant;
        const r2 = restParts[1] ? restParts[1].trim() : null;
        return `
          <div class="stack-card ${isMulti ? 'multi' : ''}">
            <div class="stack-header">
              <div>
                <span class="app-dot ${s.apps.includes('iFood') ? 'app-ifood' : (s.apps.includes('Rappi') ? 'app-rappi' : 'app-99')}">${s.apps}</span>
                ${isMulti ? '<span style="background: rgba(0, 255, 136, 0.2); color: var(--primary); font-size: 10px; font-weight: 800; padding: 2px 6px; border-radius: 4px; margin-left: 6px; border: 1px solid var(--primary);">✨ MESCLADA</span>' : ''}
                <div style="font-size: 14px; font-weight: 800; color: #ffffff; margin-top: 6px;">${s.restaurant}</div>
              </div>
              <div style="text-align: right;">
                <div class="stack-price tabular">R$ ${s.total_value.toFixed(2).replace('.', ',')}</div>
                <div class="stack-gain-tag tabular">R$ ${gain}/km</div>
              </div>
            </div>
            <div class="stack-route-display">
              <span>● ${r1}</span> <span class="route-arrow">➔</span> 
              ${r2 ? `<span>● ${r2}</span> <span class="route-arrow">➔</span>` : ''}
              <span>🏢 Entrega</span>
              <span style="margin-left: auto; color: var(--text-muted);">${s.distance_km} km • ${s.time_min} min</span>
            </div>
            <div class="stack-btn-row">
              <button class="btn btn-red" onclick="declineStack(this, '${s.id}')">❌ Recusar</button>
              <button class="btn" style="background: rgba(255, 215, 0, 0.15); color: #ffd700; border: 1px solid #ffd700;" onclick="readOfferAloud('${s.apps}', '${s.restaurant}', ${s.total_value}, ${s.distance_km}, ${gain}, ${s.time_min})">🔊 Ouvir</button>
              <button class="btn btn-maps" onclick="openMapsRoute('${r1}, Sao Paulo', ${r2 ? `'${r2}, Sao Paulo'` : 'null'}, 'Sao Paulo, SP', '${s.id}')">🗺️ Maps Rota</button>
              <button class="btn btn-green" onclick="acceptStack(this, ${s.total_value}, '${s.id}')">✅ Aceitar</button>
            </div>
          </div>
        `;
      }).join('');
    }

    // =========================================================================
    // RECHARTS DATA VISUALIZATION DASHBOARD (HISTÓRICO DIÁRIO E SEMANAL)
    // =========================================================================
    let currentRechartsPeriod = 'daily_7d'; // 'daily_7d', 'daily_30d', 'weekly', 'apps'

    function setRechartsPeriod(period) {
      currentRechartsPeriod = period;
      if (cachedEarningsData) {
        renderRechartsDashboard(cachedEarningsData);
      }
    }

    function renderRechartsDashboard(data) {
      if (!data) return;
      cachedEarningsData = data;

      const container = document.getElementById('recharts-dashboard-container');
      if (!container) return;

      const isPro = (window.AppState && window.AppState.user && window.AppState.user.plan === 'pro');
      const isRechartsAvailable = (typeof window.Recharts !== 'undefined' && typeof window.React !== 'undefined' && typeof window.ReactDOM !== 'undefined');

      if (!isRechartsAvailable) {
        // Fallback dinâmico caso o CDN de terceiros demore ou esteja offline
        renderRechartsFallback(container, data, currentRechartsPeriod, isPro);
        // Tenta re-renderizar assim que o script terminar de carregar
        setTimeout(() => {
          if (typeof window.Recharts !== 'undefined') renderRechartsDashboard(data);
        }, 1500);
        return;
      }

      try {
        const {
          ResponsiveContainer,
          ComposedChart,
          BarChart,
          Bar,
          LineChart,
          Line,
          AreaChart,
          Area,
          PieChart,
          Pie,
          Cell,
          XAxis,
          YAxis,
          CartesianGrid,
          Tooltip,
          Legend,
          ReferenceLine
        } = window.Recharts;

        const h = window.React.createElement;

        // Tooltip Customizado Recharts com Design Glassmorphism
        function CustomRechartsTooltip({ active, payload, label }) {
          if (!active || !payload || !payload.length) return null;
          const pData = payload[0].payload || {};
          return h('div', {
            style: {
              background: 'rgba(17, 17, 24, 0.95)',
              backdropFilter: 'blur(12px)',
              border: '1px solid rgba(0, 255, 136, 0.4)',
              borderRadius: '10px',
              padding: '12px 14px',
              boxShadow: '0 8px 24px rgba(0,0,0,0.6)',
              fontSize: '12px',
              color: '#ffffff',
              minWidth: '190px'
            }
          }, [
            h('div', { key: 'lbl', style: { fontWeight: '800', color: 'var(--primary)', marginBottom: '6px', borderBottom: '1px solid rgba(255,255,255,0.08)', paddingBottom: '4px' } }, 
              label || pData.day_name || pData.name || 'Registro'
            ),
            pData.amount !== undefined && h('div', { key: 'amt', style: { display: 'flex', justifyContent: 'space-between', margin: '3px 0' } }, [
              h('span', { style: { color: 'var(--text-muted)' } }, 'Faturamento Bruto:'),
              h('strong', { style: { color: '#00ff88' } }, `R$ ${Number(pData.amount).toFixed(2).replace('.', ',')}`)
            ]),
            pData.gross !== undefined && h('div', { key: 'gross', style: { display: 'flex', justifyContent: 'space-between', margin: '3px 0' } }, [
              h('span', { style: { color: 'var(--text-muted)' } }, 'Bruto Acumulado:'),
              h('strong', { style: { color: '#00ff88' } }, `R$ ${Number(pData.gross).toFixed(2).replace('.', ',')}`)
            ]),
            pData.net_profit !== undefined && h('div', { key: 'net', style: { display: 'flex', justifyContent: 'space-between', margin: '3px 0' } }, [
              h('span', { style: { color: 'var(--text-muted)' } }, 'Lucro Líquido:'),
              h('strong', { style: { color: '#00d2ff' } }, `R$ ${Number(pData.net_profit).toFixed(2).replace('.', ',')}`)
            ]),
            pData.net !== undefined && h('div', { key: 'net_w', style: { display: 'flex', justifyContent: 'space-between', margin: '3px 0' } }, [
              h('span', { style: { color: 'var(--text-muted)' } }, 'Lucro Líquido:'),
              h('strong', { style: { color: '#00d2ff' } }, `R$ ${Number(pData.net).toFixed(2).replace('.', ',')}`)
            ]),
            pData.fuel_cost !== undefined && h('div', { key: 'fuel', style: { display: 'flex', justifyContent: 'space-between', margin: '3px 0' } }, [
              h('span', { style: { color: 'var(--text-muted)' } }, 'Combustível:'),
              h('strong', { style: { color: '#ff4757' } }, `- R$ ${Number(pData.fuel_cost).toFixed(2).replace('.', ',')}`)
            ]),
            pData.fuel !== undefined && h('div', { key: 'fuel_w', style: { display: 'flex', justifyContent: 'space-between', margin: '3px 0' } }, [
              h('span', { style: { color: 'var(--text-muted)' } }, 'Combustível:'),
              h('strong', { style: { color: '#ff4757' } }, `- R$ ${Number(pData.fuel).toFixed(2).replace('.', ',')}`)
            ]),
            pData.km !== undefined && h('div', { key: 'km', style: { display: 'flex', justifyContent: 'space-between', margin: '3px 0' } }, [
              h('span', { style: { color: 'var(--text-muted)' } }, 'Km Rodados:'),
              h('strong', { style: { color: '#ffffff' } }, `${Number(pData.km).toFixed(1)} km`)
            ]),
            pData.gain_per_km !== undefined && h('div', { key: 'gpkm', style: { display: 'flex', justifyContent: 'space-between', margin: '3px 0' } }, [
              h('span', { style: { color: 'var(--text-muted)' } }, 'Ganho / Km:'),
              h('strong', { style: { color: '#ffd700' } }, `R$ ${Number(pData.gain_per_km).toFixed(2).replace('.', ',')}/km`)
            ]),
            pData.count !== undefined && h('div', { key: 'cnt', style: { display: 'flex', justifyContent: 'space-between', margin: '3px 0' } }, [
              h('span', { style: { color: 'var(--text-muted)' } }, 'Corridas Aceitas:'),
              h('strong', { style: { color: '#ffffff' } }, `${pData.count} viagens`)
            ]),
            pData.is_best_day && h('div', { key: 'best', style: { marginTop: '6px', padding: '3px 6px', background: 'rgba(255,215,0,0.15)', border: '1px solid #ffd700', borderRadius: '4px', fontSize: '10px', color: '#ffd700', textAlign: 'center', fontWeight: '800' } },
              '🏆 Melhor Faturamento da Semana!'
            )
          ]);
        }

        function RechartsApp() {
          const period = currentRechartsPeriod;
          let chartElement = null;
          let summaryTitle = '';
          let summarySub = '';

          if (period === 'daily_7d') {
            summaryTitle = 'Histórico Diário de Ganhos (Últimos 7 Dias)';
            summarySub = 'Barras Recharts de Faturamento Bruto vs Lucro Líquido Real e Combustível.';
            const chartData = data.chart_7d || [];

            chartElement = h(ResponsiveContainer, { width: '100%', height: 270 },
              h(ComposedChart, { data: chartData, margin: { top: 15, right: 10, left: -20, bottom: 5 } }, [
                h(CartesianGrid, { key: 'grid', strokeDasharray: '3 3', stroke: 'rgba(255,255,255,0.06)' }),
                h(XAxis, { key: 'x', dataKey: 'short_date', stroke: '#8e95a5', fontSize: 11, tickLine: false }),
                h(YAxis, { key: 'y', stroke: '#8e95a5', fontSize: 10, tickLine: false, tickFormatter: v => `R$${v}` }),
                h(Tooltip, { key: 'tt', content: h(CustomRechartsTooltip) }),
                h(Legend, { key: 'leg', wrapperStyle: { paddingTop: '8px', fontSize: '11px' } }),
                h(ReferenceLine, { key: 'ref', y: 350, stroke: '#ffd700', strokeDasharray: '4 4', label: { value: 'Meta R$ 350', fill: '#ffd700', fontSize: 10, position: 'top' } }),
                h(Bar, { key: 'b_amt', dataKey: 'amount', name: 'Bruto (R$)', fill: '#00ff88', radius: [4, 4, 0, 0] }),
                h(Bar, { key: 'b_net', dataKey: 'net_profit', name: 'Líquido (R$)', fill: '#00d2ff', radius: [4, 4, 0, 0] }),
                h(Bar, { key: 'b_fuel', dataKey: 'fuel_cost', name: 'Combustível (R$)', fill: '#ff4757', radius: [4, 4, 0, 0] })
              ])
            );
          } else if (period === 'daily_30d') {
            summaryTitle = 'Histórico Diário Expandido (30 Dias)';
            summarySub = isPro ? 'Telemetria integral com gradientes Recharts e lucro acumulado.' : 'Visualizando amostra recente. Desbloqueie o Jarvis Pro para o mês completo.';
            const raw30 = data.chart_30d || [];
            const chartData = isPro ? raw30 : raw30.slice(23);

            chartElement = h(ResponsiveContainer, { width: '100%', height: 270 },
              h(AreaChart, { data: chartData, margin: { top: 15, right: 10, left: -20, bottom: 5 } }, [
                h('defs', { key: 'defs' }, [
                  h('linearGradient', { id: 'rechartsGradGross', x1: '0', y1: '0', x2: '0', y2: '1' }, [
                    h('stop', { offset: '5%', stopColor: '#00ff88', stopOpacity: 0.4 }),
                    h('stop', { offset: '95%', stopColor: '#00ff88', stopOpacity: 0.0 })
                  ]),
                  h('linearGradient', { id: 'rechartsGradNet', x1: '0', y1: '0', x2: '0', y2: '1' }, [
                    h('stop', { offset: '5%', stopColor: '#00d2ff', stopOpacity: 0.35 }),
                    h('stop', { offset: '95%', stopColor: '#00d2ff', stopOpacity: 0.0 })
                  ])
                ]),
                h(CartesianGrid, { key: 'grid', strokeDasharray: '3 3', stroke: 'rgba(255,255,255,0.06)' }),
                h(XAxis, { key: 'x', dataKey: 'short_date', stroke: '#8e95a5', fontSize: 10, tickLine: false }),
                h(YAxis, { key: 'y', stroke: '#8e95a5', fontSize: 10, tickLine: false, tickFormatter: v => `R$${v}` }),
                h(Tooltip, { key: 'tt', content: h(CustomRechartsTooltip) }),
                h(Legend, { key: 'leg', wrapperStyle: { paddingTop: '8px', fontSize: '11px' } }),
                h(Area, { key: 'a_amt', type: 'monotone', dataKey: 'amount', name: 'Bruto (R$)', stroke: '#00ff88', strokeWidth: 2, fill: 'url(#rechartsGradGross)' }),
                h(Area, { key: 'a_net', type: 'monotone', dataKey: 'net_profit', name: 'Líquido (R$)', stroke: '#00d2ff', strokeWidth: 2, fill: 'url(#rechartsGradNet)' }),
                h(ReferenceLine, { key: 'ref', y: 350, stroke: '#ffd700', strokeDasharray: '4 4' })
              ])
            );
          } else if (period === 'weekly') {
            summaryTitle = 'Histórico Semanal de Ganhos (Últimas 4 Semanas)';
            summarySub = 'Comparativo de desempenho acumulado semanal e cumprimento da meta de R$ 2.200.';
            const weeklyData = data.weekly_history || [];

            chartElement = h(ResponsiveContainer, { width: '100%', height: 270 },
              h(BarChart, { data: weeklyData, margin: { top: 15, right: 10, left: -15, bottom: 5 } }, [
                h(CartesianGrid, { key: 'grid', strokeDasharray: '3 3', stroke: 'rgba(255,255,255,0.06)' }),
                h(XAxis, { key: 'x', dataKey: 'name', stroke: '#8e95a5', fontSize: 11, tickLine: false }),
                h(YAxis, { key: 'y', stroke: '#8e95a5', fontSize: 10, tickLine: false, tickFormatter: v => `R$${v}` }),
                h(Tooltip, { key: 'tt', content: h(CustomRechartsTooltip) }),
                h(Legend, { key: 'leg', wrapperStyle: { paddingTop: '8px', fontSize: '11px' } }),
                h(ReferenceLine, { key: 'ref_w', y: 2200, stroke: '#ffd700', strokeDasharray: '4 4', label: { value: 'Meta R$ 2.200', fill: '#ffd700', fontSize: 10, position: 'top' } }),
                h(Bar, { key: 'b_gross', dataKey: 'gross', name: 'Bruto Semanal', fill: '#00ff88', radius: [4, 4, 0, 0] }),
                h(Bar, { key: 'b_net_w', dataKey: 'net', name: 'Líquido Semanal', fill: '#00d2ff', radius: [4, 4, 0, 0] }),
                h(Bar, { key: 'b_fuel_w', dataKey: 'fuel', name: 'Combustível', fill: '#ff4757', radius: [4, 4, 0, 0] })
              ])
            );
          } else if (period === 'apps') {
            summaryTitle = 'Distribuição por Aplicativo Parceiro';
            summarySub = 'Fatia do faturamento gerado com base nas corridas aceitas no sistema.';
            const appList = data.app_distribution || [];

            chartElement = h('div', { style: { display: 'flex', flexDirection: 'column', alignItems: 'center' } }, [
              h(ResponsiveContainer, { key: 'pie_resp', width: '100%', height: 220 },
                h(PieChart, {}, [
                  h(Pie, {
                    key: 'pie',
                    data: appList,
                    dataKey: 'value',
                    nameKey: 'name',
                    cx: '50%',
                    cy: '50%',
                    innerRadius: 50,
                    outerRadius: 85,
                    paddingAngle: 4
                  }, appList.map((entry, index) => h(Cell, { key: `cell-${index}`, fill: entry.color || '#00d2ff' }))),
                  h(Tooltip, {
                    key: 'pie_tt',
                    formatter: (value, name, item) => [`R$ ${Number(value).toFixed(2).replace('.', ',')} (${item.payload.pct}%)`, name]
                  })
                ])
              ),
              h('div', { key: 'app_badges', style: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '8px', width: '100%', marginTop: '10px' } },
                appList.map((item, idx) => h('div', {
                  key: `app_b_${idx}`,
                  style: {
                    background: 'rgba(255,255,255,0.03)',
                    border: `1px solid ${item.color || '#ffffff'}33`,
                    borderLeft: `4px solid ${item.color || '#ffffff'}`,
                    borderRadius: '8px',
                    padding: '8px 10px'
                  }
                }, [
                  h('div', { key: 'n', style: { fontSize: '11px', fontWeight: '800', color: '#ffffff' } }, item.name),
                  h('div', { key: 'v', style: { fontSize: '13px', fontWeight: '900', color: item.color || 'var(--primary)', marginTop: '2px' } }, `R$ ${item.value.toFixed(2).replace('.', ',')}`),
                  h('div', { key: 'sub', style: { fontSize: '10px', color: 'var(--text-muted)', marginTop: '2px' } }, `${item.count} corridas • ${item.km} km (${item.pct}%)`)
                ]))
              )
            ]);
          }

          // Seletor de Período Recharts
          const buttons = [
            { id: 'daily_7d', label: '📅 7 Dias' },
            { id: 'daily_30d', label: '📆 30 Dias (Mês)' },
            { id: 'weekly', label: '📊 4 Semanas' },
            { id: 'apps', label: '🍕 Por App' }
          ].map(btn => h('button', {
            key: btn.id,
            className: 'btn',
            style: {
              padding: '6px 12px',
              fontSize: '11px',
              fontWeight: period === btn.id ? '900' : '600',
              background: period === btn.id ? 'var(--primary)' : 'rgba(255, 255, 255, 0.05)',
              color: period === btn.id ? '#0a0a0f' : 'var(--text-muted)',
              border: period === btn.id ? '1px solid var(--primary)' : '1px solid var(--surface-border)'
            },
            onClick: () => setRechartsPeriod(btn.id)
          }, btn.label));

          return h('div', { className: 'recharts-wrapper-box' }, [
            h('div', { key: 'header', style: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '10px', marginBottom: '14px' } }, [
              h('div', {}, [
                h('h3', { style: { fontSize: '14px', fontWeight: '800', color: '#ffffff', display: 'flex', alignItems: 'center', gap: '6px' } }, [
                  h('span', {}, '📈'),
                  h('span', {}, summaryTitle),
                  h('span', { style: { fontSize: '10px', padding: '2px 6px', background: 'rgba(0,255,136,0.15)', color: 'var(--primary)', borderRadius: '4px', border: '1px solid var(--primary)', fontWeight: '700' } }, 'Recharts v2')
                ]),
                h('p', { style: { fontSize: '11px', color: 'var(--text-muted)', marginTop: '3px' } }, summarySub)
              ]),
              h('div', { style: { display: 'flex', gap: '6px', flexWrap: 'wrap' } }, buttons)
            ]),
            h('div', { key: 'chart_container', style: { minHeight: '270px', width: '100%' } }, chartElement)
          ]);
        }

        if (window.ReactDOM.createRoot) {
          if (!window._rechartsRoot) {
            window._rechartsRoot = window.ReactDOM.createRoot(container);
          }
          window._rechartsRoot.render(h(RechartsApp));
        } else {
          window.ReactDOM.render(h(RechartsApp), container);
        }

      } catch (err) {
        console.error("Erro ao montar Recharts:", err);
        renderRechartsFallback(container, data, currentRechartsPeriod, isPro);
      }
    }

    // Fallback nativo dinâmico e responsivo se o script do Recharts não tiver finalizado
    function renderRechartsFallback(container, data, period, isPro) {
      if (!container) return;
      const chartData = (period === 'weekly') ? (data.weekly_history || []) : (data.chart_7d || []);
      const maxVal = Math.max(...chartData.map(d => (d.amount || d.gross || 200)), 200);

      const navButtons = [
        { id: 'daily_7d', label: '📅 7 Dias' },
        { id: 'daily_30d', label: '📆 30 Dias (Mês)' },
        { id: 'weekly', label: '📊 4 Semanas' },
        { id: 'apps', label: '🍕 Por App' }
      ].map(b => `
        <button class="btn" style="padding: 6px 12px; font-size: 11px; font-weight: ${period === b.id ? '900' : '600'}; background: ${period === b.id ? 'var(--primary)' : 'rgba(255,255,255,0.05)'}; color: ${period === b.id ? '#0a0a0f' : 'var(--text-muted)'}; border: ${period === b.id ? '1px solid var(--primary)' : '1px solid var(--surface-border)'};" onclick="setRechartsPeriod('${b.id}')">${b.label}</button>
      `).join('');

      let barsHtml = '';
      if (period === 'apps') {
        const apps = data.app_distribution || [];
        barsHtml = `
          <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 8px; width: 100%; padding: 16px 0;">
            ${apps.map(a => `
              <div style="background: rgba(255,255,255,0.03); border-left: 4px solid ${a.color}; padding: 10px; border-radius: 8px;">
                <div style="font-size: 12px; font-weight: 800; color: #fff;">${a.name}</div>
                <div style="font-size: 14px; font-weight: 900; color: ${a.color}; margin-top: 2px;">R$ ${a.value.toFixed(2).replace('.', ',')}</div>
                <div style="font-size: 10px; color: var(--text-muted);">${a.count} viagens • ${a.pct}%</div>
              </div>
            `).join('')}
          </div>
        `;
      } else {
        barsHtml = `
          <div style="display: flex; align-items: flex-end; gap: 8px; height: 200px; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.1);">
            ${chartData.map(d => {
              const val = d.amount || d.gross || 0;
              const netVal = d.net_profit || d.net || 0;
              const hGross = Math.max(10, Math.round((val / maxVal) * 160));
              const hNet = Math.max(8, Math.round((netVal / maxVal) * 160));
              const lbl = d.short_date || d.name || '';
              return `
                <div style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%;">
                  <div style="font-size: 9px; color: var(--primary); font-weight: 800; margin-bottom: 4px;">R$ ${Math.round(val)}</div>
                  <div style="display: flex; gap: 2px; align-items: flex-end; width: 100%; justify-content: center;">
                    <div style="width: 45%; height: ${hGross}px; background: #00ff88; border-radius: 4px 4px 0 0;" title="Bruto: R$ ${val}"></div>
                    <div style="width: 45%; height: ${hNet}px; background: #00d2ff; border-radius: 4px 4px 0 0;" title="Líquido: R$ ${netVal}"></div>
                  </div>
                  <div style="font-size: 10px; color: var(--text-muted); margin-top: 6px;">${lbl}</div>
                </div>
              `;
            }).join('')}
          </div>
          <div style="display: flex; justify-content: center; gap: 16px; margin-top: 10px; font-size: 11px;">
            <div style="display: flex; align-items: center; gap: 6px;"><span style="width: 10px; height: 10px; background: #00ff88; border-radius: 2px;"></span> Faturamento Bruto</div>
            <div style="display: flex; align-items: center; gap: 6px;"><span style="width: 10px; height: 10px; background: #00d2ff; border-radius: 2px;"></span> Lucro Líquido</div>
          </div>
        `;
      }

      container.innerHTML = `
        <div>
          <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 8px; margin-bottom: 12px;">
            <div>
              <h3 style="font-size: 14px; font-weight: 800; color: #ffffff;">📈 Histórico de Ganhos das Corridas Aceitas</h3>
              <p style="font-size: 11px; color: var(--text-muted);">Visualização gráfica baseada em corridas aceitas e liquidadas</p>
            </div>
            <div style="display: flex; gap: 6px; flex-wrap: wrap;">${navButtons}</div>
          </div>
          ${barsHtml}
        </div>
      `;
    }

    // Carregamento de métricas e atualização de telas
    async function loadAnalytics() {
      try {
        const res = await fetch('/api/earnings');
        const data = await res.json();
        cachedEarningsData = data;

        window.AppState.earnings.today = data.today;
        window.AppState.earnings.week = data.week;
        window.AppState.earnings.month = data.month;
        window.AppState.earnings.profit = data.profit;
        render();
        renderFinancialDashboard(data);

        // Atualiza indicadores de topo do Analytics
        const stToday = document.getElementById('stat-today');
        const stWeek = document.getElementById('stat-week');
        const stMonth = document.getElementById('stat-month');
        const stProfit = document.getElementById('stat-profit');
        const stAvgKm = document.getElementById('stat-avg-km');
        const stRunsTotal = document.getElementById('stat-runs-total');

        if (stToday) stToday.innerText = `R$ ${data.today.toFixed(2).replace('.', ',')}`;
        if (stWeek) stWeek.innerText = `R$ ${data.week.toFixed(2).replace('.', ',')}`;
        if (stMonth) stMonth.innerText = `R$ ${data.month.toFixed(2).replace('.', ',')}`;
        if (stProfit) stProfit.innerText = `R$ ${data.profit.toFixed(2).replace('.', ',')}`;
        
        const kmToday = data.todayKm || 38.2;
        const avgKm = kmToday > 0 ? (data.today / kmToday).toFixed(2).replace('.', ',') : '7,45';
        if (stAvgKm) stAvgKm.innerText = `R$ ${avgKm}/km`;
        if (stRunsTotal) stRunsTotal.innerText = `${data.recent_accepted ? data.recent_accepted.length : 11} viagens`;

        // Renderiza o Dashboard Recharts
        renderRechartsDashboard(data);

        // Preenche tabela de Corridas Aceitas Recentes
        renderAcceptedRunsTable(data.recent_accepted || []);

        const lock = document.getElementById('analytics-free-lock');
        const isFree = window.AppState.user.plan === 'free';
        if (lock) lock.style.display = isFree ? 'block' : 'none';

      } catch (e) {
        console.error("Erro ao carregar telemetria:", e);
      }
    }

    function renderAcceptedRunsTable(runs) {
      const tbody = document.getElementById('accepted-runs-tbody');
      if (!tbody) return;

      if (!runs || runs.length === 0) {
        tbody.innerHTML = `
          <tr>
            <td colspan="7" style="text-align: center; padding: 18px; color: var(--text-muted);">
              Nenhuma corrida aceita gravada recentemente no banco de dados SQLite.
            </td>
          </tr>
        `;
        return;
      }

      const appBadgeColors = {
        'iFood': 'background: rgba(234, 29, 44, 0.15); color: #ea1d2c; border: 1px solid #ea1d2c;',
        'Rappi': 'background: rgba(255, 68, 31, 0.15); color: #ff441f; border: 1px solid #ff441f;',
        'Uber': 'background: rgba(255, 255, 255, 0.15); color: #ffffff; border: 1px solid #ffffff;',
        'Uber Direct': 'background: rgba(255, 255, 255, 0.15); color: #ffffff; border: 1px solid #ffffff;',
        '99': 'background: rgba(247, 194, 0, 0.15); color: #f7c200; border: 1px solid #f7c200;',
        '99Food': 'background: rgba(247, 194, 0, 0.15); color: #f7c200; border: 1px solid #f7c200;',
        'iFood + Rappi': 'background: rgba(0, 255, 136, 0.15); color: #00ff88; border: 1px solid #00ff88;',
        'Multi-Stack': 'background: rgba(0, 255, 136, 0.15); color: #00ff88; border: 1px solid #00ff88;'
      };

      tbody.innerHTML = runs.map(r => {
        const bStyle = appBadgeColors[r.app_source] || 'background: rgba(0, 210, 255, 0.15); color: #00d2ff; border: 1px solid #00d2ff;';
        return `
          <tr style="border-bottom: 1px solid rgba(255,255,255,0.04);">
            <td style="padding: 8px 6px; color: var(--text-muted); font-size: 10px;">${r.date || 'Hoje'}</td>
            <td style="padding: 8px 6px;">
              <span style="font-size: 10px; font-weight: 800; padding: 2px 6px; border-radius: 4px; ${bStyle}">
                ${r.app_source}
              </span>
            </td>
            <td style="padding: 8px 6px; color: #ffffff; font-weight: 600;">${r.km_driven.toFixed(1)} km</td>
            <td style="padding: 8px 6px; color: #00ff88; font-weight: 800;" class="tabular">R$ ${r.amount.toFixed(2).replace('.', ',')}</td>
            <td style="padding: 8px 6px; color: #ff4757;" class="tabular">- R$ ${r.fuel_cost.toFixed(2).replace('.', ',')}</td>
            <td style="padding: 8px 6px; color: #00d2ff; font-weight: 800;" class="tabular">R$ ${r.net_profit.toFixed(2).replace('.', ',')}</td>
            <td style="padding: 8px 6px; text-align: right; color: #ffd700; font-weight: 800;" class="tabular">R$ ${r.gain_per_km.toFixed(2).replace('.', ',')}</td>
          </tr>
        `;
      }).join('');
    }

    // Ação: Simular Nova Corrida Aceita para Atualização Dinâmica do Recharts
    async function simulateAcceptedRun() {
      const sampleApps = ['iFood', 'Rappi', 'Multi-Stack', '99Food'];
      const app = sampleApps[Math.floor(Math.random() * sampleApps.length)];
      const amount = Math.round((18.0 + Math.random() * 22.0) * 10) / 10;
      const km = Math.round((2.5 + Math.random() * 3.5) * 10) / 10;

      speak(`Simulando aceite de corrida ${app}: R$ ${amount.toFixed(2).replace('.', ',')}. Sincronizando com o Recharts.`);

      try {
        await fetch('/api/stacks/accept', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            stack_id: 'sim_' + Date.now(),
            app_source: app,
            amount: amount,
            km_driven: km
          })
        });
        await loadAnalytics();
      } catch (e) {
        console.error("Erro ao simular aceite:", e);
      }
    }

    // Ação: Exportar Relatório Financeiro das Corridas Aceitas em CSV
    function exportEarningsCSV() {
      if (!cachedEarningsData || !cachedEarningsData.chart_7d) {
        alert("Dados financeiros ainda não carregados.");
        return;
      }

      speak("Exportando histórico financeiro das corridas aceitas.");
      let csv = "Data,Dia,Faturamento_Bruto_RS,Lucro_Liquido_RS,Combustivel_RS,Km_Rodados,Corridas_Aceitas,Ganho_Por_Km_RS\n";
      
      const list = cachedEarningsData.chart_30d || cachedEarningsData.chart_7d;
      list.forEach(d => {
        csv += `${d.date},${d.day_name},${d.amount.toFixed(2)},${d.net_profit.toFixed(2)},${d.fuel_cost.toFixed(2)},${d.km.toFixed(1)},${d.count},${d.gain_per_km.toFixed(2)}\n`;
      });

      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.setAttribute("href", url);
      link.setAttribute("download", `extrato_corridas_jarvis_${new Date().toISOString().slice(0,10)}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }

    // Handlers do Painel de Ganhos Diários e Semanais
    let currentFinancialPeriod = 'daily';
    let currentInspectedDayIndex = 6;
    let financialAppsExpanded = false;
    let cachedEarningsData = null;

    function switchFinancialPeriod(period) {
      currentFinancialPeriod = period;
      const btnD = document.getElementById('btn-period-daily');
      const btnW = document.getElementById('btn-period-weekly');
      const viewD = document.getElementById('financial-view-daily');
      const viewW = document.getElementById('financial-view-weekly');
      const expLbl = document.getElementById('btn-export-period-lbl');
      const appsToggleLbl = document.getElementById('fin-apps-toggle-lbl');

      if (period === 'daily') {
        if (btnD) { btnD.style.background = 'var(--primary)'; btnD.style.color = '#0a0a0f'; }
        if (btnW) { btnW.style.background = 'transparent'; btnW.style.color = 'var(--text-muted)'; }
        if (viewD) viewD.style.display = 'block';
        if (viewW) viewW.style.display = 'none';
        if (expLbl) expLbl.innerText = 'Hoje';
        if (appsToggleLbl) appsToggleLbl.innerText = 'Ver faturamento por app (Hoje)';
      } else {
        if (btnW) { btnW.style.background = 'var(--primary)'; btnW.style.color = '#0a0a0f'; }
        if (btnD) { btnD.style.background = 'transparent'; btnD.style.color = 'var(--text-muted)'; }
        if (viewD) viewD.style.display = 'none';
        if (viewW) viewW.style.display = 'block';
        if (expLbl) expLbl.innerText = 'Semana';
        if (appsToggleLbl) appsToggleLbl.innerText = 'Ver faturamento por app (Na Semana)';
      }
      renderFinancialAppBreakdown();
    }

    function renderFinancialDashboard(data) {
      if (!data) return;
      cachedEarningsData = data;

      // 1. Preenchimento Visão Diária
      const netDaily = data.profit || (data.today * 0.803);
      const grossDaily = data.today || 284.50;
      const fuelDaily = data.todayFuel || (grossDaily * 0.197);
      const marginDaily = grossDaily > 0 ? ((netDaily / grossDaily) * 100).toFixed(1) : '80.0';
      const kmDaily = data.todayKm || 38.2;
      const avgKmDaily = kmDaily > 0 ? (grossDaily / kmDaily).toFixed(2) : '7.45';
      const surplusDaily = Math.max(0, grossDaily - (kmDaily * 3.20)).toFixed(2);
      const runsDaily = Math.max(5, Math.round(grossDaily / 28));
      const ticketDaily = (grossDaily / runsDaily).toFixed(2);
      const dailyGoal = data.dailyGoal || 350.00;
      const dailyGoalPct = Math.min(100, Math.round((grossDaily / dailyGoal) * 100));
      const dailyRemain = Math.max(0, dailyGoal - grossDaily).toFixed(2);

      const elNet = document.getElementById('fin-daily-net');
      const elMargin = document.getElementById('fin-daily-margin');
      const elGross = document.getElementById('fin-daily-gross');
      const elFuel = document.getElementById('fin-daily-fuel');
      const elAvgKm = document.getElementById('fin-daily-avg-km');
      const elSurplus = document.getElementById('fin-daily-surplus');
      const elTicket = document.getElementById('fin-daily-ticket');
      const elRuns = document.getElementById('fin-daily-runs');
      const elGoalPct = document.getElementById('fin-daily-goal-pct');
      const elGoalBar = document.getElementById('fin-daily-goal-bar');
      const elGoalRemain = document.getElementById('fin-daily-goal-remain');
      const elKmDriven = document.getElementById('fin-daily-km-driven');

      if (elNet) elNet.innerText = `R$ ${netDaily.toFixed(2).replace('.', ',')}`;
      if (elMargin) elMargin.innerText = `Margem líquida de ${marginDaily}% das entregas`;
      if (elGross) elGross.innerText = `R$ ${grossDaily.toFixed(2).replace('.', ',')}`;
      if (elFuel) elFuel.innerText = `Combustível: - R$ ${fuelDaily.toFixed(2).replace('.', ',')}`;
      if (elAvgKm) elAvgKm.innerText = `R$ ${avgKmDaily.replace('.', ',')}/km`;
      if (elSurplus) elSurplus.innerText = `+ R$ ${surplusDaily.replace('.', ',')}`;
      if (elTicket) elTicket.innerText = `R$ ${ticketDaily.replace('.', ',')}`;
      if (elRuns) elRuns.innerText = `${runsDaily} corridas`;
      if (elGoalPct) elGoalPct.innerText = `${dailyGoalPct}%`;
      if (elGoalBar) elGoalBar.style.width = `${dailyGoalPct}%`;
      if (elGoalRemain) elGoalRemain.innerText = dailyGoalPct >= 100 ? '🎯 Meta diária batida! Parabéns.' : `Faltam R$ ${dailyRemain.replace('.', ',')} para bater o dia`;
      if (elKmDriven) elKmDriven.innerText = `Rodagem: ${kmDaily} km`;

      // 2. Preenchimento Visão Semanal
      const grossWeekly = data.week || 2012.50;
      const netWeekly = data.weekNet || (grossWeekly * 0.814);
      const fuelWeekly = data.weekFuel || (grossWeekly * 0.186);
      const kmWeekly = data.weekKm || 409.0;
      const runsWeekly = data.weekDeliveries || 81;
      const avgDayWeekly = data.weekDailyAvg || (grossWeekly / 7.0);
      const weeklyGoal = data.weeklyGoal || 2200.00;
      const weeklyGoalPct = Math.min(100, Math.round((grossWeekly / weeklyGoal) * 100));
      const weeklyRemain = Math.max(0, weeklyGoal - grossWeekly).toFixed(2);
      const marginWeekly = grossWeekly > 0 ? ((netWeekly / grossWeekly) * 100).toFixed(1) : '81.4';

      const elNetW = document.getElementById('fin-weekly-net');
      const elMarginW = document.getElementById('fin-weekly-margin');
      const elGrossW = document.getElementById('fin-weekly-gross');
      const elFuelW = document.getElementById('fin-weekly-fuel');
      const elAvgDayW = document.getElementById('fin-weekly-avg-day');
      const elTotalKmW = document.getElementById('fin-weekly-total-km');
      const elTotalRunsW = document.getElementById('fin-weekly-total-runs');
      const elGoalPctW = document.getElementById('fin-weekly-goal-pct');
      const elGoalBarW = document.getElementById('fin-weekly-goal-bar');
      const elGoalRemainW = document.getElementById('fin-weekly-goal-remain');

      if (elNetW) elNetW.innerText = `R$ ${netWeekly.toFixed(2).replace('.', ',')}`;
      if (elMarginW) elMarginW.innerText = `Margem acumulada de ${marginWeekly}% no período`;
      if (elGrossW) elGrossW.innerText = `R$ ${grossWeekly.toFixed(2).replace('.', ',')}`;
      if (elFuelW) elFuelW.innerText = `Combustível: - R$ ${fuelWeekly.toFixed(2).replace('.', ',')}`;
      if (elAvgDayW) elAvgDayW.innerText = `R$ ${avgDayWeekly.toFixed(2).replace('.', ',')}`;
      if (elTotalKmW) elTotalKmW.innerText = `${kmWeekly.toFixed(0)} km`;
      if (elTotalRunsW) elTotalRunsW.innerText = `${runsWeekly}`;
      if (elGoalPctW) elGoalPctW.innerText = `${weeklyGoalPct}%`;
      if (elGoalBarW) elGoalBarW.style.width = `${weeklyGoalPct}%`;
      if (elGoalRemainW) elGoalRemainW.innerText = weeklyGoalPct >= 100 ? '🏆 Meta semanal atingida!' : `Faltam R$ ${weeklyRemain.replace('.', ',')} para a meta semanal`;

      // 3. Renderizar Barras Semanal
      renderWeeklyBars(data.chart_7d);
      renderFinancialAppBreakdown();
    }

    function renderWeeklyBars(chartDays) {
      const container = document.getElementById('weekly-bars-container');
      if (!container || !chartDays || chartDays.length === 0) return;

      const maxVal = Math.max(...chartDays.map(d => d.amount), 200);

      container.innerHTML = chartDays.map((d, index) => {
        const h = Math.max(15, Math.round((d.amount / maxVal) * 85));
        const isSelected = index === currentInspectedDayIndex;
        const isToday = d.is_today;
        const barColor = isSelected ? 'var(--primary)' : (isToday ? 'rgba(0, 255, 136, 0.75)' : (d.is_best_day ? '#ffd700' : '#262638'));
        const borderStyle = isSelected ? '2px solid #ffffff' : 'none';

        return `
          <div style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; cursor: pointer; padding: 0 2px;" onclick="inspectWeeklyDay(${index})">
            <div class="tabular" style="font-size: 8px; font-weight: ${isSelected || isToday ? '900' : '500'}; color: ${isSelected || isToday ? 'var(--primary)' : 'var(--text-muted)'}; margin-bottom: 2px;">
              ${d.is_best_day ? '🏆' : 'R$' + Math.round(d.amount)}
            </div>
            <div style="width: 100%; height: ${h}px; background: ${barColor}; border-radius: 6px 6px 0 0; border: ${borderStyle}; transition: all 0.2s;"></div>
            <div style="font-size: 9px; font-weight: ${isSelected || isToday ? '900' : '600'}; color: ${isSelected || isToday ? 'var(--primary)' : '#ffffff'}; margin-top: 4px;">
              ${d.day_name || 'Dia'}
            </div>
            <div style="font-size: 7px; color: ${isToday ? 'var(--primary)' : 'var(--text-muted)'};">
              ${d.short_date}
            </div>
          </div>
        `;
      }).join('');

      updateInspectedDayCard(chartDays[currentInspectedDayIndex] || chartDays[chartDays.length - 1]);
    }

    function inspectWeeklyDay(index) {
      currentInspectedDayIndex = index;
      if (cachedEarningsData && cachedEarningsData.chart_7d) {
        renderWeeklyBars(cachedEarningsData.chart_7d);
      }
    }

    function updateInspectedDayCard(day) {
      if (!day) return;
      const title = document.getElementById('inspected-day-title');
      const tag = document.getElementById('inspected-day-tag');
      const sub = document.getElementById('inspected-day-sub');
      const gross = document.getElementById('inspected-day-gross');
      const net = document.getElementById('inspected-day-net');

      if (title) title.innerText = `${day.day_name} (${day.short_date})`;
      if (tag) {
        if (day.is_today) {
          tag.innerText = 'HOJE';
          tag.style.display = 'inline-block';
          tag.style.background = 'rgba(0,255,136,0.2)';
          tag.style.color = 'var(--primary)';
        } else if (day.is_best_day) {
          tag.innerText = 'RECORDE DA SEMANA 🏆';
          tag.style.display = 'inline-block';
          tag.style.background = 'rgba(255,215,0,0.2)';
          tag.style.color = '#ffd700';
        } else {
          tag.style.display = 'none';
        }
      }
      if (sub) sub.innerText = `${day.count || 9} entregas • ${day.km || 40} km rodados • Média R$ ${((day.amount || 100) / (day.km || 10)).toFixed(2)}/km`;
      if (gross) gross.innerText = `R$ ${Number(day.amount).toFixed(2).replace('.', ',')} Bruto`;
      if (net) net.innerText = `R$ ${Number(day.net_profit || (day.amount * 0.81)).toFixed(2).replace('.', ',')} Líquido`;
    }

    function toggleFinancialAppBreakdown() {
      financialAppsExpanded = !financialAppsExpanded;
      const box = document.getElementById('fin-apps-breakdown');
      const icon = document.getElementById('fin-apps-toggle-icon');
      if (box) box.style.display = financialAppsExpanded ? 'flex' : 'none';
      if (icon) icon.innerText = financialAppsExpanded ? '▲' : '▼';
    }

    function renderFinancialAppBreakdown() {
      const box = document.getElementById('fin-apps-breakdown');
      if (!box) return;

      const isDaily = currentFinancialPeriod === 'daily';
      const baseGross = isDaily ? (cachedEarningsData?.today || 284.50) : (cachedEarningsData?.week || 2012.50);

      const appShares = [
        { name: 'iFood', share: 0.52, color: '#ea1d2c' },
        { name: 'Rappi', share: 0.24, color: '#ff441f' },
        { name: 'Uber Direct', share: 0.16, color: '#e0e0e0' },
        { name: '99 Food', share: 0.08, color: '#f7c200' }
      ];

      box.innerHTML = appShares.map(item => {
        const val = baseGross * item.share;
        const pct = Math.round(item.share * 100);
        return `
          <div style="background: rgba(255,255,255,0.03); border-radius: 8px; padding: 8px 10px;">
            <div style="display: flex; justify-content: space-between; align-items: center; font-size: 11px; margin-bottom: 4px;">
              <span style="display: flex; align-items: center; gap: 6px; font-weight: 700; color: #ffffff;">
                <span style="width: 8px; height: 8px; border-radius: 50%; background: ${item.color}; display: inline-block;"></span>
                ${item.name}
              </span>
              <span class="tabular" style="font-weight: 900; color: #ffffff;">
                R$ ${val.toFixed(2).replace('.', ',')} (${pct}%)
              </span>
            </div>
            <div style="height: 4px; background: rgba(0,0,0,0.5); border-radius: 2px; overflow: hidden;">
              <div style="width: ${pct}%; height: 100%; background: ${item.color};"></div>
            </div>
          </div>
        `;
      }).join('');
    }

    function exportFinancialReport() {
      const isDaily = currentFinancialPeriod === 'daily';
      let reportText = '';

      if (isDaily) {
        const gross = cachedEarningsData?.today || 284.50;
        const net = cachedEarningsData?.profit || (gross * 0.803);
        const fuel = cachedEarningsData?.todayFuel || (gross * 0.197);
        const km = cachedEarningsData?.todayKm || 38.2;
        reportText = `📊 FECHAMENTO DIÁRIO RADAR AI (HOJE)\\n` +
          `💰 Faturamento Bruto: R$ ${gross.toFixed(2).replace('.', ',')}\\n` +
          `🟢 Lucro Líquido Real: R$ ${net.toFixed(2).replace('.', ',')} (80.3%)\\n` +
          `⛽ Combustível: - R$ ${fuel.toFixed(2).replace('.', ',')}\\n` +
          `🏍️ Rodagem: ${km} km | 9 entregas\\n` +
          `📈 Média R$/km: R$ ${(gross / km).toFixed(2).replace('.', ',')}/km\\n` +
          `🎯 Meta Diária: 81.3% atingida (Meta R$ 350,00)\\n` +
          `🚀 Pilotado com Radar Coordinator — Jarvis Neural Cockpit`;
      } else {
        const gross = cachedEarningsData?.week || 2012.50;
        const net = cachedEarningsData?.weekNet || (gross * 0.814);
        const fuel = cachedEarningsData?.weekFuel || (gross * 0.186);
        const km = cachedEarningsData?.weekKm || 409.0;
        const runs = cachedEarningsData?.weekDeliveries || 81;
        reportText = `📊 FECHAMENTO SEMANAL RADAR AI (7 DIAS)\\n` +
          `💰 Total Bruto Semanal: R$ ${gross.toFixed(2).replace('.', ',')}\\n` +
          `🟢 Lucro Líquido Real: R$ ${net.toFixed(2).replace('.', ',')} (81.4%)\\n` +
          `⛽ Custo Combustível: - R$ ${fuel.toFixed(2).replace('.', ',')}\\n` +
          `🏍️ Rodagem Semanal: ${km.toFixed(0)} km | ${runs} entregas\\n` +
          `📈 Média Diária: R$ ${(gross / 7.0).toFixed(2).replace('.', ',')}/dia\\n` +
          `🎯 Meta Semanal: 91.5% atingida (Meta R$ 2.200,00)\\n` +
          `🚀 Pilotado com Radar Coordinator — Jarvis Neural Cockpit`;
      }

      const formatted = reportText.replace(/\\\\n/g, '\\n');
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(formatted).then(() => {
          speak(isDaily ? 'Relatório diário copiado!' : 'Relatório semanal copiado!');
          alert(isDaily ? 'Relatório Diário copiado para a área de transferência!' : 'Relatório Semanal copiado para a área de transferência!');
        }).catch(() => {
          alert(formatted);
        });
      } else {
        alert(formatted);
      }
    }

    function resetFinancialTurn() {
      if (confirm('Deseja reiniciar as métricas do turno de hoje?')) {
        speak('Novo turno iniciado. Boas corridas!');
        loadAnalytics();
      }
    }

    async function fetchFailures() {
      try {
        const res = await fetch('/api/failures');
        const list = await res.json();
        const container = document.getElementById('admin-failure-logs');
        if (!container) return;
        if (!list || list.length === 0) {
          container.innerHTML = '<div style="font-size: 12px; color: var(--text-muted); text-align: center; padding: 12px;">Nenhuma falha de processamento registrada no momento. Sistema estável.</div>';
          return;
        }
        container.innerHTML = list.map(item => `
          <div class="glass" style="padding: 10px 12px; border-left: 3px solid #ff9f43;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
              <span style="font-size: 10px; font-weight: 800; color: #ff9f43; background: rgba(255,159,67,0.15); padding: 2px 6px; border-radius: 4px;">
                ${item.error_code || 'ERR_GENERIC'}
              </span>
              <span style="font-size: 10px; color: var(--text-muted);">${item.created_at || ''}</span>
            </div>
            <div style="font-size: 12px; font-weight: 700; color: #ffffff; margin-bottom: 2px;">
              ${item.app_name} • ${item.restaurant}
            </div>
            <div style="font-size: 11px; color: #ffb86c;">
              ⚠️ ${item.reason}
            </div>
            <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">
              Valor: R$ ${Number(item.value).toFixed(2)} | Distância: ${item.distance_km} km | ID: ${item.offer_id || 'N/A'}
            </div>
          </div>
        `).join('');
      } catch (e) {
        const container = document.getElementById('admin-failure-logs');
        if (container) container.innerHTML = '<div style="font-size: 12px; color: #ff6b6b; text-align: center; padding: 12px;">Erro ao carregar logs do servidor.</div>';
      }
    }

    window.addEventListener('DOMContentLoaded', () => {
      handleRouting();
      render();
      initFilterUI();
      fetchStacks();
      loadAnalytics();
      fetchFailures();
      updateSpeed(window.AppState.health.speed || 0, 'Inicial');
      try { initVoiceRecognition(); } catch (e) {}

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

class RadarServer(ThreadingHTTPServer):
    allow_reuse_address = True
    daemon_threads = True
    def handle_error(self, request, client_address):
        exc_type, exc_val, exc_tb = sys.exc_info()
        if exc_type in (BrokenPipeError, ConnectionResetError, socket.error):
            return
        super().handle_error(request, client_address)

def run_server(port):
    try:
        server = RadarServer(("0.0.0.0", port), RadarHTTPHandler)
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

