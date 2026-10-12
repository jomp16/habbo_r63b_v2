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
import ovh.rwx.habbo.game.user.HabboSession
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class GameCenterJoinGameHandler {
    @Handler(Incoming.GAME_CENTER_JOIN_GAME)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        sendLoadGame(habboSession, habboRequest.readInt())
    }

    private fun sendLoadGame(habboSession: HabboSession, gameId: Int) {
        val gameCenterManager = HabboServer.habboGame.gameCenterManager
        val game = gameCenterManager.findById(gameId) ?: return

        if (game.swf.isBlank()) {
            // Jogo embutido no Habbo.swf (SnowWar): roda via GAME_2_* na conexão principal;
            // GAME_CENTER_LOAD_GAME (491) é só para jogos externos (BaseJump) e causa loadGameClientError.
            if (game.name == "snowwar") {
                HabboServer.habboGame.snowWarManager.quickJoin(habboSession)
            }
            return
        }

        val ssoToken = UUID.randomUUID().toString()

        habboSession.gameSSOToken = ssoToken
        // todo: achievements?
        habboSession.sendHabboResponse(Outgoing.GAME_CENTER_LOAD_GAME,
                game.gameTypeId,
                habboSession.userInformation.id.toString(), // client id
                gameCenterManager.swfUrl(game), // SWF of game to load
                "best", // quality
                "showAll", // scale mode
                60, // frames per second,
                10, // flash major version min
                0, // flash minor version min
                mapOf(
                        "accessToken" to ssoToken,
                        "habboHost" to (HabboServer.habboConfig.habboHost ?: "127.0.0.1"),
                        "gameServerHost" to (HabboServer.habboConfig.habboHost ?: "127.0.0.1"),
                        "gameServerPort" to (HabboServer.habboConfig.port + 10).toString(),
                        "socketPolicyPort" to (HabboServer.habboConfig.port + 10).toString(),
                        "assetUrl" to (gameCenterManager.gameAssetsUrl(game) ?: gameCenterManager.swfUrl(game))
                )
        )
    }
}
