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

package ovh.rwx.habbo.game.catalog

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.subscription.ClubType
import ovh.rwx.habbo.util.ActivityPointType

class CatalogClubService(private val catalogManager: CatalogManager) {

    fun purchaseHC(habboSession: HabboSession, itemId: Int) {
        val clubOffer = catalogManager.getClubOffer(itemId, ClubType.HABBO_CLUB)

        if (clubOffer == null) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)
            return
        }

        val pointType = if (clubOffer.pointsType == 0) ActivityPointType.PIXELS else ActivityPointType.DIAMONDS
        val currentPoints = habboSession.userInformation.activityPointsCurrencies.getOrDefault(pointType, 0)

        if (habboSession.userInformation.credits < clubOffer.credits || currentPoints < clubOffer.points) return

        habboSession.userInformation.credits -= clubOffer.credits
        if (clubOffer.points > 0) {
            habboSession.userInformation.activityPointsCurrencies.merge(pointType, -clubOffer.points, Int::plus)
        }

        habboSession.habboSubscription.addOrExtendHabboClub(clubOffer.months)
        habboSession.updateAllCurrencies()
    }

    fun purchaseBuildersClub(habboSession: HabboSession, itemId: Int) {
        val clubOffer = catalogManager.getClubOffer(itemId, ClubType.BUILDERS_CLUB)

        if (clubOffer == null) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)
            return
        }

        // Trial só pode ser usado uma vez (usuários que ainda não têm BC)
        if (clubOffer.credits == 0 && clubOffer.points == 0 && habboSession.habboSubscription.hasBuildersClub) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)
            return
        }

        val pointType = if (clubOffer.pointsType == 0) ActivityPointType.PIXELS else ActivityPointType.DIAMONDS
        val currentPoints = habboSession.userInformation.activityPointsCurrencies.getOrDefault(pointType, 0)

        if (habboSession.userInformation.credits < clubOffer.credits || currentPoints < clubOffer.points) return

        habboSession.userInformation.credits -= clubOffer.credits
        if (clubOffer.points > 0) {
            habboSession.userInformation.activityPointsCurrencies.merge(pointType, -clubOffer.points, Int::plus)
        }

        // Ao extender, usar o maior itemsLimit entre atual e novo
        val newItemsLimit = maxOf(
            habboSession.habboSubscription.buildersClubSubscription.itemsLimit,
            clubOffer.itemsLimit
        )
        habboSession.habboSubscription.addOrExtendBuildersClub(clubOffer.months, newItemsLimit)
        habboSession.updateAllCurrencies()
    }
}
