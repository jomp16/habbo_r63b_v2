#!/usr/bin/env sh

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

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
TUI_BIN="$ROOT_DIR/launcher-tui/habbo-launcher-tui"
JAR_PATH="${1:-$ROOT_DIR/launcher/build/install/launcher/launcher-0.1.0-SNAPSHOT.jar}"

printf '%s\n' "Compilando TUI..."
(cd "$ROOT_DIR/launcher-tui" && go build -o habbo-launcher-tui .)

printf '%s\n' "Compilando servidor..."
"$ROOT_DIR/gradlew" :launcher:installDist

if [ ! -x "$TUI_BIN" ]; then
    printf '%s\n' "Falha ao gerar a TUI: $TUI_BIN" >&2
    exit 1
fi

if [ ! -f "$JAR_PATH" ]; then
    printf '%s\n' "JAR não encontrado: $JAR_PATH" >&2
    exit 1
fi

# Não fazemos cd: o servidor deve encontrar config.yaml no diretório atual.
exec "$TUI_BIN" "$JAR_PATH"
