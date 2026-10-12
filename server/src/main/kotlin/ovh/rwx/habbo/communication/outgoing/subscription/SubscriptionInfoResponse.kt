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

package ovh.rwx.habbo.communication.outgoing.subscription

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBetween
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.subscription.Subscription
import java.time.Duration
import java.time.LocalDateTime

@Suppress("unused", "UNUSED_PARAMETER")
class SubscriptionInfoResponse {
    @Response(Outgoing.HABBO_CLUB_INFO)
    fun response(habboResponse: HabboResponse, subscription: Subscription?) {
        // todo: stub
        val now = LocalDateTime.now()
        val activated = subscription?.takeIf { !it.trial }?.activated

        val currentStreak = activated?.let { Duration.between(it, now).toDays().toInt() } ?: 0
        val firstSubDate = activated?.format(HabboServer.DATE_TIME_FORMATTER_WITH_HOURS).orEmpty()
        val kickbackPercentage = if (activated != null) 0.50 else 0.0
        val bonus = if (activated != null) 5 else 0
        val monthlyReward = if (activated != null) 150 else 0
        val timeUntilPayday = if (activated != null) {
            Duration.between(now, now.plusMonths(2)).toMinutes().toInt()
        } else {
            0
        }

        habboResponse.apply {
            if (isVersionAtLeast(2016, 7, 26)) {
                // Redesign 20160726+: currentHcStreak, firstSubscriptionDate, kickback, missed/rewarded, spent, bonus, reward, payday
                writeInt(currentStreak)
                writeUTF(firstSubDate)
                writeDouble(kickbackPercentage)
                writeInt(0) // totalCreditsMissed
                writeInt(0) // totalCreditsRewarded
                writeInt(0) // totalCreditsSpent
                writeInt(bonus)
                writeInt(monthlyReward)
                writeInt(timeUntilPayday)
            } else {
                /*
                 * Era pré-2016 (ScrUserInfo, verificado no AS3 RELEASE63-201305221112:
                 * parser _-0sk/_SafeStr_1895, evento _-2tA/_SafeStr_1801 header 870):
                 * UTF productName, int clubDays, int (minutos restantes), int clubPeriods,
                 * int status (0=sem club), bool, bool, int, int, int
                 */
                val daysLeft = 0
                val minutesLeft = 0
                val periodsLeft = 0

                writeUTF("habbo_club")
                writeInt(daysLeft)
                writeInt(minutesLeft)
                writeInt(periodsLeft)
                writeInt(0) // status: 0 = sem assinatura
                writeBoolean(false) // hasEverBeenMember
                writeBoolean(false) // isHC
                writeInt(0)
                writeInt(0)
                writeInt(0)
            }
        }
    }
}