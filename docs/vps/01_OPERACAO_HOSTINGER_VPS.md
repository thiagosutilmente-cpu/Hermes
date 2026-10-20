# 🖥️ Manual de Operação e Manutenção da VPS Hostinger (187.77.248.73)

> **Documentação de Infraestrutura para o Dono do Projeto**  
> *Comandos rápidos para consultar a integridade do backend FastAPI, verificar logs de webhooks e manter o serviço 100% no ar.*

---

## 🌐 1. Dados de Conexão da Servidor

* **IP Fixo:** `187.77.248.73`
* **Porta da Aplicação FastAPI:** `8000`
* **URL Base de Produção:** `http://187.77.248.73:8000/`
* **Documentação Interativa Swagger:** `http://187.77.248.73:8000/docs`

---

## 🔍 2. Testando se a VPS está Online pelo Celular ou Computador

Você pode testar a qualquer momento digitando no navegador do celular:
1. `http://187.77.248.73:8000/health`  
   * Resposta esperada: `{"status": "ok", "service": "Jarvis Neural Backend"}`
2. `http://187.77.248.73:8000/api/ping`  
   * Resposta esperada: `{"pong": true, "timestamp": ...}`

---

## 💻 3. Comandos Úteis no Terminal SSH da Hostinger

Caso precise acessar o terminal da VPS via SSH (`ssh root@187.77.248.73`):

### A. Ver se os Containers Docker estão Rodando
```bash
docker ps
```
Você verá o container `jarvis_backend` ou `vps_backend` rodando na porta 8000.

### B. Acompanhar os Logs em Tempo Real (Webhooks do Asaas e Corridas)
```bash
docker logs -f jarvis_backend --tail 100
```
Para sair dos logs, pressione `Ctrl + C`.

### C. Reiniciar o Serviço
```bash
docker restart jarvis_backend
```

### D. Verificar Uso de Memória e CPU da VPS
```bash
htop
# ou
docker stats
```

---

## 🔐 4. Configuração das Variáveis no Arquivo `.env` da VPS

Localizado na VPS em `/root/jarvis/deploy/.env`:
```env
HOSTINGER_VPS_IP=187.77.248.73
FASTAPI_PORT=8000
ASAAS_API_KEY=$aact_sua_chave_real_aqui
ASAAS_ENVIRONMENT=production
SPLIT_WALLET_OWNER=sua_carteira_asaas_aqui
SPLIT_WALLET_PARTNER=carteira_parceiro_aqui
```

Quando você atualizar o `.env` com a sua chave real do Asaas, basta reiniciar o container:
```bash
cd /root/jarvis/deploy && docker compose restart
```
Tudo continuará funcionando de forma autônoma e silenciosa!
