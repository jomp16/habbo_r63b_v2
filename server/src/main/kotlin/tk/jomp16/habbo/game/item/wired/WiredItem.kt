/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.game.item.wired

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.game.item.WiredData
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room

abstract class WiredItem(protected val room: Room, val roomItem: RoomItem) {
    abstract fun setData(habboRequest: HabboRequest): Boolean

    companion object {
        fun HabboResponse.writeEmptyItems() {
            writeInt(0) // selectable items
            writeInt(0) // items
        }

        fun HabboResponse.writeItems(wiredData: WiredData) {
            writeInt(5) // selectable items
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

        fun HabboResponse.writeBlockedTriggers() {
            // todo
            /*val blockedTriggers = mutableSetOf<Int>()

            writeInt(blockedTriggers.size)
            blockedTriggers.forEach { writeInt(it) }*/

            writeInt(0)
        }

        fun HabboResponse.writeBlockedActions() {
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
                if (settings.size != exceptedSettingsSize) (0..exceptedSettingsSize - 1).forEach { writeInt(0) }
                else settings.forEach { writeInt(it) }
            }

            writeInt(0) // ???
        }
    }
}