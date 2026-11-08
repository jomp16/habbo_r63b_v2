#!/usr/bin/env bash
#
# scale_run.sh — UM degrau de carga contra o emulador que o ./run-stress-server.sh subiu:
# N bots/sala em R salas virtuais, JFR do emulador no regime e GUARDA DE MEMÓRIA (aborta os bots se a RAM
# disponível cair abaixo do limite, para não travar a máquina).
#
# Uso: scripts/stress/scale_run.sh <dir-de-saida> <botsPerRoom> <runSeconds> [minAvailMB]
#
# Variáveis (todas opcionais):
#   ROOMS=10          nº de salas virtuais (precisa ser <= virtual_rooms.count do config.yaml)
#   ACTIONS=1         ações por bot por segundo (1 = pesado; 0.2 ~ usuário real)
#   RAMP=200          bots conectando por segundo
#   CLIENT_HEAP=2g    heap da JVM dos bots
#   PIN=1             isola emulador e bots em núcleos distintos durante o teste (restaura no fim):
#     EMU_CPUS=0-7  CLIENT_CPUS=8-11  EMU_RESTORE=0-11
#   JFR_SECONDS=40    duração do JFR (use ~180 para cobrir o teste todo e correlacionar com paradas do tick)
#   SDK_DIR         repositório do habbo_client_sdk (default: ../habbo_client_sdk ao lado deste repo)
#
# Gera em <dir-de-saida>: info.txt, client.log, rec.jfr (JFR 40 s a partir de +12 s), heap_during.txt, emu_rss_kb,
# min_avail_mb, DONE (ou ABORTED/ERROR). Analise com: python3 scripts/stress/analyze.py <dir-de-saida> --jfr
#
set -u
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SDK_DIR="${SDK_DIR:-$REPO/../habbo_client_sdk}"
JCMD="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}/bin/jcmd"

OUT="$1"; PER_ROOM="$2"; RUN="${3:-75}"; MIN_AVAIL_MB="${4:-700}"
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"  # absoluto: o script faz cd para o SDK

# o PID vem do arquivo do run-stress-server.sh (e NÃO de `pgrep -f`, que casaria com a própria linha de comando)
EMU="$(cat "$REPO/.stress-server.pid" 2>/dev/null || true)"
[ -n "$EMU" ] && kill -0 "$EMU" 2>/dev/null || { echo "emulador nao esta rodando (suba com ./run-stress-server.sh start)" > "$OUT/ERROR"; exit 1; }

avail_mb() { awk '/MemAvailable/{printf "%d", $2/1024}' /proc/meminfo; }
echo "emulator_pid=$EMU start_epoch=$(date +%s) rooms=${ROOMS:-10} per_room=$PER_ROOM run=$RUN avail_before_mb=$(avail_mb)" > "$OUT/info.txt"

if [ "$(avail_mb)" -lt "$MIN_AVAIL_MB" ]; then
  echo "RAM disponivel $(avail_mb) MB < $MIN_AVAIL_MB: nao inicio" > "$OUT/ABORTED"; exit 2
fi

CPREFIX=""
if [ -n "${PIN:-}" ]; then
  taskset -a -cp "${EMU_CPUS:-0-7}" "$EMU" > /dev/null 2>&1
  CPREFIX="taskset -c ${CLIENT_CPUS:-8-11}"
fi

cd "$SDK_DIR"
JAVA_OPTS="-Xmx${CLIENT_HEAP:-2g}" SCENARIO_VIRTUAL_ROOMS="${ROOMS:-10}" SCENARIO_BOTS_PER_ROOM="$PER_ROOM" \
  SCENARIO_RAMP_UP_PER_SECOND="${RAMP:-200}" RUN_SECONDS="$RUN" ACTIONS_PER_SEC="${ACTIONS:-1}" \
  $CPREFIX ./run.sh hotel > "$OUT/client.log" 2>&1 &
CLIENT=$!

# guarda de memória: amostra a cada 2 s e aborta os bots se faltar RAM
(
  min=999999
  while kill -0 $CLIENT 2>/dev/null; do
    a=$(avail_mb); [ "$a" -lt "$min" ] && min=$a && echo "$min" > "$OUT/min_avail_mb"
    if [ "$a" -lt "$MIN_AVAIL_MB" ]; then
      echo "RAM disponivel $a MB < $MIN_AVAIL_MB: abortando os bots" > "$OUT/ABORTED"
      pkill -f 'scenarios/build/install/scenarios' 2>/dev/null
      break
    fi
    sleep 2
  done
) &
GUARD=$!

until grep -q "hotel scenario" "$OUT/client.log" 2>/dev/null || ! kill -0 $CLIENT 2>/dev/null; do sleep 0.5; done
end=$((SECONDS + 12)); until [ $SECONDS -ge $end ] || ! kill -0 $CLIENT 2>/dev/null; do sleep 1; done
kill -0 $CLIENT 2>/dev/null && timeout 30 "$JCMD" "$EMU" JFR.start name=scale settings=profile duration="${JFR_SECONDS:-40}s" filename="$OUT/rec.jfr" > "$OUT/jfr_start.txt" 2>&1
end=$((SECONDS + 30)); until [ $SECONDS -ge $end ] || ! kill -0 $CLIENT 2>/dev/null; do sleep 1; done
timeout 30 "$JCMD" "$EMU" GC.heap_info > "$OUT/heap_during.txt" 2>&1
ps -o rss= -p "$EMU" > "$OUT/emu_rss_kb" 2>/dev/null

wait $CLIENT
kill $GUARD 2>/dev/null
[ -n "${PIN:-}" ] && taskset -a -cp "${EMU_RESTORE:-0-11}" "$EMU" > /dev/null 2>&1
echo "end_epoch=$(date +%s)" >> "$OUT/info.txt"
echo done > "$OUT/DONE"
