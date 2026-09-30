/*
 * Copyright (C) 2015-2021 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.database.release

import java.util.*
import ovh.rwx.habbo.database.*

object ReleaseDao {
    fun getReleases(): List<String> = db {
        query<String>("sql/release/select_releases.sql")
    }

    fun getHeaders(): List<ReleaseHeaderInfo> = db {
        query<ReleaseHeaderInfoDto>("sql/release/select_headers.sql").map { it.toDomain() }
    }
}

data class ReleaseHeaderInfoDto(
    val type: String,
    val releaseName: String,
    val name: String,
    val header: Int? = -1,
    val overrideMethod: String?
) {
    fun toDomain() = ReleaseHeaderInfo(
        ReleaseType.valueOf(type.uppercase(Locale.getDefault())),
        releaseName,
        name,
        header ?: -1,
        overrideMethod
    )
}

data class ReleaseHeaderInfo(
        val type: ReleaseType,
        val release: String,
        val name: String,
        val header: Int,
        val overrideMethod: String?
)

enum class ReleaseType {
    INCOMING,
    OUTGOING
}
