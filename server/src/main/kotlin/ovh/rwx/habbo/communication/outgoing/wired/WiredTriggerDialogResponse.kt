/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.wired

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeBlockedActions
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeEmptyItems
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeEmptySettings
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeItemInfo
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeItems
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeSettings
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType

@Suppress("unused", "UNUSED_PARAMETER")
class WiredTriggerDialogResponse {
    @Response(Outgoing.WIRED_TRIGGER_DIALOG)
    fun response(habboResponse: HabboResponse, roomItem: RoomItem, wiredData: WiredData) {
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
                writeInt(WiredTriggerType.ENTER_ROOM.code)
                writeBlockedActions(wiredData)
            }
            InteractionType.WIRED_TRIGGER_SAYS_SOMETHING -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, wiredData.options, 1)
                writeInt(WiredTriggerType.SAY_SOMETHING.code)
                writeBlockedActions(wiredData)
            }
            InteractionType.WIRED_TRIGGER_STATE_CHANGED -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(WiredTriggerType.STATE_CHANGED.code)
                writeBlockedActions(wiredData)
            }
            InteractionType.WIRED_TRIGGER_WALKS_ON_FURNI -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(WiredTriggerType.WALKS_ON_FURNI.code)
                writeBlockedActions(wiredData)
            }
            InteractionType.WIRED_TRIGGER_WALKS_OFF_FURNI -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(WiredTriggerType.WALKS_OFF_FURNI.code)
                writeBlockedActions(wiredData)
            }
            InteractionType.WIRED_TRIGGER_PERIODICALLY -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, wiredData.options, 1)
                writeInt(WiredTriggerType.PERIODICALLY.code)
                writeBlockedActions(wiredData)
            }
            InteractionType.WIRED_TRIGGER_PERIODICALLY_LONG -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, wiredData.options, 1)
                writeInt(WiredTriggerType.PERIODICALLY_LONG.code)
                writeBlockedActions(wiredData)
            }
            else -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(WiredTriggerType.SAY_SOMETHING.code)
                writeBlockedActions(wiredData)
            }
        }
    }
}