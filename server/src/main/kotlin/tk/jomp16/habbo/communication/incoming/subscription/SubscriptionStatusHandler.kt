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

package tk.jomp16.habbo.communication.incoming.subscription

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.game.user.subscription.HabboSubscription
import tk.jomp16.habbo.kotlin.localDateTimeNowWithoutSecondsAndNanos
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

@Suppress("unused", "UNUSED_PARAMETER")
class SubscriptionStatusHandler {
    @Handler(Incoming.SUBSCRIPTION_STATUS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        val subscription = habboSession.habboSubscription.subscription

        val active = habboSession.habboSubscription.validUserSubscription
        var days = 0
        var months = 0
        var elapsedDays = 0
        var minutes = 0

        if (habboSession.habboSubscription.validUserSubscription) {
            val currentTime = localDateTimeNowWithoutSecondsAndNanos()

            days = ChronoUnit.DAYS.between(currentTime, subscription?.expire).toInt()
            months = ChronoUnit.MONTHS.between(currentTime, subscription?.expire).toInt()
            elapsedDays = ChronoUnit.DAYS.between(subscription?.activated, currentTime).toInt()
            minutes = ChronoUnit.MINUTES.between(currentTime, subscription?.expire).toInt()
            days = Period.between(LocalDate.now(),
                                  LocalDate.now().plusDays(days.toLong()).minusMonths(months.toLong())).days.toInt()

            if (days == 0) days = 1
        }

        habboSession.sendHabboResponse(Outgoing.SUBSCRIPTION_STATUS, HabboSubscription.CLUB_TYPE, active, days,
                                       if (months >= 1) months - 1 else months, elapsedDays, minutes)
    }
}