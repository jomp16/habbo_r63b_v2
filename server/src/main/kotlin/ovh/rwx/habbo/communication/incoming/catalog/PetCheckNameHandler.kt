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

package ovh.rwx.habbo.communication.incoming.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class PetCheckNameHandler {
    private val nameRegex = Regex("^[a-zA-Z0-9 ]+$")

    @Handler(Incoming.PET_CHECK_NAME)
    @HandlerR63A(IncomingR63A.PET_CHECK_NAME)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val name = habboRequest.readUTF()
        // readInt unknown, readInt petType — not needed for validation
        val result = validateName(name)

        if (habboSession.release == "R63A") {
            habboSession.sendHabboResponse(OutgoingR63A.PET_CHECK_NAME, result, name)
        } else {
            habboSession.sendHabboResponse(Outgoing.PET_CHECK_NAME, result, name)
        }
    }

    private fun validateName(name: String): Int {
        val config = HabboServer.habboConfig.petConfig

        return when {
            name.length < config.nameMinLength -> 1
            name.length > config.nameMaxLength -> 2
            !nameRegex.matches(name) -> 3
            else -> 0
        }
    }
}
