# ==============================================================================
# OPERAÇÃO SINGULARIDADE - EXECUTOR MASTER (PC & VPS)
# ==============================================================================
# Execução simples: python3 deploy/executar_operacao_singularidade.py
# Permite rodar teste imediato ou deixar rodando em segundo plano 24/7
# ==============================================================================

import os
import sys
import time
import logging
from datetime import datetime

# Adiciona o diretório atual ao path para importação limpa
CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
if CURRENT_DIR not in sys.path:
    sys.path.append(CURRENT_DIR)

try:
    from operacao_singularidade_swarm import OperacaoSingularidadeSwarm
except ImportError as e:
    print(f"[ERRO] Não foi possível importar o motor da Singularidade: {e}")
    sys.exit(1)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [SINGULARIDADE-LAUNCHER]: %(message)s"
)
logger = logging.getLogger("LAUNCHER")

def carregar_env():
    """Carrega as variáveis do .env se existir"""
    env_path = os.path.join(CURRENT_DIR, ".env")
    if not os.path.exists(env_path):
        env_example = os.path.join(CURRENT_DIR, ".env.singularidade.example")
        if os.path.exists(env_example):
            logger.info("Criando .env a partir de .env.singularidade.example...")
            with open(env_example, "r", encoding="utf-8") as f_in, open(env_path, "w", encoding="utf-8") as f_out:
                f_out.write(f_in.read())
    
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    os.environ[k.strip()] = v.strip()

def banner():
    print("""
======================================================================
  ██████╗ ██████╗ ███████╗██████╗  █████╗  ██████╗ █████╗  ██████╗ 
 ██╔═══██╗██╔══██╗██╔════╝██╔══██╗██╔══██╗██╔════╝██╔══██╗██╔═══██╗
 ██║   ██║██████╔╝█████╗  ██████╔╝███████║██║     ███████║██║   ██║
 ██║   ██║██╔═══╝ ██╔══╝  ██╔══██╗██╔══██║██║     ██╔══██║██║   ██║
 ╚██████╔╝██║     ███████╗██║  ██║██║  ██║╚██████╗██║  ██║╚██████╔╝
  ╚═════╝ ╚═╝     ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝ ╚═════╝ 
                 SINGULARIDADE - R$ 3 MILHÕES / ANO
           Núcleo Orquestrador Central (MIND-0) + 10 Agentes
======================================================================
    """)

def modo_execucao_unica(swarm: OperacaoSingularidadeSwarm):
    print("\n[+] Disparando Ciclo Único Completo dos 10 Agentes...")
    res = swarm.execute_full_swarm_cycle()
    print("\n----------------------------------------------------------------------")
    print(f"STATUS DO CICLO: {res['status']}")
    print(f"FATURAMENTO HOJE: R$ {res['current_revenue_today']:.2f} / Meta R$ {res['target_daily_revenue']:.2f}")
    print(f"LUCRO LÍQUIDO APURADO: R$ {res['net_profit']:.2f}")
    print(f"ROAS MÉDIO: {res['roas']:.2f}x")
    print("----------------------------------------------------------------------")
    print("RESUMO DOS 10 AGENTES:")
    for agente, resumo in res["agents_summary"].items():
        print(f"  • {agente.upper()}: {resumo}")
    print("----------------------------------------------------------------------")
    print("[✓] Dados gravados no banco SQLite local (operacao_singularidade.db)\n")

def modo_daemon_24h(swarm: OperacaoSingularidadeSwarm):
    print("\n[!] MODO PILOTO AUTOMÁTICO 24/7 INICIADO (MALHA FECHADA)")
    print("[!] O sistema ficará ativo verificando horários de disparo e metas.")
    print("    Horários programados: 08:00, 11:30, 15:30, 18:00, 22:00, 23:59")
    print("    Pressione CTRL+C para interromper a qualquer momento.\n")

    horarios_executados = set()

    while True:
        agora = datetime.now()
        hora_str = agora.strftime("%H:%M")
        data_str = agora.strftime("%Y-%m-%d")
        chave_exec = f"{data_str}_{hora_str}"

        # Horários de disparo da rotina neural
        if hora_str in ["08:00", "11:30", "15:30", "18:00", "22:00", "23:59"] and chave_exec not in horarios_executados:
            logger.info(f"Horário programado atingido ({hora_str}). Disparando ciclo dos 10 agentes...")
            swarm.execute_full_swarm_cycle()
            horarios_executados.add(chave_exec)

        # A cada 60 segundos verifica se há checkouts pendentes no Closer-6
        time.sleep(60)

def main():
    banner()
    carregar_env()
    swarm = OperacaoSingularidadeSwarm()

    if len(sys.argv) > 1:
        arg = sys.argv[1].lower()
        if arg in ["--daemon", "-d", "24h", "auto"]:
            modo_daemon_24h(swarm)
            return
        elif arg in ["--run", "-r", "agora", "now"]:
            modo_execucao_unica(swarm)
            return
        elif arg in ["--status", "-s", "status"]:
            status = swarm.get_live_dashboard_status()
            print(f"\n[+] STATUS DO ECOSSISTEMA SINGULARIDADE:")
            print(f"Meta Diária: R$ {status['target_daily']:.2f}")
            print(f"Faturamento Hoje: R$ {status['today_gross_revenue']:.2f} ({status['completion_pct']}%)")
            print(f"Investimento em Tráfego: R$ {status['today_ad_spend']:.2f}")
            print(f"Lucro Líquido: R$ {status['today_net_profit']:.2f}")
            print(f"ROAS Atual: {status['today_roas']:.2f}x")
            print(f"CPA Médio: R$ {status['today_cpa']:.2f}")
            print(f"Status da Meta: {status['status_goal']}")
            print(f"Sub-Agentes Autônomos Ativos: {status['active_agents_count']} / 10")
            print("\nIntegrações de API (.env):")
            for api, ok in status.get('env_configured', {}).items():
                print(f"  • {api.upper()}: {'[CONECTADO / OK]' if ok else '[CONFIGURAR NO .ENV]'}")
            print(f"\nHorário de Leitura: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            return

    # Menu interativo se executado no terminal
    print("Escolha como deseja prosseguir:")
    print("  1. Executar 1 Ciclo Imediato dos 10 Agentes (Teste e Relatório)")
    print("  2. Iniciar Modo Piloto Automático 24/7 Contínuo (Daemon)")
    print("  3. Exibir Status Consolidado da Meta Diária (R$ 8.333,00)")
    print("  0. Sair")
    
    escolha = input("\nDigite o número da opção desejada [1]: ").strip() or "1"

    if escolha == "1":
        modo_execucao_unica(swarm)
    elif escolha == "2":
        modo_daemon_24h(swarm)
    elif escolha == "3":
        status = swarm.get_live_dashboard_status()
        print(f"\nStatus: R$ {status['today_gross_revenue']:.2f} / R$ {status['target_daily']:.2f} ({status['completion_pct']}%)")
        print(f"Lucro Líquido: R$ {status['today_net_profit']:.2f} | ROAS: {status['today_roas']}x\n")
    else:
        print("Finalizado.")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n[!] Operação encerrada pelo usuário.")
