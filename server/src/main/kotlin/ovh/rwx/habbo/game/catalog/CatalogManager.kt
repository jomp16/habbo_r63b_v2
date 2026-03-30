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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogPurchaseNotAllowedErrorResponse
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogVoucherRedeemErrorResponse
import ovh.rwx.habbo.database.catalog.CatalogDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.subscription.ClubType
import ovh.rwx.habbo.util.ActivityPointType

class CatalogManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val catalogPages: MutableList<CatalogPage> = mutableListOf()
    val catalogItems: MutableList<CatalogItem> = mutableListOf()
    val catalogClubOffers: MutableList<CatalogClubOffer> = mutableListOf()
    val catalogDeals: MutableList<CatalogDeal> = mutableListOf()
    val recyclerRewards: MutableMap<Int, List<String>> = mutableMapOf()

    fun load() {
        log.info("Loading catalog...")
        // clear catalog
        catalogPages.clear()
        catalogItems.clear()
        catalogClubOffers.clear()
        catalogDeals.clear()
        // done clear catalog
        // catalog root
        catalogPages += CatalogPage(
            -1,
            0,
            "",
            "root",
            0,
            visible = false,
            enabled = true,
            minRank = 1,
            clubOnly = false,
            orderNum = 1,
            pageLayout = "",
            pageHeadline = "",
            pageTeaser = "",
            pageSpecial = "",
            pageText1 = "",
            pageText2 = "",
            pageTextDetails = "",
            pageTextTeaser = "",
            pageLinkDescription = "",
            pageLinkPagename = "",
            customData = emptyMap()
        )
        // catalog builders
        catalogPages += CatalogPage(
            -2,
            0,
            "",
            "root",
            0,
            visible = false,
            enabled = true,
            minRank = 1,
            clubOnly = false,
            orderNum = 1,
            pageLayout = "",
            pageHeadline = "",
            pageTeaser = "",
            pageSpecial = "",
            pageText1 = "",
            pageText2 = "",
            pageTextDetails = "",
            pageTextTeaser = "",
            pageLinkDescription = "",
            pageLinkPagename = "",
            customData = emptyMap()
        )

        catalogPages += CatalogDao.getCatalogPages()
        catalogItems += CatalogDao.getCatalogItems()
        catalogClubOffers += CatalogDao.getCatalogClubOffers()
        catalogDeals += CatalogDao.getCatalogDeals()
        recyclerRewards += CatalogDao.getRecyclerRewards().groupBy { it.first }
            .mapValues { it.value.map { pair -> pair.second } }

        log.info("Loaded {} catalog pages!", catalogPages.size - 2)
        log.info("Loaded {} catalog items!", catalogItems.size)
        log.info("Loaded {} club offers!", catalogClubOffers.size)
        log.info("Loaded {} catalog deals!", catalogDeals.size)
        log.info(
            "Loaded {} recycler levels and {} recycler rewards!",
            recyclerRewards.size,
            recyclerRewards.values.sumOf { it.size })
    }

    fun purchaseHC(habboSession: HabboSession, itemId: Int) {
        val clubOffer = catalogClubOffers.find { it.itemId == itemId && it.clubType == ClubType.HABBO_CLUB }

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
        val clubOffer = catalogClubOffers.find { it.itemId == itemId && it.clubType == ClubType.BUILDERS_CLUB }

        if (clubOffer == null) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)
            return
        }

        // Trial so pode ser usado uma vez (usuarios que nao tem BC)
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

    // todo: add gift support
    fun purchase(habboSession: HabboSession, catalogItem: CatalogItem, extraData: String, amount: Int) {
        if (!catalogItem.offerActive || catalogItem.clubOnly && !habboSession.habboSubscription.validUserSubscription) {
            if (habboSession.release != "R63A")
                habboSession.sendHabboResponse(
                    Outgoing.CATALOG_PURCHASE_NOT_ALLOWED_ERROR,
                    CatalogPurchaseNotAllowedErrorResponse.CatalogPurchaseNotAllowedError.NOT_HC
                )
            else
                habboSession.sendHabboResponse(
                    OutgoingR63A.CATALOG_PURCHASE_NOT_ALLOWED_ERROR,
                    CatalogPurchaseNotAllowedErrorResponse.CatalogPurchaseNotAllowedError.NOT_HC
                )

            return
        }
        val totalAmountToPurchase = amount - totalFreeAmount(amount)

        val currentPixels =
            habboSession.userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.PIXELS, 0)
        val currentVipPoints =
            habboSession.userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.DIAMONDS, 0)

        val notEnoughCredits =
            catalogItem.costCredits > 0 && habboSession.userInformation.credits < catalogItem.costCredits * totalAmountToPurchase
        val notEnoughPixels =
            catalogItem.costPixels > 0 && currentPixels < catalogItem.costPixels * totalAmountToPurchase
        val notEnoughVipPoints =
            catalogItem.costVip > 0 && currentVipPoints < catalogItem.costVip * totalAmountToPurchase

        if (notEnoughCredits || notEnoughPixels || notEnoughVipPoints) {
            val missingPointType = if (notEnoughVipPoints) ActivityPointType.DIAMONDS else ActivityPointType.PIXELS

            if (habboSession.release != "R63A")
                habboSession.sendHabboResponse(
                    Outgoing.CATALOG_PURCHASE_ERROR_NOT_ENOUGH_BALANCE,
                    notEnoughCredits,
                    notEnoughPixels || notEnoughVipPoints,
                    missingPointType
                )
            else
                habboSession.sendHabboResponse(
                    OutgoingR63A.CATALOG_PURCHASE_ERROR_NOT_ENOUGH_BALANCE,
                    notEnoughCredits,
                    notEnoughPixels || notEnoughVipPoints,
                    missingPointType
                )

            return
        }

        if (catalogItem.limited && catalogItem.limitedSells.get() >= catalogItem.limitedTotal) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_LIMITED_SOLD_OUT)

            return
        }
        // todo
        val furnishingToPurchase: MutableList<CatalogPurchaseData> = mutableListOf()

        if (catalogItem.dealId > 0) {
            catalogItem.deal!!.furnishings.forEachIndexed { i, furnishing ->
                HabboServer.habboGame.itemManager.correctExtradataCatalog(habboSession, extraData, furnishing)
                    ?.let { extraData1 ->
                        @Suppress("ForEachParameterNotUsed")
                        (0 until catalogItem.deal!!.amounts[i]).forEach {
                            furnishingToPurchase += CatalogPurchaseData(
                                furnishing,
                                extraData1,
                                if (catalogItem.limited) catalogItem.limitedSells.incrementAndGet() else 0
                            )

                            if (furnishing.interactionType == InteractionType.TELEPORT) furnishingToPurchase += furnishingToPurchase.last()
                        }
                    }
            }
        } else {
            HabboServer.habboGame.itemManager.correctExtradataCatalog(habboSession, extraData, catalogItem.furnishing)
                ?.let { extraData1 ->
                    @Suppress("ForEachParameterNotUsed")
                    (0 until catalogItem.amount * amount).forEach {
                        furnishingToPurchase += CatalogPurchaseData(
                            catalogItem.furnishing,
                            extraData1,
                            if (catalogItem.limited) catalogItem.limitedSells.incrementAndGet() else 0
                        )

                        if (catalogItem.furnishing.interactionType == InteractionType.TELEPORT) furnishingToPurchase += furnishingToPurchase.last()
                    }
                }
        }
        val userItems = ItemDao.addItems(
            habboSession.userInformation.id,
            furnishingToPurchase.map {
                ItemPurchaseData(
                    it.furnishing,
                    it.extraData,
                    it.limitedNumber > 0,
                    buildersClub = false
                )
            },
        )

        furnishingToPurchase.filter { it.limitedNumber > 0 }.forEach {
            ItemDao.addLimitedItem(
                userItems[furnishingToPurchase.indexOf(it)].id,
                it.limitedNumber,
                catalogItem.limitedTotal
            )
        }

        habboSession.habboInventory.addItems(userItems)

        if (habboSession.release != "R63A")
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_OK, catalogItem, userItems)
        else
            habboSession.sendHabboResponse(OutgoingR63A.CATALOG_PURCHASE_OK, catalogItem, userItems)

        if (catalogItem.costCredits > 0) {
            habboSession.userInformation.credits -= catalogItem.costCredits * totalAmountToPurchase
        }
        if (catalogItem.costPixels > 0) {
            habboSession.userInformation.activityPointsCurrencies.merge(
                ActivityPointType.PIXELS,
                -(catalogItem.costPixels * totalAmountToPurchase),
                Int::plus
            )
        }
        if (catalogItem.costVip > 0) {
            habboSession.userInformation.activityPointsCurrencies.merge(
                ActivityPointType.DIAMONDS,
                -(catalogItem.costVip * totalAmountToPurchase),
                Int::plus
            )
        }

        // Achievements de LTD
        if (catalogItem.limited) {
            // ACH_LTDPurchaser: comprar LTDs
            HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_LTDPurchaser", 1, accumulate = true)

            // ACH_LTDEarlyBird: comprar LTD nos primeiros momentos (primeiros 10%)
            val earlyBirdThreshold = (catalogItem.limitedTotal * 0.1).toInt()
            if (catalogItem.limitedSells.get() <= earlyBirdThreshold) {
                HabboServer.habboGame.achievementManager.progress(
                    habboSession,
                    "ACH_LTDEarlyBird",
                    1,
                    accumulate = true
                )
            }
        }

        habboSession.updateAllCurrencies()

        if (catalogItem.badge.isNotEmpty()) habboSession.habboBadge.addBadge(catalogItem.badge)

        if (catalogItem.limited) {
            // send new data to everyone logged in
            HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated }.forEach {
                if (it.release != "R63A") it.sendHabboResponse(Outgoing.CATALOG_OFFER, catalogItem)
            }
            // and save new limited sell to database
            CatalogDao.updateLimitedSells(catalogItem)
        }
    }

    fun redeemVoucher(habboSession: HabboSession, voucherCode: String) {
        // todo
        if (voucherCode == "full" && habboSession.hasPermission("acc_catalog_voucher_full")) {
            habboSession.userInformation.credits = Int.MAX_VALUE

            // Injetamos as moedas diretamente no Map do usuário!
            habboSession.userInformation.activityPointsCurrencies[ActivityPointType.PIXELS] = Int.MAX_VALUE
            if (habboSession.userInformation.vip) {
                habboSession.userInformation.activityPointsCurrencies[ActivityPointType.DIAMONDS] = Int.MAX_VALUE
            }

            habboSession.updateAllCurrencies()

            if (habboSession.release != "R63A")
                habboSession.sendHabboResponse(Outgoing.CATALOG_VOUCHER_REDEEMED, "", "")
            else
                habboSession.sendHabboResponse(OutgoingR63A.CATALOG_VOUCHER_REDEEMED, "", "")

            return
        }
        // todo: add a voucher table and redeem
        if (habboSession.release != "R63A")
            habboSession.sendHabboResponse(
                Outgoing.CATALOG_VOUCHER_REDEEM_ERROR,
                CatalogVoucherRedeemErrorResponse.CatalogVoucherRedeemError.NOT_VALID
            )
        else
            habboSession.sendHabboResponse(
                OutgoingR63A.CATALOG_VOUCHER_REDEEM_ERROR,
                CatalogVoucherRedeemErrorResponse.CatalogVoucherRedeemError.NOT_VALID
            )
    }

    private fun totalFreeAmount(amount: Int): Int =
        blackBoxMath1(amount) + blackBoxMath2(amount) + blackBoxMath3(amount)

    private fun blackBoxMath1(amount: Int): Int = (amount / FREE_AMOUNT) * 1

    private fun blackBoxMath2(amount: Int): Int {
        var int1 = 0
        val int2 = amount / FREE_AMOUNT

        if (int2 >= 1) {
            if (amount % FREE_AMOUNT == FREE_AMOUNT - 1) int1++

            int1 += (int2 - 1)
        }

        return int1
    }

    private fun blackBoxMath3(amount: Int): Int {
        var int1 = 0

        intArrayOf(40, 99).forEach {
            if (amount >= it) int1++
        }

        return int1
    }

    private fun getRandomRecyclerLevel(): Int {
        HabboServer.habboConfig.recyclerConfig.odds.entries.filter { it.key != 1 }.filter {
            recyclerRewards.containsKey(it.key)
        }.sortedByDescending { it.key }.forEach {
            if ((1..it.value).random() == it.value) return it.key
        }

        return 1
    }

    fun getRandomRecyclerReward(): Furnishing? {
        val level = getRandomRecyclerLevel()

        return HabboServer.habboGame.itemManager.furnishings[recyclerRewards[level]!!.random()]
    }

    companion object {
        const val FREE_AMOUNT = 6
    }
}