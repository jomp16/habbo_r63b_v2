#!/usr/bin/env bash
#
# deploy.sh — Compila localmente e publica o Habbo Emulator no servidor de produção.
#
# Uso:
#   ./scripts/deploy.sh                  # Compila, sincroniza binários/plugins/config e reinicia o serviço
#   ./scripts/deploy.sh --skip-build     # Pula o build local e sincroniza o build existente
#   ./scripts/deploy.sh --sync-camera    # Sincroniza também a pasta camera_data/ (1.5 GB)
#   ./scripts/deploy.sh --restart-only   # Apenas reinicia o serviço remoto
#   ./scripts/deploy.sh --status         # Verifica o status e logs recentes do emulador remoto
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

SSH_TARGET="${DEPLOY_TARGET:-${HABBO_DEPLOY_TARGET:-}}"
if [[ -z "$SSH_TARGET" ]]; then
  # Lê do config.yaml local (gitignored) se disponível
  if [[ -f "$REPO_DIR/config.yaml" ]] && command -v yq >/dev/null 2>&1; then
    SSH_TARGET="$(yq -r '.dynamic_dns.ssh_target // empty' "$REPO_DIR/config.yaml" 2>/dev/null || true)"
  fi
fi

if [[ -z "$SSH_TARGET" ]]; then
  echo "ERRO: Alvo de deploy não especificado. Defina DEPLOY_TARGET (ex: DEPLOY_TARGET=root@host ./scripts/deploy.sh)." >&2
  exit 1
fi

# No rsync com IPv6 puro, o endereço precisa ficar entre colchetes [ipv6]
if [[ "$SSH_TARGET" == *"@"* ]]; then
  USER_PART="${SSH_TARGET%@*}@"
  HOST_PART="${SSH_TARGET#*@}"
else
  USER_PART=""
  HOST_PART="$SSH_TARGET"
fi

if [[ "$HOST_PART" == *":"*":"* ]]; then
  RSYNC_TARGET="${USER_PART}[${HOST_PART}]"
else
  RSYNC_TARGET="$SSH_TARGET"
fi

REMOTE_APP_DIR="/opt/habbo"
REMOTE_CAMERA_DIR="/mnt/HDD_EXTERNO2/Habbo/camera_data"
SERVICE_NAME="habbo.service"

SKIP_BUILD=0
SYNC_CAMERA=0
RESTART_ONLY=0
CHECK_STATUS=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build)   SKIP_BUILD=1; shift ;;
    --sync-camera)  SYNC_CAMERA=1; shift ;;
    --restart-only) RESTART_ONLY=1; shift ;;
    --status)       CHECK_STATUS=1; shift ;;
    -h|--help)
      cat <<EOF
Uso: $0 [opções]

Opções:
  --skip-build     Pula o gradle :launcher:installDist e envia o build atual
  --sync-camera    Sincroniza camera_data/ (assets do renderizador) para o HDD remoto
  --restart-only   Apenas reinicia o serviço habbo remoto
  --status         Mostra status e logs recentes do emulador no servidor
EOF
      exit 0
      ;;
    *)
      echo "Opção desconhecida: $1" >&2
      exit 1
      ;;
  esac
done

# -------------------------------------------------------------
# Status check
# -------------------------------------------------------------
if [[ $CHECK_STATUS -eq 1 ]]; then
  echo "== Status do serviço remoto $SERVICE_NAME =="
  ssh "$SSH_TARGET" "systemctl status $SERVICE_NAME --no-pager -l" || true
  echo ""
  echo "== Últimas 30 linhas do log =="
  ssh "$SSH_TARGET" "journalctl -u $SERVICE_NAME -n 30 --no-pager" || true
  exit 0
fi

# -------------------------------------------------------------
# Restart only
# -------------------------------------------------------------
if [[ $RESTART_ONLY -eq 1 ]]; then
  echo "== Reiniciando $SERVICE_NAME em $SSH_TARGET =="
  ssh "$SSH_TARGET" "systemctl restart $SERVICE_NAME && systemctl is-active $SERVICE_NAME"
  echo "Pronto! Serviço reiniciado com sucesso."
  exit 0
fi

echo "========================================================"
echo "  Deploy Habbo Emulator -> $SSH_TARGET"
echo "========================================================"

# -------------------------------------------------------------
# 1. Build local (Gradle installDist)
# -------------------------------------------------------------
if [[ $SKIP_BUILD -eq 0 ]]; then
  echo ""
  echo "--> 1. Compilando o emulador localmente com Gradle (:launcher:installDist)..."
  ./gradlew :launcher:installDist --console=plain -q
  echo "    ✓ Build concluído com sucesso!"
else
  echo ""
  echo "--> 1. Pulando compilação local (--skip-build especificado)..."
fi

DIST_DIR="$REPO_DIR/launcher/build/install/launcher"
[[ -d "$DIST_DIR" ]] || {
  echo "ERRO: Diretório $DIST_DIR não existe. Rode sem --skip-build primeiro." >&2
  exit 1
}

# -------------------------------------------------------------
# 2. Gera config de produção e prepara unit
# -------------------------------------------------------------
echo ""
echo "--> 2. Validando arquivos de configuração de produção..."
if [[ ! -f "$REPO_DIR/deploy/config.prod.yaml" ]] && [[ -f "$REPO_DIR/config.yaml" ]]; then
  echo "    • Gerando deploy/config.prod.yaml a partir de config.yaml..."
  python3 - <<'PYTHON_SCRIPT'
import yaml
with open("config.yaml", "r") as f:
    cfg = yaml.safe_load(f)
cfg["debug"] = False
if "dynamic_dns" in cfg:
    del cfg["dynamic_dns"]
with open("deploy/config.prod.yaml", "w") as f:
    yaml.dump(cfg, f, default_flow_style=False, sort_keys=False, allow_unicode=True)
PYTHON_SCRIPT
fi

[[ -f "$REPO_DIR/deploy/config.prod.yaml" ]] || {
  echo "ERRO: $REPO_DIR/deploy/config.prod.yaml não encontrado!" >&2
  exit 1
}
[[ -f "$REPO_DIR/deploy/habbo.service" ]] || {
  echo "ERRO: $REPO_DIR/deploy/habbo.service não encontrado!" >&2
  exit 1
}
[[ -f "$REPO_DIR/deploy/log4j2-prod.xml" ]] || {
  echo "ERRO: $REPO_DIR/deploy/log4j2-prod.xml não encontrado!" >&2
  exit 1
}

# -------------------------------------------------------------
# 3. Setup de usuário e diretórios no servidor
# -------------------------------------------------------------
echo ""
echo "--> 3. Verificando ambiente remoto (usuário 'habbo' e diretórios)..."
ssh "$SSH_TARGET" bash -s <<EOF
set -euo pipefail
id -u habbo >/dev/null 2>&1 || useradd -r -s /usr/bin/nologin -U -d $REMOTE_APP_DIR habbo
mkdir -p "$REMOTE_APP_DIR" "$REMOTE_CAMERA_DIR" "$REMOTE_APP_DIR/logs"
ln -sfn "$REMOTE_CAMERA_DIR" "$REMOTE_APP_DIR/camera_data"
EOF

# -------------------------------------------------------------
# 4. Sincronização de arquivos do emulador (rsync incremental)
# -------------------------------------------------------------
echo ""
echo "--> 4. Sincronizando binários e bibliotecas para $REMOTE_APP_DIR..."
# Jars principais e dependências (com --delete no lib para expurgar versões antigas)
rsync -avz --delete "$DIST_DIR/lib/" "$RSYNC_TARGET:$REMOTE_APP_DIR/lib/"
rsync -avz "$DIST_DIR/launcher-0.1.0-SNAPSHOT.jar" "$RSYNC_TARGET:$REMOTE_APP_DIR/"

# Plugins
echo "    • Sincronizando plugins..."
rsync -avz --delete "$DIST_DIR/plugins/" "$RSYNC_TARGET:$REMOTE_APP_DIR/plugins/"

# Arquivos de suporte
echo "    • Sincronizando arquivos de configuração e suporte..."
rsync -avz "$DIST_DIR/fastfood_localization.json" "$RSYNC_TARGET:$REMOTE_APP_DIR/"
rsync -avz "$REPO_DIR/deploy/config.prod.yaml" "$RSYNC_TARGET:$REMOTE_APP_DIR/config.yaml"
rsync -avz "$REPO_DIR/deploy/log4j2-prod.xml" "$RSYNC_TARGET:$REMOTE_APP_DIR/log4j2-prod.xml"

# MOTD se existir
if [[ -f "$REPO_DIR/motd/welcome.txt" ]]; then
  rsync -avz "$REPO_DIR/motd/" "$RSYNC_TARGET:$REMOTE_APP_DIR/motd/"
fi

# -------------------------------------------------------------
# 5. Sincronização de camera_data (se requisitado ou ausente)
# -------------------------------------------------------------
if [[ $SYNC_CAMERA -eq 1 ]]; then
  echo ""
  echo "--> 5. Sincronizando camera_data/ para $REMOTE_CAMERA_DIR..."
  rsync -avz --info=progress2 "$REPO_DIR/camera_data/" "$RSYNC_TARGET:$REMOTE_CAMERA_DIR/"
  ssh "$SSH_TARGET" "chown -R habbo:habbo '$REMOTE_CAMERA_DIR'"
else
  echo ""
  echo "--> 5. Sincronização de camera_data ignorada (use --sync-camera para atualizar assets do renderizador)."
fi

# -------------------------------------------------------------
# 6. Atualização da unit do systemd
# -------------------------------------------------------------
echo ""
echo "--> 6. Atualizando systemd unit $SERVICE_NAME..."
rsync -avz "$REPO_DIR/deploy/habbo.service" "$RSYNC_TARGET:/etc/systemd/system/$SERVICE_NAME"
ssh "$SSH_TARGET" bash -s <<EOF
set -euo pipefail
chown -R habbo:habbo "$REMOTE_APP_DIR"
systemctl daemon-reload
systemctl enable $SERVICE_NAME
EOF

# -------------------------------------------------------------
# 7. Reinicia e valida o serviço
# -------------------------------------------------------------
echo ""
echo "--> 7. Reiniciando o emulador ($SERVICE_NAME)..."
ssh "$SSH_TARGET" "systemctl restart $SERVICE_NAME"

echo "    Aguardando serviço iniciar..."
sleep 3

if ssh "$SSH_TARGET" "systemctl is-active --quiet $SERVICE_NAME"; then
  echo "    ✓ Serviço está ativo e rodando!"
else
  echo "    ✗ ERRO: O serviço não está ativo! Exibindo logs:" >&2
  ssh "$SSH_TARGET" "journalctl -u $SERVICE_NAME -n 50 --no-pager" >&2
  exit 1
fi

echo ""
echo "========================================================"
echo "  Deploy concluído com sucesso!"
echo "========================================================"
ssh "$SSH_TARGET" "systemctl status $SERVICE_NAME --no-pager -l" || true
