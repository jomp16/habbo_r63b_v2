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

package ovh.rwx.habbo.game.gamecenter

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.gamecenter.GameCenterDao
import java.util.concurrent.ConcurrentHashMap

class GameCenterManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val games = ConcurrentHashMap<Int, GameCenterGame>()

    fun load() {
        log.info("Loading game center games...")

        games.clear()
        GameCenterDao.getGames().forEach { games[it.gameTypeId] = it }

        log.info("Loaded {} game center games!", games.size)
    }

    fun all(): List<GameCenterGame> = games.values.sortedBy { it.gameTypeId }

    fun findById(gameTypeId: Int): GameCenterGame? = games[gameTypeId]

    fun assetsBaseUrl(game: GameCenterGame): String =
        "${HabboServer.habboConfig.cImagesUrl?.trimEnd('/')}/${game.dir}"

    fun swfUrl(game: GameCenterGame): String =
        "${HabboServer.habboConfig.gamesUrl?.trimEnd('/')}/${game.dir}/${game.swf}"

    fun gameAssetsUrl(game: GameCenterGame): String? =
        game.assetsSwf?.let { "${HabboServer.habboConfig.gamesUrl?.trimEnd('/')}/${game.dir}/$it" }
}
