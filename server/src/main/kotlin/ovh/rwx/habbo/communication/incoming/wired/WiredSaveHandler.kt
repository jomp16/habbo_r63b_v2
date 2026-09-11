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

package ovh.rwx.habbo.communication.incoming.wired

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class WiredSaveHandler {
    @Handler(
        Incoming.WIRED_SAVE_TRIGGER, Incoming.WIRED_SAVE_EFFECT, Incoming.WIRED_SAVE_CONDITION,
        Incoming.WIRED_SAVE_ADDON, Incoming.WIRED_SAVE_SELECTOR, Incoming.WIRED_SAVE_VARIABLES
    )
    @HandlerR63A(IncomingR63A.WIRED_SAVE_TRIGGER, IncomingR63A.WIRED_SAVE_EFFECT, IncomingR63A.WIRED_SAVE_CONDITION)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return
        val itemId = habboRequest.readInt()
        val roomItem = habboSession.currentRoom!!.itemManager.items[itemId] ?: return

        val isModernWired = habboSession.habboVersion.isVersionAtLeast(2023, 6, 30)
        if (habboSession.currentRoom!!.itemManager.wiredHandler.saveWired(
                roomItem,
                habboRequest,
                habboAir = isModernWired
            )
        ) {
            if (habboSession.release != "R63A") {
                habboSession.sendHabboResponse(Outgoing.WIRED_SAVED)
            }
        }
    }
}