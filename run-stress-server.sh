#!/usr/bin/env bash
#
# run-stress-server.sh — sobe/para o emulador FORA da IDE com as opções de JVM do stress test
# (heap fixa, GC ajustado, GC log, heap dump em OOM), em background, para poder ser reiniciado por script.
#
# Uso (da raiz do repositório; o config.yaml e a pasta plugins/ são lidos do diretório atual):
#   ./run-stress-server.sh start      # sobe em background e espera a porta do jogo abrir
#   ./run-stress-server.sh stop       # encerra (SIGTERM, depois SIGKILL) o processo que ESTE script subiu
#   ./run-stress-server.sh restart
#   ./run-stress-server.sh status
#
# Variáveis de ambiente:
#   EXTRA_JVM      opções extras da JVM; ex.: $(scripts/stress/db/db.sh jvm-props) aponta o emulador para o banco Docker local
#   BUILD=1       roda ./gradlew :launcher:installDist antes de subir (use após mudar código)
#   HEAP           default "-Xms3g -Xmx3g"
#   CPUS           núcleos a que a JVM fica presa, ex. "0-1" (simula uma VPS de 2 vCPU); default: todos
#   GC             g1 (default) | zgc
#   WAIT_SECONDS   quanto esperar a porta abrir (default 180)
#
# Saída: stress-server.out (console do emulador), gc.log*, stress-test.log, room-slow-ticks.log (todos no CWD).
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_DIR"

PID_FILE=".stress-server.pid"
OUT_FILE="stress-server.out"
DIST_DIR="launcher/build/install/launcher"
DIST_LIB="$DIST_DIR/lib"
HEAP="${HEAP:--Xms3g -Xmx3g}"
GC="${GC:-g1}"
WAIT_SECONDS="${WAIT_SECONDS:-180}"
JAVA="${JAVA:-java}"

game_port() { grep -E '^port:' config.yaml | head -1 | awk '{print $2}'; }

is_running() {
  [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null
}

port_open() { ss -ltn 2>/dev/null | grep -qE "[:.]$1[[:space:]]"; }

gc_opts() {
  case "$GC" in
    zgc) echo "-XX:+UseZGC" ;;
    g1)  echo "-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:InitiatingHeapOccupancyPercent=45 -XX:+ParallelRefProcEnabled" ;;
    shen)  echo "-XX:+UseShenandoahGC" ;;
    par)   echo "-XX:+UseParallelGC" ;;
    g1low) echo "-XX:+UseG1GC -XX:MaxGCPauseMillis=30 -XX:InitiatingHeapOccupancyPercent=45 -XX:+ParallelRefProcEnabled" ;;
    *)   echo "GC inválido: $GC (use g1, g1low ou zgc)" >&2; exit 2 ;;
  esac
}

do_start() {
  if is_running; then echo "já rodando (pid $(cat "$PID_FILE"))"; return 0; fi

  [[ -f config.yaml ]] || { echo "ERRO: config.yaml não encontrado em $REPO_DIR" >&2; exit 1; }
  local port; port="$(game_port)"
  if port_open "$port"; then
    echo "ERRO: a porta $port já está em uso (outro emulador, talvez o da IDE). Pare-o antes." >&2
    exit 1
  fi

  if [[ "${BUILD:-0}" == "1" ]]; then
    echo "== build (installDist) =="
    ./gradlew :launcher:installDist --console=plain -q
  fi
  [[ -d "$DIST_LIB" ]] || { echo "ERRO: $DIST_LIB não existe; rode com BUILD=1" >&2; exit 1; }

  echo "== start: heap='$HEAP' gc=$GC porta=$port =="
  # shellcheck disable=SC2046
  # CPUS (ex.: "0-1") prende a JVM a esses núcleos ANTES de subir: availableProcessors, event loops do Netty e threads
  # do GC se dimensionam como numa VPS pequena (simula "2 vCPU" no notebook).
  nohup ${CPUS:+taskset -c $CPUS} "$JAVA" \
    $HEAP $(gc_opts) \
    -Dhabbo.stress.marker=1 ${EXTRA_JVM:-} -Dkotlinx.coroutines.debug=off -Dfile.encoding=UTF-8 \
    -Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=20M \
    -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=heapdump.hprof \
    -cp "$DIST_DIR/launcher-0.1.0-SNAPSHOT.jar:$DIST_LIB/*" ovh.rwx.MainKt \
    > "$OUT_FILE" 2>&1 < /dev/null &
  echo $! > "$PID_FILE"

  local waited=0
  until port_open "$port"; do
    if ! is_running; then
      echo "ERRO: o emulador morreu ao subir. Últimas linhas de $OUT_FILE:" >&2
      tail -20 "$OUT_FILE" >&2
      rm -f "$PID_FILE"
      exit 1
    fi
    if (( waited >= WAIT_SECONDS )); then
      echo "ERRO: porta $port não abriu em ${WAIT_SECONDS}s" >&2
      exit 1
    fi
    sleep 1; waited=$((waited + 1))
  done
  echo "pronto: pid $(cat "$PID_FILE"), porta $port aberta em ${waited}s"
}

do_stop() {
  if ! is_running; then echo "não está rodando (por este script)"; rm -f "$PID_FILE"; return 0; fi
  local pid; pid="$(cat "$PID_FILE")"
  echo "== stop pid $pid =="
  kill "$pid" 2>/dev/null || true
  for _ in $(seq 1 60); do
    kill -0 "$pid" 2>/dev/null || { rm -f "$PID_FILE"; echo "encerrado"; return 0; }
    sleep 0.5
  done
  echo "SIGTERM não bastou; SIGKILL"
  kill -9 "$pid" 2>/dev/null || true
  rm -f "$PID_FILE"
}

do_status() {
  if is_running; then
    local pid; pid="$(cat "$PID_FILE")"
    echo "rodando: pid $pid, rss=$(( $(ps -o rss= -p "$pid") / 1024 )) MB, porta $(game_port) $(port_open "$(game_port)" && echo aberta || echo FECHADA)"
  else
    echo "parado"
  fi
}

case "${1:-}" in
  start)   do_start ;;
  stop)    do_stop ;;
  restart) do_stop; do_start ;;
  status)  do_status ;;
  *) echo "uso: $0 start|stop|restart|status" >&2; exit 2 ;;
esac
