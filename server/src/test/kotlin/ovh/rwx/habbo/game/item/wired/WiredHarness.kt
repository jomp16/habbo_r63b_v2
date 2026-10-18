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

package ovh.rwx.habbo.game.item.wired

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.netty.channel.embedded.EmbeddedChannel
import org.junit.After
import org.junit.Before
import ovh.rwx.habbo.camera.Dimensions
import ovh.rwx.habbo.camera.SwfInfo
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.HabboGame
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemManager
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.interactors.DefaultItemInteractor
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomData
import ovh.rwx.habbo.game.room.RoomManager
import ovh.rwx.habbo.game.room.gamemap.RoomGamemap
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Vector3

/**
 * Base headless do mecanismo Wired: grafo de jogo leve no seam `HabboGame.instance`
 * (sem HabboServer/Flyway/Netty), só os dois lazy de DAO mockados
 * (`mockkObject(ItemDao)`) — todo o resto é código real. Subclasses montam suas
 * pilhas wired no [room] e disparam via `room.itemManager.wiredHandler.triggerWired`.
 */
abstract class WiredHarness {
    val wiredDataByItem = mutableMapOf<Int, WiredData>()
    protected lateinit var room: Room
    protected lateinit var itemManager: ItemManager
    protected lateinit var roomManager: RoomManager

    @Before
    fun setUpHarness() {
        itemManager = ItemManager()
        roomManager = RoomManager()
        val game = mockk<HabboGame>(relaxed = true)
        every { game.itemManager } returns itemManager
        every { game.roomManager } returns roomManager
        every { game.cameraManager.getSwfInfo(any()) } returns SwfInfo(
            checksum = "test",
            directions = listOf(0),
            states = listOf(0, 1), // interactionModesCount = 2 -> toggle 0 <-> 1
            dimensions = Dimensions(1.0, 1.0, 1.0),
            assets = emptyMap()
        )
        HabboGame.instance = game

        mockkObject(ItemDao)
        every { ItemDao.getRoomItems(any()) } returns emptyMap()
        every { ItemDao.getWiredData(any()) } answers { wiredDataByItem[firstArg()] }

        room = Room(
            RoomData.createPrivate(
                id = 1, userId = 1, name = "test", description = "",
                model = "model", category = 1, maxUsers = 10, tradeSettings = 0
            ),
            RoomModel("model", 1, Vector3(1, 1, 0.0), 2, (0 until 10).map { "0000000000" }, false)
        )
        roomManager.rooms[1] = room

        itemManager.furniInteractor[InteractionType.DEFAULT] = DefaultItemInteractor()
    }

    @After
    fun tearDownHarness() {
        unmockkObject(ItemDao)
        HabboGame.instance = null
    }

    protected fun furnishing(
        itemName: String,
        interactionType: InteractionType = InteractionType.DEFAULT,
        spriteId: Int = 1
    ) {
        itemManager.furnishings[itemName] = Furnishing(
            itemName = itemName,
            spriteId = spriteId,
            offerId = 0,
            type = ItemType.FLOOR,
            stackHeight = listOf(0.0),
            canStack = false,
            canSit = false,
            canLay = false,
            walkable = false,
            allowRecycle = false,
            allowTrade = false,
            allowMarketplaceSell = false,
            canGift = false,
            allowInventoryStack = false,
            interactionType = interactionType,
            vendingIds = emptyList()
        )
    }

    /** Cria o RoomItem e registra na sala; furnishing não registrado vira DEFAULT sprite 1. */
    protected fun item(id: Int, itemName: String, x: Int, y: Int): RoomItem {
        if (!itemManager.furnishings.containsKey(itemName)) furnishing(itemName)
        return RoomItem(id, 1, 1, itemName, "0", Vector3(x, y, 0.0), 0, "", false, false)
            .also { room.itemManager.items[id] = it }
    }

    /**
     * RoomUser headless: sessão de EmbeddedChannel (construtor leve), NÃO registrado no
     * userManager — broadcasts de sala iteram zero sessões e nunca tocam o HabboServer.
     */
    protected fun roomUser(virtualId: Int, x: Int, y: Int): RoomUser =
        RoomUser(HabboSession(EmbeddedChannel()), room, virtualId, Vector3(x, y, 0.0), 2, 2)

    /**
     * Inicializa o roomGamemap (lateinit) com os itens JÁ registrados — destrava os
     * efeitos de movimento/altitude, que atravessam setFloorItem e o gamemap.
     */
    protected fun initGamemap() {
        room.roomGamemap = RoomGamemap(room)
    }
}
