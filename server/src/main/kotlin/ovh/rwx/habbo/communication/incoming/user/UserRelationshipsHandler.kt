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

package ovh.rwx.habbo.communication.incoming.user

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.messenger.MessengerDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class UserRelationshipsHandler {
    @Handler(Incoming.USER_RELATIONSHIPS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val userId = habboRequest.readInt()

        // Usuário online: os amigos já estão em memória no messenger da sessão (sem ida ao banco).
        // O stub do server console não é uma amizade real, então fica de fora (o DAO nunca o retornava).
        val onlineMessenger = try {
            HabboServer.habboSessionManager.getHabboSessionById(userId)?.habboMessenger?.takeIf { it.initialized }
        } catch (_: UninitializedPropertyAccessException) {
            null // sessão recém-conectada que ainda não criou o messenger: cai no caminho do banco
        }
        if (onlineMessenger != null) {
            val consoleId = UserInformationDao.serverConsoleUserInformation.id
            val friends = onlineMessenger.friends.values.filter { it.userId != consoleId }
            habboSession.sendHabboResponse(Outgoing.USER_RELATIONSHIPS, userId, friends)
            return
        }

        // Usuário offline: consulta no banco FORA da thread do handler (round-trip ao banco não pode travar o pool de rede/quartos)
        HabboServer.applicationScope.launch(Dispatchers.IO) {
            habboSession.sendHabboResponse(Outgoing.USER_RELATIONSHIPS, userId, MessengerDao.getFriends(userId))
        }
    }
}
