#!/usr/bin/env bash
#
# seed_users.sh <N> — cria N usuários de teste no banco DESCARTÁVEL (Docker) para os bots do SDK logarem de verdade.
#   usuário i (0..N-1): username dbbot<i>, auth_ticket dbt-<i>, 50000 de créditos/pixels (defaults da tabela).
# Use com o SDK:  SCENARIO_BOT_TICKET_PREFIX=dbt-  (o ticket do bot i é "<prefixo><i>").
#
# NUNCA aponte isto para um banco de verdade: roda INSERT direto. Só fala com o container habbo-stress-db.
#
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
N="${1:?uso: seed_users.sh <N>}"

"$HERE/db.sh" sql <<SQL
SET @n = $N;
INSERT INTO users (username, email, password, auth_ticket)
SELECT CONCAT('dbbot', seq), CONCAT('dbbot', seq, '@stress.local'), 'x', CONCAT('dbt-', seq)
FROM seq_0_to_$((N - 1));
INSERT INTO users_stats (user_id) SELECT id FROM users WHERE username LIKE 'dbbot%';
INSERT INTO users_preferences (user_id) SELECT id FROM users WHERE username LIKE 'dbbot%';
SELECT COUNT(*) AS usuarios_criados FROM users WHERE username LIKE 'dbbot%';
SQL
