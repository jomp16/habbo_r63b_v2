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

import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.game.room.trading.ConfirmResult
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused")
class TradingConfirmAcceptHandler {
    @Handler(Incoming.TRADING_CONFIRM_ACCEPT)
    @HandlerR63A(IncomingR63A.TRADING_CONFIRM_ACCEPT)
    fun handle(habboSession: HabboSession, habboRequest: ovh.rwx.habbo.communication.HabboRequest) {
        val room = habboSession.currentRoom ?: return
        val roomUser = habboSession.roomUser ?: return

        val trade = room.tradeManager.getTrade(roomUser) ?: return

        val result = trade.confirmAccept(roomUser)

        when (result) {
            is ConfirmResult.Success -> {
                room.tradeManager.removeTrade(trade)
            }

            is ConfirmResult.NotConfirming -> {
            }

            is ConfirmResult.ItemNotFound -> {
            }

            is ConfirmResult.DatabaseError -> {
            }

            is ConfirmResult.WaitingOther -> {
            }
        }
    }
}
