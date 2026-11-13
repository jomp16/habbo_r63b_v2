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

package ovh.rwx.habbo.game.catalog.gift

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.mariadb.MariaDBContainer
import ovh.rwx.habbo.database.DatabaseManager
import ovh.rwx.habbo.database.catalog.GiftWrappingTypeDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.migration.FlywayMigration
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.writebehind.WriteKey
import ovh.rwx.habbo.game.item.user.CreatedItems
import ovh.rwx.habbo.game.item.user.UserItem
import java.time.LocalDateTime

/**
 * Persistência real dos presentes (MariaDB em container, schema pelas migrações Flyway): vínculo caixa -> conteúdo,
 * inventário sem o conteúdo escondido, fallback legado de items_gift e os dados do V30. Sem Docker é PULADO.
 */
class GiftPersistenceContainerTest {
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

            val v1 = checkNotNull(GiftPersistenceContainerTest::class.java.classLoader.getResource("db/migration/V1__initial_schema.sql")) {
                "V1__initial_schema.sql não encontrado no classpath"
            }.readText()
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

    private fun scalar(sql: String): Long = DatabaseManager.jdbi.withHandle<Long, Exception> { handle ->
        handle.createQuery(sql).mapTo(Long::class.java).one()
    }

    private fun exec(vararg statements: String) = DatabaseManager.jdbi.useHandle<Exception> { handle ->
        statements.forEach { handle.execute(it) }
    }

    private fun flush() = WriteBehindManager.flushAllSync()

    @Before
    fun clean() {
        GiftContentsStore.javaClass // objeto singleton: limpa o que sobrou de outros testes
        for (id in listOf(100, 101, 102, 103, 200)) GiftContentsStore.remove(id)
        exec(
            "SET FOREIGN_KEY_CHECKS = 0",
            "DELETE FROM items_gift_contents",
            "DELETE FROM items_gift",
            "DELETE FROM items_limited",
            "DELETE FROM items",
            "DELETE FROM users",
            "DELETE FROM furnishings WHERE item_name IN ('chair', 'ltd_chair', 'present_wrap*1')",
            "SET FOREIGN_KEY_CHECKS = 1",
            "INSERT INTO users (id, username, email, password) VALUES (1, 'receiver', 'r@test.local', 'x')",
            "INSERT INTO furnishings (item_name) VALUES ('chair'), ('ltd_chair'), ('present_wrap*1')",
        )
    }

    private fun item(id: Int, name: String = "chair", limited: Boolean = false) = UserItem(id, 1, name, "0", limited)

    /** Cria as linhas (como o addItems faria) e liga a caixa 100 aos conteúdos 101..; devolve os conteúdos. */
    private fun createBoxWithContents(contents: List<UserItem>, boxId: Int = 100): UserItem {
        val box = item(boxId, "present_wrap*1")
        ItemDao.batchInsertItems(listOf(box) + contents)
        contents.filter { it.limited }.forEach { ItemDao.addLimitedItem(it.id, it.id, 10) }
        ItemDao.linkGiftContents(box.id, contents)
        flush()
        return box
    }

    @Test
    fun `vinculo grava uma linha por conteudo e o conteudo some do inventario enquanto ligado`() {
        val box = createBoxWithContents(listOf(item(101), item(102)))

        assertEquals(2, scalar("SELECT COUNT(*) FROM items_gift_contents WHERE gift_item_id = ${box.id}"))
        assertEquals(3, scalar("SELECT COUNT(*) FROM items WHERE user_id = 1"))

        // só a caixa aparece nas duas consultas de inventário/itens do usuário
        assertEquals(setOf(100), ItemDao.getUserItems(1).keys)
        assertEquals(listOf(100), ItemDao.getAllUserItems(1).map { it.id })
        assertEquals(listOf(100), ItemDao.getAllUserItemsWithRoom(1).map { it.userItem.id })
    }

    @Test
    fun `depois do flush a leitura vem do banco e devolve os mesmos ids`() {
        createBoxWithContents(listOf(item(101), item(102)))

        assertNull("o vínculo gravado não deve ficar na memória", GiftContentsStore.peek(100))
        assertEquals(0, GiftContentsStore.size())
        assertEquals(listOf(101, 102), ItemDao.getGiftContents(100).map { it.id })
        assertTrue(ItemDao.getGiftContents(999).isEmpty())
    }

    @Test
    fun `antes do flush o conteudo vem da memoria`() {
        ItemDao.batchInsertItems(listOf(item(100, "present_wrap*1"), item(101)))
        val contents = listOf(item(101))
        GiftContentsStore.register(100, contents)

        assertEquals(contents, ItemDao.getGiftContents(100))
        GiftContentsStore.remove(100)
    }

    @Test
    fun `releaseGiftContents devolve o conteudo ao inventario com o mesmo id e apagar a caixa remove so a caixa`() {
        val box = createBoxWithContents(listOf(item(101), item(102)))

        ItemDao.releaseGiftContents(box.id, listOf(101, 102))
        ItemDao.deleteItems(listOf(box.id))
        flush()

        assertEquals(0, scalar("SELECT COUNT(*) FROM items_gift_contents"))
        assertEquals(0, scalar("SELECT COUNT(*) FROM items WHERE id = ${box.id}"))
        assertEquals(setOf(101, 102), ItemDao.getUserItems(1).keys)
        assertEquals(setOf(101, 102), ItemDao.getAllUserItems(1).map { it.id }.toSet())
        assertTrue(ItemDao.getGiftContents(box.id).isEmpty())
    }

    @Test
    fun `apagar a caixa sem soltar o conteudo apaga o vinculo (cascade) e nao deixa linha orfa`() {
        val box = createBoxWithContents(listOf(item(101)))

        ItemDao.deleteItems(listOf(box.id))
        flush()

        assertEquals(0, scalar("SELECT COUNT(*) FROM items_gift_contents"))
    }

    @Test
    fun `conteudo limitado mantem a linha de items_limited e continua limitado ao ser lido`() {
        val box = createBoxWithContents(listOf(item(101, "ltd_chair", limited = true)))

        assertEquals(1, scalar("SELECT COUNT(*) FROM items_limited WHERE item_id = 101"))
        assertTrue(ItemDao.getGiftContents(box.id).single().limited)

        ItemDao.releaseGiftContents(box.id, listOf(101))
        ItemDao.deleteItems(listOf(box.id))
        flush()

        assertEquals(1, scalar("SELECT COUNT(*) FROM items_limited WHERE item_id = 101"))
        val released = ItemDao.getUserItems(1)[101]
        assertNotNull(released)
        assertTrue(released!!.limited)
    }

    @Test
    fun `duas caixas nao dividem o mesmo conteudo (UNIQUE item_id)`() {
        createBoxWithContents(listOf(item(101)))
        ItemDao.batchInsertItems(listOf(item(103, "present_wrap*1")))

        var failed = false
        try {
            DatabaseManager.jdbi.useHandle<Exception> {
                it.execute("INSERT INTO items_gift_contents (gift_item_id, item_id) VALUES (103, 101)")
            }
        } catch (expected: Exception) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun `fallback legado le items_gift com o join de items_limited em ig item_id`() {
        // item 100 = a caixa legada; a linha items_gift tem id 7 (diferente do item_id de propósito)
        ItemDao.batchInsertItems(listOf(item(100, "present_wrap*1"), item(5)))
        exec(
            "INSERT INTO items_gift (id, item_id, item_name, amount, extradata) VALUES (7, 100, 'chair', 3, 'x')",
            // o limited do item 100 (caixa) existe; um limited com item_id = 7 (id da linha) NÃO deve contar
            "INSERT INTO items_limited (item_id, limited_num, limited_total) VALUES (100, 1, 5)",
        )

        val legacy = ItemDao.getGiftData(100)
        assertNotNull(legacy)
        assertEquals(7, legacy!!.id)
        assertEquals("chair", legacy.itemName)
        assertEquals(3, legacy.amount)
        assertEquals("x", legacy.extradata)
        assertTrue("join em ig.item_id deve achar o limitado", legacy.limited)

        // sem items_limited para o item da caixa: não pode vazar pelo id da linha
        exec("DELETE FROM items_limited", "INSERT INTO items_limited (item_id, limited_num, limited_total) VALUES (5, 1, 5)")
        assertFalse(ItemDao.getGiftData(100)!!.limited)

        assertNull(ItemDao.getGiftData(999))

        ItemDao.deleteGiftData(7, 100)
        flush()
        assertNull(ItemDao.getGiftData(100))
    }

    @Test
    fun `falha depois da criacao enfileira o rollback e nao sobra linha orfa nem conflito de numero LTD`() {
        // o que addItems/addLimitedItem/linkGiftContents enfileiram, com as mesmas WriteKeys, e a falha vem depois
        val contents = listOf(item(101, "ltd_chair", limited = true), item(102))
        val box = item(100, "present_wrap*1")
        val scope = CreatedItems()
        WriteBehindManager.queue(WriteKey.items((listOf(box) + contents).map { it.id })) {
            ItemDao.batchInsertItems(listOf(box) + contents)
        }
        scope.record(contents)
        ItemDao.addLimitedItem(101, 1, 10) // número LTD 1
        ItemDao.linkGiftContents(box.id, contents)
        scope.record(box)

        // a tentativa falha: o rollback entra na fila ANTES de qualquer flush (inserts ainda pendentes)
        scope.rollback { ItemDao.deleteItems(it) }

        // o próximo comprador reaproveita o número LTD 1 em outro item, sem esperar o flush
        val next = item(201, "ltd_chair", limited = true)
        WriteBehindManager.queue(WriteKey.item(next.id)) { ItemDao.batchInsertItems(listOf(next)) }
        ItemDao.addLimitedItem(next.id, 1, 10)
        flush()

        assertEquals(0, scalar("SELECT COUNT(*) FROM items WHERE id IN (100, 101, 102)"))
        assertEquals(0, scalar("SELECT COUNT(*) FROM items_gift_contents"))
        assertEquals(0, scalar("SELECT COUNT(*) FROM items_limited WHERE item_id IN (101)"))
        assertEquals(1, scalar("SELECT COUNT(*) FROM items_limited WHERE item_id = 201 AND limited_num = 1"))
        assertEquals(1, scalar("SELECT COUNT(*) FROM items"))
        assertNull("o conteúdo pendente da caixa não pode sobrar na memória", GiftContentsStore.peek(100))
    }

    @Test
    fun `V30 tipos de embrulho caixas 0 a 6 e 8 e fitas 0 a 10 com filtro por data da build`() {
        val all = GiftWrappingTypeDao.getAll()

        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 8), all.filter { it.kind == GiftWrappingKind.BOX }.map { it.typeId }.sorted())
        assertEquals((0..10).toList(), all.filter { it.kind == GiftWrappingKind.RIBBON }.map { it.typeId }.sorted())

        fun boxes(y: Int, m: Int, d: Int) = GiftWrapping.typesFor(all, GiftWrappingKind.BOX, LocalDateTime.of(y, m, d, 0, 0))
        fun ribbons(y: Int, m: Int, d: Int) = GiftWrapping.typesFor(all, GiftWrappingKind.RIBBON, LocalDateTime.of(y, m, d, 0, 0))

        val oldBoxes = (0..6).toList()
        assertEquals(oldBoxes, boxes(2011, 10, 5))
        assertEquals(oldBoxes, boxes(2013, 5, 22))
        assertEquals(oldBoxes, boxes(2025, 12, 9))
        assertEquals(oldBoxes + 8, boxes(2025, 12, 10))
        assertEquals(oldBoxes + 8, boxes(2026, 5, 18))
        assertEquals((0..10).toList(), ribbons(2011, 10, 5))
        assertEquals((0..10).toList(), ribbons(2026, 5, 18))
    }

    @Test
    fun `V30 cabecalhos R63A de CATALOG_PURCHASE_GIFT 472 e CATALOG_RECEIVER_NOT_FOUND 76`() {
        assertEquals(1, scalar("SELECT COUNT(*) FROM releases_incoming_headers_r63a WHERE name = 'CATALOG_PURCHASE_GIFT' AND header = 472"))
        assertEquals(1, scalar("SELECT COUNT(*) FROM releases_outgoing_headers_r63a WHERE name = 'CATALOG_RECEIVER_NOT_FOUND' AND header = 76"))
    }

    @Test
    fun `V30 limiares oficiais de ACH_GiftGiver e ACH_GiftReceiver (a UPDATE da migracao, com seed local porque as migracoes nao semeiam achievements)`() {
        // As migrações não trazem ACH_Gift*: monta os grupos com requisitos errados e reaplica a seção 3 da V30.
        val migration = checkNotNull(GiftPersistenceContainerTest::class.java.classLoader.getResource("db/migration/V30__gift_contents.sql")).readText()
        val start = migration.indexOf("UPDATE `achievements` a")
        val end = migration.indexOf("WHERE a.`progress_requirement` <> v.`req`;", start) + "WHERE a.`progress_requirement` <> v.`req`;".length
        assertTrue(start > 0 && end > start)
        val update = migration.substring(start, end)

        exec(
            "DELETE FROM achievements WHERE achievement_group_id IN (990001, 990002)",
            "DELETE FROM achievements_group WHERE id IN (990001, 990002)",
            "INSERT INTO achievements_group (id, name, category) VALUES (990001, 'ACH_GiftGiver', 'social'), (990002, 'ACH_GiftReceiver', 'social')",
        )
        for (level in 1..15) {
            exec("INSERT INTO achievements (achievement_group_id, level, reward_activity_points, reward_achievement_points, progress_requirement, enabled) VALUES (990001, $level, 0, 0, 999999, 1)")
        }
        for (level in 1..10) {
            exec("INSERT INTO achievements (achievement_group_id, level, reward_activity_points, reward_achievement_points, progress_requirement, enabled) VALUES (990002, $level, 0, 0, 999999, 1)")
        }

        exec(update)

        fun requirements(group: Int) = DatabaseManager.jdbi.withHandle<List<Int>, Exception> { h ->
            h.createQuery("SELECT progress_requirement FROM achievements WHERE achievement_group_id = $group ORDER BY level")
                .mapTo(Int::class.java).list()
        }

        assertEquals(
            listOf(1, 6, 14, 26, 46, 86, 146, 236, 366, 566, 816, 1141, 1541, 2041, 2591),
            requirements(990001)
        )
        assertEquals(listOf(1, 6, 14, 26, 46, 86, 146, 236, 366, 566), requirements(990002))

        exec("DELETE FROM achievements_group WHERE id IN (990001, 990002)")
    }
}
