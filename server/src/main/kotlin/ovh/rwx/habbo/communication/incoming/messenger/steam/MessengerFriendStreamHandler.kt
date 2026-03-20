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

package ovh.rwx.habbo.communication.incoming.messenger.steam

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.messenger.stream.MessengerFriendStream
import ovh.rwx.habbo.game.user.messenger.stream.StreamLinkTarget
import ovh.rwx.habbo.game.user.messenger.stream.StreamType

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerFriendStreamHandler {
    @HandlerR63A(IncomingR63A.MESSENGER_LOAD_STREAM)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.habboMessenger.initialized) return

        // todo:
        val streamEvents = mutableListOf<MessengerFriendStream>()

        // Criando a mensagem do Hotel Staff
        streamEvents.add(
            MessengerFriendStream(
                id = 1,                             // ID único no Feed
                type = StreamType.HOTEL_ALERT,      // Tipo 4
                accountId = "-1",                   // ID do Staff (ou 0)
                userName = "Gerente",               // Nome que aparece no título
                imageFilePath = "hr-115-42.hd-190-1.ch-215-62.lg-285-64.sh-300-64", // Look do Frank/Staff
                userGender = "M",
                minutesAgo = 0,                     // "Agora mesmo"
                likesCount = 0,
                linkTargetType = StreamLinkTarget.URL_LINK,
                canLike = false,                    // Desativa botão curtir
                data1 = "Parabéns! Você acaba de descobrir uma funcionalidade secreta: o Friend Stream!",
                data2 = "https://habbo.rwx.ovh"     // Link que o botão "Saiba mais" ou similar usará
            )
        )

        habboSession.sendHabboResponse(OutgoingR63A.MESSENGER_FRIEND_STREAM, streamEvents)
    }
}