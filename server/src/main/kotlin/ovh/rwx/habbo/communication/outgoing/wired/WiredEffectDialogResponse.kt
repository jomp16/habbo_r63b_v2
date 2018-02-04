/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeBlockedTriggers
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeDelay
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeEmptyItems
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeEmptySettings
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeItemInfo
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeItems
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeSettings

@Suppress("unused", "UNUSED_PARAMETER")
class WiredEffectDialogResponse {
    @Response(Outgoing.WIRED_EFFECT_DIALOG)
    fun handle(habboResponse: HabboResponse, roomItem: RoomItem, wiredData: WiredData) {
        habboResponse.apply {
            // todo: add support to all teh effects
            writeBoolean(false)

            parseWired(roomItem, wiredData)
        }
    }

    private fun HabboResponse.parseWired(roomItem: RoomItem, wiredData: WiredData) {
        when (roomItem.furnishing.interactionType) {
            InteractionType.WIRED_ACTION_TOGGLE_STATE,
            InteractionType.WIRED_ACTION_TELEPORT_TO -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(8)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_ACTION_KICK_USER -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, emptyList(), 0)
                writeInt(7)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_ACTION_MOVE_ROTATE -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeSettings("", wiredData.options, 2)
                writeInt(4)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_ACTION_MATCH_TO_SCREENSHOT -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeSettings("", wiredData.options, 3)
                writeInt(3)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_ACTION_CALL_STACK -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(18)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_ACTION_SHOW_MESSAGE -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, emptyList(), 0)
                writeInt(7)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            else -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(0)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
        }
    }
}