/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.game.user

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import java.util.*

class HabboSessionManager {
    val habboSessions: MutableMap<Channel, HabboSession> = HashMap()

    fun makeHabboSession(channel: Channel): Boolean {
        val habboSession = HabboSession(channel)
        channel.attr(habboSessionAttributeKey).set(habboSession)

        return habboSessions.put(channel, habboSession) == null
    }

    fun removeHabboSession(channel: Channel): Boolean {
        if (habboSessions.containsKey(channel)) {
            habboSessions.remove(channel)?.close()

            return true
        } else {
            return false
        }
    }

    fun getHabboSessionById(id: Int) = habboSessions.values.find { it.authenticated && it.userInformation.id == id }

    fun getHabboSessionByUsername(username: String) = habboSessions.values.find { it.authenticated && it.userInformation.username == username }

    fun containsHabboSessionById(id: Int) = getHabboSessionById(id) != null

    companion object {
        val habboSessionAttributeKey = AttributeKey.valueOf<HabboSession>("HABBO_SESSION")
    }
}