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

package ovh.rwx.habbo.game.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.currency.CurrencyService
import ovh.rwx.habbo.util.ActivityPointType
import java.time.LocalDateTime

fun HabboSession.rewardUserImpl() {
    val rewardConfig = HabboServer.habboConfig.rewardConfig
    val currencies = userInformation.activityPointsCurrencies
    val localDateTime = userStats.creditsLastUpdate.plusSeconds(HabboServer.habboConfig.timerConfig.creditsSeconds.toLong())
    var update = false
    var grantCredits = 0
    val grantPoints = mutableMapOf<Int, Int>()

    if (LocalDateTime.now().isAfter(localDateTime)) {
        if (rewardConfig.creditsMax < 0 && rewardConfig.credits > 0 && userInformation.credits < Int.MAX_VALUE) {
            grantCredits = rewardConfig.credits
            update = true
        }

        if (rewardConfig.pixelsMax < 0 && rewardConfig.pixels > 0 && currencies[ActivityPointType.PIXELS] < Int.MAX_VALUE) {
            grantPoints.merge(ActivityPointType.PIXELS.code, rewardConfig.pixels, Int::plus)
            update = true
        }

        if (userInformation.vip && rewardConfig.vipPointsMax < 0 && rewardConfig.vipPoints > 0
            && currencies[ActivityPointType.DIAMONDS] < Int.MAX_VALUE
        ) {
            grantPoints.merge(ActivityPointType.DIAMONDS.code, rewardConfig.vipPoints, Int::plus)
            update = true
        }

        // Outras moedas (duckets, conchas...) configuradas em reward.extra_currencies
        for (extra in rewardConfig.extraCurrencies) {
            if (extra.amount <= 0 || (extra.vipOnly && !userInformation.vip)) continue
            val current = currencies[extra.type]
            if (extra.max >= 0 && current >= extra.max) continue

            grantPoints.merge(extra.type, extra.amount, Int::plus)
            update = true
        }
    }

    if (update) {
        userStats.creditsLastUpdate = LocalDateTime.now()
        // Crédito sob o lock do CurrencyService; ele reenvia os saldos (já aplicando os tetos)
        CurrencyService.grantAll(this, grantCredits, grantPoints)
    }
}

fun HabboSession.updateAllCurrenciesImpl() {
    val rewardConfig = HabboServer.habboConfig.rewardConfig
    val currencies = userInformation.activityPointsCurrencies

    // Tetos de configuração: normalização sob o mesmo lock das operações do CurrencyService
    synchronized(userInformation) {
        if (userInformation.credits < 0) userInformation.credits = Int.MAX_VALUE
        if (rewardConfig.creditsMax >= 0 && userInformation.credits > rewardConfig.creditsMax) {
            userInformation.credits = rewardConfig.creditsMax
        }

        capCurrency(ActivityPointType.PIXELS.code, rewardConfig.pixelsMax)
        if (userInformation.vip) capCurrency(ActivityPointType.DIAMONDS.code, rewardConfig.vipPointsMax)
        rewardConfig.extraCurrencies.forEach { capCurrency(it.type, it.max) }
    }

    sendResponse(Outgoing.CREDITS_BALANCE, OutgoingR63A.CREDITS_BALANCE, userInformation.credits)
    sendResponse(Outgoing.ACTIVITY_POINTS_BALANCE, OutgoingR63A.ACTIVITY_POINTS_BALANCE, currencies)
}

/** Aplica o teto de uma moeda (max < 0 = sem teto). */
private fun HabboSession.capCurrency(type: Int, max: Int) {
    val currencies = userInformation.activityPointsCurrencies
    if (max >= 0 && currencies[type] > max) currencies[type] = max
}
