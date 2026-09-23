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

package ovh.rwx.habbo.game.item.wired.trigger.triggers

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.StateTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room
import kotlin.math.abs

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_STATE_CHANGED)
class WiredTriggerStateChanged(room: Room, roomItem: RoomItem) : WiredTrigger<StateTriggerData>(room, roomItem) {
    init {
        setData()
    }

    override fun code() = WiredTriggerType.USE_STUFF.code
    override val requiresItems = true

    override fun onTrigger(wiredContext: WiredContext, data: StateTriggerData): Boolean {
        val changedItem = data.roomItem
        if (changedItem.id == roomItem.id) return false

        val items = wiredContext.getEffectiveFurnis(this)
        val matchByType = roomItem.wiredData?.furniSources?.contains(WiredFurniSource.TRIGGERING_ITEM) == true
        val triggered = if (matchByType) {
            val targetSprites = items.map { it.furnishing.spriteId }.toSet()
            changedItem.furnishing.spriteId in targetSprites
        } else {
            items.contains(changedItem)
        }

        if (triggered) {
            // Verifica se o usuário ativador está próximo do mobi
            val triggerer = wiredContext.triggererUser
            if (triggerer != null && changedItem.furnishing.type == ItemType.FLOOR) {
                val userPos = triggerer.currentVector3.vector2
                val isNear = changedItem.affectedTiles.any { tile ->
                    abs(tile.x - userPos.x) <= 1 && abs(tile.y - userPos.y) <= 1
                }
                if (!isNear) {
                    return false
                }
            }

            // Define a fonte de origem para os efeitos (Fonte 0)
            wiredContext.sourceItem = changedItem
        }

        return triggered
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}