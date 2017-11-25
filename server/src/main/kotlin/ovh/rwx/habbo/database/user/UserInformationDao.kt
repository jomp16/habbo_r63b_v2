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

import com.github.andrewoma.kwery.core.Row
import ovh.rwx.habbo.BuildConfig
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.kotlin.localDateTime
import java.time.LocalDateTime

object UserInformationDao {
    private val userInformationsList: MutableMap<Int, UserInformation> = LinkedHashMap()
    val serverConsoleUserInformation: UserInformation = UserInformation(Int.MAX_VALUE, // max int, since Habbo doesn't show figures when id == 0
            "SERVER SCRIPTING CONSOLE", // name
            "", // email, empty
            LocalDateTime.of(2015, 1, 1, 0, 0),
            "${BuildConfig.NAME} scripting console.", // realname
            7, // rank
            0, // credits
            0, // pixels
            0, // vip points
            HabboServer.habboConfig.serverConsoleFigure, // figure
            "M", // gender
            "Version: ${BuildConfig.VERSION}", // motto
            0, // homeroom
            false // vip
    )

    fun getUserInformationById(userId: Int): UserInformation? {
        if (userId == serverConsoleUserInformation.id) return serverConsoleUserInformation

        if (!userInformationsList.containsKey(userId)) {
            val userInformation = HabboServer.database {
                select(javaClass.classLoader.getResource("sql/users/information/select_user_information_from_id.sql").readText(),
                        mapOf(
                                "user_id" to userId
                        )
                ) { getUserInformation(it) }.firstOrNull()
            } ?: return null

            userInformationsList.put(userId, userInformation)
        }

        return userInformationsList[userId]
    }

    fun getUserInformationByAuthTicket(ssoTicket: String): UserInformation? {
        return HabboServer.database {
            select(javaClass.classLoader.getResource("sql/users/information/select_user_information_from_auth_ticket.sql").readText(),
                    mapOf(
                            "ticket" to ssoTicket
                    )
            ) { getUserInformationById(it.int("id")) }.firstOrNull()
        }
    }

    fun getUserInformationByUsername(username: String): UserInformation? {
        val userInformation: UserInformation? = userInformationsList.values.find { it.username == username }

        return if (userInformation != null) userInformation
        else {
            val userInformation1 = HabboServer.database {
                select(javaClass.classLoader.getResource("sql/users/information/select_user_information_from_username.sql").readText(),
                        mapOf(
                                "username" to username
                        )
                ) { getUserInformation(it) }.firstOrNull()
            } ?: return null

            userInformationsList.put(userInformation1.id, userInformation1)

            userInformation1
        }
    }

    fun saveInformation(userInformation: UserInformation, online: Boolean, ip: String, authTicket: String = "") {
        HabboServer.database {
            update(javaClass.classLoader.getResource("sql/users/information/update_user_information.sql").readText(),
                    mapOf(
                            "ticket" to authTicket,
                            "online" to online,
                            "ip_last" to ip,
                            "credits" to userInformation.credits,
                            "pixels" to userInformation.pixels,
                            "vip_points" to userInformation.vipPoints,
                            "figure" to userInformation.figure,
                            "gender" to userInformation.gender,
                            "motto" to userInformation.motto,
                            "home_room" to userInformation.homeRoom,
                            "id" to userInformation.id
                    )
            )
        }
    }

    private fun getUserInformation(row: Row): UserInformation = UserInformation(
            row.int("id"),
            row.string("username"),
            row.string("email"),
            row.localDateTime("account_created"),
            row.string("realname"),
            row.int("rank"),
            row.int("credits"),
            row.int("pixels"),
            row.int("vip_points"),
            row.string("figure"),
            row.string("gender"),
            row.string("motto"),
            row.int("home_room"),
            row.boolean("vip")
    )
}