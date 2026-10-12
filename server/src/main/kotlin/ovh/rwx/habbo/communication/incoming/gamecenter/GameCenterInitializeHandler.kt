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

package ovh.rwx.habbo.communication.incoming.gamecenter

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.gamecenter.Game2AccountStatusData
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class GameCenterInitializeHandler {
    @Handler(Incoming.INITIALIZE_GAME_CENTER)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val manager = HabboServer.habboGame.gameCenterManager
        val requested = habboRequest.readInt()
        // 997 (vazio) drena 0 e nao pede status de jogo nenhum: o layout
        // pede explicitamente via 3548. Chutar um jogo aqui manda status do
        // snowwar e confunde o cliente.
        val game = manager.findById(requested) ?: return

        // ponytail: gamesLeft/promote fixos (-1 = ilimitado); contagem semanal fica pro game loop
        habboSession.sendHabboResponse(
            Outgoing.GAME_2_ACCOUNT_GAME_STATUS,
            Game2AccountStatusData(game.gameTypeId, -1, 1)
        )
    }
}
