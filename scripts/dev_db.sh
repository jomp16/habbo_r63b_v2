#!/usr/bin/env bash
#
# dev_db.sh — Gerencia o MariaDB local (Docker) e sincroniza a base de produção (Kurisu) para dev.
#
# Uso:
#   ./scripts/dev_db.sh sync             # Baixa dump de prod e restaura no MariaDB local
#   ./scripts/dev_db.sh sync --baseline  # Baixa apenas estrutura + tabelas estáticas (catálogo/mobis/headers)
#   ./scripts/dev_db.sh reset            # Recria o banco local a partir do último dump baixado (sem refazer download)
#   ./scripts/dev_db.sh up               # Inicia o container Docker local
#   ./scripts/dev_db.sh down             # Para e remove o container Docker
#   ./scripts/dev_db.sh status           # Mostra status do container e contagem de itens
#   ./scripts/dev_db.sh cli              # Abre o client interativo mariadb no banco local
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_PROD="$REPO_DIR/deploy/config.prod.yaml"
[[ -f "$CONFIG_PROD" ]] || CONFIG_PROD="$REPO_DIR/config.yaml"

CONTAINER_NAME="habbo-dev-mariadb"
IMAGE="mariadb:13.0"
LOCAL_PORT="${LOCAL_DB_PORT:-3306}"
LOCAL_DB="habbixed_r63b_v2"
LOCAL_USER="root"
LOCAL_PASS="habbo"

DUMP_DIR="$REPO_DIR/build/dev-db"
DUMP_FILE="$DUMP_DIR/prod-dump.sql.gz"

ensure_docker_running() {
  if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
      echo "--> Iniciando container existente $CONTAINER_NAME..."
      docker start "$CONTAINER_NAME" >/dev/null
    else
      echo "--> Criando novo container $CONTAINER_NAME ($IMAGE) na porta 127.0.0.1:$LOCAL_PORT..."
      docker run -d \
        --name "$CONTAINER_NAME" \
        -p "127.0.0.1:$LOCAL_PORT:3306" \
        -e MARIADB_ROOT_PASSWORD="$LOCAL_PASS" \
        -e MARIADB_DATABASE="$LOCAL_DB" \
        "$IMAGE" \
        --character-set-server=utf8mb4 \
        --collation-server=utf8mb4_unicode_520_ci \
        --max-connections=500 >/dev/null
    fi

    echo -n "--> Aguardando MariaDB local iniciar..."
    for _ in $(seq 1 30); do
      if docker exec "$CONTAINER_NAME" mariadb -u"$LOCAL_USER" -p"$LOCAL_PASS" -e "SELECT 1;" >/dev/null 2>&1; then
        echo " pronto!"
        return 0
      fi
      echo -n "."
      sleep 1
    done
    echo " ERRO: Timeout aguardando MariaDB local." >&2
    exit 1
  fi
}

do_dump() {
  local mode="${1:-full}"
  mkdir -p "$DUMP_DIR"

  echo "--> Conectando ao banco de produção na Kurisu para dump ($mode)..."
  python3 - <<PYTHON_SCRIPT
import yaml, subprocess, os, sys

config_path = "$CONFIG_PROD"
with open(config_path) as f:
    cfg = yaml.safe_load(f)["database"]

env = os.environ.copy()
env["MYSQL_PWD"] = str(cfg["password"])

host = cfg["host"]
port = str(cfg["port"])
user = cfg["user"]
db_name = cfg["name"]

mode = "$mode"
out_file = "$DUMP_FILE"

common_args = [
    "mariadb-dump",
    "-h", host,
    "-P", port,
    "-u", user,
    "--single-transaction",
    "--skip-lock-tables",
    "--skip-comments",
    "--default-character-set=utf8mb4"
]

print(f"    • Origem: {user}@{host}:{port}/{db_name}")
print(f"    • Destino: {out_file}")

import gzip

with gzip.open(out_file, "wb") as gz:
    gz.write(b"SET FOREIGN_KEY_CHECKS=0;\n")
    if mode == "baseline":
        static_tables = [
            "achievements", "achievements_group",
            "catalog_club_offers", "catalog_deals", "catalog_items", "catalog_pages", "catalog_recycler", "catalog_targeted_offer", "catalog_vouchers",
            "campaign_calendars", "campaign_calendar_rewards",
            "flyway_schema_history", "furnishings", "game_center_games",
            "groups_badges_background_color", "groups_badges_base", "groups_badges_base_color", "groups_badges_symbol", "groups_badges_symbol_color",
            "habbicons", "habbicon_collections", "landing_promos",
            "moderation_categories", "moderation_topics", "navigator_event_categories", "navigator_room_categories",
            "permissions_ranks", "pet_breeds", "ranks", "releases",
            "releases_incoming_headers", "releases_incoming_headers_r63a", "releases_outgoing_headers", "releases_outgoing_headers_r63a",
            "rooms_models", "sequences", "snowwar_arenas", "snowwar_arena_items"
        ]
        # 1. Estrutura de todas as tabelas
        p1 = subprocess.Popen(common_args + ["--no-data", db_name], env=env, stdout=subprocess.PIPE)
        for chunk in iter(lambda: p1.stdout.read(65536), b""):
            gz.write(chunk)
        p1.wait()
        # 2. Dados estáticos
        p2 = subprocess.Popen(common_args + ["--no-create-info", "--skip-triggers", "--extended-insert", db_name] + static_tables, env=env, stdout=subprocess.PIPE)
        for chunk in iter(lambda: p2.stdout.read(65536), b""):
            gz.write(chunk)
        p2.wait()
    else:
        # Full dump
        p = subprocess.Popen(common_args + [db_name], env=env, stdout=subprocess.PIPE)
        for chunk in iter(lambda: p.stdout.read(65536), b""):
            gz.write(chunk)
        p.wait()
        if p.returncode != 0:
            print("ERRO ao gerar dump de produção!", file=sys.stderr)
            sys.exit(1)

    gz.write(b"\nSET FOREIGN_KEY_CHECKS=1;\n")

size_mb = os.path.getsize(out_file) / (1024 * 1024)
print(f"    ✓ Dump concluído: {size_mb:.2f} MB")
PYTHON_SCRIPT
}

do_restore() {
  [[ -f "$DUMP_FILE" ]] || {
    echo "ERRO: Arquivo $DUMP_FILE não encontrado. Rode '$0 sync' primeiro." >&2
    exit 1
  }

  ensure_docker_running

  echo "--> Recriando banco '$LOCAL_DB' no container local..."
  docker exec -i "$CONTAINER_NAME" mariadb -u"$LOCAL_USER" -p"$LOCAL_PASS" -e \
    "DROP DATABASE IF EXISTS $LOCAL_DB; CREATE DATABASE $LOCAL_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci;"

  echo "--> Restaurando dump no container local..."
  gzip -dc "$DUMP_FILE" | docker exec -i "$CONTAINER_NAME" mariadb -u"$LOCAL_USER" -p"$LOCAL_PASS" "$LOCAL_DB"

  local items
  items="$(docker exec "$CONTAINER_NAME" mariadb -u"$LOCAL_USER" -p"$LOCAL_PASS" -N -e "SELECT count(*) FROM $LOCAL_DB.catalog_items;" 2>/dev/null || echo 0)"
  echo "    ✓ Restauração concluída! ($items itens de catálogo presentes no banco local)"
}

case "${1:-}" in
  sync)
    mode="full"
    [[ "${2:-}" == "--baseline" ]] && mode="baseline"
    do_dump "$mode"
    do_restore
    echo ""
    echo "================================================================"
    echo " Banco local de staging/dev sincronizado com sucesso!"
    echo " Para apontar seu emulador local para este container, configure:"
    echo "   database:"
    echo "     host: 127.0.0.1"
    echo "     port: $LOCAL_PORT"
    echo "     user: $LOCAL_USER"
    echo "     password: $LOCAL_PASS"
    echo "     name: $LOCAL_DB"
    echo "================================================================"
    ;;
  reset)
    do_restore
    ;;
  up)
    ensure_docker_running
    echo "MariaDB local rodando em 127.0.0.1:$LOCAL_PORT (banco: $LOCAL_DB, user: $LOCAL_USER, pass: $LOCAL_PASS)"
    ;;
  down)
    echo "--> Parando e removendo container $CONTAINER_NAME..."
    docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 && echo "✓ Container removido." || echo "Container não existia."
    ;;
  status)
    if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
      echo "● Container $CONTAINER_NAME está RODANDO na porta 127.0.0.1:$LOCAL_PORT."
      docker exec "$CONTAINER_NAME" mariadb -u"$LOCAL_USER" -p"$LOCAL_PASS" -e "
        SELECT 'Tabelas' AS Metrica, count(*) AS Valor FROM information_schema.tables WHERE table_schema='$LOCAL_DB'
        UNION ALL
        SELECT 'Catalog Items', count(*) FROM $LOCAL_DB.catalog_items
        UNION ALL
        SELECT 'Furnishings', count(*) FROM $LOCAL_DB.furnishings
        UNION ALL
        SELECT 'Usuários', count(*) FROM $LOCAL_DB.users;
      " 2>/dev/null || echo "Aviso: Banco $LOCAL_DB ainda não restaurado."
    else
      echo "○ Container $CONTAINER_NAME está PARADO."
    fi
    ;;
  cli)
    ensure_docker_running
    docker exec -it "$CONTAINER_NAME" mariadb -u"$LOCAL_USER" -p"$LOCAL_PASS" "$LOCAL_DB"
    ;;
  *)
    cat <<EOF
Uso: $0 {sync|reset|up|down|status|cli}

Comandos:
  sync [--baseline]  Baixa dump de prod (Kurisu) e restaura no Docker local (--baseline = sem jogadores)
  reset              Recria o banco local a partir do último dump baixado (sem refazer download de prod)
  up                 Garante que o container MariaDB local está rodando em 127.0.0.1:$LOCAL_PORT
  down               Para e remove o container MariaDB local
  status             Mostra o status do container e contagem de registros
  cli                Abre o client SQL interativo no banco local
EOF
    exit 2
    ;;
esac
