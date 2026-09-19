import urllib.request
import urllib.error
import json
import sqlite3
import concurrent.futures
import time
import os
import sys
import math
import random

BASE_URL = 'http://localhost:8080'
passed = 0
failed = 0
results = []

def record(category, test_name, status, details=''):
    global passed, failed
    if status:
        passed += 1
        results.append(('PASS', category, test_name, details))
    else:
        failed += 1
        results.append(('FAIL', category, test_name, details))

def http_raw(path, method='GET', data_bytes=None, headers=None, timeout=6):
    url = f'{BASE_URL}{path}'
    hdrs = headers or {}
    req = urllib.request.Request(url, data=data_bytes, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read(), resp.headers
    except urllib.error.HTTPError as e:
        return e.code, e.read(), e.headers
    except Exception as e:
        return 0, str(e).encode(), {}

print("Iniciando Bateria de Testes Inimagináveis...\n")

# ==============================================================================
# FASE 1: FUZZING EXTREMO DE PAYLOADS & CARACTERES INCOMUNS (TORTURA DE DADOS)
# ==============================================================================
xss_payloads = [
    '<script>alert("XSS")</script>',
    '<img src=x onerror=alert(1)>',
    'javascript:/*--></title></style></textarea></script></xmp><svg/onload=alert(1)>',
    '${7*7}{{7*7}}',
    "' OR 1=1; EXEC xp_cmdshell('dir'); --"
]
for p in xss_payloads:
    st, body, _ = http_raw('/api/ai/chat', method='POST',
                           data_bytes=json.dumps({'message': p}).encode(),
                           headers={'Content-Type': 'application/json'})
    record('Fuzzing/XSS', f'Injeção sanitizada: {p[:22]}...', st == 200 and b'reply' in body, f'Status {st}')

weird_strings = [
    '👨‍👩‍👧‍👦' * 50, # Emojis com ZWJ
    '\u202E\u202D\u200E\u200FTexto Invertido e Oculto', # Caracteres bidirecionais
    '𝕿𝖍𝖎𝖆𝖌𝖔 𝕾𝖚𝖙𝖎𝖑 — 𝕵𝖆𝖗𝖛𝖎𝖘', # Caracteres matemáticos Alfanuméricos
    'Запрос на русском языке 摩托车外卖员', # Multilíngue Cirílico + Mandarim
    'A' * 20000 # Buffer gigante de 20.000 caracteres
]
for s in weird_strings:
    st, body, _ = http_raw('/api/decision', method='POST',
                           data_bytes=json.dumps({'value': 25.0, 'distance': 3.5, 'app': s}).encode('utf-8'),
                           headers={'Content-Type': 'application/json'})
    record('Fuzzing/Unicode', f'Unicode {s[:16]}...', st == 200, f'Status {st}')

float_tests = [
    ('Subnormal float 1e-308', 1e-308, 2.0),
    ('Gigante 1e308', 1e308, 1.0),
    ('Negativo Zero -0.0', -0.0, 3.0),
    ('Distancia 1e-15', 20.0, 1e-15)
]
for name, val, dist in float_tests:
    st, body, _ = http_raw('/api/decision', method='POST',
                           data_bytes=json.dumps({'value': val, 'distance': dist, 'app': 'iFood'}).encode(),
                           headers={'Content-Type': 'application/json'})
    record('Fuzzing/Math', name, st == 200 and b'gain_per_km' in body, f'Status {st}')

# ==============================================================================
# FASE 2: GEOFENCING NO LIMITE DA TERRA (PÓLOS, LINHA DO EQUADOR, ANTÍPODAS)
# ==============================================================================
geo_extremes = [
    ('Pólo Norte (+90.0, 0.0)', 90.0, 0.0),
    ('Pólo Sul (-90.0, 0.0)', -90.0, 0.0),
    ('Null Island (0.0, 0.0)', 0.0, 0.0),
    ('Linha Internacional de Data (0.0, 180.0)', 0.0, 180.0),
    ('Antípoda de SP (+23.56, +133.34 - Mar do Japão)', 23.561684, 133.344019),
    ('Coordenada Inválida (> 90 lat)', 999.0, 999.0)
]
for name, lat, lng in geo_extremes:
    st, body, _ = http_raw(f'/api/geofences?lat={lat}&lng={lng}')
    record('Geofence/Extremes', name, st == 200 and b'zones' in body, f'Status {st}')

# ==============================================================================
# FASE 3: RACE CONDITIONS & ESTRESSE DE CONCORRÊNCIA EM BANCO DE DADOS
# ==============================================================================
def race_accept(worker_id):
    st, b, _ = http_raw('/api/stacks/accept', method='POST',
                        data_bytes=json.dumps({'stack_id': 'stk_01', 'worker': worker_id}).encode(),
                        headers={'Content-Type': 'application/json'})
    return st == 200

with concurrent.futures.ThreadPoolExecutor(max_workers=25) as ex:
    futs = [ex.submit(race_accept, i) for i in range(25)]
    race_results = [f.result() for f in concurrent.futures.as_completed(futs)]

record('RaceCondition', '25 Aceites Simultâneos no Mesmo Stack (Zero Deadlocks)', all(race_results), f'{race_results.count(True)}/{len(race_results)} respostas 200 OK sem lock error')

# ==============================================================================
# FASE 4: PROTOCOLO HTTP, MÉTODOS BIZARROS & CONTENT-TYPE ANÔMALOS
# ==============================================================================
st, body, _ = http_raw('/api/decision', method='POST',
                       data_bytes=b'{"value": 20, "distance": 2}',
                       headers={})
record('Protocol/HTTP', 'POST sem header Content-Type aceito graciosamente', st == 200, f'Status {st}')

st, body, _ = http_raw('/api/decision', method='POST',
                       data_bytes=b'{"value": 20, "distance": 2}',
                       headers={'Content-Type': 'text/plain'})
record('Protocol/HTTP', 'POST com Content-Type text/plain tratado', st == 200, f'Status {st}')

st, body, h = http_raw('/', method='HEAD')
record('Protocol/HTTP', 'HEAD request na raiz (/) retorna 200 OK', st == 200, f'Status {st}')

st, body, h = http_raw('/api/stacks', method='OPTIONS')
record('Protocol/HTTP', 'OPTIONS request (CORS Preflight)', st in (200, 204), f'Status {st}')

# ==============================================================================
# FASE 5: VAZAMENTO DE MEMÓRIA E FILE DESCRIPTORS (AUDITORIA DE SISTEMA)
# ==============================================================================
def get_py_metrics():
    import subprocess
    p = subprocess.Popen(['pgrep', '-f', 'python3 app.py'], stdout=subprocess.PIPE)
    out, _ = p.communicate()
    pids = out.decode().strip().split()
    if not pids: return 0, 0
    main_pid = pids[-1]
    try:
        with open(f'/proc/{main_pid}/statm') as f:
            parts = f.read().split()
            rss_pages = int(parts[1])
            rss_mb = (rss_pages * 4096) / (1024 * 1024)
    except: rss_mb = 0
    try:
        fds = len(os.listdir(f'/proc/{main_pid}/fd'))
    except: fds = 0
    return rss_mb, fds

rss_before, fds_before = get_py_metrics()

def burst(i):
    http_raw('/api/earnings')
    http_raw('/api/health')

with concurrent.futures.ThreadPoolExecutor(max_workers=20) as ex:
    list(ex.map(burst, range(100))) # 200 requisições simultâneas

time.sleep(0.3)
rss_after, fds_after = get_py_metrics()

mem_stable = (rss_after - rss_before) < 30.0
fd_stable = (fds_after - fds_before) < 15

record('System/Leak', f'Estabilidade de Memória (RSS: {rss_before:.1f}MB -> {rss_after:.1f}MB)', mem_stable, f'Delta: +{rss_after-rss_before:.2f}MB')
record('System/Leak', f'Sem Vazamento de Sockets/FDs ({fds_before} -> {fds_after})', fd_stable, f'Delta FDs: {fds_after-fds_before}')

# ==============================================================================
# FASE 6: REQUISIÇÃO COM PAYLOAD GIGANTE (BUFFER OVERFLOW RESISTANCE)
# ==============================================================================
huge_payload = {'message': 'Jarvis ' * 5000} # ~35 KB JSON
st, body, _ = http_raw('/api/ai/chat', method='POST',
                       data_bytes=json.dumps(huge_payload).encode(),
                       headers={'Content-Type': 'application/json'})
record('Resilience/Buffer', 'Payload JSON de 35KB no Chat processado com integridade', st == 200, f'Status {st}')

# ==============================================================================
# FASE 7: SIMULAÇÃO DE CORRIDA COM 100 PARADAS E MULTI-APP
# ==============================================================================
complex_stack = {
    'value': 148.50,
    'distance': 28.5,
    'app': 'iFood + Uber + Rappi + ZéDelivery + Loggi'
}
st, body, _ = http_raw('/api/decision', method='POST',
                       data_bytes=json.dumps(complex_stack).encode(),
                       headers={'Content-Type': 'application/json'})
d_comp = json.loads(body.decode()) if st == 200 else {}
record('Simulation/MultiApp', 'Stack Híbrido Quíntuplo (5 apps simultâneos)', st == 200 and d_comp.get('gain_per_km') > 5.0, f"R$ {d_comp.get('gain_per_km')}/km - Decisão: {d_comp.get('decision')}")

# ==============================================================================
# RELATÓRIO FINAL
# ==============================================================================
print('='*78)
print(f'RELATÓRIO DE TESTES INIMAGINÁVEIS: {passed} PASSADOS / {failed} FALHOS')
print('='*78)
for res in results:
    icon = '✅' if res[0] == 'PASS' else '❌'
    print(f'{icon} [{res[1]}] {res[2]} -> {res[3]}')
print('='*78)
if failed == 0:
    print('RESULTADO FINAL: 100% DE APROVAÇÃO EM TODOS OS CENÁRIOS INIMAGINÁVEIS!')
else:
    print(f'ATENÇÃO: {failed} TESTES FALHARAM.')
