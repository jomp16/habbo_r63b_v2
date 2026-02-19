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

package ovh.rwx.habbo.communication.outgoing.builders

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.subscription.HabboSubscription
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Suppress("unused", "UNUSED_PARAMETER")
class BuildersClubSubscriptionStatusResponse {
    @Response(Outgoing.BUILDERS_SUBSCRIPTION_STATUS)
    fun response(habboResponse: HabboResponse, habboSubscription: HabboSubscription) {
        habboResponse.apply {
            val subscription = habboSubscription.buildersClubSubscription

            if (subscription == null) {
                writeInt(0) // secondsLeft
                writeInt(0) // furniLimit
                writeInt(0) // maxFurniLimit
                writeInt(0) // secondsLeftWithGrace
            } else {
                val secondsLeft = ChronoUnit.SECONDS.between(
                    LocalDateTime.now(),
                    subscription.expire
                ).toInt().coerceAtLeast(0)

                val secondsLeftWithGrace = secondsLeft + 3600

                writeInt(secondsLeft)
                writeInt(subscription.itemsLimit)
                writeInt(subscription.itemsLimit)
                writeInt(secondsLeftWithGrace)
            }
        }
    }
}
