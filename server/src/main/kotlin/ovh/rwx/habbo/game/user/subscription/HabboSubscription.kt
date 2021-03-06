/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.user.subscription

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.subscription.SubscriptionDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.localDateTimeNowWithoutSecondsAndNanos
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

class HabboSubscription(private val habboSession: HabboSession) {
    var subscription: Subscription? = null
        private set
    val validUserSubscription: Boolean
        get() = subscription != null && localDateTimeNowWithoutSecondsAndNanos().isBefore(subscription?.expire)
    private var initialized: Boolean = false

    internal fun load() {
        if (!initialized) {
            subscription = SubscriptionDao.getSubscription(habboSession.userInformation.id)

            if (!validUserSubscription) clearSubscription()

            initialized = true
        }
    }

    fun addOrExtend(months: Int) {
        if (subscription == null) subscription = SubscriptionDao.createSubscription(habboSession.userInformation.id, months.toLong())
        else SubscriptionDao.extendSubscription(subscription, months.toLong())

        if (validUserSubscription && !habboSession.habboBadge.badges.containsKey("ACH_VipHC1")) habboSession.habboBadge.addBadge("ACH_VipHC1")

        updateStatus()
    }

    fun clearSubscription() {
        if (subscription == null) return

        SubscriptionDao.clearSubscription(subscription)

        subscription = null

        updateStatus()
    }

    fun updateStatus() {
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
            days = Period.between(LocalDate.now(), LocalDate.now().plusDays(days.toLong()).minusMonths(months.toLong())).days

            if (days == 0) days = 1
        }

        habboSession.sendHabboResponse(Outgoing.SUBSCRIPTION_STATUS, CLUB_TYPE, active, days, if (months >= 1) months - 1 else months, elapsedDays, minutes)
        habboSession.sendHabboResponse(Outgoing.USER_RIGHTS, if (habboSession.userInformation.vip || habboSession.habboSubscription.validUserSubscription) 2 else 0, habboSession.userInformation.rank, habboSession.userInformation.ambassador)
    }

    companion object {
        const val CLUB_TYPE = "club_habbo"
    }
}