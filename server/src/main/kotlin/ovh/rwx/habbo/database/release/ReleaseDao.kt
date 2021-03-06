/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.HabboServer

object ReleaseDao {
    fun getReleases(): List<String> = HabboServer.database {
        select(javaClass.classLoader.getResource("sql/release/select_releases.sql").readText()) {
            it.string("release_name")
        }
    }

    fun getHeaders(): List<ReleaseHeaderInfo> = HabboServer.database {
        select(javaClass.classLoader.getResource("sql/release/select_headers.sql").readText()) {
            ReleaseHeaderInfo(
                    ReleaseType.valueOf(it.string("type").toUpperCase()),
                    it.string("release_name"),
                    it.string("name"),
                    it.intOrNull("header") ?: -1,
                    it.stringOrNull("override_method")
            )
        }
    }
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