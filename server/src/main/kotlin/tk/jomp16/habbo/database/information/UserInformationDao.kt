/*
 * Copyright (C) 2017 jomp16
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

package tk.jomp16.habbo.database.information

import com.github.andrewoma.kwery.core.Row
import net.sf.ehcache.Ehcache
import net.sf.ehcache.Element
import tk.jomp16.habbo.BuildConfig
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.information.UserInformation
import tk.jomp16.habbo.kotlin.addAndGetEhCache
import java.util.concurrent.atomic.AtomicInteger

object UserInformationDao {
    private val userInformationByIdCache: Ehcache = HabboServer.cacheManager.addAndGetEhCache("userInformationByIdCache")
    private val userInformationByUsernameCache: Ehcache = HabboServer.cacheManager.addAndGetEhCache("userInformationByUsernameCache")

    init {
        userInformationByIdCache.put(Element(Int.MAX_VALUE, UserInformation(
                Int.MAX_VALUE, // max int, since Habbo doesn't show figures when id == 0
                "SERVER SCRIPTING CONSOLE", // name
                "", // email, empty
                "${BuildConfig.NAME} scripting console.", // realname
                7, // rank
                AtomicInteger(0), // credits
                AtomicInteger(0), // pixels
                AtomicInteger(0), // vip points
                HabboServer.habboConfig.serverConsoleFigure, // figure
                "M", // gender
                "Version: ${BuildConfig.VERSION}", // motto
                0, // homeroom
                false // vip
        )))
    }

    fun getUserInformationById(id: Int, cache: Boolean = true): UserInformation? {
        if (!cache || !userInformationByIdCache.isKeyInCache(id)) {
            HabboServer.database {
                select("SELECT * FROM users WHERE id = :id LIMIT 1",
                        mapOf(
                                "id" to id
                        )
                ) { getUserInformation(it) }.firstOrNull()
            }?.let { addCache(it) }
        }

        return userInformationByIdCache.get(id).objectValue as UserInformation
    }

    fun getUserInformationByAuthTicket(ssoTicket: String): UserInformation? {
        val userInformation = HabboServer.database {
            select("SELECT id FROM users WHERE auth_ticket = :ticket LIMIT 1",
                    mapOf(
                            "ticket" to ssoTicket
                    )
            ) { getUserInformationById(it.int("id")) }.firstOrNull()
        } ?: return null

        addCache(userInformation)

        return userInformation
    }

    fun getUserInformationByUsername(username: String, cache: Boolean = true): UserInformation? {
        if (!cache || !userInformationByUsernameCache.isKeyInCache(username)) {
            HabboServer.database {
                select("SELECT * FROM users WHERE username = :username",
                        mapOf(
                                "username" to username
                        )
                ) { getUserInformation(it) }.firstOrNull()
            }?.let { addCache(it) }
        }

        return userInformationByUsernameCache.get(username).objectValue as UserInformation
    }

    private fun addCache(userInformation: UserInformation) {
        userInformationByIdCache.put(Element(userInformation.id, userInformation))
        userInformationByUsernameCache.put(Element(userInformation.username, userInformation))
    }

    private fun getUserInformation(row: Row) = UserInformation(
            row.int("id"),
            row.string("username"),
            row.string("email"),
            row.string("realname"),
            row.int("rank"),
            AtomicInteger(row.int("credits")),
            AtomicInteger(row.int("pixels")),
            AtomicInteger(row.int("vip_points")),
            row.string("figure"),
            row.string("gender"),
            row.string("motto"),
            row.int("home_room"),
            row.boolean("vip")
    )

    fun saveInformation(userInformation: UserInformation, online: Boolean, ip: String, authTicket: String = "") {
        HabboServer.database {
            update("UPDATE users SET auth_ticket = :ticket, online = :online, ip_last = :ip_last, credits = :credits, pixels = :pixels, " +
                    "vip_points = :vip_points, figure = :figure, gender = :gender, motto = :motto, home_room = :home_room WHERE id = :id",
                    mapOf(
                            "ticket" to authTicket,
                            "online" to online,
                            "ip_last" to ip,
                            "credits" to userInformation.credits.get(),
                            "pixels" to userInformation.pixels.get(),
                            "vip_points" to userInformation.vipPoints.get(),
                            "figure" to userInformation.figure,
                            "gender" to userInformation.gender,
                            "motto" to userInformation.motto,
                            "home_room" to userInformation.homeRoom,
                            "id" to userInformation.id
                    )
            )
        }
    }
}