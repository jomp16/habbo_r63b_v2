#!/usr/bin/env bash
#
# run_db_test.sh <N> [segundos] — teste de write-behind com banco REAL descartável:
#   1. recria o MariaDB Docker do zero a partir do baseline (estrutura + dados estáticos de prod);
#   2. cria N usuários (dbt-0..dbt-N-1); 3. sobe o emulador apontado para esse banco;
#   4. roda N bots do SDK (login real, entram em salas virtuais); 5. espera o flush e confere o banco.
# Ao final o emulador é parado; o banco fica de pé para inspeção (scripts/stress/db/db.sh sql ...) até o próximo `up`.
#
# Variáveis: ROOMS (10, salas virtuais), ACTIONS (0.5 ações/s por bot), CPUS (ex. 0-3), HEAP ("-Xms1g -Xmx1g"),
#            SDK_DIR (../habbo_client_sdk), SDK_SCENARIO (hotel), SDK_ENV (variáveis extras do cenário, ex. "SCENARIO_X=1").
#
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"
SDK_DIR="${SDK_DIR:-$REPO/../habbo_client_sdk}"
N="${1:?uso: run_db_test.sh <N> [segundos]}"
RUN="${2:-60}"
ROOMS="${ROOMS:-10}"
PER_ROOM=$(( (N + ROOMS - 1) / ROOMS ))
OUT="$REPO/build/stress-runs/db_${N}_$(date +%H%M%S)"
mkdir -p "$OUT"

echo "== 1/5 banco novo =="
"$HERE/db.sh" up
echo "== 2/5 $N usuários =="
"$HERE/seed_users.sh" "$N" | tail -1

counters() { "$HERE/db.sh" sql -N -e "SHOW GLOBAL STATUS WHERE Variable_name IN ('Com_insert','Com_update','Com_delete','Com_commit','Innodb_rows_inserted','Innodb_rows_updated','Innodb_rows_deleted','Queries')" | sort; }
counters > "$OUT/db_counters_before.txt"

echo "== 3/5 emulador (banco local) =="
"$REPO/run-stress-server.sh" stop > /dev/null 2>&1 || true
(cd "$REPO" && CPUS="${CPUS:-}" HEAP="${HEAP:--Xms1g -Xmx1g}" EXTRA_JVM="$("$HERE/db.sh" jvm-props)" ./run-stress-server.sh start | tail -1)

echo "== 4/5 $N bots, ${RUN}s =="
(cd "$SDK_DIR" && env SCENARIO_BOT_TICKET_PREFIX=dbt- SCENARIO_VIRTUAL_ROOMS="$ROOMS" SCENARIO_BOTS_PER_ROOM="$PER_ROOM" \
  SCENARIO_RAMP_UP_PER_SECOND="${RAMP:-50}" RUN_SECONDS="$RUN" ACTIONS_PER_SEC="${ACTIONS:-0.5}" ${SDK_ENV:-} \
  ./run.sh "${SDK_SCENARIO:-hotel}" > "$OUT/client.log" 2>&1) || true
grep -E "^\[report\]" "$OUT/client.log" | tail -1

echo "== 5/5 confere o banco (aguarda o write-behind) =="
sleep 10
"$HERE/db.sh" sql -t > "$OUT/db_state.txt" <<'SQL'
SELECT COUNT(*) AS usuarios, SUM(online) AS ainda_online FROM users WHERE username LIKE 'dbbot%';
SELECT COUNT(*) AS com_online_seconds FROM users_stats WHERE online_seconds > 0;
SELECT (SELECT COUNT(*) FROM items) AS items, (SELECT COUNT(*) FROM users_currencies) AS users_currencies,
       (SELECT COUNT(*) FROM users_badges) AS users_badges, (SELECT COUNT(*) FROM users_achievements) AS users_achievements;
SQL
cat "$OUT/db_state.txt"
counters > "$OUT/db_counters_after.txt"
echo "operações no banco durante o teste (depois do seed):"
join "$OUT/db_counters_before.txt" "$OUT/db_counters_after.txt" | awk '{printf "  %-22s %10d\n", $1, $3 - $2}'
echo "erros no emulador: $(grep -c 'ERROR' "$REPO/stress-server.out" || true)"
"$REPO/run-stress-server.sh" stop | tail -1
echo "saída: $OUT"
