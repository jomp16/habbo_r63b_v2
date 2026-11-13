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

package ovh.rwx.habbo.database.user

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.mariadb.MariaDBContainer
import ovh.rwx.habbo.database.DatabaseManager
import ovh.rwx.habbo.database.messenger.MessengerDao
import ovh.rwx.habbo.database.migration.FlywayMigration
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.game.user.ignore.IgnoredUser

/**
 * Persistência real de ignorar/bloquear (MariaDB descartável + migrações Flyway de verdade): DAOs, remoção de pedidos de
 * amizade entre um par e as linhas de header das migrações V28/V29. Sem Docker o teste é PULADO.
 *
 * Não sobe o HabboServer (lê config.yaml) nem toca o banco de dev.
 */
class IgnoreBlockPersistenceContainerTest {
    companion object {
        private var container: MariaDBContainer? = null
        private var dataSource: HikariDataSource? = null

        // header por release: lista | ignorar | designorar | lista_resp | resultado (IGNORE_SPEC.md)
        private val IGNORE_HEADERS = """
            RELEASE63-201110051250-692472661	3652	3362	1629	2571	3030
            RELEASE63-201112130905-713192101	604	1822	1610	3101	1322
            RELEASE63-201204021456-654375178	487	3662	1129	1173	320
            RELEASE63-201207040943-767851649	1630	405	256	495	3830
            RELEASE63-201305221112-286302280	1808	2406	3934	801	1693
            PRODUCTION-201611291003-338511768	3878	1117	2061	126	207
            PRODUCTION-202104091531-593308026	2055	3386	479	1016	3905
            PRODUCTION-202111191349-636893688	3660	399	42	1159	3100
            PRODUCTION-202311200305-397722618	1056	3968	407	2871	2149
            WIN63-202512101753-364126151	3217	1478	2575	2030	1260
            WIN63-202601062255-671620806	3347	2418	1206	977	69
            WIN63-202601121721-391685409	2077	3182	3762	2499	2293
            WIN63-202601221005-86019430	1601	1334	2493	3554	329
            WIN63-202602091725-383015954	664	3414	908	2459	835
            WIN63-202602131518-521997437	3666	2350	1498	2542	2573
            WIN63-202602181049-129106954	813	624	158	1533	889
            WIN63-202604081729-823620372	114	3868	276	2689	3246
            WIN63-202608061514-667123007	3533	1657	2073	642	1109
            WIN63-202609021410-356326612	3681	3784	2490	3025	2852
            WIN63-202609021434-290314188	2229	1639	3645	2576	1419
            WIN63-202609091217-117204808	2145	506	237	1256	331
            WIN63-202609161723-93809945	638	3743	1071	2659	1553
            WIN63-202610121709-785561319	2986	2400	2333	726	1828
            WIN63-202610271147-806594867	680	80	850	3311	871
        """.trimIndent().lines().map { it.trim().split('\t') }

        @BeforeClass
        @JvmStatic
        fun startDatabase() {
            assumeTrue("Docker indisponível: teste com MariaDB real pulado", DockerClientFactory.instance().isDockerAvailable)

            val mariadb = MariaDBContainer("mariadb:13.0")
                .withDatabaseName("habbo")
                .withUsername("habbo")
                .withPassword("habbo")
                .withTmpFs(mapOf("/var/lib/mysql" to "rw"))
            mariadb.start()
            container = mariadb

            val hikari = HikariConfig().apply {
                jdbcUrl = mariadb.jdbcUrl + (if ('?' in mariadb.jdbcUrl) "&" else "?") + "allowMultiQueries=true"
                username = mariadb.username
                password = mariadb.password
                maximumPoolSize = 4
            }
            val ds = HikariDataSource(hikari)
            dataSource = ds

            // mesmo caminho de produção: V1 como "banco existente" + baseline + demais migrações
            val v1 = checkNotNull(
                IgnoreBlockPersistenceContainerTest::class.java.classLoader.getResource("db/migration/V1__initial_schema.sql")
            ) { "V1__initial_schema.sql não encontrado no classpath" }.readText()
            ds.connection.use { connection ->
                connection.createStatement().use {
                    it.execute("SET FOREIGN_KEY_CHECKS = 0;\n$v1\nDROP TABLE IF EXISTS flyway_schema_history;\nSET FOREIGN_KEY_CHECKS = 1;")
                }
            }
            FlywayMigration.migrate(ds)
            DatabaseManager.init(ds)
        }

        @AfterClass
        @JvmStatic
        fun stopDatabase() {
            dataSource?.close()
            container?.stop()
        }
    }

    private fun scalar(sql: String): Long = DatabaseManager.jdbi.withHandle<Long, Exception> { h ->
        h.createQuery(sql).mapTo(Long::class.java).one()
    }

    private fun exec(vararg sql: String) = DatabaseManager.jdbi.useHandle<Exception> { h -> sql.forEach { h.execute(it) } }

    private fun flush() = WriteBehindManager.flushAllSync()

    @Before
    fun clean() {
        exec(
            "SET FOREIGN_KEY_CHECKS = 0",
            "DELETE FROM users_ignores",
            "DELETE FROM users_blocks",
            "DELETE FROM messenger_requests",
            "DELETE FROM users",
            "SET FOREIGN_KEY_CHECKS = 1",
        )
        (1..5).forEach { exec("INSERT INTO users (id, username, email, password) VALUES ($it, 'user$it', 'u$it@test.local', 'x')") }
    }

    // ---------- UserIgnoresDao ----------

    @Test
    fun `ignorar grava, e repetir o mesmo alvo e idempotente`() {
        UserIgnoresDao.addIgnore(1, 2)
        UserIgnoresDao.addIgnore(1, 2)
        flush()

        assertEquals(1, scalar("SELECT COUNT(*) FROM users_ignores WHERE user_id = 1"))
        assertEquals(listOf(IgnoredUser(2, "user2")), UserIgnoresDao.getIgnores(1))
        assertEquals(emptyList<IgnoredUser>(), UserIgnoresDao.getIgnores(2))
    }

    @Test
    fun `designorar remove so aquela linha`() {
        UserIgnoresDao.addIgnore(1, 2)
        UserIgnoresDao.addIgnore(1, 3)
        UserIgnoresDao.addIgnore(4, 2)
        UserIgnoresDao.removeIgnore(1, 2)
        flush()

        assertEquals(listOf(3), UserIgnoresDao.getIgnores(1).map { it.userId })
        assertEquals(listOf(2), UserIgnoresDao.getIgnores(4).map { it.userId })
    }

    @Test
    fun `adicionar e remover em sequencia mantem a ordem (mesma chave do dono)`() {
        UserIgnoresDao.addIgnore(1, 2)
        UserIgnoresDao.removeIgnore(1, 2)
        UserIgnoresDao.addIgnore(1, 3)
        flush()

        assertEquals(listOf(3), UserIgnoresDao.getIgnores(1).map { it.userId })
    }

    @Test
    fun `carregar ignorados ordena por created_at e desempata por id, com o username atual`() {
        exec(
            "INSERT INTO users_ignores (user_id, ignored_user_id, created_at) VALUES (1, 4, '2026-01-01 10:00:00.002')",
            "INSERT INTO users_ignores (user_id, ignored_user_id, created_at) VALUES (1, 2, '2026-01-01 10:00:00.003')",
            "INSERT INTO users_ignores (user_id, ignored_user_id, created_at) VALUES (1, 3, '2026-01-01 10:00:00.001')",
            "INSERT INTO users_ignores (user_id, ignored_user_id, created_at) VALUES (1, 5, '2026-01-01 10:00:00.003')",
            "UPDATE users SET username = 'RenomeadoQuatro' WHERE id = 4",
        )

        assertEquals(
            listOf(IgnoredUser(3, "user3"), IgnoredUser(4, "RenomeadoQuatro"), IgnoredUser(2, "user2"), IgnoredUser(5, "user5")),
            UserIgnoresDao.getIgnores(1)
        )
    }

    @Test
    fun `apagar o usuario ignorado remove a linha em cascata`() {
        UserIgnoresDao.addIgnore(1, 2)
        flush()
        exec("DELETE FROM users WHERE id = 2")

        assertEquals(0, scalar("SELECT COUNT(*) FROM users_ignores"))
    }

    // ---------- UserBlocksDao ----------

    @Test
    fun `bloquear grava e getBlockedIds e getBlockedByIds enxergam cada lado`() {
        UserBlocksDao.addBlock(1, 2)
        UserBlocksDao.addBlock(1, 2)
        UserBlocksDao.addBlock(1, 3)
        UserBlocksDao.addBlock(4, 2)
        flush()

        assertEquals(setOf(2, 3), UserBlocksDao.getBlockedIds(1).toSet())
        assertEquals(listOf(2), UserBlocksDao.getBlockedIds(4))
        assertEquals(emptyList<Int>(), UserBlocksDao.getBlockedIds(2))
        assertEquals(setOf(1, 4), UserBlocksDao.getBlockedByIds(2).toSet())
        assertEquals(listOf(1), UserBlocksDao.getBlockedByIds(3))
        assertEquals(emptyList<Int>(), UserBlocksDao.getBlockedByIds(1))
    }

    @Test
    fun `desbloquear remove so aquela linha e a ordem e por created_at`() {
        exec(
            "INSERT INTO users_blocks (user_id, blocked_user_id, created_at) VALUES (1, 4, '2026-01-01 10:00:00.002')",
            "INSERT INTO users_blocks (user_id, blocked_user_id, created_at) VALUES (1, 2, '2026-01-01 10:00:00.003')",
            "INSERT INTO users_blocks (user_id, blocked_user_id, created_at) VALUES (1, 3, '2026-01-01 10:00:00.001')",
        )
        assertEquals(listOf(3, 4, 2), UserBlocksDao.getBlockedIds(1))

        UserBlocksDao.removeBlock(1, 4)
        flush()

        assertEquals(listOf(3, 2), UserBlocksDao.getBlockedIds(1))
        assertEquals(emptyList<Int>(), UserBlocksDao.getBlockedByIds(4))
    }

    @Test
    fun `bloquear e desbloquear em sequencia respeita a ordem`() {
        UserBlocksDao.addBlock(1, 2)
        UserBlocksDao.removeBlock(1, 2)
        UserBlocksDao.addBlock(1, 2)
        UserBlocksDao.removeBlock(1, 2)
        flush()

        assertEquals(0, scalar("SELECT COUNT(*) FROM users_blocks"))
    }

    // ---------- MessengerDao.removeRequestsBetween ----------

    @Test
    fun `removeRequestsBetween apaga os dois sentidos so daquele par`() {
        exec(
            "INSERT INTO messenger_requests (from_id, to_id) VALUES (1, 2)",
            "INSERT INTO messenger_requests (from_id, to_id) VALUES (2, 1)",
            "INSERT INTO messenger_requests (from_id, to_id) VALUES (1, 3)",
            "INSERT INTO messenger_requests (from_id, to_id) VALUES (3, 2)",
            "INSERT INTO messenger_requests (from_id, to_id) VALUES (4, 1)",
        )

        MessengerDao.removeRequestsBetween(1, 2)
        flush()

        val remaining = DatabaseManager.jdbi.withHandle<List<String>, Exception> { h ->
            h.createQuery("SELECT CONCAT(from_id, '>', to_id) FROM messenger_requests ORDER BY from_id, to_id")
                .mapTo(String::class.java).list()
        }
        assertEquals(listOf("1>3", "3>2", "4>1"), remaining)
    }

    @Test
    fun `removeRequestsBetween sem pedidos nao falha`() {
        MessengerDao.removeRequestsBetween(1, 2)
        flush()
        assertEquals(0, scalar("SELECT COUNT(*) FROM messenger_requests"))
    }

    // ---------- headers V28 / V29 ----------

    private data class Row(val header: Long?, val override: String?)

    private fun rows(table: String, release: String, name: String): List<Row> =
        DatabaseManager.jdbi.withHandle<List<Row>, Exception> { h ->
            h.createQuery("SELECT header, override_method FROM $table WHERE release_name = :r AND name = :n")
                .bind("r", release).bind("n", name)
                .map { rs, _ -> Row(rs.getLong("header").takeUnless { rs.wasNull() }, rs.getString("override_method")) }
                .list()
        }

    @Test
    fun `V28 mapeia os 5 headers de ignorar exatos nas 24 releases`() {
        assertEquals(24, IGNORE_HEADERS.size)
        for (cols in IGNORE_HEADERS) {
            val release = cols[0]
            val all = cols.drop(1).map { it.toLong() }
            val lookups = listOf(
                Triple("releases_incoming_headers", "MESSENGER_IGNORED_USERS", all[0]),
                Triple("releases_incoming_headers", "ROOM_IGNORE_USER", all[1]),
                Triple("releases_incoming_headers", "ROOM_UNIGNORE_USER", all[2]),
                Triple("releases_outgoing_headers", "ROOM_IGNORED_USERS", all[3]),
                Triple("releases_outgoing_headers", "ROOM_IGNORE_USER_RESULT", all[4]),
            )
            for ((table, name, header) in lookups) {
                val found = rows(table, release, name)
                assertEquals("uma linha de $name em $release", 1, found.size)
                assertEquals("header de $name em $release", Row(header, null), found.single())
            }
        }
    }

    @Test
    fun `V28 corrige ROOM_IGNORE_USER que tinha o header de designorar`() {
        assertEquals(1822L, rows("releases_incoming_headers", "RELEASE63-201112130905-713192101", "ROOM_IGNORE_USER").single().header)
        assertEquals(3662L, rows("releases_incoming_headers", "RELEASE63-201204021456-654375178", "ROOM_IGNORE_USER").single().header)
        assertEquals(2406L, rows("releases_incoming_headers", "RELEASE63-201305221112-286302280", "ROOM_IGNORE_USER").single().header)
    }

    @Test
    fun `V28 cria os headers estaticos do R63A`() {
        fun header(table: String, name: String) = DatabaseManager.jdbi.withHandle<Long?, Exception> { h ->
            h.createQuery("SELECT header FROM $table WHERE name = :n").bind("n", name).mapTo(Long::class.java).findOne().orElse(null)
        }
        assertEquals(321L, header("releases_incoming_headers_r63a", "MESSENGER_IGNORED_USERS"))
        assertEquals(319L, header("releases_incoming_headers_r63a", "ROOM_IGNORE_USER"))
        assertEquals(322L, header("releases_incoming_headers_r63a", "ROOM_UNIGNORE_USER"))
        assertEquals(419L, header("releases_outgoing_headers_r63a", "ROOM_IGNORE_USER_RESULT"))
        assertEquals(420L, header("releases_outgoing_headers_r63a", "ROOM_IGNORED_USERS"))
    }

    private val incomingBlockNames = listOf("MESSENGER_BLOCK_USER", "MESSENGER_UNBLOCK_USER")
    private val outgoingBlockNames = listOf("MESSENGER_BLOCKED_USERS", "MESSENGER_BLOCK_USER_UPDATE")

    @Test
    fun `V29 as 15 releases WIN63 tem os 5 headers de bloquear mapeados e sem override`() {
        val win = IGNORE_HEADERS.map { it[0] }.filter { it.startsWith("WIN63-") }
        assertEquals(15, win.size)

        for (release in win) {
            for (n in incomingBlockNames) {
                val r = rows("releases_incoming_headers", release, n).single()
                assertTrue("$n em $release sem header", r.header != null)
                assertEquals(null, r.override)
            }
            for (n in outgoingBlockNames + "MESSENGER_ERROR") {
                val r = rows("releases_outgoing_headers", release, n).single()
                assertTrue("$n em $release sem header", r.header != null)
                assertEquals(null, r.override)
            }
        }
    }

    @Test
    fun `V29 as 9 releases anteriores tem DISABLED nos nomes de bloqueio e NULL no MESSENGER_ERROR`() {
        val pre = IGNORE_HEADERS.map { it[0] }.filterNot { it.startsWith("WIN63-") }
        assertEquals(9, pre.size)

        for (release in pre) {
            for (n in incomingBlockNames) {
                assertEquals("$n em $release", Row(null, "DISABLED"), rows("releases_incoming_headers", release, n).single())
            }
            for (n in outgoingBlockNames) {
                assertEquals("$n em $release", Row(null, "DISABLED"), rows("releases_outgoing_headers", release, n).single())
            }
            assertEquals("MESSENGER_ERROR em $release", Row(null, null), rows("releases_outgoing_headers", release, "MESSENGER_ERROR").single())
        }
    }

    @Test
    fun `nenhum header repetido por release nas tabelas de entrada e saida`() {
        for (table in listOf("releases_incoming_headers", "releases_outgoing_headers")) {
            assertEquals(
                "header duplicado em $table",
                0,
                scalar("SELECT COUNT(*) FROM (SELECT release_name, header FROM $table WHERE header IS NOT NULL GROUP BY release_name, header HAVING COUNT(*) > 1) d")
            )
            assertEquals(
                "(release, nome) duplicado em $table",
                0,
                scalar("SELECT COUNT(*) FROM (SELECT release_name, name FROM $table GROUP BY release_name, name HAVING COUNT(*) > 1) d")
            )
        }
    }
}
