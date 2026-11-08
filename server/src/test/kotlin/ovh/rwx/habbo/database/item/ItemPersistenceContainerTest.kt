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

package ovh.rwx.habbo.database.item

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.mariadb.MariaDBContainer
import ovh.rwx.habbo.database.DatabaseManager
import ovh.rwx.habbo.database.migration.FlywayMigration
import ovh.rwx.habbo.game.item.user.NewItemSpec
import ovh.rwx.habbo.game.item.user.planNewItems

/**
 * Persistência real: MariaDB 13.0 (mesma versão de produção) num container descartável, com o schema criado pelas
 * migrações Flyway de verdade e o SQL do [ItemDao]. Sem Docker o teste é PULADO (não falha), então o CI sem Docker
 * continua verde.
 *
 * Não sobe o HabboServer (que lê o config.yaml): só o necessário — DataSource + [DatabaseManager].
 */
class ItemPersistenceContainerTest {
    companion object {
        private var container: MariaDBContainer? = null
        private var dataSource: HikariDataSource? = null

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

            // Espelha produção: o emulador sempre parte de um banco que JÁ está no schema do V1 (o V1 é um dump que não cria
            // um banco vazio por conta própria: sem FOREIGN_KEY_CHECKS=0 e com a própria flyway_schema_history). Então carrega
            // o V1 como "banco existente" e deixa o caminho real (FlywayMigration: baseline na versão 1) aplicar o resto.
            val v1 = checkNotNull(ItemPersistenceContainerTest::class.java.classLoader.getResource("db/migration/V1__initial_schema.sql")) {
                "V1__initial_schema.sql não encontrado no classpath"
            }.readText()
            ds.connection.use { connection ->
                connection.createStatement().use {
                    // o dump traz uma flyway_schema_history vazia; sem ela o Flyway enxerga "schema existente sem histórico"
                    // e faz o baseline na versão 1 (o caminho de produção) em vez de reexecutar o V1
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

    private fun scalar(sql: String): Long = DatabaseManager.jdbi.withHandle<Long, Exception> { handle ->
        handle.createQuery(sql).mapTo(Long::class.java).one()
    }

    @Before
    fun clean() {
        DatabaseManager.jdbi.useHandle<Exception> { handle ->
            handle.execute("SET FOREIGN_KEY_CHECKS = 0")
            handle.execute("DELETE FROM items_teleport")
            handle.execute("DELETE FROM items")
            handle.execute("DELETE FROM users")
            handle.execute("DELETE FROM furnishings WHERE item_name IN ('tele_a', 'chair')")
            handle.execute("SET FOREIGN_KEY_CHECKS = 1")
            handle.execute("INSERT INTO users (id, username, email, password) VALUES (1, 'tester', 'tester@test.local', 'x')")
            // items.item_name tem FK para furnishings; o resto das colunas usa os defaults do schema
            handle.execute("INSERT INTO furnishings (item_name) VALUES ('tele_a'), ('chair')")
        }
    }

    @Test
    fun `as migracoes Flyway criam o schema de itens`() {
        assertTrue(scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1") > 0)
        // as tabelas que o ItemDao usa existem
        assertEquals(0, scalar("SELECT COUNT(*) FROM items"))
        assertEquals(0, scalar("SELECT COUNT(*) FROM items_teleport"))
    }

    @Test
    fun `comprar teletransportes grava 2 itens e 1 par por unidade`() {
        var next = 1000
        val units = 3
        val plan = planNewItems(
            userId = 1,
            specs = List(units) { NewItemSpec("tele_a", "0", limited = false, buildersClub = false) },
            isTeleport = { true },
            nextId = { next++ },
        )

        ItemDao.batchInsertItems(plan.all)
        ItemDao.insertTeleportLinks(plan.teleportLinks)

        assertEquals("itens", (2 * units).toLong(), scalar("SELECT COUNT(*) FROM items WHERE user_id = 1"))
        // uma linha por sentido
        assertEquals("ligações", (2 * units).toLong(), scalar("SELECT COUNT(*) FROM items_teleport"))
        // cada item está ligado a exatamente um parceiro, do mesmo dono e do mesmo tipo
        assertEquals(
            "itens sem exatamente um parceiro",
            0,
            scalar(
                "SELECT COUNT(*) FROM items i WHERE (SELECT COUNT(*) FROM items_teleport t WHERE t.teleport_one_id = i.id) <> 1"
            ),
        )
        assertEquals(
            "pares inconsistentes",
            (2 * units).toLong(),
            scalar(
                "SELECT COUNT(*) FROM items_teleport t JOIN items a ON a.id = t.teleport_one_id " +
                    "JOIN items b ON b.id = t.teleport_two_id WHERE a.user_id = b.user_id AND a.item_name = b.item_name"
            ),
        )
        // a ligação é simétrica
        assertEquals(
            0,
            scalar(
                "SELECT COUNT(*) FROM items_teleport t WHERE NOT EXISTS " +
                    "(SELECT 1 FROM items_teleport r WHERE r.teleport_one_id = t.teleport_two_id AND r.teleport_two_id = t.teleport_one_id)"
            ),
        )
    }

    @Test
    fun `mobi comum nao cria ligacao`() {
        var next = 2000
        val plan = planNewItems(1, List(4) { NewItemSpec("chair", "0", false, false) }, isTeleport = { false }, nextId = { next++ })

        ItemDao.batchInsertItems(plan.all)
        ItemDao.insertTeleportLinks(plan.teleportLinks)

        assertEquals(4, scalar("SELECT COUNT(*) FROM items WHERE user_id = 1"))
        assertEquals(0, scalar("SELECT COUNT(*) FROM items_teleport"))
    }

    @Test
    fun `ligar o par antes de criar os itens e rejeitado pelo banco (por isso a escrita declara as chaves dos dois itens)`() {
        try {
            ItemDao.insertTeleportLinks(listOf(3000 to 3001))
            fail("o banco deveria rejeitar ligação para itens inexistentes")
        } catch (expected: Exception) {
            // FK de items_teleport -> items
        }
        assertEquals(0, scalar("SELECT COUNT(*) FROM items_teleport"))
    }
}
