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

package ovh.rwx.habbo.game.item.wired.variable.variables

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.variable.*
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_VARIABLE_ECHO)
class WiredVariableEcho(room: Room, roomItem: RoomItem) : WiredVariableItem(room, roomItem) {
    override val variableItemType = WiredVariableItemType.ECHO_VARIABLE

    override fun buildVariableDefinition(): WiredVariable {
        val name = roomItem.wiredData?.message?.trim() ?: ""
        return WiredVariable(
            variableId = roomItem.id.toString(),
            variableType = WiredVariableType.USER_DEFINED,
            variableName = name,
            availabilityType = VariableAvailabilityType.TEMPORARY_ROOM,
            variableTarget = WiredVariableTarget.FURNI,
            alwaysAvailable = true,
            canCreateAndDelete = true,
            hasValue = true,
            canWriteValue = true,
            canInterceptChanges = true,
            isInvisible = false,
            canReadCreationTime = true,
            canReadLastUpdateTime = true,
            textConnectors = findStackedTextConnectors()
        )
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}

