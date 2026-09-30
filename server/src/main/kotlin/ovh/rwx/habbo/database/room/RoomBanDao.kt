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

package ovh.rwx.habbo.database.room

import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.database.writebehind.WriteBehindManager

object RoomBanDao {
    fun getBansByRoomId(roomId: Int): List<RoomBanEntry> = db {
        val now = System.currentTimeMillis() / 1000
        query<RoomBanEntryDto>(
            """
            SELECT b.user_id, u.username, b.expire_timestamp
            FROM rooms_bans b
            JOIN users u ON u.id = b.user_id
            WHERE b.room_id = :room_id AND b.expire_timestamp > :now
            """.trimIndent(),
            mapOf("room_id" to roomId, "now" to now)
        ).map { it.toDomain() }
    }

    fun addBan(roomId: Int, userId: Int, expireTimestamp: Long) {
        WriteBehindManager.queue {
            db {
                update(
                    """
                    INSERT INTO rooms_bans (room_id, user_id, expire_timestamp)
                    VALUES (:room_id, :user_id, :expire_timestamp)
                    ON DUPLICATE KEY UPDATE expire_timestamp = VALUES(expire_timestamp)
                    """.trimIndent(),
                    mapOf(
                        "room_id" to roomId,
                        "user_id" to userId,
                        "expire_timestamp" to expireTimestamp
                    )
                )
            }
        }
    }

    fun removeBan(roomId: Int, userId: Int) {
        WriteBehindManager.queue {
            db {
                update(
                    "DELETE FROM rooms_bans WHERE room_id = :room_id AND user_id = :user_id",
                    mapOf("room_id" to roomId, "user_id" to userId)
                )
            }
        }
    }

    fun deleteExpiredBans() {
        WriteBehindManager.queue {
            db {
                val now = System.currentTimeMillis() / 1000
                update(
                    "DELETE FROM rooms_bans WHERE expire_timestamp <= :now",
                    mapOf("now" to now)
                )
            }
        }
    }
}

data class RoomBanEntry(
    val userId: Int,
    val username: String,
    val expireTimestamp: Long
)

data class RoomBanEntryDto(
    val userId: Int,
    val username: String,
    val expireTimestamp: Long
) {
    fun toDomain() = RoomBanEntry(userId, username, expireTimestamp)
}
