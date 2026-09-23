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
import ovh.rwx.habbo.game.item.wired.variable.WiredVariableItem
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_VARIABLE_TEXT_CONNECTOR)
class WiredAddonVariableTextConnector(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.VARIABLE_TEXT_CONVERTER

    override fun setData() {
        val itemsOnTile = room.roomGamemap.getItemsFromVector2(roomItem.position.vector2)
        itemsOnTile.forEach { item ->
            if (item.id != roomItem.id && item.furnishing.interactionType.name.startsWith("WIRED_VARIABLE_")) {
                val variableItem = HabboServer.habboGame.itemManager.getWiredInstance(room, item) as? WiredVariableItem
                variableItem?.registerToManager()
            }
        }
    }

    override fun onAddon(wiredContext: WiredContext) {
        val template = roomItem.wiredData?.message ?: ""
        if (template.isBlank()) return

        val formattedText = wiredContext.formatPlaceholders(template)
        wiredContext.placeholders["text"] = formattedText
        wiredContext.placeholders["connector_text"] = formattedText
        wiredContext.variables["text"] = formattedText
    }


    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
