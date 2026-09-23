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

package ovh.rwx.habbo.game.snowwar

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.snowwar.SnowWarDao
import ovh.rwx.habbo.game.snowwar.enums.SnowWarFieldType
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object SnowWarArenaMaps {
    private val log = LoggerFactory.getLogger(SnowWarArenaMaps::class.java)
    val arenas = ConcurrentHashMap<Int, SnowWarArenaData>()

    fun loadArenas() {
        try {
            val list = SnowWarDao.getArenas()
            arenas.clear()
            for (arena in list) {
                arenas[arena.id] = arena
            }
            log.info("Loaded ${arenas.size} SnowWar arenas from database.")
        } catch (e: Exception) {
            log.error("Failed to load SnowWar arenas from database: ${e.message}", e)
        }
    }

    fun getArena(id: Int): SnowWarArenaData? = arenas[id]

    fun getArena(fieldType: SnowWarFieldType): SnowWarArenaData? = arenas[fieldType.id]

    fun getRandomArena(): SnowWarArenaData? {
        val list = arenas.values.toList()
        return if (list.isNotEmpty()) list[Random.nextInt(list.size)] else null
    }
}
