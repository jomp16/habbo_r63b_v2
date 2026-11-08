#!/usr/bin/env bash
#
# db.sh — banco MariaDB DESCARTÁVEL (Docker) para testar o write-behind sem tocar em prod.
#
#   db.sh up           recria o container do zero (datadir em tmpfs) e carrega build/stress-db/baseline.sql.gz
#   db.sh down         remove o container (e todos os dados)
#   db.sh jvm-props    imprime as -D que apontam o emulador para este banco (use em EXTRA_JVM=...)
#   db.sh sql [..]     cliente mariadb dentro do container (ex.: db.sh sql -e 'select count(*) from items')
#
# Variáveis: DB_PORT (33306), DB_NAME (habbo), DB_PASSWORD (stress), DB_MEMORY (limite do container, ex. 1g).
# O baseline é gerado por scripts/stress/db/dump_baseline.sh (estrutura + dados estáticos; sem dados de jogadores).
#
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
NAME_C="habbo-stress-db"
PORT="${DB_PORT:-33306}"
DB="${DB_NAME:-habbo}"
PASS="${DB_PASSWORD:-stress}"
BASELINE="$REPO/build/stress-db/baseline.sql.gz"
IMAGE="mariadb:13.0" # mesma versão de prod (13.0.2)

case "${1:-}" in
  up)
    [[ -f "$BASELINE" ]] || { echo "ERRO: $BASELINE não existe; rode scripts/stress/db/dump_baseline.sh" >&2; exit 1; }
    docker rm -f "$NAME_C" > /dev/null 2>&1 || true
    docker run -d --name "$NAME_C" -p "127.0.0.1:$PORT:3306" \
      -e MARIADB_ROOT_PASSWORD="$PASS" -e MARIADB_DATABASE="$DB" \
      --tmpfs /var/lib/mysql:rw,size=2g \
      ${DB_MEMORY:+--memory "$DB_MEMORY"} \
      -v "$BASELINE:/docker-entrypoint-initdb.d/00-baseline.sql.gz:ro" \
      "$IMAGE" \
      --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_520_ci --max-connections=500 > /dev/null
    echo -n "carregando baseline"
    for _ in $(seq 1 180); do
      # a entrada do container sobe um servidor temporário para rodar o init; só vale quando o definitivo (porta TCP) responde
      if docker exec "$NAME_C" mariadb -uroot -p"$PASS" -h127.0.0.1 -e "select count(*) from $DB.catalog_items" > /dev/null 2>&1; then
        echo; echo "pronto: $(docker exec "$NAME_C" mariadb -uroot -p"$PASS" -h127.0.0.1 -N -e "select count(*) from $DB.catalog_items") itens de catálogo, porta $PORT"
        exit 0
      fi
      echo -n "."; sleep 1
    done
    echo; echo "ERRO: o banco não ficou pronto em 180 s; veja: docker logs $NAME_C" >&2; exit 1
    ;;
  down)
    docker rm -f "$NAME_C" > /dev/null 2>&1 && echo "removido" || echo "não existia"
    ;;
  jvm-props)
    echo "-Dhabbo.db.host=127.0.0.1 -Dhabbo.db.port=$PORT -Dhabbo.db.name=$DB -Dhabbo.db.user=root -Dhabbo.db.password=$PASS"
    ;;
  sql)
    shift
    docker exec -i "$NAME_C" mariadb -uroot -p"$PASS" -h127.0.0.1 "$DB" "$@"
    ;;
  *)
    echo "uso: $0 {up|down|jvm-props|sql ...}" >&2; exit 2
    ;;
esac
