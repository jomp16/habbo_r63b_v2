/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.communication.incoming.gamecenter

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class GameCenterJoinGameHandler {
    @Handler(Incoming.GAME_CENTER_JOIN_GAME)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return
        val gameId = habboRequest.readInt()

        if (gameId != 3) return
        val ssoToken = UUID.randomUUID().toString()

        habboSession.gameSSOToken = ssoToken
        // BaseJump
        // todo: achievements?
        habboSession.sendHabboResponse(Outgoing.GAME_CENTER_LOAD_GAME,
                gameId,
                habboSession.userInformation.id.toString(), // client id
                "https://swf.hfinch.tk/games/gamecenter_basejump/BaseJump.swf", // SWF of game to load
                "best", // quality
                "showAll", // scale mode
                60, // frames per second,
                10, // flash major version min
                0, // flash minor version min
                mapOf(
                        "accessToken" to ssoToken,
                        "habboHost" to "jomp16.ddns.net",
                        "gameServerHost" to "jomp16.ddns.net",
                        "gameServerPort" to (HabboServer.habboConfig.port + 1).toString(),
                        "socketPolicyPort" to (HabboServer.habboConfig.port + 1).toString(),
                        "assetUrl" to "https://swf.hfinch.tk/games/gamecenter_basejump/BasicAssets.swf"
                )
        )
    }
}