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

package ovh.rwx.habbo.database.migration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jdbi.v3.core.Jdbi
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.mariadb.MariaDBContainer
import ovh.rwx.habbo.database.DatabaseManager
import ovh.rwx.habbo.database.minimail.MinimailDao

/**
 * V31 (tabelas do Minimail do habbo-cms) num MariaDB real, schema pelas migrações Flyway: as três tabelas, as FKs
 * (apagar um usuário leva as mensagens dele, as cópias e as denúncias; apagar a mensagem respondida zera o reply_to),
 * uma denúncia por pessoa, o script rodando de novo sem erro, os headers do aviso no jogo (uma linha por nome em cada
 * release, DISABLED nas R63 de 2011) e o DAO que conta as não lidas. Sem Docker é PULADO.
 */
class CmsMinimailMigrationContainerTest {
    companion object {
        private var container: MariaDBContainer? = null
        private var dataSource: HikariDataSource? = null
        private lateinit var jdbi: Jdbi

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
                maximumPoolSize = 2
            }
            val ds = HikariDataSource(hikari)
            dataSource = ds

            ds.connection.use { connection ->
                connection.createStatement().use {
                    it.execute("SET FOREIGN_KEY_CHECKS = 0;\n${resource("V1__initial_schema.sql")}\nDROP TABLE IF EXISTS flyway_schema_history;\nSET FOREIGN_KEY_CHECKS = 1;")
                }
            }
            FlywayMigration.migrate(ds)
            DatabaseManager.init(ds)
            jdbi = Jdbi.create(ds)
        }

        @AfterClass
        @JvmStatic
        fun stopDatabase() {
            dataSource?.close()
            container?.stop()
        }

        private fun resource(name: String): String =
            checkNotNull(CmsMinimailMigrationContainerTest::class.java.classLoader.getResource("db/migration/$name")) {
                "$name não encontrado no classpath"
            }.readText()
    }

    private fun scalar(sql: String): Long = jdbi.withHandle<Long, Exception> { it.createQuery(sql).mapTo(Long::class.java).one() }

    private fun exec(vararg statements: String) = jdbi.useHandle<Exception> { handle -> statements.forEach { handle.execute(it) } }

    @Before
    fun clean() {
        exec(
            "SET FOREIGN_KEY_CHECKS = 0",
            "DELETE FROM cms_minimail_reports",
            "DELETE FROM cms_minimail_recipients",
            "DELETE FROM cms_minimail_messages",
            "DELETE FROM users",
            "SET FOREIGN_KEY_CHECKS = 1",
            "INSERT INTO users (id, username, email, password) VALUES (1, 'ana', 'a@test.local', 'x'), (2, 'bia', 'b@test.local', 'x'), (3, 'caio', 'c@test.local', 'x')",
        )
    }

    @Test
    fun `a V31 cria as tres tabelas do Minimail`() {
        assertEquals(31L, scalar("SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1"))
        assertEquals(
            3L,
            scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('cms_minimail_messages', 'cms_minimail_recipients', 'cms_minimail_reports')"),
        )
    }

    @Test
    fun `apagar o remetente leva a mensagem, as copias e as denuncias`() {
        exec(
            "INSERT INTO cms_minimail_messages (id, sender_id, subject, body) VALUES (10, 1, 'oi', 'x')",
            "INSERT INTO cms_minimail_recipients (message_id, user_id) VALUES (10, 2), (10, 3)",
            "INSERT INTO cms_minimail_reports (message_id, reporter_id, reason) VALUES (10, 2, 'spam')",
            "DELETE FROM users WHERE id = 1",
        )
        assertEquals(0L, scalar("SELECT COUNT(*) FROM cms_minimail_messages"))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM cms_minimail_recipients"))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM cms_minimail_reports"))
    }

    @Test
    fun `apagar um destinatario leva so a copia dele e apagar a mensagem respondida zera o reply_to`() {
        exec(
            "INSERT INTO cms_minimail_messages (id, sender_id, subject, body) VALUES (10, 1, 'oi', 'x')",
            "INSERT INTO cms_minimail_messages (id, sender_id, subject, body, reply_to) VALUES (11, 2, 'Re: oi', 'y', 10)",
            "INSERT INTO cms_minimail_recipients (message_id, user_id) VALUES (10, 2), (10, 3), (11, 1)",
            "DELETE FROM users WHERE id = 3",
        )
        assertEquals(2L, scalar("SELECT COUNT(*) FROM cms_minimail_recipients"))
        exec("DELETE FROM cms_minimail_messages WHERE id = 10")
        val replyTo = jdbi.withHandle<Long?, Exception> {
            it.createQuery("SELECT reply_to FROM cms_minimail_messages WHERE id = 11").mapTo(Long::class.javaObjectType).one()
        }
        assertNull(replyTo)
        assertEquals(1L, scalar("SELECT COUNT(*) FROM cms_minimail_recipients"))
    }

    @Test
    fun `uma denuncia por pessoa e por mensagem`() {
        exec(
            "INSERT INTO cms_minimail_messages (id, sender_id, subject, body) VALUES (10, 1, 'oi', 'x')",
            "INSERT INTO cms_minimail_reports (message_id, reporter_id, reason) VALUES (10, 2, 'spam')",
        )
        try {
            exec("INSERT INTO cms_minimail_reports (message_id, reporter_id, reason) VALUES (10, 2, 'de novo')")
            fail("a segunda denúncia da mesma pessoa devia violar a chave única")
        } catch (_: Exception) {
        }
        exec("INSERT IGNORE INTO cms_minimail_reports (message_id, reporter_id, reason) VALUES (10, 2, 'de novo')")
        assertEquals(1L, scalar("SELECT COUNT(*) FROM cms_minimail_reports"))
    }

    @Test
    fun `o script da V31 roda de novo sem erro e sem perder dados`() {
        exec("INSERT INTO cms_minimail_messages (id, sender_id, subject, body) VALUES (10, 1, 'oi', 'x')")
        dataSource!!.connection.use { c -> c.createStatement().use { it.execute(resource("V31__cms_minimail.sql")) } }
        assertEquals(1L, scalar("SELECT COUNT(*) FROM cms_minimail_messages"))
    }

    @Test
    fun `cada release tem as duas linhas do aviso e as R63 de 2011 ficam DISABLED`() {
        val releases = scalar("SELECT COUNT(*) FROM releases")
        assertEquals(2 * releases, scalar("SELECT COUNT(*) FROM releases_outgoing_headers WHERE name IN ('MINIMAIL_NEW_MESSAGE', 'MINIMAIL_UNREAD_COUNT')"))
        assertEquals(
            0L,
            scalar("SELECT COUNT(*) FROM releases_outgoing_headers WHERE name LIKE 'MINIMAIL_%' AND release_name LIKE 'RELEASE63-2011%' AND (header IS NOT NULL OR override_method <> 'DISABLED')"),
        )
        // os headers provados no AS3 (registro de eventos) de três eras
        val header = { r: String, n: String ->
            scalar("SELECT COALESCE(MAX(header), -1) FROM releases_outgoing_headers WHERE release_name = '$r' AND name = '$n'")
        }
        if (scalar("SELECT COUNT(*) FROM releases WHERE release_name = 'RELEASE63-201207040943-767851649'") > 0) {
            assertEquals(110L, header("RELEASE63-201207040943-767851649", "MINIMAIL_NEW_MESSAGE"))
            assertEquals(854L, header("RELEASE63-201207040943-767851649", "MINIMAIL_UNREAD_COUNT"))
        }
        if (scalar("SELECT COUNT(*) FROM releases WHERE release_name = 'PRODUCTION-202311200305-397722618'") > 0) {
            assertEquals(255L, header("PRODUCTION-202311200305-397722618", "MINIMAIL_NEW_MESSAGE"))
            assertEquals(389L, header("PRODUCTION-202311200305-397722618", "MINIMAIL_UNREAD_COUNT"))
        }
    }

    @Test
    fun `o DAO conta so as nao lidas da Entrada`() {
        exec(
            "INSERT INTO cms_minimail_messages (id, sender_id, subject, body) VALUES (10, 1, 'a', 'x'), (11, 1, 'b', 'y'), (12, 1, 'c', 'z')",
            "INSERT INTO cms_minimail_recipients (message_id, user_id, read_at, trashed_at) VALUES " +
                "(10, 2, NULL, NULL), (11, 2, NULL, NULL), (12, 2, NOW(), NULL), (12, 3, NULL, NOW())",
        )
        assertEquals(2, MinimailDao.countUnread(2))
        assertEquals(0, MinimailDao.countUnread(3))
        assertEquals(0, MinimailDao.countUnread(1))
    }
}
