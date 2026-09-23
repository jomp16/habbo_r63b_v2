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

package ovh.rwx.habbo.communication.incoming.trading

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.trading.AddItemResult
import ovh.rwx.habbo.game.user.HabboSession
import kotlin.math.abs

@Suppress("unused")
class TradingAddItemHandler {
    @Handler(Incoming.TRADING_ADD_ITEM)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        val roomUser = habboSession.roomUser ?: return

        val itemId = habboRequest.readInt()


        val trade = room.tradeManager.getTrade(roomUser)
        if (trade == null) {
            habboSession.sendHabboResponse(Outgoing.TRADING_NOT_OPEN)
            return
        }

        val item = habboSession.habboInventory.items[abs(itemId)]
        if (item == null) {
//            habboSession.sendHabboResponse(Outgoing.TRADING_NO_SUCH_ITEM)
            return
        }

        val result = trade.addItem(roomUser, item)

        when (result) {
            is AddItemResult.Success -> {
            }

            is AddItemResult.NotOpen -> {
                habboSession.sendHabboResponse(Outgoing.TRADING_NOT_OPEN)
            }

            is AddItemResult.AlreadyInTrade -> {
            }

            is AddItemResult.ItemNotOwned -> {
//                habboSession.sendHabboResponse(Outgoing.TRADING_NO_SUCH_ITEM)
            }

            is AddItemResult.ItemLocked -> {
            }

            is AddItemResult.NotTradable -> {
//                habboSession.sendHabboResponse(Outgoing.TRADING_NO_SUCH_ITEM)
            }
        }
    }

    @HandlerR63A(IncomingR63A.TRADING_ADD_ITEM)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        val roomUser = habboSession.roomUser ?: return

        val itemId = habboRequest.readInt()


        val trade = room.tradeManager.getTrade(roomUser)
        if (trade == null) {
            habboSession.sendHabboResponse(OutgoingR63A.TRADING_NOT_OPEN)
            return
        }

        val item = habboSession.habboInventory.items[itemId]
        if (item == null) {
            habboSession.sendHabboResponse(OutgoingR63A.TRADING_NO_SUCH_ITEM)
            return
        }

        val result = trade.addItem(roomUser, item)

        when (result) {
            is AddItemResult.Success -> {
            }

            is AddItemResult.NotOpen -> {
                habboSession.sendHabboResponse(OutgoingR63A.TRADING_NOT_OPEN)
            }

            is AddItemResult.AlreadyInTrade -> {
            }

            is AddItemResult.ItemNotOwned -> {
                habboSession.sendHabboResponse(OutgoingR63A.TRADING_NO_SUCH_ITEM)
            }

            is AddItemResult.ItemLocked -> {
            }

            is AddItemResult.NotTradable -> {
                habboSession.sendHabboResponse(OutgoingR63A.TRADING_NO_SUCH_ITEM)
            }
        }
    }
}
