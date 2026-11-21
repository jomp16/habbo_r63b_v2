/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
 *
 * This file is part of habbo_r63b_v2.
 *
 * habbo_r63b_v2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b_v2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b_v2. If not, see <http://www.gnu.org/licenses/>.
 */

package ovh.rwx.habbo.database.debug

import ovh.rwx.habbo.database.DatabaseManager

/**
 * Consulta somente leitura do debug tool (`db_query`). A transação é `READ ONLY` (o MariaDB recusa qualquer escrita,
 * mesmo vinda de função ou procedure) e termina em rollback; nunca é usada pelo jogo.
 */
object DebugDao {
    data class QueryResult(val columns: List<String>, val rows: List<List<Any?>>, val truncated: Boolean)

    private val allowedStart = Regex("""^(select|show|explain|describe|desc|with)\b""", RegexOption.IGNORE_CASE)
    private val forbidden = Regex("""\binto\s+(outfile|dumpfile)\b|\b(load_file|sleep|benchmark|get_lock)\s*\(""", RegexOption.IGNORE_CASE)

    /**
     * Motivo da recusa, ou null. O READ ONLY não segura DDL (commit implícito) nem `INTO OUTFILE`, então só passa UM
     * comando de leitura.
     */
    fun rejectReason(sql: String): String? {
        val s = sql.trim().removeSuffix(";").trim()
        return when {
            s.isEmpty() -> "consulta vazia"
            !allowedStart.containsMatchIn(s) -> "só SELECT, SHOW, EXPLAIN, DESCRIBE ou WITH"
            ';' in s -> "um comando só (sem ';')"
            forbidden.containsMatchIn(s) -> "INTO OUTFILE/DUMPFILE, LOAD_FILE, SLEEP, BENCHMARK e GET_LOCK não são permitidos"
            else -> null
        }
    }

    fun readOnlyQuery(sql: String, maxRows: Int, timeoutSeconds: Int): QueryResult {
        rejectReason(sql)?.let { throw IllegalArgumentException(it) }
        val start = System.nanoTime()
        try {
            return DatabaseManager.jdbi.withHandle<QueryResult, Exception> { handle ->
                val conn = handle.connection
                val autoCommit = conn.autoCommit
                conn.autoCommit = false
                try {
                    conn.createStatement().use { it.execute("START TRANSACTION READ ONLY") }
                    conn.createStatement().use { st ->
                        st.queryTimeout = timeoutSeconds
                        st.maxRows = maxRows + 1
                        st.executeQuery(sql.trim().removeSuffix(";")).use { rs ->
                            val md = rs.metaData
                            val columns = (1..md.columnCount).map { md.getColumnLabel(it) }
                            val rows = ArrayList<List<Any?>>()
                            while (rs.next() && rows.size <= maxRows) {
                                rows += (1..md.columnCount).map { i ->
                                    when (val v = rs.getObject(i)) {
                                        null, is Number, is Boolean, is String -> v
                                        is ByteArray -> "<${v.size} bytes>"
                                        else -> v.toString()
                                    }
                                }
                            }
                            val truncated = rows.size > maxRows
                            QueryResult(columns, if (truncated) rows.subList(0, maxRows) else rows, truncated)
                        }
                    }
                } finally {
                    conn.rollback()
                    conn.autoCommit = autoCommit
                }
            }
        } finally {
            DatabaseManager.onQueryFinished(start)
        }
    }
}
