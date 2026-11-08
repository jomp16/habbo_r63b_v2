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

package ovh.rwx.habbo.game.room.virtual

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ovh.rwx.habbo.config.VirtualRoomsConfig
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.pet.PetDao
import ovh.rwx.habbo.database.room.RoomBanDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.database.wired.WiredVariableDao
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.util.Vector3

/**
 * Quarto virtual de stress: fábrica determinística e, principalmente, NENHUM acesso a banco —
 * todo DAO que uma sala comum toca (carga, bans, direitos, filtro, dono, salvamento) é mockado para
 * lançar [AssertionError]; se um guard de `roomData.virtual` regredir, o teste falha.
 */
class VirtualRoomsTest {
    private val models = listOf("model_a", "model_b", "model_c").mapIndexed { i, name ->
        RoomModel(name, 0, Vector3(1, 1, 0.0), 2, (0 until 8 + i).map { "0".repeat(8 + i) }, false)
    }
    private val config = VirtualRoomsConfig(enabled = true, count = 12, maxUsers = 1000, seed = 7)

    private fun forbidden(name: String): Nothing = throw AssertionError("quarto virtual tocou o banco: $name")

    @Before
    fun forbidDatabase() {
        // UserInformationDao não entra: o inicializador dele já lê HabboServer.habboConfig (config.yaml), então
        // qualquer regressão que o chame (ex.: ownerName) também derruba o teste, sem mock
        mockkObject(RoomDao, RoomBanDao, ItemDao, PetDao, WiredVariableDao)
        every { RoomDao.getRights(any()) } answers { forbidden("RoomDao.getRights") }
        every { RoomDao.getWordFilter(any()) } answers { forbidden("RoomDao.getWordFilter") }
        every { RoomDao.updateRoomData(any()) } answers { forbidden("RoomDao.updateRoomData") }
        every { RoomDao.saveItems(any(), any()) } answers { forbidden("RoomDao.saveItems") }
        every { RoomBanDao.getBansByRoomId(any()) } answers { forbidden("RoomBanDao.getBansByRoomId") }
        every { RoomBanDao.addBan(any(), any(), any()) } answers { forbidden("RoomBanDao.addBan") }
        every { RoomBanDao.removeBan(any(), any()) } answers { forbidden("RoomBanDao.removeBan") }
        every { ItemDao.getRoomItems(any()) } answers { forbidden("ItemDao.getRoomItems") }
        every { ItemDao.preloadWiredData(any()) } answers { forbidden("ItemDao.preloadWiredData") }
        every { PetDao.getPetsByRoomId(any()) } answers { forbidden("PetDao.getPetsByRoomId") }
        every { WiredVariableDao.getVariablesForOwner(any(), any()) } answers { forbidden("WiredVariableDao.getVariablesForOwner") }
        every { WiredVariableDao.getVariablesForOwners(any(), any()) } answers { forbidden("WiredVariableDao.getVariablesForOwners") }
    }

    @After
    fun restore() {
        unmockkObject(RoomDao, RoomBanDao, ItemDao, PetDao, WiredVariableDao)
    }

    @Test
    fun `cria count quartos com ids deterministicos e marcados como virtuais`() {
        val rooms = VirtualRooms.create(config, models, categories = listOf(3, 4))

        assertEquals((1..12).map { VirtualRooms.ID_BASE + it }, rooms.keys.sorted())
        rooms.values.forEach { room ->
            assertTrue(room.roomData.virtual)
            assertTrue(VirtualRooms.isVirtualRoomId(room.roomData.id))
            assertEquals(1000, room.roomData.usersMax)
            assertEquals(VirtualRooms.OWNER_ID, room.roomData.ownerId)
            assertEquals(VirtualRooms.TAGS, room.roomData.tags)
            assertTrue(room.roomData.category in listOf(3, 4))
            assertTrue(room.roomData.modelName in models.map { it.id })
        }
    }

    @Test
    fun `mesma semente gera a mesma distribuicao de modelos e categorias`() {
        fun signature(seed: Long) = VirtualRooms.create(config.copy(seed = seed), models, listOf(1, 2, 3))
            .toSortedMap().values.map { Triple(it.roomData.name, it.roomData.modelName, it.roomData.category) }

        assertEquals(signature(7), signature(7))
        assertFalse(signature(7) == signature(8))
    }

    @Test
    fun `nomes sao pesquisaveis e unicos`() {
        val names = VirtualRooms.create(config, models, emptyList()).values.map { it.roomData.name }

        assertEquals(names.size, names.toSet().size)
        assertTrue(names.all { it.startsWith(config.namePrefix) })
    }

    @Test
    fun `ids de salas reais do banco nao sao virtuais`() {
        assertFalse(VirtualRooms.isVirtualRoomId(1))
        assertFalse(VirtualRooms.isVirtualRoomId(VirtualRooms.ID_BASE))
        assertTrue(VirtualRooms.isVirtualRoomId(VirtualRooms.ID_BASE + 1))
    }

    @Test
    fun `quarto virtual inicializa e opera sem tocar o banco`() {
        val room: Room = VirtualRooms.create(config, models, listOf(1)).values.first()

        room.initialize()

        assertTrue(room.initialized)
        assertTrue(room.itemManager.items.isEmpty())
        assertEquals(VirtualRooms.OWNER_NAME, room.roomData.ownerName)
        assertTrue(room.userManager.rights.isEmpty())
        assertTrue(room.wordFilter.isEmpty())
        assertTrue(room.bannedUsers.isEmpty())
        assertFalse(room.isBanned(5))

        // ban/unban só em memória
        room.banUser(5, "x", 60)
        assertTrue(room.isBanned(5))
        room.unbanUser(5)
        assertFalse(room.isBanned(5))

        room.wiredVariableManager.onUserEnter(2_000_000_001)

        // nada é gravado: nem a linha da sala, nem o conjunto sujo
        room.roomData.name = "renomeada"
        room.roomData.flush()
        room.saveRoom()
    }
}
