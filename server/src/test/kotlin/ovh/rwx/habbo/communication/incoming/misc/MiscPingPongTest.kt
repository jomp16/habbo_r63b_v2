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

package ovh.rwx.habbo.communication.incoming.misc

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.HabboSessionManager
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.information.UserPreferences
import ovh.rwx.habbo.game.user.information.UserStats
import ovh.rwx.habbo.metrics.ServerMetrics
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class MiscPingPongTest {

    @Before
    fun resetMetrics() {
        ServerMetrics.sampleNow()
    }

    private fun authenticatedSession(channel: EmbeddedChannel = EmbeddedChannel()): HabboSession =
        HabboSession(channel).apply {
            release = "RELEASE63-201305221112-286302280"
            userInformation = UserInformation(
                id = 1,
                username = "testUser",
                email = "test@test.com",
                accountCreated = LocalDateTime.now(),
                realname = "test",
                rank = 1,
                credits = 100,
                figure = "-",
                gender = "M",
                motto = "",
                homeRoom = 1,
                vip = false,
                password = "",
                activityPointsCurrencies = mutableMapOf()
            )
            val now = LocalDateTime.now()
            userStats = UserStats(
                id = 1,
                lastOnline = now,
                lastOnlineDatabase = now,
                onlineSeconds = 0,
                roomVisits = 0,
                respect = 0,
                giftsGiven = 0,
                giftsReceived = 0,
                dailyRespectPoints = 3,
                dailyPetRespectPoints = 3,
                dailyCompetitionVotes = 3,
                achievementScore = 0,
                questId = 0,
                questProgress = 0,
                favoriteGroupId = 0,
                ticketsAnswered = 0,
                marketplaceTickets = 0,
                creditsLastUpdate = now,
                respectLastUpdate = now
            )
            userPreferences = UserPreferences(
                id = 1,
                volume = "100,100,100",
                preferOldChat = false,
                ignoreRoomInvite = false,
                disableCameraFollow = false,
                navigatorX = 0,
                navigatorY = 0,
                navigatorWidth = 0,
                navigatorHeight = 0,
                hideInRoom = false,
                blockNewFriends = false,
                chatColor = 0,
                friendBarOpen = false,
                friendStreamEnabled = false
            )
        }

    @Test
    fun `misc pong records ping metric when ping was active`() {
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)
        val elapsedMs = 50L
        session.ping = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(elapsedMs)

        val handler = MiscPongHandler()
        val request = HabboRequest(0, Unpooled.buffer())
        handler.handle(session, request)

        assertEquals(0L, session.ping)

        val sample = ServerMetrics.sampleNow()
        assertEquals(1L, sample.pings)
        assertTrue("Expected avg ping around ${elapsedMs}ms but was ${sample.pingAvgMs}ms", sample.pingAvgMs in 40.0..100.0)
    }

    @Test
    fun `misc pong does not record metric when session ping is zero`() {
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)
        session.ping = 0L

        val handler = MiscPongHandler()
        val request = HabboRequest(0, Unpooled.buffer())
        handler.handle(session, request)

        val sample = ServerMetrics.sampleNow()
        assertEquals(0L, sample.pings)
        assertEquals(0.0, sample.pingAvgMs, 0.001)
    }

    @Test
    fun `misc latency tracker records client reported latency into server metrics`() {
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)

        val buffer = Unpooled.buffer()
        buffer.writeInt(38) // average latency ms
        buffer.writeInt(35) // filtered average ms
        buffer.writeInt(3)  // sample count

        val handler = MiscLatencyTrackerHandler()
        handler.handle(session, HabboRequest(0, buffer))

        val sample = ServerMetrics.sampleNow()
        assertEquals(1L, sample.pings)
        assertEquals(38.0, sample.pingAvgMs, 1.0)
    }

    @Test
    fun `session heartbeat sends ping to authenticated active sessions`() {
        val sessionManager = HabboSessionManager()
        val channel = EmbeddedChannel()
        val session = authenticatedSession(channel)
        sessionManager.habboSessions[channel.id()] = session

        assertEquals(0L, session.ping)
        sessionManager.heartbeat()

        assertTrue(session.ping > 0L)
    }

    @Test
    fun `session heartbeat does not re-ping session with ping already in flight`() {
        val sessionManager = HabboSessionManager()
        val channel = EmbeddedChannel()
        val session = authenticatedSession(channel)
        val initialPing = 123456789L
        session.ping = initialPing
        sessionManager.habboSessions[channel.id()] = session

        sessionManager.heartbeat()

        assertEquals(initialPing, session.ping)
    }

    @Test
    fun `session heartbeat skips unauthenticated session`() {
        val sessionManager = HabboSessionManager()
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)
        sessionManager.habboSessions[channel.id()] = session

        sessionManager.heartbeat()

        assertEquals(0L, session.ping)
    }
}
