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
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectMoveRotate
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectSetAltitude
import ovh.rwx.habbo.game.item.wired.trigger.FurniTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerClickFurni
import ovh.rwx.habbo.util.Vector2

/**
 * Efeitos de movimento/altitude headless: o roomGamemap (lateinit no Room) é
 * inicializado pelo harness com os itens já registrados — os efeitos atravessam
 * setFloorItem e o gamemap de verdade.
 */
class WiredMovementTest : WiredHarness() {
    @Before
    fun setUpStacks() {
        furnishing("wired_click_trigger", InteractionType.WIRED_TRIGGER_CLICK_FURNI)
        furnishing("wired_move_rotate", InteractionType.WIRED_EFFECT_MOVE_ROTATE)
        furnishing("wired_set_altitude", InteractionType.WIRED_EFFECT_SET_ALTITUDE)

        item(10, "leverA", 3, 3)
        item(11, "sofa", 5, 5)
        item(12, "leverB", 3, 4)
        item(13, "table", 6, 6)

        // gamemap DEPOIS dos itens (o construtor varre os floorItems da sala)
        initGamemap()

        val handler = room.itemManager.wiredHandler

        // Pilha ROTATE (1,1): click na alavanca A -> Move Rotate (girar horário) no sofá
        wiredDataByItem[70] = WiredData(70, 0, listOf(10), "", emptyList(), "")
        wiredDataByItem[71] = WiredData(71, 0, listOf(11), "", listOf(0, 1), "") // NONE + CLOCKWISE
        handler.addWiredItem(Vector2(1, 1), WiredTriggerClickFurni(room, item(70, "wired_click_trigger", 1, 1)))
        handler.addWiredItem(Vector2(1, 1), WiredEffectMoveRotate(room, item(71, "wired_move_rotate", 1, 1)))

        // Pilha ALTITUDE (2,2): click na alavanca B -> Set Altitude (SET 1.0) na mesa
        wiredDataByItem[72] = WiredData(72, 0, listOf(12), "", emptyList(), "")
        wiredDataByItem[73] = WiredData(73, 0, listOf(13), "", listOf(100, 2), "") // 100/100 = 1.0, SET
        handler.addWiredItem(Vector2(2, 2), WiredTriggerClickFurni(room, item(72, "wired_click_trigger", 2, 2)))
        handler.addWiredItem(Vector2(2, 2), WiredEffectSetAltitude(room, item(73, "wired_set_altitude", 2, 2)))
    }

    @Test
    fun testMoveRotateRotatesClockwiseKeepingPosition() {
        val sofa = room.itemManager.items[11]!!

        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerClickFurni::class, null, FurniTriggerData(room.itemManager.items[10]!!)
        )

        assertEquals("Rotação 0 -> 2 (horário)", 2, sofa.rotation)
        assertEquals("Posição não muda (direção NONE)", Vector2(5, 5), sofa.position.vector2)
    }

    @Test
    fun testSetAltitudeSetsExactHeight() {
        val table = room.itemManager.items[13]!!

        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerClickFurni::class, null, FurniTriggerData(room.itemManager.items[12]!!)
        )

        assertEquals("Altitude configurada em 1.0", 1.0, table.position.z, 0.001)
    }
}
