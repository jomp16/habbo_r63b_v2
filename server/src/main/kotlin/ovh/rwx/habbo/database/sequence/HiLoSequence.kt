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

package ovh.rwx.habbo.database.sequence

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe Hi-Lo sequence ID generator.
 * Allocates blocks of IDs from database to generate unique integer IDs in memory at nanosecond speed.
 */
class HiLoSequence(
    val sequenceName: String,
    val tableName: String = sequenceName,
    val idColumn: String = "id",
    val blockSize: Int = 1000
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var currentHi: Int = 0
    private val currentLo = AtomicInteger(blockSize) // Start exhausted to trigger initial fetch
    private val lock = Any()

    fun nextId(): Int {
        while (true) {
            val lo = currentLo.getAndIncrement()
            if (lo < blockSize) {
                return (currentHi * blockSize) + lo
            }
            synchronized(lock) {
                if (currentLo.get() >= blockSize) {
                    currentHi = fetchNextHi()
                    currentLo.set(0)
                    log.info("Reserved Hi-Lo block for '{}': hi={}, range=[{}..{}]",
                        sequenceName, currentHi, currentHi * blockSize, (currentHi + 1) * blockSize - 1)
                }
            }
        }
    }

    private fun fetchNextHi(): Int {
        return dbTransaction {
            val existing = queryOne<Int>(
                "SELECT `next_hi` FROM `sequences` WHERE `sequence_name` = :name FOR UPDATE",
                mapOf("name" to sequenceName)
            )

            if (existing == null) {
                val maxId = try {
                    queryOne<Int>("SELECT COALESCE(MAX(`$idColumn`), 0) FROM `$tableName`") ?: 0
                } catch (e: Exception) {
                    0
                }
                val initialHi = (maxId / blockSize) + 1
                update(
                    "INSERT INTO `sequences` (`sequence_name`, `next_hi`) VALUES (:name, :hi)",
                    mapOf("name" to sequenceName, "hi" to initialHi + 1)
                )
                initialHi
            } else {
                update(
                    "UPDATE `sequences` SET `next_hi` = `next_hi` + 1 WHERE `sequence_name` = :name",
                    mapOf("name" to sequenceName)
                )
                existing
            }
        }
    }
}
