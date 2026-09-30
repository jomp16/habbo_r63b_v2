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
import ovh.rwx.habbo.util.ActivityPointType
import java.time.LocalDateTime

fun HabboSession.rewardUserImpl() {
    val localDateTime =
        userStats.creditsLastUpdate.plusSeconds(HabboServer.habboConfig.timerConfig.creditsSeconds.toLong())
    var update = false

    if (LocalDateTime.now().isAfter(localDateTime)) {
        if (HabboServer.habboConfig.rewardConfig.creditsMax < 0 && HabboServer.habboConfig.rewardConfig.credits > 0
            && userInformation.credits < Int.MAX_VALUE
        ) {
            userInformation.credits += HabboServer.habboConfig.rewardConfig.credits
            update = true
        }

        val currentPixels = userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.PIXELS, 0)
        if (HabboServer.habboConfig.rewardConfig.pixelsMax < 0 && HabboServer.habboConfig.rewardConfig.pixels > 0
            && currentPixels < Int.MAX_VALUE
        ) {
            userInformation.activityPointsCurrencies.merge(
                ActivityPointType.PIXELS,
                HabboServer.habboConfig.rewardConfig.pixels,
                Int::plus
            )
            update = true
        }

        val currentVipPoints = userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.DIAMONDS, 0)
        if (userInformation.vip && HabboServer.habboConfig.rewardConfig.vipPointsMax < 0
            && HabboServer.habboConfig.rewardConfig.vipPoints > 0
            && currentVipPoints < Int.MAX_VALUE
        ) {
            userInformation.activityPointsCurrencies.merge(
                ActivityPointType.DIAMONDS,
                HabboServer.habboConfig.rewardConfig.vipPoints,
                Int::plus
            )
            update = true
        }
    }

    if (update) {
        userStats.creditsLastUpdate = LocalDateTime.now()
        updateAllCurrenciesImpl()
    }
}

fun HabboSession.updateAllCurrenciesImpl() {
    // === CREDITS ===
    if (userInformation.credits < 0) userInformation.credits = Int.MAX_VALUE
    if (HabboServer.habboConfig.rewardConfig.creditsMax >= 0 && userInformation.credits > HabboServer.habboConfig.rewardConfig.creditsMax) {
        userInformation.credits = HabboServer.habboConfig.rewardConfig.creditsMax
    }

    // === PIXELS / DUCKETS ===
    var currentPixels = userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.PIXELS, 0)
    if (currentPixels < 0) currentPixels = Int.MAX_VALUE
    if (HabboServer.habboConfig.rewardConfig.pixelsMax >= 0 && currentPixels > HabboServer.habboConfig.rewardConfig.pixelsMax) {
        currentPixels = HabboServer.habboConfig.rewardConfig.pixelsMax
    }
    userInformation.activityPointsCurrencies[ActivityPointType.PIXELS] = currentPixels

    // === VIP POINTS / DIAMANTES ===
    var currentVipPoints = userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.DIAMONDS, 0)
    if (userInformation.vip) {
        if (currentVipPoints < 0) currentVipPoints = Int.MAX_VALUE
        if (HabboServer.habboConfig.rewardConfig.vipPointsMax >= 0 && currentVipPoints > HabboServer.habboConfig.rewardConfig.vipPointsMax) {
            currentVipPoints = HabboServer.habboConfig.rewardConfig.vipPointsMax
        }
        userInformation.activityPointsCurrencies[ActivityPointType.DIAMONDS] = currentVipPoints
    }

    // === ENVIAR PACOTES ===
    if (release != "R63A") {
        sendHabboResponse(Outgoing.CREDITS_BALANCE, userInformation.credits)
        sendHabboResponse(Outgoing.ACTIVITY_POINTS_BALANCE, userInformation.activityPointsCurrencies)
    } else {
        sendHabboResponse(OutgoingR63A.CREDITS_BALANCE, userInformation.credits)
        sendHabboResponse(OutgoingR63A.ACTIVITY_POINTS_BALANCE, userInformation.activityPointsCurrencies)
    }
}
