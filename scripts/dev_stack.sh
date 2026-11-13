#!/usr/bin/env bash
#
# dev_stack.sh — Sobe a stack local completa de desenvolvimento sem tocar em produção:
#   1) MariaDB local (habbo-dev-mariadb via dev_db.sh)
#   2) Proxy leve local (dev_swf_proxy.go em 127.0.0.1:8081) dividindo /api/v1 + banner pro emulador local
#   3) Emulador habbo_r63b_v2 (portas 35000/35001/30003)
#   4) CMS local (/home/jomp16/Downloads/Habbo/cms) com config isolada em build/dev-stack/
#
# Uso:
#   ./scripts/dev_stack.sh setup    # Prepara DB, build do proxy/emulador/CMS e gera config.local.json
#   ./scripts/dev_stack.sh start    # Sobe todos os serviços em background
#   ./scripts/dev_stack.sh stop     # Para proxy, emulador e CMS (mantém MariaDB; use dev_db.sh down p/ parar)
#   ./scripts/dev_stack.sh status   # Mostra o status dos processos
#   ./scripts/dev_stack.sh logs [proxy|emulator|cms]  # Acompanha logs
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CMS_DIR="${CMS_DIR:-/home/jomp16/Downloads/Habbo/cms}"
STATE_DIR="$REPO_DIR/build/dev-stack"

PROXY_ADDR="${DEV_PROXY_ADDR:-127.0.0.1:8081}"
UPSTREAM_SWF="${DEV_UPSTREAM_SWF:-https://swf.habbo.rwx.ovh}"

PROXY_BIN="$STATE_DIR/dev_swf_proxy"
CMS_BIN="$STATE_DIR/habbo-cms"
CMS_CONFIG="$STATE_DIR/cms.config.local.json"

EMU_JAR="$REPO_DIR/launcher/build/distributions/launcher-0.1.0-SNAPSHOT.jar"

mkdir -p "$STATE_DIR"

die() {
  echo "ERRO: $*" >&2
  exit 1
}

check_safety() {
  [[ -f "$REPO_DIR/config.yaml" ]] || die "config.yaml não encontrado em $REPO_DIR"
  if command -v yq >/dev/null 2>&1; then
    if [[ "$(yq 'has("dynamic_dns")' "$REPO_DIR/config.yaml")" == "true" ]]; then
      die "config.yaml contém 'dynamic_dns'! Remova/comente antes de rodar em dev local para não alterar o /etc/hosts de produção."
    fi
  else
    if grep -qE '^[[:space:]]*dynamic_dns:' "$REPO_DIR/config.yaml"; then
      die "config.yaml contém 'dynamic_dns'! Remova/comente antes de rodar em dev local."
    fi
  fi
}

read_emu_ports() {
  EMU_PORT="$(yq '.port // 35000' "$REPO_DIR/config.yaml")"
  EMU_WS_PORT="$(yq '.ws_port // 35001' "$REPO_DIR/config.yaml")"
  EMU_WEB_PORT="$(yq '.web_port // 30003' "$REPO_DIR/config.yaml")"
  EMU_JWT="$(yq -r '.jwt_secret // ""' "$REPO_DIR/config.yaml")"
}

is_running() {
  local pidfile="$1"
  [[ -f "$pidfile" ]] || return 1
  local pid
  pid="$(cat "$pidfile" 2>/dev/null || true)"
  [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

start_bg() {
  local name="$1" pidfile="$2" logfile="$3" workdir="$4"
  shift 4
  if is_running "$pidfile"; then
    echo "--> $name já está rodando (PID $(cat "$pidfile"))."
    return 0
  fi
  echo "--> Iniciando $name..."
  (
    cd "$workdir"
    nohup "$@" >"$logfile" 2>&1 &
    echo $! >"$pidfile"
  )
  sleep 1
  if ! is_running "$pidfile"; then
    echo "ERRO: $name falhou ao iniciar. Últimas linhas de $logfile:" >&2
    tail -n 25 "$logfile" >&2 || true
    exit 1
  fi
  echo "    ✓ $name rodando (PID $(cat "$pidfile"), log: $logfile)"
}

stop_proc() {
  local name="$1" pidfile="$2"
  if is_running "$pidfile"; then
    local pid
    pid="$(cat "$pidfile")"
    echo "--> Parando $name (PID $pid)..."
    kill "$pid" 2>/dev/null || true
    for _ in $(seq 1 15); do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    kill -9 "$pid" 2>/dev/null || true
  fi
  rm -f "$pidfile"
}

do_setup() {
  check_safety
  read_emu_ports

  echo "--> 1/4 Garantindo MariaDB local..."
  "$REPO_DIR/scripts/dev_db.sh" up

  echo "--> 2/4 Compilando proxy leve local (dev_swf_proxy)..."
  go build -o "$PROXY_BIN" "$REPO_DIR/scripts/dev_swf_proxy.go"

  if [[ ! -f "$EMU_JAR" ]] || [[ "${FORCE_BUILD:-0}" == "1" ]]; then
    echo "--> 3/4 Compilando emulador (:assembleDist)..."
    (cd "$REPO_DIR" && ./gradlew assembleDist --no-daemon)
  else
    echo "--> 3/4 Emulador já compilado ($EMU_JAR). Use FORCE_BUILD=1 para recompilar."
  fi

  [[ -d "$CMS_DIR/backend" ]] || die "CMS não encontrada em $CMS_DIR"
  echo "--> 4/4 Preparando CMS local em $CMS_DIR..."
  if [[ ! -d "$CMS_DIR/frontend/dist" ]] || [[ "${FORCE_BUILD:-0}" == "1" ]]; then
    (cd "$CMS_DIR/frontend" && npm run build)
  fi
  (cd "$CMS_DIR/backend" && go build -o "$CMS_BIN" .)

  local src_cfg="$CMS_DIR/backend/config.json"
  [[ -f "$src_cfg" ]] || src_cfg="$CMS_DIR/backend/config.example.json"

  python3 - "$src_cfg" "$CMS_CONFIG" "$EMU_PORT" "$EMU_WS_PORT" "$EMU_WEB_PORT" "$EMU_JWT" "$PROXY_ADDR" "$CMS_DIR/frontend/dist" <<'PY'
import json, sys

src_path, dst_path, tcp_port, ws_port, web_port, emu_jwt, proxy_addr, frontend_dist = sys.argv[1:]
with open(src_path, "r", encoding="utf-8") as f:
    cfg = json.load(f)

local_proxy = f"http://{proxy_addr}"

cfg.setdefault("server", {})
cfg["server"]["host"] = "127.0.0.1"
cfg["server"]["port"] = int(cfg["server"].get("port") or 8080)
if emu_jwt and len(emu_jwt) >= 16:
    cfg["server"]["jwt_secret"] = emu_jwt

cfg["db"] = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "habbo",
    "name": "habbixed_r63b_v2",
}

cfg["emulator"] = {
    "host": "127.0.0.1",
    "tcp_port": int(tcp_port),
    "ws_port": int(ws_port),
    "socket_proxy": f"ws://127.0.0.1:{ws_port}",
}

cfg["emulator_api"] = {"url": f"http://127.0.0.1:{web_port}/api/v1"}
cfg["swf_base"] = local_proxy
cfg["hero_image"] = f"{local_proxy}/c_images/hotel_view_images_hq/hotelview_default.png"
cfg["avatar_imager"] = f"{local_proxy}/api/v1/habbo-imaging/avatarimage"
cfg["frontend_dist"] = frontend_dist

for rel in cfg.get("flash_releases", []):
    if isinstance(rel, dict) and isinstance(rel.get("swf"), str):
        rel["swf"] = rel["swf"].replace("https://swf.habbo.rwx.ovh", local_proxy)

with open(dst_path, "w", encoding="utf-8") as f:
    json.dump(cfg, f, indent=2, ensure_ascii=False)
PY

  echo "    ✓ Config local da CMS gerada em $CMS_CONFIG"
}

do_start() {
  do_setup

  local emu_dist_dir="$REPO_DIR/launcher/build/distributions"
  [[ -f "$emu_dist_dir/config.yaml" ]] || cp "$REPO_DIR/config.yaml" "$emu_dist_dir/config.yaml"

  start_bg "dev_swf_proxy" "$STATE_DIR/proxy.pid" "$STATE_DIR/proxy.log" "$REPO_DIR" \
    "$PROXY_BIN" -listen "$PROXY_ADDR" -emulator "http://127.0.0.1:${EMU_WEB_PORT}" -upstream "$UPSTREAM_SWF"

  start_bg "habbo-emulator" "$STATE_DIR/emulator.pid" "$STATE_DIR/emulator.log" "$emu_dist_dir" \
    java -jar "$EMU_JAR"

  start_bg "habbo-cms" "$STATE_DIR/cms.pid" "$STATE_DIR/cms.log" "$CMS_DIR/backend" \
    "$CMS_BIN" -config "$CMS_CONFIG"

  local cms_port
  cms_port="$(python3 -c "import json; print(json.load(open('$CMS_CONFIG'))['server']['port'])")"

  echo ""
  echo "=== Stack local de dev ativa (isolada de produção) ==="
  echo "  • MariaDB:    127.0.0.1:3306 (habbo-dev-mariadb)"
  echo "  • SWF Proxy:  http://$PROXY_ADDR  (/api/v1 & /api/camera -> 127.0.0.1:$EMU_WEB_PORT)"
  echo "  • Emulador:   TCP :$EMU_PORT | WS :$EMU_WS_PORT | Web :$EMU_WEB_PORT"
  echo "  • CMS:        http://127.0.0.1:$cms_port"
  echo "  • Logs:       ./scripts/dev_stack.sh logs [proxy|emulator|cms]"
  echo "  • Parar:      ./scripts/dev_stack.sh stop"
}

do_stop() {
  stop_proc "habbo-cms" "$STATE_DIR/cms.pid"
  stop_proc "habbo-emulator" "$STATE_DIR/emulator.pid"
  stop_proc "dev_swf_proxy" "$STATE_DIR/proxy.pid"
  echo "--> Stack parada. (MariaDB continua rodando; use ./scripts/dev_db.sh down se quiser parar.)"
}

do_status() {
  "$REPO_DIR/scripts/dev_db.sh" status || true
  echo ""
  for pair in "dev_swf_proxy:$STATE_DIR/proxy.pid" "habbo-emulator:$STATE_DIR/emulator.pid" "habbo-cms:$STATE_DIR/cms.pid"; do
    local name="${pair%%:*}" pidfile="${pair#*:}"
    if is_running "$pidfile"; then
      echo "  ✓ $name: rodando (PID $(cat "$pidfile"))"
    else
      echo "  ✗ $name: parado"
    fi
  done
}

do_logs() {
  local target="${1:-emulator}"
  case "$target" in
    proxy|emulator|cms)
      tail -n 50 -f "$STATE_DIR/${target}.log"
      ;;
    *)
      die "Alvo inválido: $target (use proxy, emulator ou cms)"
      ;;
  esac
}

case "${1:-start}" in
  setup)  do_setup ;;
  start)  do_start ;;
  stop)   do_stop ;;
  status) do_status ;;
  logs)   do_logs "${2:-emulator}" ;;
  *)      die "Uso: $0 {setup|start|stop|status|logs [proxy|emulator|cms]}" ;;
esac
