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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectFreezeUser
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectMuteTriggerer
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectToggleState
import ovh.rwx.habbo.game.item.wired.trigger.FurniTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.SayTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerClickFurni
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerSaysSomething
import ovh.rwx.habbo.util.Vector2

/**
 * Efeitos e triggers de usuário Wired headless: o triggerer é um RoomUser real
 * (sessão de EmbeddedChannel, fora do userManager — broadcasts no-op).
 */
class WiredUserEffectTest : WiredHarness() {
    private lateinit var lamp: ovh.rwx.habbo.game.item.room.RoomItem

    @Before
    fun setUpStacks() {
        furnishing("wired_click_trigger", InteractionType.WIRED_TRIGGER_CLICK_FURNI)
        furnishing("wired_says_trigger", InteractionType.WIRED_TRIGGER_SAYS_SOMETHING)
        furnishing("wired_mute", InteractionType.WIRED_EFFECT_MUTE_AVATAR)
        furnishing("wired_freeze", InteractionType.WIRED_EFFECT_FREEZE)
        furnishing("wired_toggle_effect", InteractionType.WIRED_EFFECT_TOGGLE_STATE)

        item(10, "lever", 1, 1)
        lamp = item(11, "lamp", 2, 2)

        val handler = room.itemManager.wiredHandler

        // Pilha MUTE (1,1): click na alavanca -> Mute Triggerer (5 min)
        wiredDataByItem[60] = WiredData(60, 0, listOf(10), "", emptyList(), "")
        wiredDataByItem[61] = WiredData(61, 0, emptyList(), "", listOf(5), "")
        handler.addWiredItem(Vector2(1, 1), WiredTriggerClickFurni(room, item(60, "wired_click_trigger", 1, 1)))
        handler.addWiredItem(Vector2(1, 1), WiredEffectMuteTriggerer(room, item(61, "wired_mute", 1, 1)))

        // Pilha FREEZE (2,2): click na alavanca -> Freeze User (efeito wired)
        wiredDataByItem[62] = WiredData(62, 0, listOf(10), "", emptyList(), "")
        wiredDataByItem[63] = WiredData(63, 0, emptyList(), "", listOf(0, 0), "")
        handler.addWiredItem(Vector2(2, 2), WiredTriggerClickFurni(room, item(62, "wired_click_trigger", 2, 2)))
        handler.addWiredItem(Vector2(2, 2), WiredEffectFreezeUser(room, item(63, "wired_freeze", 2, 2)))

        // Pilha SAYS (3,3): "diz algo contendo 'abrir'" + Toggle State no lamp
        wiredDataByItem[64] = WiredData(64, 0, emptyList(), "abrir", listOf(0, 0, 0), "")
        wiredDataByItem[65] = WiredData(65, 0, listOf(11), "", emptyList(), "")
        handler.addWiredItem(Vector2(3, 3), WiredTriggerSaysSomething(room, item(64, "wired_says_trigger", 3, 3)))
        handler.addWiredItem(Vector2(3, 3), WiredEffectToggleState(room, item(65, "wired_toggle_effect", 3, 3)))
    }

    @Test
    fun testMuteTriggererAddsMuteStatusToTriggerer() {
        val user = roomUser(50, 2, 2)

        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerClickFurni::class, user, FurniTriggerData(room.itemManager.items[10]!!)
        )

        assertTrue("O triggerer deve ganhar o status mute", user.statusMap.containsKey("mute"))
    }

    @Test
    fun testFreezeUserFreezesTriggererAndAppliesEffect() {
        val user = roomUser(51, 2, 2)

        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerClickFurni::class, user, FurniTriggerData(room.itemManager.items[10]!!)
        )

        assertTrue(user.frozen)
        assertTrue(user.effect != null) // efeito visual WIRED (218)
    }

    @Test
    fun testSaysSomethingContainsMatchesAndMisses() {
        val user = roomUser(52, 2, 2)
        val handler = room.itemManager.wiredHandler

        // mensagem contendo "abrir" -> efeito roda
        handler.triggerWired(WiredTriggerSaysSomething::class, user, SayTriggerData("comando: abrir!"))
        assertEquals("1", lamp.extraData)

        // mensagem sem "abrir" -> nada acontece
        lamp.extraData = "0"
        handler.triggerWired(WiredTriggerSaysSomething::class, user, SayTriggerData("nada a ver"))
        assertEquals("0", lamp.extraData)
        assertFalse(user.statusMap.containsKey("mute")) // pilhas de efeito de usuário intactas
    }
}
