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

package ovh.rwx.habbo.game.user.subscription

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.subscription.SubscriptionDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.localDateTimeNowWithoutSecondsAndNanos
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

class HabboSubscription(private val habboSession: HabboSession) {
    var habboClubSubscription: Subscription? = null
        private set
    var buildersClubSubscription: Subscription? = null
        private set

    val validUserSubscription: Boolean
        get() = habboClubSubscription != null && localDateTimeNowWithoutSecondsAndNanos().isBefore(habboClubSubscription?.expire)

    val hasHabboClub: Boolean
        get() = habboClubSubscription != null && isActive(habboClubSubscription)

    val hasBuildersClub: Boolean
        get() = buildersClubSubscription != null && isActive(buildersClubSubscription)

    val buildersItemsUsed: Int
        get() = buildersClubSubscription?.itemsUsed ?: 0

    val buildersItemsLimit: Int
        get() = buildersClubSubscription?.itemsLimit ?: 0

    val buildersItemsAvailable: Int
        get() = buildersItemsLimit - buildersItemsUsed

    private var initialized: Boolean = false

    internal fun load() {
        if (!initialized) {
            habboClubSubscription =
                SubscriptionDao.getSubscription(habboSession.userInformation.id, ClubType.HABBO_CLUB)
            buildersClubSubscription =
                SubscriptionDao.getSubscription(habboSession.userInformation.id, ClubType.BUILDERS_CLUB)

            if (!hasHabboClub) {
                SubscriptionDao.clearSubscription(habboClubSubscription)
                habboClubSubscription = null
            }

            if (!hasBuildersClub) {
                SubscriptionDao.clearSubscription(buildersClubSubscription)
                buildersClubSubscription = null
            } else {
                // Sincroniza contador de itens BC ao carregar
                SubscriptionDao.syncBuildersItemsUsed(buildersClubSubscription, habboSession.userInformation.id)
            }

            initialized = true
        }
    }

    private fun isActive(subscription: Subscription?): Boolean {
        return subscription != null && localDateTimeNowWithoutSecondsAndNanos().isBefore(subscription.expire)
    }

    fun addOrExtendHabboClub(months: Int) {
        val isFirstTime = habboClubSubscription == null

        if (habboClubSubscription == null) {
            habboClubSubscription = SubscriptionDao.createSubscription(
                habboSession.userInformation.id,
                months.toLong(),
                ClubType.HABBO_CLUB
            )
        } else {
            SubscriptionDao.extendSubscription(habboClubSubscription, months.toLong())
        }

        if (hasHabboClub && !habboSession.habboBadge.badges.containsKey("ACH_VipHC1")) {
            habboSession.habboBadge.addBadge("ACH_VipHC1")
        }

        if (isFirstTime) {
            HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_BasicClub", 1, accumulate = true)
            HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_HC", 1, accumulate = true)
            HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_VipHC", 1, accumulate = true)
        }

        updateHabboClubStatus()
    }

    fun addOrExtendBuildersClub(months: Int, itemsLimit: Int) {
        val isFirstTime = buildersClubSubscription == null
        val currentSubscription = buildersClubSubscription

        if (currentSubscription == null) {
            buildersClubSubscription = SubscriptionDao.createSubscription(
                habboSession.userInformation.id,
                months.toLong(),
                ClubType.BUILDERS_CLUB,
                itemsLimit
            )
        } else {
            val newLimit = maxOf(currentSubscription.itemsLimit, itemsLimit)
            currentSubscription.itemsLimit = newLimit
            SubscriptionDao.extendBuildersClubSubscription(currentSubscription, months.toLong(), newLimit)
        }

        if (isFirstTime) {
            HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_BuildersClub", 1, accumulate = true)
        }

        updateBuildersClubStatus()
    }

    fun incrementBuildersItemsUsed() {
        if (buildersClubSubscription == null) return
        SubscriptionDao.updateBuildersItemsUsed(buildersClubSubscription, buildersItemsUsed + 1)
    }

    fun decrementBuildersItemsUsed() {
        if (buildersClubSubscription == null) return
        if (buildersItemsUsed > 0) {
            SubscriptionDao.updateBuildersItemsUsed(buildersClubSubscription, buildersItemsUsed - 1)
        }
    }

    fun clearHabboClub() {
        if (habboClubSubscription == null) return

        SubscriptionDao.clearSubscription(habboClubSubscription)
        habboClubSubscription = null

        updateHabboClubStatus()
    }

    fun clearBuildersClub() {
        if (buildersClubSubscription == null) return

        SubscriptionDao.clearSubscription(buildersClubSubscription)
        buildersClubSubscription = null
    }

    fun updateHabboClubStatus() {
        val active = hasHabboClub
        var days = 0
        var months = 0
        var elapsedDays = 0
        var minutes = 0

        if (active) {
            val currentTime = localDateTimeNowWithoutSecondsAndNanos()

            days = ChronoUnit.DAYS.between(currentTime, habboClubSubscription?.expire).toInt()
            months = ChronoUnit.MONTHS.between(currentTime, habboClubSubscription?.expire).toInt()
            elapsedDays = ChronoUnit.DAYS.between(habboClubSubscription?.activated, currentTime).toInt()
            minutes = ChronoUnit.MINUTES.between(currentTime, habboClubSubscription?.expire).toInt()
            days = Period.between(
                LocalDate.now(),
                LocalDate.now().plusDays(days.toLong()).minusMonths(months.toLong())
            ).days

            if (days == 0) days = 1
        }

        if (habboSession.release != "R63A") {
            habboSession.sendHabboResponse(
                Outgoing.SUBSCRIPTION_STATUS,
                CLUB_TYPE,
                active,
                days,
                if (months >= 1) months - 1 else months,
                elapsedDays,
                minutes
            )
            habboSession.sendHabboResponse(
                Outgoing.USER_RIGHTS,
                if (habboSession.userInformation.vip || hasHabboClub) 2 else 0,
                habboSession.userInformation.rank,
                habboSession.userInformation.ambassador
            )
        } else {
            habboSession.sendHabboResponse(
                OutgoingR63A.SUBSCRIPTION_STATUS,
                CLUB_TYPE,
                active,
                days,
                if (months >= 1) months - 1 else months,
                elapsedDays,
                minutes
            )
            habboSession.sendHabboResponse(
                OutgoingR63A.USER_RIGHTS,
                if (habboSession.userInformation.vip || hasHabboClub) 2 else 0,
                habboSession.userInformation.rank
            )
        }
    }

    fun updateBuildersClubStatus() {
        if (habboSession.release == "R63A") return

        habboSession.sendHabboResponse(Outgoing.BUILDERS_SUBSCRIPTION_STATUS, this)
    }

    companion object {
        const val CLUB_TYPE = "club_habbo"
        const val BUILDERS_CLUB_TYPE = "builders_club"
    }
}
