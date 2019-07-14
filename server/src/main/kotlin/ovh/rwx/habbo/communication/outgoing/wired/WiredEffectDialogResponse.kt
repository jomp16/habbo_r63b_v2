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
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeBlockedTriggers
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeDelay
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeEmptyItems
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeEmptySettings
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeItemInfo
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeItems
import ovh.rwx.habbo.game.item.wired.WiredItem.Companion.writeSettings
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType

@Suppress("unused", "UNUSED_PARAMETER")
class WiredEffectDialogResponse {
    @Response(Outgoing.WIRED_EFFECT_DIALOG)
    fun response(habboResponse: HabboResponse, roomItem: RoomItem, wiredData: WiredData) {
        habboResponse.apply {
            // todo: add support to all teh effects
            writeBoolean(false)

            parseWired(roomItem, wiredData)
        }
    }

    private fun HabboResponse.parseWired(roomItem: RoomItem, wiredData: WiredData) {
        when (roomItem.furnishing.interactionType) {
            InteractionType.WIRED_EFFECT_TOGGLE_STATE -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(WiredEffectType.TOGGLE_STATE.code)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_EFFECT_TELEPORT_TO -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(WiredEffectType.TELEPORT.code)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_EFFECT_KICK_USER -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, emptyList(), 0)
                writeInt(WiredEffectType.KICK_USER.code)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_EFFECT_MOVE_ROTATE -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeSettings("", wiredData.options, 2)
                writeInt(WiredEffectType.MOVE_ROTATE.code)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_EFFECT_MATCH_TO_SCREENSHOT -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeSettings("", wiredData.options, 3)
                writeInt(WiredEffectType.MATCH_SSHOT.code)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_EFFECT_CALL_STACK -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(WiredEffectType.CALL_STACKS.code)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            InteractionType.WIRED_EFFECT_SHOW_MESSAGE -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, emptyList(), 0)
                writeInt(WiredEffectType.SHOW_MESSAGE.code)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
            else -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(WiredEffectType.TOGGLE_STATE.code)
                writeDelay(wiredData)
                writeBlockedTriggers(wiredData)
            }
        }
    }
}