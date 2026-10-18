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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
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
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.condition.conditions.WiredConditionStuffIs
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectToggleState
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectToggleToRandom
import ovh.rwx.habbo.game.item.wired.trigger.FurniTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.StateTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerClickFurni
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerStateChanged
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerStuffState
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomData
import ovh.rwx.habbo.game.room.RoomManager
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3

/**
 * Mecanismo Wired inteiro (trigger -> condição -> efeito) rodando headless: grafo de
 * jogo leve injetado no seam `HabboGame.instance` (sem HabboServer/Flyway/Netty) e só
 * os dois lazy de DAO (`ItemDao.getRoomItems`/`getWiredData`) mockados — todo o resto
 * (Room, WiredHandler, triggers, efeitos, interactor, RoomItem) é o código real.
 */
class WiredMechanismTest {
    private val wiredDataByItem = mutableMapOf<Int, WiredData>()
    private lateinit var room: Room
    private lateinit var lever: RoomItem
    private lateinit var lamp: RoomItem
    private lateinit var lamp3: RoomItem

    @Before
    fun setUp() {
        val itemManager = ItemManager()
        val roomManager = RoomManager()
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
            RoomModel("model", 1, Vector3(1, 1, 0.0), 2, listOf("0000000000"), false)
        )
        roomManager.rooms[1] = room

        fun furnishing(itemName: String, interactionType: InteractionType, spriteId: Int = 1) = Furnishing(
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
        itemManager.furnishings["lever"] = furnishing("lever", InteractionType.DEFAULT)
        itemManager.furnishings["lamp"] = furnishing("lamp", InteractionType.DEFAULT)
        itemManager.furnishings["lamp3"] = furnishing("lamp3", InteractionType.DEFAULT)
        itemManager.furnishings["wired_use_trigger"] = furnishing("wired_use_trigger", InteractionType.WIRED_TRIGGER_STATE_CHANGED)
        itemManager.furnishings["wired_toggle_effect_a"] = furnishing("wired_toggle_effect_a", InteractionType.WIRED_EFFECT_TOGGLE_STATE)
        itemManager.furnishings["wired_state_trigger"] = furnishing("wired_state_trigger", InteractionType.WIRED_TRIGGER_STUFF_STATE)
        itemManager.furnishings["wired_toggle_effect_b"] = furnishing("wired_toggle_effect_b", InteractionType.WIRED_EFFECT_TOGGLE_STATE)
        itemManager.furnishings["wired_click_trigger"] = furnishing("wired_click_trigger", InteractionType.WIRED_TRIGGER_CLICK_FURNI)
        itemManager.furnishings["wired_toggle_random"] = furnishing("wired_toggle_random", InteractionType.WIRED_EFFECT_TOGGLE_TO_RANDOM)
        itemManager.furnishings["wired_condition_stuff"] = furnishing("wired_condition_stuff", InteractionType.WIRED_CONDITION_STUFF_IS)
        itemManager.furnishings["lever2"] = furnishing("lever2", InteractionType.DEFAULT)
        itemManager.furnishings["lever3"] = furnishing("lever3", InteractionType.DEFAULT)
        itemManager.furnishings["alien"] = furnishing("alien", InteractionType.DEFAULT, spriteId = 999)
        itemManager.furniInteractor[InteractionType.DEFAULT] = DefaultItemInteractor()

        fun item(id: Int, itemName: String, x: Int, y: Int) =
            RoomItem(id, 1, 1, itemName, "0", Vector3(x, y, 0.0), 0, "", false, false)
                .also { room.itemManager.items[id] = it }

        lever = item(10, "lever", 1, 1)
        lamp = item(11, "lamp", 2, 2)
        lamp3 = item(12, "lamp3", 3, 3)

        // Pilha A (tile 5,5): "mobi é usado" (alvo: alavanca) + Toggle State (alvo: lamp)
        wiredDataByItem[20] = WiredData(20, 0, listOf(10), "", emptyList(), "")
        wiredDataByItem[21] = WiredData(21, 0, listOf(11), "", emptyList(), "")
        room.itemManager.wiredHandler.addWiredItem(Vector2(5, 5), WiredTriggerStateChanged(room, item(20, "wired_use_trigger", 5, 5)))
        room.itemManager.wiredHandler.addWiredItem(Vector2(5, 5), WiredEffectToggleState(room, item(21, "wired_toggle_effect_a", 5, 5)))

        // Pilha B (tile 6,6): "estado do mobi é modificado" (alvo: lamp) + Toggle State (alvo: lamp3)
        wiredDataByItem[22] = WiredData(22, 0, listOf(11), "", emptyList(), "")
        wiredDataByItem[23] = WiredData(23, 0, listOf(12), "", emptyList(), "")
        room.itemManager.wiredHandler.addWiredItem(Vector2(6, 6), WiredTriggerStuffState(room, item(22, "wired_state_trigger", 6, 6)))
        room.itemManager.wiredHandler.addWiredItem(Vector2(6, 6), WiredEffectToggleState(room, item(23, "wired_toggle_effect_b", 6, 6)))

        // Pilha C (tile 7,7): "mobi é clicado" (alvo: alavanca 10) + Toggle To Random (alvo: lamp)
        wiredDataByItem[24] = WiredData(24, 0, listOf(10), "", emptyList(), "")
        wiredDataByItem[25] = WiredData(25, 0, listOf(11), "", emptyList(), "")
        room.itemManager.wiredHandler.addWiredItem(Vector2(7, 7), WiredTriggerClickFurni(room, item(24, "wired_click_trigger", 7, 7)))
        room.itemManager.wiredHandler.addWiredItem(Vector2(7, 7), WiredEffectToggleToRandom(room, item(25, "wired_toggle_random", 7, 7)))

        // Pilha D (tile 8,8): "mobi é clicado" (alvo: alavanca 14) + condição Stuff Is
        // (whitelist de sprite = item 10) + Toggle State (alvo: lamp) — condição PASSA
        wiredDataByItem[26] = WiredData(26, 0, listOf(14), "", emptyList(), "")
        wiredDataByItem[27] = WiredData(27, 0, listOf(10), "", emptyList(), "", furniSources = listOf(WiredFurniSource.TRIGGERING_ITEM))
        wiredDataByItem[28] = WiredData(28, 0, listOf(11), "", emptyList(), "")
        room.itemManager.wiredHandler.addWiredItem(Vector2(8, 8), WiredTriggerClickFurni(room, item(26, "wired_click_trigger", 8, 8)))
        room.itemManager.wiredHandler.addWiredItem(Vector2(8, 8), WiredConditionStuffIs(room, item(27, "wired_condition_stuff", 8, 8)))
        room.itemManager.wiredHandler.addWiredItem(Vector2(8, 8), WiredEffectToggleState(room, item(28, "wired_toggle_effect_b", 8, 8)))

        // Pilha E (tile 9,9): "mobi é clicado" (alvo: alavanca 15) + condição Stuff Is
        // (whitelist de sprite = item 16, sprite 999) + Toggle State (alvo: lamp) — condição BLOQUEIA
        wiredDataByItem[29] = WiredData(29, 0, listOf(15), "", emptyList(), "")
        wiredDataByItem[30] = WiredData(30, 0, listOf(16), "", emptyList(), "", furniSources = listOf(WiredFurniSource.TRIGGERING_ITEM))
        wiredDataByItem[31] = WiredData(31, 0, listOf(11), "", emptyList(), "")
        room.itemManager.wiredHandler.addWiredItem(Vector2(9, 9), WiredTriggerClickFurni(room, item(29, "wired_click_trigger", 9, 9)))
        room.itemManager.wiredHandler.addWiredItem(Vector2(9, 9), WiredConditionStuffIs(room, item(30, "wired_condition_stuff", 9, 9)))
        room.itemManager.wiredHandler.addWiredItem(Vector2(9, 9), WiredEffectToggleState(room, item(31, "wired_toggle_effect_b", 9, 9)))

        // itens das alavancas/whitelists das pilhas C-E
        item(14, "lever2", 4, 1)
        item(15, "lever3", 4, 2)
        item(16, "alien", 4, 3)
    }

    @After
    fun tearDown() {
        unmockkObject(ItemDao)
        HabboGame.instance = null
    }

    @Test
    fun testWiredChainExecutesDownstreamEffectsExactlyOnce() {
        // alavanca usada -> pilha A: Toggle State no lamp -> interactor notifica o trigger
        // de estado -> pilha B: Toggle State no lamp3
        room.itemManager.wiredHandler.triggerWired(WiredTriggerStateChanged::class, null, StateTriggerData(lever))

        assertEquals("1", lamp.extraData) // toggle 0 -> 1 (exatamente 1x)
        // regressão do disparo duplicado do WiredEffectToggleState: com 2 notificações
        // de estado o lamp3 voltava a "0" (toggle 2x); 1 notificação = "1"
        assertEquals("1", lamp3.extraData)
    }

    @Test
    fun testManualStateChangeFiresDownstreamExactlyOnce() {
        // mudança manual de estado (só o evento, sem wired intermediário)
        room.itemManager.wiredHandler.triggerWired(WiredTriggerStuffState::class, null, StateTriggerData(lamp))

        assertEquals("1", lamp3.extraData)
        assertEquals("0", lamp.extraData) // ninguém toggled o lamp — só o evento de estado
    }

    @Test
    fun testClickFurniTriggersToggleToRandomAndNotifiesStateOnce() {
        // pilha C: alavanca 10 clicada -> Toggle To Random no lamp (muda estado direto,
        // sem interactor) -> notifica o trigger de estado 1x -> lamp3 toggled 1x
        room.itemManager.wiredHandler.triggerWired(WiredTriggerClickFurni::class, null, FurniTriggerData(lever))

        val newMode = lamp.extraData.toIntOrNull()
        assertEquals("O estado deve mudar (guard do ToggleToRandom garante != do atual)", true, newMode != null && newMode != 0)
        assertEquals("1", lamp3.extraData) // notificação de estado disparou a pilha B exatamente 1x
    }

    @Test
    fun testConditionStuffIsPassesWhenSpriteMatches() {
        // pilha D: alavanca 14 (sprite 1) clicada -> condição Stuff Is com whitelist de
        // sprite do item 10 (sprite 1) -> PASSA -> Toggle State no lamp
        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerClickFurni::class, null, FurniTriggerData(room.itemManager.items[14]!!)
        )

        assertEquals("1", lamp.extraData)
        assertEquals("1", lamp3.extraData) // pilha B notificada 1x
    }

    @Test
    fun testConditionStuffIsBlocksWhenSpriteDoesNotMatch() {
        // pilha E: alavanca 15 (sprite 1) clicada -> condição Stuff Is com whitelist do
        // item 16 (sprite 999) -> FALHA -> efeitos bloqueados, nada muda
        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerClickFurni::class, null, FurniTriggerData(room.itemManager.items[15]!!)
        )

        assertEquals("0", lamp.extraData)
        assertEquals("0", lamp3.extraData) // sem mudança de estado, sem notificação
    }
}
