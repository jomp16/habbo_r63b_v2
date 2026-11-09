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
import ovh.rwx.habbo.communication.outgoing.misc.MiscPingResponse
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.metrics.ServerMetrics
import java.util.concurrent.TimeUnit

class MiscPingPongTest {

    @Before
    fun resetMetrics() {
        ServerMetrics.sampleNow()
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
}
