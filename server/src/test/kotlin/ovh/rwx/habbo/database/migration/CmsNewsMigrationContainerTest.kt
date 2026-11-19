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
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.mariadb.MariaDBContainer

/**
 * V32 (Notícias do habbo-cms) num MariaDB real, schema pelas migrações Flyway: a tabela, os padrões (rascunho sem
 * published_at), a FK do autor (apagar o autor mantém a notícia, com author_id NULL) e o script rodando de novo sem
 * erro. Sem Docker é PULADO.
 */
class CmsNewsMigrationContainerTest {
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
            jdbi = Jdbi.create(ds)
        }

        @AfterClass
        @JvmStatic
        fun stopDatabase() {
            dataSource?.close()
            container?.stop()
        }

        private fun resource(name: String): String =
            checkNotNull(CmsNewsMigrationContainerTest::class.java.classLoader.getResource("db/migration/$name")) {
                "$name não encontrado no classpath"
            }.readText()
    }

    private fun scalar(sql: String): Long = jdbi.withHandle<Long, Exception> { it.createQuery(sql).mapTo(Long::class.java).one() }

    private fun exec(vararg statements: String) = jdbi.useHandle<Exception> { handle -> statements.forEach { handle.execute(it) } }

    @Before
    fun clean() {
        exec(
            "SET FOREIGN_KEY_CHECKS = 0",
            "DELETE FROM cms_news",
            "DELETE FROM users",
            "SET FOREIGN_KEY_CHECKS = 1",
            "INSERT INTO users (id, username, email, password) VALUES (1, 'ana', 'a@test.local', 'x'), (2, 'bia', 'b@test.local', 'x')",
        )
    }

    @Test
    fun `a V32 cria a tabela de noticias`() {
        assertEquals(1L, scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '32' AND success = 1"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'cms_news'"))
    }

    @Test
    fun `uma noticia nova e rascunho e tem os horarios preenchidos`() {
        exec("INSERT INTO cms_news (id, title, body, author_id) VALUES (10, 'Oi', '**corpo**', 1)")
        assertEquals(1L, scalar("SELECT COUNT(*) FROM cms_news WHERE id = 10 AND published_at IS NULL AND summary = '' AND image_url = ''"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM cms_news WHERE id = 10 AND created_at IS NOT NULL AND updated_at IS NOT NULL"))
    }

    @Test
    fun `apagar o autor mantem a noticia sem autor`() {
        exec(
            "INSERT INTO cms_news (id, title, body, author_id, published_at) VALUES (10, 'a', 'x', 1, NOW()), (11, 'b', 'y', 2, NOW())",
            "DELETE FROM users WHERE id = 1",
        )
        assertEquals(2L, scalar("SELECT COUNT(*) FROM cms_news"))
        val author = jdbi.withHandle<Long?, Exception> {
            it.createQuery("SELECT author_id FROM cms_news WHERE id = 10").mapTo(Long::class.javaObjectType).one()
        }
        assertNull(author)
        assertEquals(2L, scalar("SELECT author_id FROM cms_news WHERE id = 11"))
    }

    @Test
    fun `o script da V32 roda de novo sem erro e sem perder dados`() {
        exec("INSERT INTO cms_news (id, title, body) VALUES (10, 'a', 'x')")
        dataSource!!.connection.use { c -> c.createStatement().use { it.execute(resource("V32__cms_news.sql")) } }
        assertEquals(1L, scalar("SELECT COUNT(*) FROM cms_news"))
    }
}
