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
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.VariableTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_TRIGGER_VARIABLE_CHANGED)
class WiredTriggerVariableChanged(room: Room, roomItem: RoomItem) : WiredTrigger<VariableTriggerData>(room, roomItem) {
    override fun code() = WiredTriggerType.VARIABLE_UPDATE.code

    override fun onTrigger(wiredContext: WiredContext, data: VariableTriggerData): Boolean {
        val monitoredVarIds = roomItem.wiredData?.variableIds ?: emptyList()
        if (monitoredVarIds.isNotEmpty()) {
            return monitoredVarIds.contains(data.variableId)
        }

        val paramName = roomItem.wiredData?.message?.trim() ?: ""
        return paramName.isBlank() || paramName.equals(data.variableId.trim(), ignoreCase = true)
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
