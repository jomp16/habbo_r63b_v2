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

package ovh.rwx.habbo.database.user

import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.writebehind.WriteKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Uma linha por usuário por dia em `users_logins` (a primeira entrada do dia). Base do login diário
 * ([ovh.rwx.habbo.game.user.LoginStreak]).
 */
object UserLoginsDao {
    /**
     * Data da última entrada por usuário. O INSERT é write-behind, então uma reentrada logo em seguida ainda não o
     * encontraria no banco e contaria o dia duas vezes: o cache é atualizado de forma síncrona em [recordLogin].
     */
    private val lastLoginCache = ConcurrentHashMap<Int, LocalDate>()

    /** Data da última entrada registrada, ou null se o usuário nunca teve uma (contas anteriores à tabela). */
    fun lastLoginDate(userId: Int): LocalDate? =
        lastLoginCache[userId] ?: db {
            queryOne<LocalDateTime>(
                "SELECT MAX(`date`) FROM `users_logins` WHERE `user_id` = :user_id",
                mapOf("user_id" to userId)
            )
        }?.toLocalDate()

    /** Registra a entrada de [now]; no máximo uma linha por dia. */
    fun recordLogin(userId: Int, now: LocalDateTime) {
        val today = now.toLocalDate()
        if (lastLoginCache.put(userId, today) == today) return

        WriteBehindManager.queue(WriteKey.user(userId)) {
            db {
                update(
                    "INSERT INTO `users_logins` (`user_id`, `date`) SELECT :user_id, :now FROM DUAL " +
                        "WHERE NOT EXISTS (SELECT 1 FROM `users_logins` WHERE `user_id` = :user_id " +
                        "AND `date` >= :day_start AND `date` < :day_end)",
                    mapOf(
                        "user_id" to userId,
                        "now" to now,
                        "day_start" to today.atStartOfDay(),
                        "day_end" to today.plusDays(1).atStartOfDay()
                    )
                )
            }
        }
    }
}
