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

package ovh.rwx.habbo.game.item.wired.addon.addons

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.addon.WiredAddon
import ovh.rwx.habbo.game.item.wired.addon.WiredAddonType
import ovh.rwx.habbo.game.item.wired.variable.VariableOwnerType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_FILTER_USERS_BY_VAR)
class WiredAddonFilterUsersByVariable(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.USER_VARIABLE_FILTER

    override fun onAddon(wiredContext: WiredContext) {
        val wiredData = roomItem.wiredData ?: return
        val variableId = wiredData.variableIds.firstOrNull() ?: wiredData.message
        if (variableId.isBlank()) return

        val operator = wiredData.options.getOrElse(0) { 1 }
        val checkValue = wiredData.options.getOrElse(1) { 0 } != 0
        val refValue = wiredData.options.getOrElse(2) { 0 }.toDouble()

        wiredContext.targetUsers.retainAll { entity ->
            val userId = (entity as? RoomUser)?.habboSession?.userInformation?.id ?: entity.virtualID
            val varVal =
                wiredContext.variableManager.getVariableValue(variableId, userId, VariableOwnerType.USER)?.value
            if (!checkValue) {
                varVal != null
            } else {
                compareValue(varVal, operator, refValue)
            }
        }
    }

    private fun compareValue(varValue: Any?, operator: Int, refValue: Double): Boolean {
        if (varValue == null) return false
        val num = (varValue as? Number)?.toDouble() ?: varValue.toString().toDoubleOrNull()
        if (num != null) {
            return when (operator) {
                0 -> num < refValue
                1 -> Math.abs(num - refValue) < 0.0001
                2 -> num > refValue
                3 -> num <= refValue
                4 -> Math.abs(num - refValue) >= 0.0001
                5 -> num >= refValue
                else -> false
            }
        }
        val str = varValue.toString()
        val refStr = if (refValue % 1.0 == 0.0) refValue.toLong().toString() else refValue.toString()
        return when (operator) {
            1 -> str.equals(refStr, ignoreCase = true)
            4 -> !str.equals(refStr, ignoreCase = true)
            else -> false
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(1, 0, 0), "")
        }
    }
}
