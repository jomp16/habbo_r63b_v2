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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.addon.WiredAddon
import ovh.rwx.habbo.game.item.wired.addon.WiredAddonType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_GLOBAL_PLACEHOLDER)
class WiredAddonGlobalPlaceholder(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.GLOBAL_PLACEHOLDER

    override fun onAddon(wiredContext: WiredContext) {
        val messageParts = (roomItem.wiredData?.message ?: "").split("\t")
        val placeholder = messageParts.firstOrNull()?.trim() ?: ""
        if (placeholder.isBlank()) return

        val cleanName = placeholder.removePrefix("$").removePrefix("#").removePrefix("%").removeSuffix("%")
        val staticValueOrSharedName = if (messageParts.size > 1) messageParts[1] else ""
        val mode = roomItem.wiredData?.options?.getOrElse(0) { 0 } ?: 0
        val targetRoomId = roomItem.wiredData?.options?.getOrElse(2) { 0 } ?: 0

        val resolvedValue = if (mode == 0) {
            wiredContext.formatPlaceholders(staticValueOrSharedName)
        } else {
            val targetRoom = HabboServer.habboGame.roomManager.rooms[targetRoomId]
            val sharedVal = targetRoom?.wiredVariableManager?.getVariableValue(
                staticValueOrSharedName,
                targetRoomId
            )?.value?.toString()
                ?: staticValueOrSharedName
            wiredContext.formatPlaceholders(sharedVal)
        }

        wiredContext.placeholders[cleanName] = resolvedValue
        wiredContext.placeholders[placeholder] = resolvedValue
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
