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
 * Espelho de _SafeStr_3253 (_-s15). Semântica real do wire:
 *   confirm=false = ACCEPT (client state 1→2, countdown)
 *   confirm=true  = CONFIRM (server executa o depósito)
 */
@Suppress("unused", "UNUSED_PARAMETER")
class WiredTradeConfirmHandler {
    @Handler(Incoming.WIRED_TRADE_CONFIRM)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        val confirm = habboRequest.readBoolean()

        HabboServer.habboGame.chestManager.wiredTradeConfirm(habboSession, room, confirm)
    }
}
