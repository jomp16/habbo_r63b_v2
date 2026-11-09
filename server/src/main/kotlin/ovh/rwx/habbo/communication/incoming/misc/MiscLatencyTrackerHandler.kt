/*
 * Copyright (C) 2015-2021 jomp16 <root@rwx.ovh>
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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.ip
import ovh.rwx.habbo.metrics.ServerMetrics
import java.util.concurrent.TimeUnit

@Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")
class MiscLatencyTrackerHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Handler(Incoming.MISC_LATENCY_TRACKER)
    @HandlerR63A(IncomingR63A.MISC_LATENCY_TRACKER)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val averageLatencyMs = habboRequest.readInt()
        val filteredAverageMs = habboRequest.readInt()
        val sampleCount = habboRequest.readInt()

        if (averageLatencyMs >= 0) {
            ServerMetrics.recordPing(TimeUnit.MILLISECONDS.toNanos(averageLatencyMs.toLong()))
            val username = if (habboSession.authenticated) habboSession.userInformation.username else habboSession.channel.ip()
            log.debug("User $username reported latency: avg={}ms filtered={}ms samples={}", averageLatencyMs, filteredAverageMs, sampleCount)
        }
    }
}