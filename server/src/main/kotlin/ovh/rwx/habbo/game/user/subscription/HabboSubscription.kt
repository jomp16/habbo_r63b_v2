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
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.communication.outgoing.subscription.SubscriptionStatusData
import ovh.rwx.habbo.communication.outgoing.user.UserLevelRightsData
import ovh.rwx.habbo.database.subscription.SubscriptionDao
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.localDateTimeNowWithoutSecondsAndNanos
import java.time.temporal.ChronoUnit

class HabboSubscription(private val habboSession: HabboSession) {
    var habboClubSubscription: Subscription? = null
        private set
    val buildersClubSubscription: Subscription by lazy {
        if (habboSession.isBot) {
            // Bot: in-memory trial subscription (zero DB access). The real
            // getSubscription() would UPSERT a row in `users_subscriptions`,
            // which fails the FK constraint because bots have no `users` row.
            Subscription(0, habboSession.userInformation.id, ClubType.BUILDERS_CLUB, null, null, 0, 0)
        } else {
            SubscriptionDao.getSubscription(habboSession.userInformation.id, ClubType.BUILDERS_CLUB)!!
        }
    }

    val validUserSubscription: Boolean
        get() = habboClubSubscription != null && localDateTimeNowWithoutSecondsAndNanos().isBefore(habboClubSubscription?.expire)

    val hasHabboClub: Boolean
        get() = habboClubSubscription != null && isActive(habboClubSubscription)

    val hasBuildersClub: Boolean
        get() = isActive(buildersClubSubscription)

    private var initialized: Boolean = false

    internal fun load() {
        if (habboSession.isBot) return

        if (!initialized) {
            habboClubSubscription =
                SubscriptionDao.getSubscription(habboSession.userInformation.id, ClubType.HABBO_CLUB)

            if (!hasHabboClub) {
                SubscriptionDao.clearSubscription(habboClubSubscription)
                habboClubSubscription = null
            }

            if (!hasBuildersClub && !buildersClubSubscription.trial) {
                buildersClubSubscription.expire = null
                buildersClubSubscription.activated = null
                buildersClubSubscription.itemsLimit = 100

                SubscriptionDao.updateBuildersClubSubscription(buildersClubSubscription)
            } else {
                // Sincroniza contador de itens BC ao carregar
                SubscriptionDao.syncBuildersItemsUsed(buildersClubSubscription, habboSession.userInformation.id)
            }

            initialized = true
        }
    }

    private fun isActive(subscription: Subscription?): Boolean {
        return !(subscription == null || subscription.trial) && localDateTimeNowWithoutSecondsAndNanos().isBefore(subscription.expire)
    }

    fun addOrExtendHabboClub(months: Int) {
        if (habboClubSubscription == null) {
            habboClubSubscription = SubscriptionDao.createSubscription(
                habboSession.userInformation.id,
                months.toLong(),
                ClubType.HABBO_CLUB
            )
        } else {
            SubscriptionDao.extendSubscription(habboClubSubscription, months.toLong())
        }

        updateHabboClubStatus()
    }

    fun addOrExtendBuildersClub(months: Int, itemsLimit: Int) {
        val currentSubscription = buildersClubSubscription

        val newLimit = maxOf(currentSubscription.itemsLimit, itemsLimit)
        currentSubscription.itemsLimit = newLimit
        currentSubscription.expire = currentSubscription.expire?.plusMonths(months.toLong())
        SubscriptionDao.updateBuildersClubSubscription(currentSubscription)

        updateBuildersClubStatus()
    }

    fun incrementBuildersItemsUsed(room: Room) {
        HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_RoomDecoBC", 1, accumulate = true)
        SubscriptionDao.updateBuildersItemsUsed(buildersClubSubscription, buildersClubSubscription.itemsUsed + 1)

        if (buildersClubSubscription.trial && room.itemManager.items.values.count { it.buildersClub } == 1) {
            if (habboSession.isPreShuffle) {
                habboSession.sendNotification($$"${notification.builders_club.room_locked.message}")
            } else {
                habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.BUILDERS_CLUB_ROOM_LOCKED)
                habboSession.sendHabboResponse(Outgoing.BUILDERS_FURNI_COUNT, habboSession.habboSubscription)
            }
        }
    }

    fun decrementBuildersItemsUsed(room: Room, count: Int = 1) {
        if (buildersClubSubscription.itemsUsed > 0) {
            SubscriptionDao.updateBuildersItemsUsed(
                buildersClubSubscription,
                buildersClubSubscription.itemsUsed - count
            )

            if (room.itemManager.items.values.count { it.buildersClub } <= 0) {
                if (habboSession.isPreShuffle) {
                    habboSession.sendNotification($$"${notification.builders_club.room_unlocked.message}")
                } else {
                    habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.BUILDERS_CLUB_ROOM_UNLOCKED)
                    habboSession.sendHabboResponse(Outgoing.BUILDERS_FURNI_COUNT, habboSession.habboSubscription)
                }
            }
        }
    }

    fun clearHabboClub() {
        if (habboClubSubscription == null) return

        SubscriptionDao.clearSubscription(habboClubSubscription)
        habboClubSubscription = null

        updateHabboClubStatus()
    }

    fun updateHabboClubStatus() {
        val active = hasHabboClub
        var days = 0
        var months = 0
        var elapsedDays = 0
        var minutes = 0

        if (active) {
            val currentTime = localDateTimeNowWithoutSecondsAndNanos()

            elapsedDays = ChronoUnit.DAYS.between(habboClubSubscription?.activated, currentTime).toInt()
            minutes = ChronoUnit.MINUTES.between(currentTime, habboClubSubscription?.expire).toInt()

            // Em todos os clientes (2011 → WIN63 2026) o total de dias exibido é `clubPeriods * 31 + clubDays`
            // e o status "ativo" exige `clubDays > 0`: enviar o total real dividido em períodos de 31 dias
            // (meses de calendário têm ~30,4 d e subcontavam). Datas/tempo restante vêm de `minutes`.
            val totalDays = ChronoUnit.DAYS.between(currentTime, habboClubSubscription?.expire).toInt().coerceAtLeast(1)
            months = totalDays / 31
            days = totalDays % 31

            if (days == 0) {
                days = 31
                months -= 1
            }
        }

        habboSession.sendResponse(
            Outgoing.SUBSCRIPTION_STATUS,
            OutgoingR63A.SUBSCRIPTION_STATUS,
            SubscriptionStatusData(
                clubType = CLUB_TYPE,
                active = active,
                days = days,
                months = months,
                elapsedDays = elapsedDays,
                minutes = minutes
            )
        )
        habboSession.sendResponse(
            Outgoing.USER_RIGHTS,
            OutgoingR63A.USER_RIGHTS,
            UserLevelRightsData(
                club = if (habboSession.userInformation.vip || habboSession.habboSubscription.validUserSubscription) 2 else 0,
                rank = habboSession.userInformation.rank,
                ambassador = habboSession.userInformation.ambassador
            )
        )
    }

    fun updateBuildersClubStatus() {
        if (habboSession.isPreShuffle) return

        habboSession.sendHabboResponse(Outgoing.BUILDERS_SUBSCRIPTION_STATUS, this)
    }

    companion object {
        const val CLUB_TYPE = "club_habbo"
        const val BUILDERS_CLUB_TYPE = "builders_club"
    }
}
