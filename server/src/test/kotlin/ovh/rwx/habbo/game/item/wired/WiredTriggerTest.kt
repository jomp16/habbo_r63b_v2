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
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectToggleState
import ovh.rwx.habbo.game.item.wired.trigger.GameTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.PeriodicTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerPeriodically
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerScoreAchieved
import ovh.rwx.habbo.game.room.games.GameTeam
import ovh.rwx.habbo.util.Vector2

/**
 * Triggers com máquina de estados própria headless: Periodically (contador de ticks,
 * o mesmo gate que o loop da sala chama a cada 50ms) e ScoreAchieved (latch).
 */
class WiredTriggerTest : WiredHarness() {
    private lateinit var lamp: ovh.rwx.habbo.game.item.room.RoomItem

    @Before
    fun setUpStacks() {
        furnishing("wired_periodic_trigger", InteractionType.WIRED_TRIGGER_PERIODICALLY)
        furnishing("wired_score_trigger", InteractionType.WIRED_TRIGGER_SCORE_ACHIEVED)
        furnishing("wired_toggle_effect", InteractionType.WIRED_EFFECT_TOGGLE_STATE)

        lamp = item(11, "lamp", 2, 2)

        val handler = room.itemManager.wiredHandler

        // Pilha PERIODIC (1,1): periodicamente (1 bloco de 0.5s = 10 ticks) + Toggle State
        wiredDataByItem[80] = WiredData(80, 0, emptyList(), "", listOf(1), "")
        wiredDataByItem[81] = WiredData(81, 0, listOf(11), "", emptyList(), "")
        handler.addWiredItem(Vector2(1, 1), WiredTriggerPeriodically(room, item(80, "wired_periodic_trigger", 1, 1)))
        handler.addWiredItem(Vector2(1, 1), WiredEffectToggleState(room, item(81, "wired_toggle_effect", 1, 1)))

        // Pilha SCORE (2,2): score >= 10 do time VERMELHO + Toggle State
        wiredDataByItem[82] = WiredData(82, 0, emptyList(), "", listOf(10, GameTeam.RED.color), "")
        wiredDataByItem[83] = WiredData(83, 0, listOf(11), "", emptyList(), "")
        handler.addWiredItem(Vector2(2, 2), WiredTriggerScoreAchieved(room, item(82, "wired_score_trigger", 2, 2)))
        handler.addWiredItem(Vector2(2, 2), WiredEffectToggleState(room, item(83, "wired_toggle_effect", 2, 2)))
    }

    @Test
    fun testPeriodicallyFiresOnlyOnConfiguredTickInterval() {
        val handler = room.itemManager.wiredHandler

        // 9 ticks: abaixo do intervalo (options[0] = 1 bloco de 0.5s = 10 ticks de 50ms)
        repeat(9) { handler.triggerWired(WiredTriggerPeriodically::class, null, PeriodicTriggerData) }
        assertEquals("0", lamp.extraData)

        // 10º tick: dispara
        handler.triggerWired(WiredTriggerPeriodically::class, null, PeriodicTriggerData)
        assertEquals("1", lamp.extraData)

        // mais 9 ticks: espera o próximo intervalo
        lamp.extraData = "0"
        repeat(9) { handler.triggerWired(WiredTriggerPeriodically::class, null, PeriodicTriggerData) }
        assertEquals("0", lamp.extraData)
        handler.triggerWired(WiredTriggerPeriodically::class, null, PeriodicTriggerData)
        assertEquals("1", lamp.extraData)
    }

    @Test
    fun testScoreAchievedLatchesUntilReset() {
        val handler = room.itemManager.wiredHandler

        // abaixo do requisito: não dispara
        handler.triggerWired(WiredTriggerScoreAchieved::class, null, GameTriggerData(GameTeam.RED, 5))
        assertEquals("0", lamp.extraData)

        // atinge o requisito: dispara
        handler.triggerWired(WiredTriggerScoreAchieved::class, null, GameTriggerData(GameTeam.RED, 10))
        assertEquals("1", lamp.extraData)

        // latch: atinge de novo e não re-dispara até resetTriggered (unload da sala)
        lamp.extraData = "0"
        handler.triggerWired(WiredTriggerScoreAchieved::class, null, GameTriggerData(GameTeam.RED, 10))
        assertEquals("0", lamp.extraData)
    }
}
