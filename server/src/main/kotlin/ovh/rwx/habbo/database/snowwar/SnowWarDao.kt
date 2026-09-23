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

package ovh.rwx.habbo.database.snowwar

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.gamecenter.Game2ArenaFuseObjectData
import ovh.rwx.habbo.game.snowwar.SnowWarArenaData
import ovh.rwx.habbo.game.snowwar.SnowWarLeaderboardUserEntry
import ovh.rwx.habbo.game.snowwar.SnowWarPlayerStats

object SnowWarDao {
    fun getStats(userId: Int): SnowWarPlayerStats? = HabboServer.database {
        select(
            javaClass.classLoader.getResource("sql/snowwar/select_user_snowwar_stats.sql")!!.readText(),
            mapOf("user_id" to userId)
        ) {
            SnowWarPlayerStats(
                it.int("user_id"),
                it.int("total_score"),
                it.int("weekly_score"),
                it.int("games_played"),
                it.int("weekly_games_played"),
                it.int("skill_level"),
                it.int("last_week_number"),
                it.int("last_year")
            )
        }.firstOrNull()
    }

    fun saveStats(stats: SnowWarPlayerStats) {
        HabboServer.database {
            update(
                javaClass.classLoader.getResource("sql/snowwar/insert_user_snowwar_stats.sql")!!.readText(),
                mapOf(
                    "user_id" to stats.userId,
                    "total_score" to stats.totalScore,
                    "weekly_score" to stats.weeklyScore,
                    "games_played" to stats.gamesPlayed,
                    "weekly_games_played" to stats.weeklyGamesPlayed,
                    "skill_level" to stats.skillLevel,
                    "last_week_number" to stats.lastWeekNumber,
                    "last_year" to stats.lastYear
                )
            )
        }
    }

    fun getTotalLeaderboard(limit: Int, offset: Int): List<SnowWarLeaderboardUserEntry> = HabboServer.database {
        var currentRank = offset + 1
        select(
            javaClass.classLoader.getResource("sql/snowwar/select_total_leaderboard.sql")!!.readText(),
            mapOf("limit" to limit, "offset" to offset)
        ) {
            SnowWarLeaderboardUserEntry(
                it.int("user_id"),
                it.int("score"),
                currentRank++,
                it.string("username"),
                it.string("figure"),
                it.string("gender")
            )
        }
    }

    fun getWeeklyLeaderboard(year: Int, week: Int, limit: Int, offset: Int): List<SnowWarLeaderboardUserEntry> =
        HabboServer.database {
            var currentRank = offset + 1
            select(
                javaClass.classLoader.getResource("sql/snowwar/select_weekly_leaderboard.sql")!!.readText(),
                mapOf("year" to year, "week" to week, "limit" to limit, "offset" to offset)
            ) {
                SnowWarLeaderboardUserEntry(
                    it.int("user_id"),
                    it.int("score"),
                    currentRank++,
                    it.string("username"),
                    it.string("figure"),
                    it.string("gender")
                )
            }
        }

    fun getArenas(): List<SnowWarArenaData> = HabboServer.database {
        val objectMapper = jacksonObjectMapper()

        val arenaItems = select(
            javaClass.classLoader.getResource("sql/snowwar/select_snowwar_arena_items.sql")!!.readText()
        ) {
            val arenaId = it.int("arena_id")
            val fuseObj = Game2ArenaFuseObjectData(
                name = it.string("item_name"),
                id = it.int("id"),
                x = it.int("x"),
                y = it.int("y"),
                z = it.int("z"),
                direction = it.int("rot"),
                extraParams = objectMapper.convertValue(objectMapper.readTree(it.string("extra_params"))),
            )
            arenaId to fuseObj
        }.groupBy({ it.first }, { it.second })

        select(
            javaClass.classLoader.getResource("sql/snowwar/select_snowwar_arenas.sql")!!.readText()
        ) {
            val id = it.int("id")
            val name = it.string("name")
            val width = it.int("width")
            val height = it.int("height")
            val raw = it.string("heightmap")
            val rawHeightmap = if (raw.contains("\r") || raw.contains("\n")) {
                raw.replace("\r\n", "\r").replace("\n", "\r")
            } else if (width > 0 && raw.length >= width * height) {
                raw.chunked(width).take(height).joinToString("\r")
            } else {
                raw
            }
            val blueSpawnsRaw = it.string("blue_spawns")
            val redSpawnsRaw = it.string("red_spawns")

            val blueSpawns = blueSpawnsRaw.split(";").mapNotNull { coord ->
                val parts = coord.split(",")
                if (parts.size >= 2) {
                    val x = parts[0].trim().toIntOrNull()
                    val y = parts[1].trim().toIntOrNull()
                    if (x != null && y != null) x to y else null
                } else null
            }

            val redSpawns = redSpawnsRaw.split(";").mapNotNull { coord ->
                val parts = coord.split(",")
                if (parts.size >= 2) {
                    val x = parts[0].trim().toIntOrNull()
                    val y = parts[1].trim().toIntOrNull()
                    if (x != null && y != null) x to y else null
                } else null
            }

            SnowWarArenaData(
                id = id,
                name = name,
                width = width,
                height = height,
                heightMap = rawHeightmap,
                fuseObjects = arenaItems[id] ?: emptyList(),
                blueSpawns = blueSpawns,
                redSpawns = redSpawns
            )
        }
    }
}
