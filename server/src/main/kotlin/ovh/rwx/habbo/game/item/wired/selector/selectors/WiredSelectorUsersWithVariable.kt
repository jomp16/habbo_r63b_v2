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

package ovh.rwx.habbo.game.item.wired.selector.selectors

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.selector.WiredSelector
import ovh.rwx.habbo.game.item.wired.selector.WiredSelectorType
import ovh.rwx.habbo.game.item.wired.variable.VariableOwnerType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_SELECTOR_USERS_WITH_VAR)
class WiredSelectorUsersWithVariable(room: Room, roomItem: RoomItem) : WiredSelector(room, roomItem) {
    override fun code() = WiredSelectorType.USERS_WITH_VARIABLE.code

    override fun onSelect(context: WiredContext) {
        val wiredData = roomItem.wiredData ?: return
        val variableId = wiredData.variableIds.firstOrNull() ?: wiredData.message
        if (variableId.isBlank()) return

        val operator = wiredData.options.getOrElse(0) { 1 }
        val checkValue = wiredData.options.getOrElse(1) { 0 } != 0
        val refValue = wiredData.options.getOrElse(2) { 0 }.toDouble()

        val matchingUsers = room.userManager.entities.values.filter { entity ->
            val userId = (entity as? RoomUser)?.habboSession?.userInformation?.id ?: entity.virtualID
            val varVal = context.variableManager.getVariableValue(variableId, userId, VariableOwnerType.USER)?.value
            if (!checkValue) {
                varVal != null
            } else {
                compareVariableValue(varVal, operator, refValue)
            }
        }

        refineTargets(
            contextTargets = context.targetUsers,
            currentSelection = matchingUsers,
            isFilter = wiredData.filter,
            isInverse = wiredData.inverse,
            allPossibleTargets = { room.userManager.entities.values }
        )
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(1, 0, 0), "")
        }
    }
}
