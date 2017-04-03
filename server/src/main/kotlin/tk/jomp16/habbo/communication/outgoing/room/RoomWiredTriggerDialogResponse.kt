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

package tk.jomp16.habbo.communication.outgoing.room

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.InteractionType
import tk.jomp16.habbo.game.item.WiredData
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeBlockedActions
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeEmptyItems
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeEmptySettings
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeItemInfo
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeItems
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeSettings

@Suppress("unused", "UNUSED_PARAMETER")
class RoomWiredTriggerDialogResponse {
    @Response(Outgoing.ROOM_WIRED_TRIGGER_DIALOG)
    fun handle(habboResponse: HabboResponse, roomItem: RoomItem, wiredData: WiredData) {
        habboResponse.apply {
            writeBoolean(false)

            parseWired(roomItem, wiredData)
        }
    }

    private fun HabboResponse.parseWired(roomItem: RoomItem, wiredData: WiredData) {
        when (roomItem.furnishing.interactionType) {
            InteractionType.WIRED_TRIGGER_ENTER_ROOM -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, emptyList(), 0)
                writeInt(7)
                writeBlockedActions()
            }
            InteractionType.WIRED_TRIGGER_SAYS_SOMETHING -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, wiredData.options, 1)
                writeInt(0)
                writeBlockedActions()
            }
            InteractionType.WIRED_TRIGGER_STATE_CHANGED -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(4)
                writeBlockedActions()
            }
            InteractionType.WIRED_TRIGGER_WALKS_ON_FURNI,
            InteractionType.WIRED_TRIGGER_WALKS_OFF_FURNI -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptyItems()
                writeInt(1)
                writeBlockedActions()
            }
            else -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(0)
                writeBlockedActions()
            }
        }
    }
}