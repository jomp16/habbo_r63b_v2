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

import ovh.rwx.habbo.database.*
import org.bouncycastle.crypto.generators.OpenBSDBCrypt
import ovh.rwx.habbo.BuildConfig
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.util.ActivityPointType
import java.time.LocalDateTime
import ovh.rwx.habbo.database.db

object UserInformationDao {
    private val userInformationsList: MutableMap<Int, UserInformation> = LinkedHashMap()
    val serverConsoleUserInformation: UserInformation = UserInformation(
        Int.MAX_VALUE, // max int, since Habbo doesn't show figures when id == 0
        "SERVER SCRIPTING CONSOLE", // name
        "", // email, empty
        LocalDateTime.of(2015, 1, 1, 0, 0),
        "${BuildConfig.NAME} scripting console.", // realname
        7, // rank
        0, // credits
        HabboServer.habboConfig.serverConsoleFigure, // figure
        "M", // gender
        "Version: ${BuildConfig.VERSION}", // motto
        0, // homeroom
        false, // vip
        "",
        mutableMapOf()
    )

    fun getUserInformationById(userId: Int): UserInformation? {
        if (userId == serverConsoleUserInformation.id) return serverConsoleUserInformation

        if (!userInformationsList.containsKey(userId)) {
            val userInformation = db {
                queryOne<UserInformationDto>(
                    "sql/users/information/select_user_information_from_id.sql",
                    mapOf("user_id" to userId)
                )?.toDomain()
            } ?: return null

            userInformationsList[userId] = userInformation
        }

        return userInformationsList[userId]
    }

    fun getUserInformationByAuthTicket(ssoTicket: String): UserInformation? = db {
        queryOne<Int>(
            "sql/users/information/select_user_information_from_auth_ticket.sql",
            mapOf("ticket" to ssoTicket)
        )?.let { getUserInformationById(it) }
    }

    fun getUserInformationByUsername(username: String): UserInformation? {
        val userInformation: UserInformation? = userInformationsList.values.find { it.username == username }

        return if (userInformation != null) userInformation
        else {
            val userInformation1 = db {
                queryOne<UserInformationDto>(
                    "sql/users/information/select_user_information_from_username.sql",
                    mapOf("username" to username)
                )?.toDomain()
            } ?: return null

            userInformationsList[userInformation1.id] = userInformation1
            userInformation1
        }
    }

    fun getUserInformationByEmail(email: String): UserInformation? {
        val userInformation: UserInformation? = userInformationsList.values.find { it.email == email }

        return if (userInformation != null) userInformation
        else {
            val userInformation1 = db {
                queryOne<UserInformationDto>(
                    "sql/users/information/select_user_information_from_email.sql",
                    mapOf("email" to email)
                )?.toDomain()
            } ?: return null

            userInformationsList[userInformation1.id] = userInformation1
            userInformation1
        }
    }

    fun getUserInformationByEmailAndPassword(email: String, password: String): UserInformation? {
        val userInformation = db {
            queryOne<UserInformationDto>(
                "sql/users/information/select_user_information_from_email.sql",
                mapOf("email" to email)
            )?.toDomain()
        } ?: return null

        if (OpenBSDBCrypt.checkPassword(userInformation.password, password.toCharArray())) {
            return userInformation
        }

        return null
    }

    fun saveInformation(userInformation: UserInformation, online: Boolean, ip: String) {
        db {
            update(
                javaClass.classLoader.getResource("sql/users/information/update_user_information.sql")!!.readText(),
                mapOf(
                    "online" to online,
                    "ip_last" to ip,
                    "credits" to userInformation.credits,
                    "figure" to userInformation.figure,
                    "gender" to userInformation.gender,
                    "motto" to userInformation.motto,
                    "home_room" to userInformation.homeRoom,
                    "id" to userInformation.id
                )
            )

            batchUpdate(
                javaClass.classLoader.getResource("sql/users/information/update_user_currencies.sql")!!.readText(),
                userInformation.activityPointsCurrencies.map {
                    mapOf(
                        "user_id" to userInformation.id,
                        "type" to it.key.code,
                        "points" to it.value
                    )
                }
            )
        }
    }

    fun updateAuthTicket(userInformation: UserInformation, authTicket: String? = null) {
        db {
            update(
                javaClass.classLoader.getResource("sql/users/information/update_auth_ticket_information.sql")!!
                    .readText(),
                mapOf(
                    "auth_ticket" to authTicket,
                    "id" to userInformation.id
                )
            )
        }
    }
}

/**
 * DTO matching SQL columns 1:1, auto-mapped by Jdbi.
 */
data class UserInformationDto(
    val id: Int,
    val username: String,
    val email: String = "",
    val accountCreated: LocalDateTime = LocalDateTime.now(),
    val realname: String = "",
    val rank: Int = 1,
    val credits: Int = 0,
    val figure: String = "",
    val gender: String = "M",
    val motto: String = "",
    val homeRoom: Int = 0,
    val vip: Boolean = false,
    val password: String = "",
    val currenciesStr: String = ""
) {
    fun toDomain(): UserInformation {
        val currenciesMap = mutableMapOf<ActivityPointType, Int>()
        if (currenciesStr.isNotEmpty()) {
            currenciesStr.split(";").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val typeInt = parts[0].toIntOrNull() ?: return@forEach
                    val points = parts[1].toIntOrNull() ?: 0
                    val typeEnum = ActivityPointType.fromType(typeInt)
                    if (typeEnum != null) {
                        currenciesMap[typeEnum] = points
                    }
                }
            }
        }
        return UserInformation(
            id, username, email, accountCreated, realname, rank, credits,
            figure, gender, motto, homeRoom, vip, password, currenciesMap
        )
    }
}
