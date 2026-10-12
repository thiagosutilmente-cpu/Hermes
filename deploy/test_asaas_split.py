"""
JARVIS NEURAL COCKPIT - TESTE DE SPLIT FINANCEIRO ASAAS (PASSO 1)
Validação de criação de cliente, cobrança semanal de R$ 25,00 via PIX e split fixo de 40% (R$ 10,00)
para o General Líder de Praça.
"""

import os
import sys
import json
from datetime import datetime, timedelta
import requests

# 1. Configurações de Ambiente (Sandbox vs Produção)
ENV = os.getenv("ASAAS_ENV", "sandbox").lower()
if ENV == "production" or ENV == "producao":
    BASE_URL = "https://www.asaas.com/api/v3"
else:
    BASE_URL = "https://sandbox.asaas.com/api/v3"

# Chave de API lida de variável de ambiente ou parâmetro
ASAAS_API_KEY = os.getenv("ASAAS_API_KEY", "$aact_YTU5YTE0M2M6NZ2I...")

HEADERS = {
    "access_token": ASAAS_API_KEY,
    "Content-Type": "application/json"
}

# 2. Criar Cliente de Teste no Asaas
def criar_cliente_teste(nome="Motoboy Teste Jarvis", cpf="00000000000", telefone="11999999999"):
    url = f"{BASE_URL}/customers"
    payload = {
        "name": nome,
        "cpfCnpj": cpf,
        "mobilePhone": telefone
    }
    try:
        res = requests.post(url, json=payload, headers=HEADERS, timeout=15)
        if res.status_code in (200, 201):
            customer_id = res.json().get("id")
            print(f"[OK] Cliente criado com sucesso: {customer_id}")
            return customer_id
        else:
            print(f"[ERRO] Falha ao criar cliente ({res.status_code}): {res.text}")
            return None
    except Exception as e:
        print(f"[EXCEPTION] Erro ao conectar com Asaas: {e}")
        return None

# 3. Gerar Cobrança PIX com Split de 40% (R$ 10,00 fixo)
def gerar_pix_com_split(customer_id, wallet_id_general, valor=25.00, split_valor=10.00):
    url = f"{BASE_URL}/payments"
    due_date = (datetime.now() + timedelta(days=7)).strftime("%Y-%m-%d")
    
    payload = {
        "customer": customer_id,
        "billingType": "PIX",
        "value": valor,
        "dueDate": due_date,
        "description": "Assinatura Semanal - Jarvis Neural Cockpit VIP",
        "split": [
            {
                "walletId": wallet_id_general,  # Wallet ID do General de Praça
                "fixedValue": split_valor       # Split de 40% (R$ 10,00)
            }
        ]
    }
    
    try:
        res = requests.post(url, json=payload, headers=HEADERS, timeout=15)
        if res.status_code not in (200, 201):
            print(f"[ERRO] Falha ao criar cobrança ({res.status_code}): {res.text}")
            return None
            
        dados = res.json()
        payment_id = dados.get("id")
        print(f"[OK] Cobrança criada com ID: {payment_id} | Valor: R$ {valor:.2f} | Vencimento: {due_date}")
        print(f"[OK] Split configurado: R$ {split_valor:.2f} para wallet: {wallet_id_general}")
        
        # Busca o payload Copia e Cola do PIX e QR Code
        pix_url = f"{BASE_URL}/payments/{payment_id}/pixQrCode"
        pix_res = requests.get(pix_url, headers=HEADERS, timeout=15)
        
        if pix_res.status_code == 200:
            pix_data = pix_res.json()
            copia_e_cola = pix_data.get("payload")
            print(f"[OK] PIX Copia e Cola obtido com sucesso!")
            return {
                "payment_id": payment_id,
                "customer_id": customer_id,
                "valor_total": valor,
                "split_general": split_valor,
                "split_master": valor - split_valor,
                "wallet_id_general": wallet_id_general,
                "pix_copia_e_cola": copia_e_cola,
                "encoded_image": pix_data.get("encodedImage")
            }
        else:
            print(f"[AVISO] Não foi possível obter o QR Code do PIX ({pix_res.status_code}): {pix_res.text}")
            return {
                "payment_id": payment_id,
                "customer_id": customer_id,
                "valor_total": valor,
                "split_general": split_valor,
                "split_master": valor - split_valor,
                "wallet_id_general": wallet_id_general,
                "pix_copia_e_cola": None
            }
    except Exception as e:
        print(f"[EXCEPTION] Erro ao gerar PIX com Split: {e}")
        return None

if __name__ == "__main__":
    wallet = sys.argv[1] if len(sys.argv) > 1 else "WALLET_ID_DO_GENERAL_AQUI"
    print(f"--- INICIANDO TESTE ASAAS ({ENV.upper()}) ---")
    print(f"Base URL: {BASE_URL}")
    print(f"Wallet General Alvo: {wallet}")
    print("---------------------------------------------")
    
    # Execução de teste
    customer = criar_cliente_teste()
    if customer:
        resultado = gerar_pix_com_split(customer, wallet)
        if resultado:
            print("\n=== RESULTADO DA OPERAÇÃO ===")
            print(json.dumps(resultado, indent=2, ensure_ascii=False))
