#!/bin/bash

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

# Define a raiz do repositório para garantir que os caminhos funcionem
# mesmo se você rodar o script de dentro de uma subpasta.
REPO_ROOT=$(git rev-parse --show-toplevel)
CONFIG_FILE="$REPO_ROOT/config.yaml"
OUTPUT_FILE="$REPO_ROOT/sql_dump/database.sql"

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "🔄 Iniciando backup do banco de dados..."

# 1. Verifica dependências
if ! command -v yq &> /dev/null; then
    echo -e "${RED}Erro: 'yq' não está instalado. Instale com 'sudo pacman -S go-yq'${NC}"
    exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo -e "${RED}Erro: Arquivo '$CONFIG_FILE' não encontrado na raiz.${NC}"
    exit 1
fi

# 2. Lê configurações
DB_HOST=$(yq -r '.database.host' "$CONFIG_FILE")
DB_PORT=$(yq -r '.database.port' "$CONFIG_FILE")
DB_USER=$(yq -r '.database.user' "$CONFIG_FILE")
DB_PASS=$(yq -r '.database.password' "$CONFIG_FILE")
DB_NAME=$(yq -r '.database.name' "$CONFIG_FILE")

# 3. Executa o dump
export MYSQL_PWD="$DB_PASS"

mariadb-dump \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DB_USER" \
  --single-transaction \
  --quick \
  --result-file="$OUTPUT_FILE" \
  "$DB_NAME"

STATUS=$?
unset MYSQL_PWD

if [ $STATUS -eq 0 ]; then
    echo -e "${GREEN}✅ Sucesso! Backup salvo em: $OUTPUT_FILE${NC}"
    exit 0
else
    echo -e "${RED}❌ Falha ao realizar o backup.${NC}"
    rm -f "$OUTPUT_FILE" # Remove arquivo corrompido/vazio
    exit 1
fi