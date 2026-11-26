#!/usr/bin/env bash
#
# Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
#
# This file is part of habbo_r63b_v2.
#
# habbo_r63b_v2 is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# habbo_r63b_v2 is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with habbo_r63b_v2. If not, see <http://www.gnu.org/licenses/>.
#

#
# deploy.sh — Compila localmente e publica o Habbo Emulator no servidor de produção.
#
# Uso:
#   ./scripts/deploy.sh                  # Compila, sincroniza e faz reload seamless (fallback para restart)
#   ./scripts/deploy.sh --skip-build     # Pula o build local e sincroniza o build existente
#   ./scripts/deploy.sh --sync-camera    # Sincroniza também a pasta camera_data/ (1.5 GB)
#   ./scripts/deploy.sh --no-seamless    # Força restart completo em vez de reload seamless
#   ./scripts/deploy.sh --restart-only   # Apenas reinicia o serviço remoto
#   ./scripts/deploy.sh --status         # Verifica o status e logs recentes do emulador remoto
#   ./scripts/deploy.sh --tail [N]       # Acompanha logs do emulador em tempo real (ou --status --tail)
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
REMOTE_RELEASES_DIR="$REMOTE_APP_DIR/releases"
REMOTE_SHARED_DIR="$REMOTE_APP_DIR/shared"
REMOTE_CURRENT_LINK="$REMOTE_APP_DIR/current"
REMOTE_HANDOFF_SOCK="$REMOTE_SHARED_DIR/run/handoff.sock"
REMOTE_CAMERA_DIR="/mnt/HDD_EXTERNO2/Habbo/camera_data"
SERVICE_NAME="habbo.service"

SKIP_BUILD=0
SYNC_CAMERA=0
SEAMLESS=1
RESTART_ONLY=0
CHECK_STATUS=0
TAIL_LOGS=0
TAIL_LINES=50

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build)   SKIP_BUILD=1; shift ;;
    --sync-camera)  SYNC_CAMERA=1; shift ;;
    --seamless)     SEAMLESS=1; shift ;;
    --no-seamless|--hard-restart) SEAMLESS=0; shift ;;
    --restart-only) RESTART_ONLY=1; shift ;;
    --status)       CHECK_STATUS=1; shift ;;
    --tail=*|--lines=*)
      TAIL_LOGS=1
      TAIL_LINES="${1#*=}"
      shift
      ;;
    --tail|-t|-f|--follow)
      TAIL_LOGS=1
      if [[ $# -gt 1 ]] && [[ "$2" =~ ^[0-9]+$ ]]; then
        TAIL_LINES="$2"
        shift
      fi
      shift
      ;;
    -h|--help)
      cat <<EOF
Uso: $0 [opções]

Opções:
  --skip-build     Pula o gradle :launcher:installDist e envia o build atual
  --sync-camera    Sincroniza camera_data/ (assets do renderizador) para o HDD remoto
  --seamless       (padrão) Usa reload via socket de handoff se o serviço estiver ativo
  --no-seamless    Força restart completo em vez de reload seamless
  --restart-only   Apenas reinicia o serviço habbo remoto
  --status         Mostra status e logs recentes do emulador no servidor
  --tail [N]       Acompanha os logs em tempo real (journalctl -f, default: 50 linhas)
EOF
      exit 0
      ;;
    *)
      echo "Opção desconhecida: $1" >&2
      exit 1
      ;;
  esac
done

SSH_INTERACTIVE_FLAG=""
if [[ -t 0 ]]; then
  SSH_INTERACTIVE_FLAG="-t"
fi

# -------------------------------------------------------------
# Status check & log tailing
# -------------------------------------------------------------
if [[ $CHECK_STATUS -eq 1 ]]; then
  echo "== Status do serviço remoto $SERVICE_NAME =="
  ssh "$SSH_TARGET" "systemctl status $SERVICE_NAME --no-pager -l" || true
  echo ""
  if [[ $TAIL_LOGS -eq 1 ]]; then
    echo "== Acompanhando logs em tempo real ($SERVICE_NAME, últimas $TAIL_LINES linhas) [Ctrl+C para sair] =="
    ssh $SSH_INTERACTIVE_FLAG "$SSH_TARGET" "journalctl -u $SERVICE_NAME -f -n $TAIL_LINES" || true
  else
    echo "== Últimas 30 linhas do log (use --tail para acompanhar em tempo real) =="
    ssh "$SSH_TARGET" "journalctl -u $SERVICE_NAME -n 30 --no-pager" || true
  fi
  exit 0
fi

if [[ $TAIL_LOGS -eq 1 ]]; then
  echo "== Acompanhando logs em tempo real ($SERVICE_NAME, últimas $TAIL_LINES linhas) [Ctrl+C para sair] =="
  ssh $SSH_INTERACTIVE_FLAG "$SSH_TARGET" "journalctl -u $SERVICE_NAME -f -n $TAIL_LINES" || true
  exit 0
fi

reload_or_restart_remote() {
  if [[ $SEAMLESS -eq 1 ]]; then
    ssh "$SSH_TARGET" bash -s -- "$REMOTE_HANDOFF_SOCK" "$SERVICE_NAME" <<'EOF'
set -euo pipefail
handoff_sock="$1"
service_name="$2"
if [[ -S "$handoff_sock" ]] && systemctl is-active --quiet "$service_name"; then
  echo "    • Socket de handoff ativo; executando systemctl reload $service_name..."
  systemctl reload "$service_name"
else
  echo "    • Socket de handoff ausente ou serviço inativo; executando systemctl restart $service_name..."
  systemctl restart "$service_name"
fi
EOF
  else
    ssh "$SSH_TARGET" "systemctl restart $SERVICE_NAME"
  fi
}

# -------------------------------------------------------------
# Restart only
# -------------------------------------------------------------
if [[ $RESTART_ONLY -eq 1 ]]; then
  echo "== Reiniciando $SERVICE_NAME em $SSH_TARGET =="
  reload_or_restart_remote
  ssh "$SSH_TARGET" "systemctl is-active $SERVICE_NAME"
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

RELEASE_ID="$(date +%Y%m%d-%H%M%S)"
REMOTE_RELEASE_DIR="$REMOTE_RELEASES_DIR/$RELEASE_ID"

# -------------------------------------------------------------
# 3. Setup de usuário e diretórios no servidor
# -------------------------------------------------------------
echo ""
echo "--> 3. Verificando ambiente remoto (usuário 'habbo', releases e shared)..."
ssh "$SSH_TARGET" bash -s -- "$REMOTE_APP_DIR" "$REMOTE_RELEASES_DIR" "$REMOTE_RELEASE_DIR" "$REMOTE_SHARED_DIR" "$REMOTE_CAMERA_DIR" <<'EOF'
set -euo pipefail
app_dir="$1"
releases_dir="$2"
release_dir="$3"
shared_dir="$4"
camera_dir="$5"

id -u habbo >/dev/null 2>&1 || useradd -r -s /usr/bin/nologin -U -d "$app_dir" habbo
mkdir -p "$releases_dir" "$release_dir" "$shared_dir/logs" "$shared_dir/cache" "$shared_dir/motd" "$shared_dir/run" "$camera_dir"
if [[ -d "$app_dir/cache" && ! -L "$app_dir/cache" ]]; then
  cp -an "$app_dir/cache/." "$shared_dir/cache/" 2>/dev/null || true
fi
ln -sfn "$camera_dir" "$shared_dir/camera_data"
EOF

# -------------------------------------------------------------
# 4. Sincronização de arquivos do emulador (release versionada + shared)
# -------------------------------------------------------------
echo ""
echo "--> 4. Sincronizando release $RELEASE_ID para $REMOTE_RELEASE_DIR..."
# Jars principais e dependências
rsync -avz --delete "$DIST_DIR/lib/" "$RSYNC_TARGET:$REMOTE_RELEASE_DIR/lib/"
rsync -avz "$DIST_DIR/launcher-0.1.0-SNAPSHOT.jar" "$RSYNC_TARGET:$REMOTE_RELEASE_DIR/"

# Plugins
echo "    • Sincronizando plugins..."
rsync -avz --delete "$DIST_DIR/plugins/" "$RSYNC_TARGET:$REMOTE_RELEASE_DIR/plugins/"

# Arquivos de suporte da release
echo "    • Sincronizando arquivos de suporte da release..."
rsync -avz "$DIST_DIR/fastfood_localization.json" "$RSYNC_TARGET:$REMOTE_RELEASE_DIR/"

# Arquivos compartilhados
echo "    • Sincronizando arquivos compartilhados em $REMOTE_SHARED_DIR..."
rsync -avz "$REPO_DIR/deploy/config.prod.yaml" "$RSYNC_TARGET:$REMOTE_SHARED_DIR/config.yaml"
rsync -avz "$REPO_DIR/deploy/log4j2-prod.xml" "$RSYNC_TARGET:$REMOTE_SHARED_DIR/log4j2-prod.xml"
rsync -avz "$REPO_DIR/deploy/handoff-reload.sh" "$RSYNC_TARGET:$REMOTE_SHARED_DIR/handoff-reload.sh"
ssh "$SSH_TARGET" "chmod +x '$REMOTE_SHARED_DIR/handoff-reload.sh'"

# MOTD se existir
if [[ -f "$REPO_DIR/motd/welcome.txt" ]]; then
  rsync -avz "$REPO_DIR/motd/" "$RSYNC_TARGET:$REMOTE_SHARED_DIR/motd/"
fi

# Links simbólicos de shared -> release, ativação atômica de current e limpeza (mantém últimas 3)
echo "    • Criando links simbólicos compartilhados, atualizando current e limpando releases antigas..."
ssh "$SSH_TARGET" bash -s -- "$REMOTE_APP_DIR" "$REMOTE_RELEASES_DIR" "$REMOTE_RELEASE_DIR" "$REMOTE_SHARED_DIR" "$REMOTE_CURRENT_LINK" <<'EOF'
set -euo pipefail
app_dir="$1"
releases_dir="$2"
release_dir="$3"
shared_dir="$4"
current_link="$5"

ln -sfn "$shared_dir/config.yaml" "$release_dir/config.yaml"
ln -sfn "$shared_dir/log4j2-prod.xml" "$release_dir/log4j2-prod.xml"
ln -sfn "$shared_dir/logs" "$release_dir/logs"
ln -sfn "$shared_dir/cache" "$release_dir/cache"
ln -sfn "$shared_dir/motd" "$release_dir/motd"
ln -sfn "$shared_dir/camera_data" "$release_dir/camera_data"
ln -sfn "$shared_dir/run" "$release_dir/run"

chown -R habbo:habbo "$release_dir" "$shared_dir"
chown habbo:habbo "$app_dir" "$releases_dir"

ln -sfn "$release_dir" "$current_link"
chown -h habbo:habbo "$current_link"

current_target="$(readlink -f "$current_link" || true)"
mapfile -t all_releases < <(find "$releases_dir" -mindepth 1 -maxdepth 1 -type d | sort -r)
if (( ${#all_releases[@]} > 3 )); then
  for old_release in "${all_releases[@]:3}"; do
    if [[ "$old_release" != "$current_target" ]]; then
      rm -rf "$old_release"
    fi
  done
fi
EOF

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
ssh "$SSH_TARGET" bash -s -- "$SERVICE_NAME" <<'EOF'
set -euo pipefail
systemctl daemon-reload
systemctl enable "$1"
EOF

# -------------------------------------------------------------
# 7. Reinicia (ou recarrega em --seamless) e valida o serviço
# -------------------------------------------------------------
echo ""
if [[ $SEAMLESS -eq 1 ]]; then
  echo "--> 7. Recarregando/reiniciando o emulador em modo seamless ($SERVICE_NAME)..."
else
  echo "--> 7. Reiniciando o emulador ($SERVICE_NAME)..."
fi
reload_or_restart_remote

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
