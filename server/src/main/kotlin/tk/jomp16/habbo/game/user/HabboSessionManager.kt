/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.game.user

import io.netty.channel.Channel
import io.netty.channel.ChannelId
import io.netty.util.AttributeKey
import tk.jomp16.fastfood.game.FastFoodSession

class HabboSessionManager {
    val habboSessions: MutableMap<ChannelId, HabboSession> = mutableMapOf()
    val fastFoodSessions: MutableMap<ChannelId, FastFoodSession> = mutableMapOf()

    fun makeHabboSession(channel: Channel): Boolean {
        val habboSession = HabboSession(channel)
        channel.attr(habboSessionAttributeKey).set(habboSession)

        return habboSessions.put(channel.id(), habboSession) == null
    }

    fun makeFastFoodSession(channel: Channel): Boolean {
        val fastFoodSession = FastFoodSession(channel)
        channel.attr(fastFoodAttributeKey).set(fastFoodSession)

        return fastFoodSessions.put(channel.id(), fastFoodSession) == null
    }

    fun removeHabboSession(channel: Channel): Boolean {
        return if (habboSessions.containsKey(channel.id())) {
            habboSessions.remove(channel.id())?.close()

            true
        } else {
            false
        }
    }

    fun removeFastFoodSession(channel: Channel): Boolean {
        return if (fastFoodSessions.containsKey(channel.id())) {
            fastFoodSessions.remove(channel.id())?.close()

            true
        } else {
            false
        }
    }

    fun getHabboSessionById(id: Int) = habboSessions.values.find { it.authenticated && it.userInformation.id == id }

    fun getHabboSessionByUsername(username: String) = habboSessions.values.find { it.authenticated && it.userInformation.username == username }

    fun containsHabboSessionById(id: Int) = getHabboSessionById(id) != null

    companion object {
        val habboSessionAttributeKey: AttributeKey<HabboSession> = AttributeKey.valueOf<HabboSession>("HABBO_SESSION")
        val fastFoodAttributeKey: AttributeKey<FastFoodSession> = AttributeKey.valueOf<FastFoodSession>("FAST_FOOD_SESSION")
    }
}