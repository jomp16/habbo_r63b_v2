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
import ovh.rwx.habbo.communication.outgoing.gamecenter.Game2ArenaFuseObjectData
import ovh.rwx.habbo.game.snowwar.SnowWarArenaData
import ovh.rwx.habbo.game.snowwar.SnowWarLeaderboardUserEntry
import ovh.rwx.habbo.game.snowwar.SnowWarPlayerStats
import ovh.rwx.habbo.database.*

object SnowWarDao {
    fun getStats(userId: Int): SnowWarPlayerStats? = db {
        queryOne<SnowWarPlayerStats>(
            "sql/snowwar/select_user_snowwar_stats.sql",
            mapOf("user_id" to userId)
        )
    }

    fun saveStats(stats: SnowWarPlayerStats) {
        db {
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

    fun getTotalLeaderboard(limit: Int, offset: Int): List<SnowWarLeaderboardUserEntry> = db {
        var currentRank = offset + 1
        query<SnowWarLeaderboardRowDto>(
            javaClass.classLoader.getResource("sql/snowwar/select_total_leaderboard.sql")!!.readText(),
            mapOf("limit" to limit, "offset" to offset)
        ).map {
            SnowWarLeaderboardUserEntry(
                it.userId,
                it.score,
                currentRank++,
                it.username,
                it.figure,
                it.gender
            )
        }
    }

    fun getWeeklyLeaderboard(year: Int, week: Int, limit: Int, offset: Int): List<SnowWarLeaderboardUserEntry> =
        db {
            var currentRank = offset + 1
            query<SnowWarLeaderboardRowDto>(
                javaClass.classLoader.getResource("sql/snowwar/select_weekly_leaderboard.sql")!!.readText(),
                mapOf("year" to year, "week" to week, "limit" to limit, "offset" to offset)
            ).map {
                SnowWarLeaderboardUserEntry(
                    it.userId,
                    it.score,
                    currentRank++,
                    it.username,
                    it.figure,
                    it.gender
                )
            }
        }

    fun getArenas(): List<SnowWarArenaData> = db {
        val objectMapper = jacksonObjectMapper()

        val arenaItems = query<SnowWarArenaItemDto>(
            javaClass.classLoader.getResource("sql/snowwar/select_snowwar_arena_items.sql")!!.readText()
        ).groupBy({ it.arenaId }, { it.toFuseObject(objectMapper) })

        query<SnowWarArenaRowDto>(
            javaClass.classLoader.getResource("sql/snowwar/select_snowwar_arenas.sql")!!.readText()
        ).map { it.toDomain(arenaItems) }
    }
}

data class SnowWarLeaderboardRowDto(
    val userId: Int,
    val score: Int,
    val username: String,
    val figure: String,
    val gender: String
)

data class SnowWarArenaItemDto(
    val arenaId: Int,
    val itemName: String,
    val id: Int,
    val x: Int,
    val y: Int,
    val z: Int,
    val rot: Int,
    val extraParams: String
) {
    fun toFuseObject(objectMapper: com.fasterxml.jackson.databind.ObjectMapper) = Game2ArenaFuseObjectData(
        name = itemName,
        id = id,
        x = x,
        y = y,
        z = z,
        direction = rot,
        extraParams = objectMapper.convertValue(objectMapper.readTree(extraParams)),
    )
}

data class SnowWarArenaRowDto(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val heightmap: String,
    val blueSpawns: String,
    val redSpawns: String
) {
    fun toDomain(arenaItems: Map<Int, List<Game2ArenaFuseObjectData>>): SnowWarArenaData {
        val raw = heightmap
        val rawHeightmap = if (raw.contains("\r") || raw.contains("\n")) {
            raw.replace("\r\n", "\r").replace("\n", "\r")
        } else if (width > 0 && raw.length >= width * height) {
            raw.chunked(width).take(height).joinToString("\r")
        } else {
            raw
        }
        val blue = blueSpawns.split(";").mapNotNull { coord ->
            val parts = coord.split(",")
            if (parts.size >= 2) {
                val x = parts[0].trim().toIntOrNull()
                val y = parts[1].trim().toIntOrNull()
                if (x != null && y != null) x to y else null
            } else null
        }
        val red = redSpawns.split(";").mapNotNull { coord ->
            val parts = coord.split(",")
            if (parts.size >= 2) {
                val x = parts[0].trim().toIntOrNull()
                val y = parts[1].trim().toIntOrNull()
                if (x != null && y != null) x to y else null
            } else null
        }
        return SnowWarArenaData(
            id = id,
            name = name,
            width = width,
            height = height,
            heightMap = rawHeightmap,
            fuseObjects = arenaItems[id] ?: emptyList(),
            blueSpawns = blue,
            redSpawns = red
        )
    }
}
