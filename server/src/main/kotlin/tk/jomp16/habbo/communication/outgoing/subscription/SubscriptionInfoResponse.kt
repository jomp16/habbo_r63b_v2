/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.communication.outgoing.subscription

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.subscription.Subscription
import java.time.Clock
import java.time.LocalDateTime

@Suppress("unused", "UNUSED_PARAMETER")
class SubscriptionInfoResponse {
    @Response(Outgoing.HABBO_CLUB_INFO)
    fun handle(habboResponse: HabboResponse, subscription: Subscription?) {
        habboResponse.apply {
            if (subscription != null) {
                // todo: stub

                writeInt(1) // streakduration - in days
                writeUTF(subscription.activated.format(HabboServer.DATE_TIME_FORMATTER)) // Time joined HC
                writeDouble((50.toDouble() / 100)) // credits multiplier / 10 -- %streakduration%
                writeInt(0) // useless, I couldn't find any references on Habbo_scripts.txt
                writeInt(0) // useless, I couldn't find any references on Habbo_scripts.txt
                writeInt(100) // credits spent on catalog
                writeInt(5) // credits bonus
                writeInt(150) // credits to receive with multiplier
                writeInt(LocalDateTime.now(Clock.systemUTC()).plusMonths(1).second) // how long to get next bonus
            } else {
                writeInt(0)
                writeUTF("")
                writeDouble(0.toDouble())
                writeInt(0)
                writeInt(0)
                writeInt(0)
                writeInt(0)
                writeInt(0)
                writeInt(LocalDateTime.now(Clock.systemUTC()).plusDays(1).second)
            }
        }
    }
}