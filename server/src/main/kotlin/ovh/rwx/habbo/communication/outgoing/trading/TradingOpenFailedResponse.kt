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

package ovh.rwx.habbo.communication.outgoing.trading

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused")
class TradingOpenFailedResponse {
    @Response(Outgoing.TRADING_OPEN_FAILED)
    fun response(habboResponse: HabboResponse, failReason: TradingOpenFailedStatus, tradingUser: String) {
        habboResponse.apply {
            writeInt(failReason.code)
            writeUTF(tradingUser)
        }
    }

    enum class TradingOpenFailedStatus(val code: Int) {
        HOTEL_TRADING_NOT_ALLOWED(1),
        YOU_TRADING_OFF(2),
        TARGET_TRADING_NOT_ALLOWED(4),
        ROOM_TRADING_NOT_ALLOWED(6),
        YOU_ALREADY_TRADING(7),
        TARGET_ALREADY_TRADING(8);
    }
}
