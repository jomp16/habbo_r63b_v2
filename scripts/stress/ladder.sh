#!/usr/bin/env bash
#
# ladder.sh — escada de carga: roda scale_run.sh para cada valor de bots/sala, parando se um degrau abortar por RAM.
#
# Uso: scripts/stress/ladder.sh <tag> <botsPerRoom1> [<botsPerRoom2> ...]
#   ex.: ROOMS=100 ACTIONS=0.2 RAMP=100 RUN=200 scripts/stress/ladder.sh V2 60 80 100
#
# Saída: $STRESS_OUT/<tag>_<n>/ por degrau (default STRESS_OUT = <repo>/build/stress-runs) e $STRESS_OUT/ladder_<tag>.log.
# RUN (default 75) é a duração de cada degrau em segundos; as demais variáveis são as do scale_run.sh.
#
set -u
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HERE="$REPO/scripts/stress"
STRESS_OUT="${STRESS_OUT:-$REPO/build/stress-runs}"
mkdir -p "$STRESS_OUT"

TAG="$1"; shift
for step in "$@"; do
  name="${TAG}_${step}"
  rm -rf "${STRESS_OUT:?}/$name"
  "$HERE/scale_run.sh" "$STRESS_OUT/$name" "$step" "${RUN:-75}" 700
  if [ -f "$STRESS_OUT/$name/ABORTED" ]; then
    echo "$name ABORTADO: $(cat "$STRESS_OUT/$name/ABORTED")" >> "$STRESS_OUT/ladder_$TAG.log"
    break
  fi
  sleep 12
done
echo finished >> "$STRESS_OUT/ladder_$TAG.log"
