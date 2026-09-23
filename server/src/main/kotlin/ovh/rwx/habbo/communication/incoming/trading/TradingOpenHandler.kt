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
import ovh.rwx.habbo.game.room.trading.TradeStartResult
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused")
class TradingOpenHandler {
    @Handler(Incoming.TRADING_OPEN)
    @HandlerR63A(IncomingR63A.TRADING_OPEN)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        val roomUser = habboSession.roomUser ?: return

        val userId = habboRequest.readInt()

        if (userId == habboSession.userInformation.id) {
            return
        }

        val targetUser = room.userManager.entities[userId] as? RoomUser ?: return

        val result = room.tradeManager.startTrade(roomUser, targetUser)

        when (result) {
            is TradeStartResult.AlreadyInTrade -> {
            }

            is TradeStartResult.TargetAlreadyInTrade -> {
            }

            is TradeStartResult.NotInRoom -> {
            }

            is TradeStartResult.Success -> {
            }
        }
    }
}
