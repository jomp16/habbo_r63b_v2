/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerChatData
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerInitData
import ovh.rwx.habbo.database.messenger.MessengerDao
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerInitializeHandler {
    @Handler(Incoming.MESSENGER_INIT)
    @HandlerR63A(IncomingR63A.MESSENGER_INIT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        var attempts = 0
        while (!habboSession.habboMessenger.initialized && attempts < 10) {
            Thread.sleep(500)
            attempts++
        }
        if (!habboSession.habboMessenger.initialized) return

        // R63A envia o init combinado (pré-shuffle o header estático 12 carrega limites + amigos + requests)
        habboSession.sendResponse(
            Outgoing.MESSENGER_INIT,
            OutgoingR63A.MESSENGER_INIT,
            MessengerInitData(
                maxFriends = 500,
                maxFriendsHC = 3000,
                friends = habboSession.habboMessenger.friends.values,
            )
        )

        // A partir de 2014-12-10 os amigos saem do init e vão num pacote separado (BuddyList);
        // antes disso a lista já vai embutida no próprio init
        if (habboSession.isVersionAtLeast(2014, 12, 10)) {
            habboSession.sendHabboResponse(Outgoing.MESSENGER_FRIENDS, habboSession.habboMessenger.friends.values)
        }

        MessengerDao.getOfflineMessages(habboSession.userInformation.id).forEach {
            habboSession.sendResponse(
                Outgoing.MESSENGER_CHAT,
                OutgoingR63A.MESSENGER_CHAT,
                MessengerChatData(
                    id = it.first,
                    message = it.second,
                    diffTimestamp = it.third,
                )
            )
        }

        habboSession.habboMessenger.notifyFriends()
    }
}
