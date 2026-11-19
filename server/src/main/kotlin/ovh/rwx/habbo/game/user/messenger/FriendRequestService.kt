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

package ovh.rwx.habbo.game.user.messenger

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerErrorResponse
import ovh.rwx.habbo.database.messenger.MessengerDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.database.user.UserPreferencesDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.block.BlockPolicy

/**
 * Um pedido de amizade pelo nome: do console do hotel (MESSENGER_REQUEST_FRIEND) ou da tela de resultado do Fast
 * Food (o botão ao lado do adversário). Respeita os bloqueios (o solicitante é avisado do motivo) e quem não aceita
 * novos amigos; o destinatário online recebe o pedido na hora.
 */
object FriendRequestService {
    fun request(habboSession: HabboSession, username: String) {
        if (!habboSession.habboMessenger.initialized || username.isBlank()) return
        val userInformation = UserInformationDao.getUserInformationByUsername(username) ?: return
        if (userInformation.id == habboSession.userInformation.id) return
        if (habboSession.habboMessenger.friends.containsKey(userInformation.id)) return

        // bloqueio: o solicitante é avisado do motivo (7 = bloqueado por ele, 8 = bloqueado por você)
        val blockError = BlockPolicy.friendRequestError(habboSession.habboBlocks, userInformation.id)

        if (blockError != null) {
            habboSession.sendHabboResponse(
                Outgoing.MESSENGER_ERROR, 0,
                if (blockError == BlockPolicy.FRIEND_REQUEST_BLOCKED_BY_THEM) {
                    MessengerErrorResponse.MessengerError.BLOCKED_BY_THEM
                } else {
                    MessengerErrorResponse.MessengerError.BLOCKED_BY_YOU
                }
            )
            return
        }

        val userPreferences = UserPreferencesDao.getUserPreferences(userInformation.id)

        if (userPreferences.blockNewFriends) return
        val messengerRequest = MessengerDao.addRequest(habboSession.userInformation.id, userInformation.id)
        val friendHabboSession = HabboServer.habboSessionManager.getHabboSessionById(userInformation.id) ?: return

        if (!friendHabboSession.habboMessenger.initialized) return

        friendHabboSession.habboMessenger.requests[messengerRequest.fromId] = messengerRequest
        friendHabboSession.sendHabboResponse(Outgoing.MESSENGER_REQUEST_FRIEND, messengerRequest)
    }
}
