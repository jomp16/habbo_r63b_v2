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

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
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
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerWalksOnFurni
import ovh.rwx.habbo.util.Vector2

/**
 * Mecanismo Wired inteiro (trigger -> condição -> efeito) rodando headless sobre o
 * [WiredHarness] — todo o resto (Room, WiredHandler, triggers, efeitos, interactor,
 * RoomItem) é código real.
 */
class WiredMechanismTest : WiredHarness() {
    private lateinit var lever: RoomItem
    private lateinit var lamp: RoomItem
    private lateinit var lamp3: RoomItem

    @Before
    fun setUpStacks() {
        // furnishings wired (o auto-registro do harness criaria DEFAULT — type errado)
        furnishing("wired_use_trigger", InteractionType.WIRED_TRIGGER_STATE_CHANGED)
        furnishing("wired_state_trigger", InteractionType.WIRED_TRIGGER_STUFF_STATE)
        furnishing("wired_click_trigger", InteractionType.WIRED_TRIGGER_CLICK_FURNI)
        furnishing("wired_walks_on_trigger", InteractionType.WIRED_TRIGGER_WALKS_ON_FURNI)
        furnishing("wired_toggle_effect_a", InteractionType.WIRED_EFFECT_TOGGLE_STATE)
        furnishing("wired_toggle_effect_b", InteractionType.WIRED_EFFECT_TOGGLE_STATE)
        furnishing("wired_toggle_random", InteractionType.WIRED_EFFECT_TOGGLE_TO_RANDOM)
        furnishing("wired_condition_stuff", InteractionType.WIRED_CONDITION_STUFF_IS)
        furnishing("alien", spriteId = 999)

        lever = item(10, "lever", 1, 1)
        lamp = item(11, "lamp", 2, 2)
        lamp3 = item(12, "lamp3", 3, 3)
        item(14, "lever2", 4, 1)
        item(15, "lever3", 4, 2)
        item(16, "alien", 4, 3)
        item(17, "lever4", 4, 4)

        val handler = room.itemManager.wiredHandler

        // Pilha A (5,5): "mobi é usado" (alvo: alavanca) + Toggle State (alvo: lamp)
        wiredDataByItem[20] = WiredData(20, 0, listOf(10), "", emptyList(), "")
        wiredDataByItem[21] = WiredData(21, 0, listOf(11), "", emptyList(), "")
        handler.addWiredItem(Vector2(5, 5), WiredTriggerStateChanged(room, item(20, "wired_use_trigger", 5, 5)))
        handler.addWiredItem(Vector2(5, 5), WiredEffectToggleState(room, item(21, "wired_toggle_effect_a", 5, 5)))

        // Pilha B (6,6): "estado do mobi é modificado" (alvo: lamp) + Toggle State (alvo: lamp3)
        wiredDataByItem[22] = WiredData(22, 0, listOf(11), "", emptyList(), "")
        wiredDataByItem[23] = WiredData(23, 0, listOf(12), "", emptyList(), "")
        handler.addWiredItem(Vector2(6, 6), WiredTriggerStuffState(room, item(22, "wired_state_trigger", 6, 6)))
        handler.addWiredItem(Vector2(6, 6), WiredEffectToggleState(room, item(23, "wired_toggle_effect_b", 6, 6)))

        // Pilha C (7,7): "mobi é clicado" (alvo: alavanca 10) + Toggle To Random (alvo: lamp)
        wiredDataByItem[24] = WiredData(24, 0, listOf(10), "", emptyList(), "")
        wiredDataByItem[25] = WiredData(25, 0, listOf(11), "", emptyList(), "")
        handler.addWiredItem(Vector2(7, 7), WiredTriggerClickFurni(room, item(24, "wired_click_trigger", 7, 7)))
        handler.addWiredItem(Vector2(7, 7), WiredEffectToggleToRandom(room, item(25, "wired_toggle_random", 7, 7)))

        // Pilha D (8,8): click (alavanca 14) + Stuff Is whitelist sprite do item 10 (PASSA) + Toggle State
        wiredDataByItem[26] = WiredData(26, 0, listOf(14), "", emptyList(), "")
        wiredDataByItem[27] = WiredData(27, 0, listOf(10), "", emptyList(), "", furniSources = listOf(WiredFurniSource.TRIGGERING_ITEM))
        wiredDataByItem[28] = WiredData(28, 0, listOf(11), "", emptyList(), "")
        handler.addWiredItem(Vector2(8, 8), WiredTriggerClickFurni(room, item(26, "wired_click_trigger", 8, 8)))
        handler.addWiredItem(Vector2(8, 8), WiredConditionStuffIs(room, item(27, "wired_condition_stuff", 8, 8)))
        handler.addWiredItem(Vector2(8, 8), WiredEffectToggleState(room, item(28, "wired_toggle_effect_b", 8, 8)))

        // Pilha E (9,9): click (alavanca 15) + Stuff Is whitelist do item 16/sprite 999 (BLOQUEIA) + Toggle State
        wiredDataByItem[29] = WiredData(29, 0, listOf(15), "", emptyList(), "")
        wiredDataByItem[30] = WiredData(30, 0, listOf(16), "", emptyList(), "", furniSources = listOf(WiredFurniSource.TRIGGERING_ITEM))
        wiredDataByItem[31] = WiredData(31, 0, listOf(11), "", emptyList(), "")
        handler.addWiredItem(Vector2(9, 9), WiredTriggerClickFurni(room, item(29, "wired_click_trigger", 9, 9)))
        handler.addWiredItem(Vector2(9, 9), WiredConditionStuffIs(room, item(30, "wired_condition_stuff", 9, 9)))
        handler.addWiredItem(Vector2(9, 9), WiredEffectToggleState(room, item(31, "wired_toggle_effect_b", 9, 9)))

        // Pilha F (10,10): "pisa no mobi" (alvo: alavanca 17) + Toggle State (alvo: lamp)
        wiredDataByItem[32] = WiredData(32, 0, listOf(17), "", emptyList(), "")
        wiredDataByItem[33] = WiredData(33, 0, listOf(11), "", emptyList(), "")
        handler.addWiredItem(Vector2(10, 10), WiredTriggerWalksOnFurni(room, item(32, "wired_walks_on_trigger", 10, 10)))
        handler.addWiredItem(Vector2(10, 10), WiredEffectToggleState(room, item(33, "wired_toggle_effect_b", 10, 10)))
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

    @Test
    fun testWalksOnFurniTriggersEffect() {
        // pilha F: pisou na alavanca 17 -> Toggle State no lamp (mesma forma do click,
        // sem proximidade nem usuário)
        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerWalksOnFurni::class, null, FurniTriggerData(room.itemManager.items[17]!!)
        )

        assertEquals("1", lamp.extraData)
    }
}
