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

package ovh.rwx.habbo.game.user

import io.netty.channel.Channel
import io.netty.channel.ChannelId
import io.netty.util.AttributeKey
import ovh.rwx.fastfood.game.FastFoodSession
import java.util.concurrent.ConcurrentHashMap

class HabboSessionManager {
    val habboSessions: ConcurrentHashMap<ChannelId, HabboSession> = ConcurrentHashMap()
    @Suppress("MemberVisibilityCanBePrivate")
    val fastFoodSessions: ConcurrentHashMap<ChannelId, FastFoodSession> = ConcurrentHashMap()

    val sessionsCount: Int
        get() = habboSessions.size

    fun makeHabboSession(channel: Channel): Boolean {
        val habboSession = HabboSession(channel)
        channel.attr(habboSessionAttributeKey).set(habboSession)

        return habboSessions.putIfAbsent(channel.id(), habboSession) == null
    }

    fun makeFastFoodSession(channel: Channel): Boolean {
        val fastFoodSession = FastFoodSession(channel)
        channel.attr(fastFoodAttributeKey).set(fastFoodSession)

        return fastFoodSessions.putIfAbsent(channel.id(), fastFoodSession) == null
    }

    fun removeHabboSession(channel: Channel): Boolean {
        val session = habboSessions.remove(channel.id()) ?: return false
        session.close()
        return true
    }

    fun removeFastFoodSession(channel: Channel): Boolean {
        val session = fastFoodSessions.remove(channel.id()) ?: return false
        session.close()
        return true
    }

    fun getHabboSessionById(id: Int): HabboSession? =
        habboSessions.values.find { it.authenticated && it.userInformation.id == id }

    fun getHabboSessionByUsername(username: String): HabboSession? =
        habboSessions.values.find { it.authenticated && it.userInformation.username == username }

    fun getHabboSessionByCryptoToken(token: String): HabboSession? =
        habboSessions.values.find { it.cryptoToken == token }

    fun containsHabboSessionById(id: Int): Boolean = getHabboSessionById(id) != null

    companion object {
        val habboSessionAttributeKey: AttributeKey<HabboSession> = AttributeKey.valueOf<HabboSession>("HABBO_SESSION")
        val fastFoodAttributeKey: AttributeKey<FastFoodSession> = AttributeKey.valueOf<FastFoodSession>("FAST_FOOD_SESSION")
    }
}