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

package ovh.rwx.habbo.database.currency

import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.game.currency.ActivityPointTypeInfo
import java.time.LocalDateTime

object ActivityPointTypeDao {
    fun getAll(): List<ActivityPointTypeInfo> = db {
        query<ActivityPointTypeDto>("sql/currency/select_activity_point_types.sql").map { it.toDomain() }
    }
}

data class ActivityPointTypeDto(
    val code: Int,
    val name: String,
    val availableFrom: LocalDateTime? = null,
    val availableUntil: LocalDateTime? = null,
    val enabled: Boolean = true
) {
    fun toDomain() = ActivityPointTypeInfo(code, name, availableFrom, availableUntil, enabled)
}
