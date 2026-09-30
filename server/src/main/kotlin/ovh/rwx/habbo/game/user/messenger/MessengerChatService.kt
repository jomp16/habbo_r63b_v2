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

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerChatData
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerChatErrorResponse
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.user.HabboSession

object MessengerChatService {
    private val log = LoggerFactory.getLogger(javaClass)

    fun processChat(habboSession: HabboSession, targetUserId: Int, rawMessage: String) {
        if (!habboSession.habboMessenger.initialized) return
        val message = rawMessage.trim()
        if (message.isBlank()) return

        // 1. Verificação do Server Console
        val isConsoleTarget = targetUserId == UserInformationDao.serverConsoleUserInformation.id
        if (isConsoleTarget) {
            if (!habboSession.hasPermission("acc_server_console")) {
                sendNotFriendsError(habboSession, targetUserId, message)
                return
            }

            MessengerServerConsoleService.execute(habboSession, message) { responseMessage ->
                habboSession.sendResponse(
                    Outgoing.MESSENGER_CHAT,
                    OutgoingR63A.MESSENGER_CHAT,
                    MessengerChatData(
                        id = targetUserId,
                        message = responseMessage,
                        diffTimestamp = 0,
                        userId = UserInformationDao.serverConsoleUserInformation.id,
                        username = UserInformationDao.serverConsoleUserInformation.username,
                        figure = UserInformationDao.serverConsoleUserInformation.figure,
                    )
                )
            }
            return
        }

        // 2. Validação de Amizade
        if (!habboSession.habboMessenger.friends.containsKey(targetUserId)) {
            sendNotFriendsError(habboSession, targetUserId, message)
            return
        }

        // 3. Destinos Especiais / Mensagens Negativas (ex: eco de canal -1)
        if (targetUserId < 0) {
            if (targetUserId == -1) {
                habboSession.sendResponse(
                    Outgoing.MESSENGER_CHAT,
                    OutgoingR63A.MESSENGER_CHAT,
                    MessengerChatData(
                        id = targetUserId,
                        message = message,
                        diffTimestamp = 0,
                        userId = habboSession.userInformation.id,
                        username = habboSession.userInformation.username,
                        figure = habboSession.userInformation.figure,
                    )
                )
            }
            return
        }

        // 4. Envio para Amigo (Online ou Offline)
        val messengerBuddy = habboSession.habboMessenger.friends[targetUserId] ?: return
        val friendSession = messengerBuddy.habboSession

        if (!messengerBuddy.online || friendSession == null || !friendSession.authenticated) {
            MessengerOfflineService.sendOfflineMessage(habboSession.userInformation.id, targetUserId, message)
            return
        }

        friendSession.sendResponse(
            Outgoing.MESSENGER_CHAT,
            OutgoingR63A.MESSENGER_CHAT,
            MessengerChatData(
                id = habboSession.userInformation.id,
                message = message,
                diffTimestamp = 0,
                userId = habboSession.userInformation.id,
                username = habboSession.userInformation.username,
                figure = habboSession.userInformation.figure,
            )
        )
    }

    private fun sendNotFriendsError(habboSession: HabboSession, userId: Int, message: String) {
        habboSession.sendResponse(
            Outgoing.MESSENGER_CHAT_ERROR,
            OutgoingR63A.MESSENGER_CHAT_ERROR,
            MessengerChatErrorResponse.MessengerChatError.NOT_FRIENDS,
            userId,
            message
        )
    }
}
