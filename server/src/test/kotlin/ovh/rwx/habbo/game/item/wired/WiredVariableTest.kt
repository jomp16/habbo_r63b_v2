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
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectChangeVariableValue
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectGiveVariable
import ovh.rwx.habbo.game.item.wired.trigger.FurniTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerClickFurni
import ovh.rwx.habbo.game.item.wired.variable.WiredVariableTarget
import ovh.rwx.habbo.util.Vector2

/**
 * Efeitos de variável Wired headless: Give Variable (alvo FURNI e GLOBAL) e
 * Change Variable Value (operador add) — o WiredVariableManager é todo em memória.
 */
class WiredVariableTest : WiredHarness() {
    @Before
    fun setUpStacks() {
        furnishing("wired_click_trigger", InteractionType.WIRED_TRIGGER_CLICK_FURNI)
        furnishing("wired_give_variable", InteractionType.WIRED_EFFECT_GIVE_VARIABLE)
        furnishing("wired_change_variable", InteractionType.WIRED_EFFECT_CHANGE_VARIABLE_VALUE)

        item(10, "leverA", 1, 1)
        item(11, "leverB", 1, 2)

        val handler = room.itemManager.wiredHandler

        // Pilha 1 (1,1): click na alavanca A -> Give Variable score=42 na alavanca A
        // (triggering item) + Give Variable roomvar=99 (escopo GLOBAL/da sala)
        wiredDataByItem[40] = WiredData(40, 0, listOf(10), "", emptyList(), "")
        wiredDataByItem[41] = WiredData(
            41, 0, emptyList(), "",
            listOf(WiredVariableTarget.FURNI.code, 0, 42, 1), "",
            variableIds = listOf("score")
        )
        wiredDataByItem[42] = WiredData(
            42, 0, emptyList(), "",
            listOf(WiredVariableTarget.GLOBAL.code, 0, 99, 1), "",
            variableIds = listOf("roomvar")
        )
        handler.addWiredItem(Vector2(1, 1), WiredTriggerClickFurni(room, item(40, "wired_click_trigger", 1, 1)))
        handler.addWiredItem(Vector2(1, 1), WiredEffectGiveVariable(room, item(41, "wired_give_variable", 1, 1)))
        handler.addWiredItem(Vector2(1, 1), WiredEffectGiveVariable(room, item(42, "wired_give_variable", 1, 1)))

        // Pilha 2 (2,2): click na alavanca B -> Change Variable Value score += 10 na
        // alavanca A (itens selecionados), operador add (1), operando constante 10
        wiredDataByItem[43] = WiredData(43, 0, listOf(11), "", emptyList(), "")
        wiredDataByItem[44] = WiredData(
            44, 0, listOf(10), "",
            listOf(WiredVariableTarget.FURNI.code, 1, 0, 0, 10, WiredVariableTarget.CONTEXT.code), "",
            furniSources = listOf(WiredFurniSource.SELECTED_ITEMS),
            variableIds = listOf("score", "")
        )
        handler.addWiredItem(Vector2(2, 2), WiredTriggerClickFurni(room, item(43, "wired_click_trigger", 2, 2)))
        handler.addWiredItem(Vector2(2, 2), WiredEffectChangeVariableValue(room, item(44, "wired_change_variable", 2, 2)))
    }

    @Test
    fun testGiveVariableWritesFurniAndRoomScope() {
        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerClickFurni::class, null, FurniTriggerData(room.itemManager.items[10]!!)
        )

        assertEquals(42L, room.wiredVariableManager.variablesOfFurni(10)["score"]?.value as? Long)
        assertEquals(99L, room.wiredVariableManager.roomVariables()["roomvar"]?.value as? Long)
    }

    @Test
    fun testChangeVariableValueAddsToExistingFurniVariable() {
        val handler = room.itemManager.wiredHandler

        // 1. cria score=42 na alavanca A (pilha 1)
        handler.triggerWired(WiredTriggerClickFurni::class, null, FurniTriggerData(room.itemManager.items[10]!!))

        // 2. soma 10 (pilha 2): o alvo é a alavanca A via SELECTED_ITEMS
        handler.triggerWired(WiredTriggerClickFurni::class, null, FurniTriggerData(room.itemManager.items[11]!!))

        assertEquals(52L, room.wiredVariableManager.variablesOfFurni(10)["score"]?.value as? Long)
    }
}
