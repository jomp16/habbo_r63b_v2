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
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_TEXT_INPUT_VARIABLE)
class WiredAddonTextInputVariable(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.VARIABLE_CAPTURER

    override fun onAddon(wiredContext: WiredContext) {
        val rawName = roomItem.wiredData?.message?.split("\t")?.firstOrNull()?.trim() ?: ""
        if (rawName.isBlank()) return

        val cleanName = rawName.removePrefix("#").removePrefix("$").removePrefix("%").removeSuffix("%")
        val isTextMode = roomItem.wiredData?.options?.firstOrNull() == 1
        val variableId = roomItem.wiredData?.variableIds?.firstOrNull() ?: cleanName

        val value = wiredContext.placeholders[cleanName]
            ?: wiredContext.placeholders[rawName]
            ?: wiredContext.placeholders["#$cleanName"]
            ?: wiredContext.placeholders["$$cleanName"]
            ?: wiredContext.placeholders["%$cleanName%"]
            ?: (wiredContext.triggererUser as? RoomUser)?.habboSession?.userInformation?.username
            ?: ""

        if (isTextMode) {
            wiredContext.setVariable(variableId, value)
        } else {
            val numVal = value.toDoubleOrNull()?.let { if (it % 1.0 == 0.0) it.toLong() else it } ?: value
            wiredContext.setVariable(variableId, numVal)
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
