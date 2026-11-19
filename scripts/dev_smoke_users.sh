#!/usr/bin/env bash
#
# dev_smoke_users.sh — Cria (ou corrige) os usuários de smoke test no MariaDB LOCAL (habbo-dev-mariadb).
#
# São os usuários com que o smoke test dos clientes PIXI loga (habbo-as3-transpiler: scripts/smoke-all.mjs com
# SMOKE_USERS=4,5,6,7, um usuário por cliente aberto ao mesmo tempo): smoke1..smokeN, ids 4..(3+N), rank 1, quarto
# inicial 14. O login do smoke é por um token assinado com o jwt_secret da CMS, então a senha ('!') nunca é usada.
# Um dump de produção (dev_db.sh sync/reset) não os traz: rode este script depois.
#
# Uso:
#   ./scripts/dev_smoke_users.sh            # smoke1..smoke6 (ids 4..9)
#   SMOKE_COUNT=8 ./scripts/dev_smoke_users.sh
#   SMOKE_HOME_ROOM=11 ./scripts/dev_smoke_users.sh
#
# Idempotente: rodar de novo só reaplica rank e quarto inicial. Só toca no container local, nunca em produção.
#
set -euo pipefail

CONTAINER_NAME="habbo-dev-mariadb"
LOCAL_DB="habbixed_r63b_v2"
FIRST_ID=4
COUNT="${SMOKE_COUNT:-6}"
HOME_ROOM="${SMOKE_HOME_ROOM:-14}"

die() {
  echo "ERRO: $*" >&2
  exit 1
}

[[ "$COUNT" =~ ^[1-9][0-9]*$ ]] || die "SMOKE_COUNT inválido: $COUNT"
[[ "$HOME_ROOM" =~ ^[0-9]+$ ]] || die "SMOKE_HOME_ROOM inválido: $HOME_ROOM"
docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$" \
  || die "o container $CONTAINER_NAME não está rodando (./scripts/dev_db.sh up)"

sql() {
  docker exec -i -e MYSQL_PWD=habbo "$CONTAINER_NAME" mariadb -uroot -N "$LOCAL_DB" "$@"
}

if [[ "$HOME_ROOM" != 0 && -z "$(sql -e "SELECT id FROM rooms WHERE id = $HOME_ROOM")" ]]; then
  echo "AVISO: o quarto $HOME_ROOM não existe no banco local; os clientes web não vão entrar num quarto." >&2
fi

# um id já usado por outro usuário (que não um smoke) não é sobrescrito
values=()
for ((i = 1; i <= COUNT; i++)); do
  id=$((FIRST_ID + i - 1))
  name="smoke$i"
  other="$(sql -e "SELECT username FROM users WHERE id = $id AND username <> '$name'")"
  [[ -z "$other" ]] || die "o id $id já é do usuário '$other' (não é um usuário de smoke)"
  values+=("($id, '$name', '$name@localhost', '!', 1, $HOME_ROOM)")
done

joined="$(IFS=,; echo "${values[*]}")"
sql <<SQL
INSERT INTO users (id, username, email, password, \`rank\`, home_room)
VALUES $joined
ON DUPLICATE KEY UPDATE \`rank\` = VALUES(\`rank\`), home_room = VALUES(home_room);
SQL

echo "✓ Usuários de smoke no banco local:"
sql -e "SELECT CONCAT('    ', id, '  ', username, '  rank ', \`rank\`, '  quarto ', home_room) FROM users
        WHERE id BETWEEN $FIRST_ID AND $((FIRST_ID + COUNT - 1)) ORDER BY id"
echo "  Smoke: SMOKE_USERS=$FIRST_ID,$((FIRST_ID + 1)),$((FIRST_ID + 2)),$((FIRST_ID + 3)) node scripts/smoke-all.mjs (no habbo-as3-transpiler)"
