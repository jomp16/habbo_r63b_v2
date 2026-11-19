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

package ovh.rwx.fastfood.communication.incoming.game

import ovh.rwx.fastfood.communication.FFHandler
import ovh.rwx.fastfood.communication.incoming.FFIncoming
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.game.user.messenger.FriendRequestService

/**
 * O botão de amizade ao lado de um adversário na tela de resultado (só aparece para quem é do mesmo hotel e ainda
 * não é amigo: FRIENDS_IN_GAME). Vira um pedido de amizade comum no console do hotel.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class GameFriendRequestHandler {
    @FFHandler(FFIncoming.FRIEND_REQUEST)
    fun handle(fastFoodSession: FastFoodSession, habboRequest: HabboRequest) {
        val habboSession = fastFoodSession.habboSession ?: return
        FriendRequestService.request(habboSession, habboRequest.readUTF())
    }
}
