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

package tk.jomp16.utils.plugin.game.fastfood.impl

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.gamemanager.api.GameHandler
import tk.jomp16.habbo.game.gamemanager.data.GameData
import tk.jomp16.habbo.game.gamemanager.data.JoinGameData
import tk.jomp16.habbo.game.user.HabboSession
import java.util.*

class FastFoodGameHandler : GameHandler {
    override val gameData: GameData = GameData(
            3,
            "basejump",
            "68bbd2",
            "",
            "gamecenter_basejump"
    )

    override fun createToken(habboSession: HabboSession): String = UUID.randomUUID().toString()

    override fun joinGame(habboSession: HabboSession): JoinGameData {
        return JoinGameData(
                "BaseJump.swf",
                "best",
                "showAll",
                60,
                10,
                0,
                mapOf(
                        "accessToken" to createToken(habboSession),
                        "habboHost" to "jomp16.ddns.net", // todo: remove url
                        "gameServerHost" to "jomp16.ddns.net", // todo: remove url
                        "gameServerPort" to (HabboServer.habboConfig.port + gameData.id).toString(), // todo: config port
                        "socketPolicyPort" to HabboServer.habboConfig.port.toString(),
                        "assetUrl" to "${HabboServer.habboConfig.gamesUrl}/${gameData.imagesPath}/BasicAssets.swf"
                )
        )
    }
}