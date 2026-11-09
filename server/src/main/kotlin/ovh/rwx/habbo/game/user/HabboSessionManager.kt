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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class HabboSessionManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val habboSessions: ConcurrentHashMap<ChannelId, HabboSession> = ConcurrentHashMap()
    @Suppress("MemberVisibilityCanBePrivate")
    val fastFoodSessions: ConcurrentHashMap<ChannelId, FastFoodSession> = ConcurrentHashMap()
    private var heartbeatFuture: ScheduledFuture<*>? = null

    val sessionsCount: Int
        get() = habboSessions.size

    fun startHeartbeat(executor: ScheduledExecutorService, intervalSeconds: Long = 20L) {
        if (heartbeatFuture != null) return
        log.info("Iniciando heartbeat ativo de sessões a cada {}s...", intervalSeconds)
        heartbeatFuture = executor.scheduleWithFixedDelay({
            try {
                heartbeat()
            } catch (t: Throwable) {
                log.error("Erro no heartbeat periódico das sessões", t)
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS)
    }

    fun stopHeartbeat() {
        heartbeatFuture?.cancel(false)
        heartbeatFuture = null
    }

    fun heartbeat() {
        val now = System.nanoTime()
        for (session in habboSessions.values) {
            if (!session.authenticated || !session.channel.isActive) continue
            if (session.fastFoodSession != null) continue

            if (session.ping == 0L) {
                session.ping = now
                session.sendResponse(Outgoing.MISC_PING, OutgoingR63A.MISC_PING)
            }
        }
    }

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