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

package ovh.rwx.habbo.communication.incoming.wiredtrade

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Espelho de _SafeStr_2833 (_-G1e): o client adiciona/remove itens da seleção
 * da Wired Trade de depósito no baú.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class WiredTradeAddDeleteItemsHandler {
    @Handler(Incoming.WIRED_TRADE_ADD_DELETE_ITEMS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        val remove = habboRequest.readBoolean()
        val count = habboRequest.readInt()
        val itemIds = (0 until count.coerceIn(0, 10000)).map { habboRequest.readInt() * -1 }

        HabboServer.habboGame.chestManager.wiredTradeAddDeleteItems(habboSession, room, remove, itemIds)
    }
}
