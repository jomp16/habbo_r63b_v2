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

package ovh.rwx.habbo.database.gamecenter

import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.writebehind.WriteKey
import ovh.rwx.habbo.game.gamecenter.DailyPlays
import java.time.LocalDate

/** As partidas do dia por usuário e jogo (users_game_daily_plays, V35). */
object GameDailyPlaysDao {
    fun get(userId: Int, gameTypeId: Int): DailyPlays? = db {
        queryOne<DailyPlays>(
            "SELECT `play_date` AS `date`, `plays` FROM `users_game_daily_plays` WHERE `user_id` = :userId AND `game_type_id` = :gameTypeId",
            mapOf("userId" to userId, "gameTypeId" to gameTypeId)
        )
    }

    fun save(userId: Int, gameTypeId: Int, date: LocalDate, plays: Int) {
        WriteBehindManager.queue(WriteKey.user(userId)) {
            db {
                update(
                    "INSERT INTO `users_game_daily_plays` (`user_id`, `game_type_id`, `play_date`, `plays`) " +
                        "VALUES (:userId, :gameTypeId, :date, :plays) " +
                        "ON DUPLICATE KEY UPDATE `play_date` = VALUES(`play_date`), `plays` = VALUES(`plays`)",
                    mapOf("userId" to userId, "gameTypeId" to gameTypeId, "date" to date, "plays" to plays)
                )
            }
        }
    }
}
