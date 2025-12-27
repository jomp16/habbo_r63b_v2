/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.item.wired

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room

abstract class WiredItem(protected val room: Room, val roomItem: RoomItem) {
    open fun saveWired(habboRequest: HabboRequest): Boolean {
        roomItem.wiredData?.let {
            val settingsIntCount = habboRequest.readInt()
            val options = mutableListOf<Int>()

            repeat(settingsIntCount) { _ ->
                options += habboRequest.readInt()
            }

            it.options = options
            it.message = habboRequest.readUTF()

            val itemsCount = habboRequest.readInt()
            val roomItemsIds = mutableListOf<Int>()

            repeat(itemsCount) { _ ->
                val itemId = habboRequest.readInt()

                if (room.roomItems.containsKey(itemId)) {
                    val roomItem1 = room.roomItems[itemId] ?: return@repeat

                    if (!roomItem1.furnishing.interactionType.name.startsWith("WIRED")) roomItemsIds += itemId
                }
            }

            it.items = roomItemsIds

            if (roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT")) {
                it.delay = habboRequest.readInt()
            }

            setData()
            return true
        }
        return false
    }

    open fun setData() {
        // Override in subclasses to reload internal variables
    }

    open fun writeDialog(habboResponse: HabboResponse, wiredData: WiredData) {
        // Default implementation - override in subclasses
    }

    companion object {
        fun HabboResponse.writeEmptyItems() {
            writeInt(0) // selectable items
            writeInt(0) // items
        }

        fun HabboResponse.writeItems(wiredData: WiredData) {
            writeInt(20) // selectable items
            if (wiredData.items.isEmpty()) writeInt(0)
            else wiredData.items.let { roomItems ->
                writeInt(roomItems.size) // how many selected items
                roomItems.forEach { writeInt(it) } // items
            }
        }

        fun HabboResponse.writeItemInfo(roomItem: RoomItem) {
            writeInt(roomItem.furnishing.spriteId)
            writeInt(roomItem.id)
        }

        fun HabboResponse.writeDelay(wiredData: WiredData) {
            writeInt(wiredData.delay)
        }

        @Suppress("UNUSED_PARAMETER")
        fun HabboResponse.writeBlockedTriggers(wiredData: WiredData) {
            // todo
            /*val blockedTriggers = mutableSetOf<Int>()

            writeInt(blockedTriggers.size)
            blockedTriggers.forEach { writeInt(it) }*/

            writeInt(0)
        }

        @Suppress("UNUSED_PARAMETER")
        fun HabboResponse.writeBlockedActions(wiredData: WiredData) {
            // todo
            writeInt(0)
        }

        fun HabboResponse.writeEmptySettings() {
            writeUTF("") // no text box
            writeInt(0) // no options
            writeInt(0) // ???
        }

        fun HabboResponse.writeSettings(textBox: String, settings: List<Int>, exceptedSettingsSize: Int) {
            writeUTF(textBox)
            writeInt(exceptedSettingsSize)

            if (exceptedSettingsSize > 0) {
                @Suppress("ForEachParameterNotUsed")
                if (settings.size != exceptedSettingsSize) (0 until exceptedSettingsSize).forEach { writeInt(0) }
                else settings.forEach { writeInt(it) }
            }

            writeInt(0) // ???
        }
    }
}