/*
 * Copyright (C) 2015-2021 jomp16 <root@rwx.ovh>
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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerFriendUpdateResponse
import ovh.rwx.habbo.database.messenger.MessengerDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.database.user.UserStatsDao
import ovh.rwx.habbo.game.user.HabboSession

class HabboMessenger(private val habboSession: HabboSession) {
    val friends: MutableMap<Int, MessengerFriend> = mutableMapOf()
    val requests: MutableMap<Int, MessengerRequest> = mutableMapOf()
    var initialized: Boolean = false

    internal fun load() {
        if (!initialized) {
            if (habboSession.hasPermission("acc_server_console")) {
                // server console!
                friends += UserInformationDao.serverConsoleUserInformation.id to MessengerFriend(0, UserInformationDao.serverConsoleUserInformation.id, MessengerRelationship.NONE)
                // stub group
                // friends += -1 to MessengerFriend(-1)
            }

            friends += MessengerDao.getFriends(habboSession.userInformation.id).associateBy { it.userId }
            requests += MessengerDao.getRequests(habboSession.userInformation.id).associateBy { it.fromId }

            // Pré-carrega os dados dos amigos em lote (1 query cada, em paralelo) em vez de 2 idas por amigo,
            // duas vezes, durante a serialização do MESSENGER_INIT.
            val friendIds = friends.values.map { it.userId }.filter { it > 0 }
            if (friendIds.isNotEmpty()) {
                runBlocking(Dispatchers.IO) {
                    launch { UserInformationDao.prefetchUserInformations(friendIds) }
                    launch { UserStatsDao.prefetchLastOnline(friendIds) }
                }
            }

            initialized = true
        }
    }

    /**
     * Remove amizades nos dois sentidos: memória das duas sessões (a do outro, se online), pacotes de atualização da
     * lista e banco. Fluxo único do "remover amigo" e do bloqueio. Devolve os amigos removidos.
     */
    fun removeFriends(userIds: Collection<Int>): List<MessengerFriend> {
        val removed = userIds.mapNotNull { friends.remove(it) }

        if (removed.isEmpty()) return removed

        habboSession.sendResponse(
            Outgoing.MESSENGER_FRIENDS_UPDATE,
            OutgoingR63A.MESSENGER_FRIENDS_UPDATE,
            removed,
            MessengerFriendUpdateResponse.MessengerFriendUpdateMode.REMOVE
        )

        removed.forEach {
            val friendHabboSession = HabboServer.habboSessionManager.getHabboSessionById(it.userId) ?: return@forEach

            if (!friendHabboSession.habboMessenger.initialized) return@forEach
            val messengerFriend = friendHabboSession.habboMessenger.friends.remove(habboSession.userInformation.id)
                ?: return@forEach

            friendHabboSession.sendResponse(
                Outgoing.MESSENGER_FRIENDS_UPDATE,
                OutgoingR63A.MESSENGER_FRIENDS_UPDATE,
                listOf(messengerFriend),
                MessengerFriendUpdateResponse.MessengerFriendUpdateMode.REMOVE
            )
        }

        MessengerDao.removeFriendships(habboSession.userInformation.id, removed.map { it.userId })

        return removed
    }

    /** Descarta os pedidos de amizade pendentes entre os dois, nos dois sentidos (memória das duas sessões e banco). */
    fun removeRequestsWith(userId: Int) {
        val ownId = habboSession.userInformation.id

        requests.remove(userId)
        HabboServer.habboSessionManager.getHabboSessionById(userId)
            ?.takeIf { it.habboMessenger.initialized }
            ?.habboMessenger?.requests?.remove(ownId)

        MessengerDao.removeRequestsBetween(ownId, userId)
    }

    fun notifyFriends() {
        // sem amigos (todo bot, muitos usuários novos): não aloca as 2 listas intermediárias a cada entrada em sala
        if (friends.isEmpty()) return

        friends.values.filter { it.userId > 0 }.filter { it.online && it.habboSession?.habboMessenger?.initialized == true }.forEach {
            if (!it.habboSession!!.isPreShuffle) {
                it.habboSession!!.sendHabboResponse(
                    Outgoing.MESSENGER_FRIENDS_UPDATE,
                    listOf(it.habboSession!!.habboMessenger.friends[habboSession.userInformation.id]),
                    MessengerFriendUpdateResponse.MessengerFriendUpdateMode.UPDATE
                )
            } else {
                it.habboSession!!.sendHabboResponse(
                    OutgoingR63A.MESSENGER_FRIENDS_UPDATE,
                    listOf(it.habboSession!!.habboMessenger.friends[habboSession.userInformation.id]),
                    MessengerFriendUpdateResponse.MessengerFriendUpdateMode.UPDATE
                )
            }
        }
    }
}