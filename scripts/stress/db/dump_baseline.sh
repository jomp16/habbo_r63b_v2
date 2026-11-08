#!/usr/bin/env bash
#
# dump_baseline.sh — UMA vez: tira de prod (SOMENTE LEITURA) a base do banco local de stress:
#   • estrutura de TODAS as tabelas;
#   • dados só das tabelas ESTÁTICAS (catálogo, mobília, modelos de sala, headers, achievements...).
# Tabelas de usuário (contas, senhas, e-mails, inventários, salas de jogadores...) vêm SÓ com a estrutura:
# nenhum dado pessoal sai de prod. O banco de origem é o do config.yaml (mesmo critério do sql_dump/dump_sql.sh).
#
# Saída: build/stress-db/baseline.sql.gz (build/ é ignorado pelo git).
#
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
CONFIG="$REPO/config.yaml"
OUT_DIR="$REPO/build/stress-db"
OUT="$OUT_DIR/baseline.sql.gz"
mkdir -p "$OUT_DIR"

command -v yq > /dev/null || { echo "ERRO: instale yq (sudo pacman -S go-yq)" >&2; exit 1; }
command -v mariadb-dump > /dev/null || { echo "ERRO: mariadb-dump não encontrado" >&2; exit 1; }

HOST="$(yq -r '.database.host' "$CONFIG")"
PORT="$(yq -r '.database.port' "$CONFIG")"
USER_="$(yq -r '.database.user' "$CONFIG")"
NAME="$(yq -r '.database.name' "$CONFIG")"
export MYSQL_PWD="$(yq -r '.database.password' "$CONFIG")"

# Tabelas com dados copiados (sem informação de jogadores). Todo o resto: só estrutura.
STATIC_TABLES=(
  achievements achievements_group
  catalog_club_offers catalog_deals catalog_items catalog_pages catalog_recycler catalog_targeted_offer catalog_vouchers
  campaign_calendars campaign_calendar_rewards
  flyway_schema_history furnishings game_center_games
  groups_badges_background_color groups_badges_base groups_badges_base_color groups_badges_symbol groups_badges_symbol_color
  habbicons habbicon_collections landing_promos
  moderation_categories moderation_topics navigator_event_categories navigator_room_categories
  permissions_ranks pet_breeds ranks releases
  releases_incoming_headers releases_incoming_headers_r63a releases_outgoing_headers releases_outgoing_headers_r63a
  rooms_models sequences snowwar_arenas snowwar_arena_items
)

COMMON=(-h "$HOST" -P "$PORT" -u "$USER_" --single-transaction --skip-lock-tables --skip-comments --default-character-set=utf8mb4)

echo "== dump (somente leitura) de $NAME@$HOST: estrutura + ${#STATIC_TABLES[@]} tabelas estáticas =="
{
  echo "SET FOREIGN_KEY_CHECKS=0;"
  mariadb-dump "${COMMON[@]}" --no-data "$NAME"
  mariadb-dump "${COMMON[@]}" --no-create-info --skip-triggers --extended-insert "$NAME" "${STATIC_TABLES[@]}"
  echo "SET FOREIGN_KEY_CHECKS=1;"
} | gzip -9 > "$OUT"

echo "ok: $OUT ($(du -h "$OUT" | cut -f1))"
