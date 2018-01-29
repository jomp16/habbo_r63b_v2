/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.HabboServer

object UserIPDao {
    @Suppress("unused")
    fun getIPsForUser(userId: Int): List<String> {
        return HabboServer.database {
            select(javaClass.getResource("/sql/users/ips/select_ips.sql").readText(),
                    mapOf(
                            "user_id" to userId
                    )
            ) {
                it.string("ip")
            }
        }
    }

    fun addIPForUser(userId: Int, ip: String) {
        HabboServer.database {
            update(javaClass.getResource("/sql/users/ips/insert_ip.sql").readText(),
                    mapOf(
                            "user_id" to userId,
                            "ip" to ip
                    )
            )
        }
    }

    fun containsIPForUser(userId: Int, ip: String): Boolean {
        return HabboServer.database {
            select(javaClass.getResource("/sql/users/ips/select_count_ips.sql").readText(),
                    mapOf(
                            "user_id" to userId,
                            "ip" to ip
                    )
            ) {
                it.boolean("ip_exists")
            }.first()
        }
    }
}