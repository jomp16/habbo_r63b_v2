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

package ovh.rwx.habbo.game.room.trading

import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.game.HabboGame
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemManager
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.room.RightData
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomData
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.inventory.HabboInventory
import ovh.rwx.habbo.util.Vector3
import java.time.LocalDateTime

class TradeManagerTest {
    private lateinit var room: Room
    private lateinit var tradeManager: TradeManager
    private lateinit var itemManager: ItemManager

    @Before
    fun setUp() {
        itemManager = ItemManager()
        val game = mockk<HabboGame>(relaxed = true)
        every { game.itemManager } returns itemManager
        HabboGame.instance = game

        mockkObject(ItemDao)
        every { ItemDao.transferTradeItems(any(), any(), any(), any()) } just runs

        mockkObject(RoomDao)
        every { RoomDao.getRights(any()) } returns emptyList()
        every { RoomDao.getWordFilter(any()) } returns emptyList()
        every { RoomDao.updateRoomData(any()) } just runs

        room = Room(
            RoomData.createPrivate(
                id = 99999,
                userId = 999, // Dono diferente para testar direitos explicitamente
                name = "Test Room",
                description = "Test Desc",
                model = "model_a",
                category = 1,
                maxUsers = 25,
                tradeSettings = 2, // 2 = Todos podem trocar
                virtual = true
            ),
            RoomModel("model_a", 1, Vector3(0, 0, 0.0), 2, listOf("0000", "0000"), false)
        )
        tradeManager = room.tradeManager
    }

    @After
    fun tearDown() {
        unmockkObject(ItemDao)
        unmockkObject(RoomDao)
        HabboGame.instance = null
    }

    private fun createRoomUser(virtualId: Int, userId: Int, customRoom: Room = room): RoomUser {
        val session = mockk<HabboSession>(relaxed = true)
        val userInformation = UserInformation(
            id = userId,
            username = "User$userId",
            email = "user$userId@test.com",
            accountCreated = LocalDateTime.now(),
            realname = "User $userId",
            rank = 1,
            credits = 100,
            figure = "hr-115-42.hd-190-1.ch-215-62.lg-285-91.sh-290-62",
            gender = "M",
            motto = "Testing",
            homeRoom = 99999,
            vip = false,
            password = "",
            activityPointsCurrencies = mutableMapOf()
        )
        val inventory = HabboInventory(session)

        every { session.authenticated } returns true
        every { session.isPreShuffle } returns false
        every { session.userInformation } returns userInformation
        every { session.habboInventory } returns inventory

        val roomUser = RoomUser(
            habboSession = session,
            room = customRoom,
            virtualID = virtualId,
            currentVector3 = Vector3(1, 1, 0.0),
            headRotation = 2,
            bodyRotation = 2
        )
        customRoom.userManager.entities[virtualId] = roomUser
        return roomUser
    }

    private fun registerFurnishing(itemName: String, allowTrade: Boolean = true) {
        itemManager.furnishings[itemName] = Furnishing(
            itemName = itemName,
            spriteId = 100,
            offerId = 100,
            type = ItemType.FLOOR,
            stackHeight = listOf(1.0),
            canStack = true,
            canSit = false,
            canLay = false,
            walkable = false,
            allowRecycle = true,
            allowTrade = allowTrade,
            allowMarketplaceSell = true,
            canGift = true,
            allowInventoryStack = true,
            interactionType = InteractionType.DEFAULT,
            vendingIds = emptyList()
        )
    }

    private fun createUserItem(id: Int, userId: Int, itemName: String): UserItem {
        return UserItem(
            id = id,
            userId = userId,
            itemName = itemName,
            extraData = "",
            limited = false
        )
    }

    @Test
    fun `fluxo basico de iniciar troca com sucesso registra nos mapas do TradeManager`() {
        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)

        val result = tradeManager.startTrade(user1, user2)

        assertTrue(result is TradeStartResult.Success)
        val trade = (result as TradeStartResult.Success).trade

        assertEquals(1, tradeManager.getActiveTradeCount())
        assertEquals(trade, tradeManager.getTrade(user1))
        assertEquals(trade, tradeManager.getTrade(user2))
        assertEquals(TradeState.OPEN, trade.state)
    }

    @Test
    fun `iniciar troca quando usuario ja esta em negociacao retorna AlreadyInTrade ou TargetAlreadyInTrade`() {
        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)
        val user3 = createRoomUser(3, 103)

        val firstResult = tradeManager.startTrade(user1, user2)
        assertTrue(firstResult is TradeStartResult.Success)

        val initiatorBusy = tradeManager.startTrade(user1, user3)
        assertEquals(TradeStartResult.AlreadyInTrade, initiatorBusy)

        val targetBusy = tradeManager.startTrade(user3, user2)
        assertEquals(TradeStartResult.TargetAlreadyInTrade, targetBusy)
    }

    @Test
    fun `iniciar troca com usuario em outro quarto retorna NotInRoom`() {
        val otherRoom = Room(
            RoomData.createPrivate(99998, 2, "Other", "", "model_a", 1, 10, 2, virtual = true),
            RoomModel("model_a", 1, Vector3(0, 0, 0.0), 2, listOf("0000"), false)
        )
        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102, customRoom = otherRoom)

        val result = tradeManager.startTrade(user1, user2)
        assertEquals(TradeStartResult.NotInRoom, result)
    }

    @Test
    fun `quarto com trocas desativadas impede inicio da negociacao`() {
        room.roomData.tradeState = 0
        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)

        val result = tradeManager.startTrade(user1, user2)
        assertEquals(TradeStartResult.RoomTradingNotAllowed, result)
        assertEquals(0, tradeManager.getActiveTradeCount())
    }

    @Test
    fun `quarto apenas para direitos bloqueia usuarios sem direito`() {
        room.roomData.tradeState = 1 // Apenas com direitos
        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)

        // Ambos sem direito
        val result = tradeManager.startTrade(user1, user2)
        assertEquals(TradeStartResult.YouNotAllowed, result)

        // Dá direitos para user1 mas não para user2
        room.userManager.rights.add(RightData(1, 101))
        val resultTargetBlocked = tradeManager.startTrade(user1, user2)
        assertEquals(TradeStartResult.TargetNotAllowed, resultTargetBlocked)

        // Dá direitos para user2 também -> sucesso
        room.userManager.rights.add(RightData(2, 102))
        val resultSuccess = tradeManager.startTrade(user1, user2)
        assertTrue(resultSuccess is TradeStartResult.Success)
    }

    @Test
    fun `adicionar e remover itens na troca atualiza TradeManager isItemInTrade`() {
        registerFurnishing("table_wood", allowTrade = true)
        registerFurnishing("untradable_trophy", allowTrade = false)

        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)

        val item1 = createUserItem(501, 101, "table_wood")
        val itemNotTradable = createUserItem(502, 101, "untradable_trophy")
        val itemNotOwned = createUserItem(503, 101, "table_wood")

        user1.habboSession.habboInventory.items[item1.id] = item1
        user1.habboSession.habboInventory.items[itemNotTradable.id] = itemNotTradable

        val trade = (tradeManager.startTrade(user1, user2) as TradeStartResult.Success).trade

        assertFalse(tradeManager.isItemInTrade(item1.id, user1))

        // Adiciona item válido
        val addResult = trade.addItem(user1, item1)
        assertEquals(AddItemResult.Success, addResult)
        assertTrue(tradeManager.isItemInTrade(item1.id, user1))

        // Tentativa de adicionar o mesmo item novamente
        val duplicateResult = trade.addItem(user1, item1)
        assertEquals(AddItemResult.AlreadyInTrade, duplicateResult)

        // Tentativa de adicionar item não possuído
        val notOwnedResult = trade.addItem(user1, itemNotOwned)
        assertEquals(AddItemResult.ItemNotOwned, notOwnedResult)

        // Tentativa de adicionar item intransferível
        val notTradableResult = trade.addItem(user1, itemNotTradable)
        assertEquals(AddItemResult.NotTradable, notTradableResult)

        // Remove item da troca
        val removeResult = trade.removeItem(user1, item1.id)
        assertTrue(removeResult)
        assertFalse(tradeManager.isItemInTrade(item1.id, user1))
    }

    @Test
    fun `adicionar multiplos itens em lote (Habbo AIR) adiciona itens validos e ignora invalidos`() {
        registerFurnishing("table_wood", allowTrade = true)
        registerFurnishing("untradable_trophy", allowTrade = false)

        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)

        val item1 = createUserItem(701, 101, "table_wood")
        val item2 = createUserItem(702, 101, "table_wood")
        val itemUntradable = createUserItem(703, 101, "untradable_trophy")
        val itemNotOwned = createUserItem(704, 101, "table_wood")

        user1.habboSession.habboInventory.items[item1.id] = item1
        user1.habboSession.habboInventory.items[item2.id] = item2
        user1.habboSession.habboInventory.items[itemUntradable.id] = itemUntradable

        val trade = (tradeManager.startTrade(user1, user2) as TradeStartResult.Success).trade

        // Adiciona lote: item1, item2, itemUntradable, itemNotOwned
        val addedCount = trade.addItems(user1, listOf(item1, item2, itemUntradable, itemNotOwned))
        assertEquals(2, addedCount)

        assertTrue(tradeManager.isItemInTrade(item1.id, user1))
        assertTrue(tradeManager.isItemInTrade(item2.id, user1))
        assertFalse(tradeManager.isItemInTrade(itemUntradable.id, user1))
        assertFalse(tradeManager.isItemInTrade(itemNotOwned.id, user1))

        assertEquals(2, trade.getItems(user1).size)
    }

    @Test
    fun `ciclo completo de aceite, confirmacao e conclusao remove troca do TradeManager`() {
        registerFurnishing("chair_nordic", allowTrade = true)
        registerFurnishing("lamp_basic", allowTrade = true)

        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)

        val item1 = createUserItem(601, 101, "chair_nordic")
        val item2 = createUserItem(602, 102, "lamp_basic")

        user1.habboSession.habboInventory.items[item1.id] = item1
        user2.habboSession.habboInventory.items[item2.id] = item2

        val trade = (tradeManager.startTrade(user1, user2) as TradeStartResult.Success).trade
        trade.addItem(user1, item1)
        trade.addItem(user2, item2)

        // User1 aceita
        assertTrue(trade.acceptTrade(user1))
        assertEquals(TradeState.OPEN, trade.state)

        // Se user1 modificar a grade (ou unaccept), aceite é resetado
        assertTrue(trade.unacceptTrade(user1))

        // Ambos aceitam -> transita para CONFIRMING
        assertTrue(trade.acceptTrade(user1))
        assertTrue(trade.acceptTrade(user2))
        assertEquals(TradeState.CONFIRMING, trade.state)

        // User1 confirma -> aguarda o outro
        val confirm1 = trade.confirmAccept(user1)
        assertEquals(ConfirmResult.WaitingOther, confirm1)
        assertEquals(TradeState.CONFIRMING, trade.state)

        // User2 confirma -> transferência executada e trade completa
        val confirm2 = trade.confirmAccept(user2)
        assertEquals(ConfirmResult.Success, confirm2)
        assertEquals(TradeState.COMPLETED, trade.state)

        // Quando o handler recebe Success, remove a trade do manager
        tradeManager.removeTrade(trade)
        assertEquals(0, tradeManager.getActiveTradeCount())
        assertNull(tradeManager.getTrade(user1))
        assertNull(tradeManager.getTrade(user2))
    }

    @Test
    fun `declinar confirmacao cancela troca e limpa TradeManager`() {
        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)

        val trade = (tradeManager.startTrade(user1, user2) as TradeStartResult.Success).trade
        trade.acceptTrade(user1)
        trade.acceptTrade(user2)
        assertEquals(TradeState.CONFIRMING, trade.state)

        assertTrue(trade.confirmDecline())
        assertEquals(TradeState.CANCELLED, trade.state)

        tradeManager.removeTrade(trade)
        assertEquals(0, tradeManager.getActiveTradeCount())
    }

    @Test
    fun `fechar troca no estado aberto cancela e remove`() {
        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)

        val trade = (tradeManager.startTrade(user1, user2) as TradeStartResult.Success).trade
        assertTrue(trade.closeTrade())
        assertEquals(TradeState.CANCELLED, trade.state)

        tradeManager.removeTrade(trade)
        assertEquals(0, tradeManager.getActiveTradeCount())
    }

    @Test
    fun `desconexao de usuario cancela a troca e limpa ambos os participantes`() {
        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)

        val trade = (tradeManager.startTrade(user1, user2) as TradeStartResult.Success).trade
        assertNotNull(tradeManager.getTrade(user1))
        assertNotNull(tradeManager.getTrade(user2))

        tradeManager.onUserDisconnect(user1)

        assertEquals(TradeState.CANCELLED, trade.state)
        assertEquals(0, tradeManager.getActiveTradeCount())
        assertNull(tradeManager.getTrade(user1))
        assertNull(tradeManager.getTrade(user2))
    }

    @Test
    fun `clearAllTrades descarrega todas as negociacoes ativas`() {
        val user1 = createRoomUser(1, 101)
        val user2 = createRoomUser(2, 102)
        val user3 = createRoomUser(3, 103)
        val user4 = createRoomUser(4, 104)

        tradeManager.startTrade(user1, user2)
        tradeManager.startTrade(user3, user4)
        assertEquals(2, tradeManager.getActiveTradeCount())

        tradeManager.clearAllTrades()
        assertEquals(0, tradeManager.getActiveTradeCount())
        assertNull(tradeManager.getTrade(user1))
        assertNull(tradeManager.getTrade(user2))
        assertNull(tradeManager.getTrade(user3))
        assertNull(tradeManager.getTrade(user4))
    }
}
