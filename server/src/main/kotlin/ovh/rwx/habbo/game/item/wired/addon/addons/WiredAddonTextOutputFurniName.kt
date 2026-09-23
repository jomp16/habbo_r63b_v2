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

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_TEXT_OUTPUT_FURNI_NAME)
class WiredAddonTextOutputFurniName(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.FURNI_NAME_PLACEHOLDER

    override fun onAddon(wiredContext: WiredContext) {
        val messageParts = (roomItem.wiredData?.message ?: "").split("\t")
        val placeholder = messageParts.firstOrNull()?.trim() ?: ""
        if (placeholder.isBlank()) return

        val delimiter = if (messageParts.size > 1) messageParts[1] else ", "
        val isShowMultiple = roomItem.wiredData?.options?.getOrElse(0) { 0 } == 1

        val value = if (isShowMultiple && wiredContext.targetFurnis.isNotEmpty()) {
            wiredContext.targetFurnis.joinToString(delimiter) { it.furnishing.itemName }
        } else {
            wiredContext.targetFurnis.firstOrNull()?.furnishing?.itemName
                ?: wiredContext.sourceItem?.furnishing?.itemName
                ?: ""
        }

        wiredContext.placeholders[placeholder] = value
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0), "")
        }
    }
}
