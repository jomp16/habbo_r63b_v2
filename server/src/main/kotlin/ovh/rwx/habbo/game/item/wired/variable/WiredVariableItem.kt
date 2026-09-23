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

package ovh.rwx.habbo.game.item.wired.variable

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredItem
import ovh.rwx.habbo.game.room.Room

abstract class WiredVariableItem(room: Room, roomItem: RoomItem) : WiredItem(room, roomItem) {
    abstract val variableItemType: WiredVariableItemType

    override fun code(): Int = variableItemType.code

    abstract fun buildVariableDefinition(): WiredVariable

    fun registerToManager() {
        val name = roomItem.wiredData?.message?.trim() ?: ""
        if (name.isNotEmpty()) {
            val def = buildVariableDefinition()
            room.wiredVariableManager.registerVariable(def)
        } else {
            room.wiredVariableManager.deleteVariable(roomItem.id.toString())
        }
    }

    override fun setData() {
        registerToManager()
    }

    fun findStackedTextConnectors(): Map<Int, String> {
        val itemsOnTile = room.roomGamemap.getItemsFromVector2(roomItem.position.vector2)
        val textConnectorItem = itemsOnTile.firstOrNull {
            it.furnishing.interactionType == InteractionType.WIRED_EXTRA_VARIABLE_TEXT_CONNECTOR ||
                    it.furnishing.itemName == "wf_xtra_text_connector" ||
                    it.furnishing.itemName == "wf_xtra_var_text_connector" ||
                    it.furnishing.interactionType.name.contains("VARIABLE_TEXT_CONNECTOR")
        } ?: return emptyMap()

        val msg = textConnectorItem.wiredData?.message ?: return emptyMap()
        return parseTextConnectors(msg)
    }

    companion object {
        fun parseTextConnectors(message: String): Map<Int, String> {
            if (message.isBlank()) return emptyMap()
            val map = mutableMapOf<Int, String>()
            message.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && trimmed.contains("=")) {
                    val parts = trimmed.split("=", limit = 2)
                    val key = parts[0].trim().toIntOrNull()
                    if (key != null) {
                        map[key] = parts[1]
                    }
                }
            }
            return map
        }
    }
}

