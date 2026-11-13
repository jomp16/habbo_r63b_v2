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

package ovh.rwx.habbo.communication.incoming.messenger

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.block.BlockResult

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerBlockUserHandler {
    @Handler(Incoming.MESSENGER_BLOCK_USER)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val userId = habboRequest.readInt()

        // recusa (a si mesmo, desconhecido, já bloqueado): nada é enviado (um resultado 0 mostraria "desbloqueado")
        if (userId == habboSession.userInformation.id) return
        if (UserInformationDao.getUserInformationById(userId) == null) return
        if (habboSession.habboBlocks.block(userId) != BlockResult.BLOCKED) return

        habboSession.sendHabboResponse(Outgoing.MESSENGER_BLOCK_USER_UPDATE, 1, userId)

        // bloquear desfaz a amizade e os pedidos pendentes nos dois sentidos (desbloquear não os restaura)
        if (habboSession.habboMessenger.initialized) {
            habboSession.habboMessenger.removeFriends(listOf(userId))
            habboSession.habboMessenger.removeRequestsWith(userId)
        }
    }
}
