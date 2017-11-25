/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogPurchaseNotAllowedErrorResponse
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogVoucherRedeemErrorResponse
import ovh.rwx.habbo.database.catalog.CatalogDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.batchInsertAndGetGeneratedKeys
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey
import ovh.rwx.habbo.kotlin.random
import ovh.rwx.habbo.util.Utils
import java.security.SecureRandom
import java.util.*

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
        catalogPages += CatalogPage(-1, 0, "", "root", 0, true, true, 1, false, 1, "", "", "", "", "", "", "", "", "", "")
        // catalog builders
        catalogPages += CatalogPage(-2, 0, "", "root", 0, true, true, 1, false, 1, "", "", "", "", "", "", "", "", "", "")

        catalogPages += CatalogDao.getCatalogPages()
        catalogItems += CatalogDao.getCatalogItems()
        catalogClubOffers += CatalogDao.getCatalogClubOffers()
        catalogDeals += CatalogDao.getCatalogDeals()
        recyclerRewards += CatalogDao.getRecyclerRewards().groupBy { it.first }.mapValues { it.value.map { it.second } }

        log.info("Loaded {} catalog pages!", catalogPages.size - 2)
        log.info("Loaded {} catalog items!", catalogItems.size)
        log.info("Loaded {} club offers!", catalogClubOffers.size)
        log.info("Loaded {} catalog deals!", catalogDeals.size)
        log.info("Loaded {} recycler levels and {} recycler rewards!", recyclerRewards.size, recyclerRewards.values.sumBy { it.size })
    }

    fun purchaseHC(habboSession: HabboSession, itemId: Int) {
        if (!catalogClubOffers.any { it.itemId == itemId }) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)

            return
        }
        val clubOffer = catalogClubOffers.find { it.itemId == itemId }

        if (clubOffer == null) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)

            return
        }

        if (habboSession.userInformation.credits < clubOffer.credits ||
                (if (clubOffer.pointsType == 0) habboSession.userInformation.pixels < clubOffer.points
                else habboSession.userInformation.vipPoints < clubOffer.points)) return

        habboSession.userInformation.credits -= clubOffer.credits

        when (clubOffer.pointsType) {
            0 -> habboSession.userInformation.pixels -= clubOffer.points
            else -> habboSession.userInformation.vipPoints -= clubOffer.points
        }

        habboSession.habboSubscription.addOrExtend(clubOffer.months)

        habboSession.updateAllCurrencies()

        return
    }

    // todo: add gift support
    fun purchase(habboSession: HabboSession, catalogItem: CatalogItem, extraData: String, amount: Int) {
        if (!catalogItem.offerActive || catalogItem.clubOnly && !habboSession.habboSubscription.validUserSubscription) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_NOT_ALLOWED_ERROR, CatalogPurchaseNotAllowedErrorResponse.CatalogPurchaseNotAllowedError.NOT_HC)

            return
        }
        val totalAmountToPurchase = amount - totalFreeAmount(amount)

        if (catalogItem.costCredits > 0 && habboSession.userInformation.credits < catalogItem.costCredits * totalAmountToPurchase
                || catalogItem.costPixels > 0 && habboSession.userInformation.pixels < catalogItem.costPixels * totalAmountToPurchase
                || catalogItem.costVip > 0 && habboSession.userInformation.vipPoints < catalogItem.costVip * totalAmountToPurchase) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)

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
                HabboServer.habboGame.itemManager.correctExtradataCatalog(habboSession, extraData, furnishing)?.let { extraData1 ->
                    (0 until catalogItem.deal!!.amounts[i]).forEach {
                        furnishingToPurchase += CatalogPurchaseData(furnishing, extraData1, if (catalogItem.limited) catalogItem.limitedSells.incrementAndGet() else 0)

                        if (furnishing.interactionType == InteractionType.TELEPORT) furnishingToPurchase += furnishingToPurchase.last()
                    }
                }
            }
        } else {
            HabboServer.habboGame.itemManager.correctExtradataCatalog(habboSession, extraData, catalogItem.furnishing)?.let { extraData1 ->
                (0 until catalogItem.amount * amount).forEach {
                    furnishingToPurchase += CatalogPurchaseData(catalogItem.furnishing, extraData1, if (catalogItem.limited) catalogItem.limitedSells.incrementAndGet() else 0)

                    if (catalogItem.furnishing.interactionType == InteractionType.TELEPORT) furnishingToPurchase += furnishingToPurchase.last()
                }
            }
        }
        val userItems = ItemDao.addItems(habboSession.userInformation.id, furnishingToPurchase.map { it.furnishing }, furnishingToPurchase.map { it.extraData })

        furnishingToPurchase.filter { it.limitedNumber > 0 }.forEach {
            ItemDao.addLimitedItem(userItems[furnishingToPurchase.indexOf(it)].id, it.limitedNumber, catalogItem.limitedTotal)
        }
        // todo: move queries to ItemDao
        HabboServer.database {
            val copyUserItems = ArrayList(userItems)

            userItems.forEach { userItem ->
                if (!copyUserItems.contains(userItem)) return@forEach

                when {
                    userItem.furnishing.interactionType == InteractionType.TELEPORT -> {
                        val teleporterItem = copyUserItems.find { it.furnishing == userItem.furnishing && it != userItem } ?: return@forEach

                        copyUserItems.remove(teleporterItem)

                        batchInsertAndGetGeneratedKeys("INSERT INTO `items_teleport` (`teleport_one_id`, `teleport_two_id`) VALUES (:teleport_one_id, :teleport_two_id)",
                                listOf(
                                        mapOf(
                                                "teleport_one_id" to userItem.id,
                                                "teleport_two_id" to teleporterItem.id
                                        ),
                                        mapOf(
                                                "teleport_two_id" to userItem.id,
                                                "teleport_one_id" to teleporterItem.id
                                        )
                                )
                        )

                        HabboServer.habboGame.itemManager.teleportLinks.put(userItem.id, teleporterItem.id)
                        HabboServer.habboGame.itemManager.roomTeleportLinks.put(userItem.id, 0)
                        HabboServer.habboGame.itemManager.teleportLinks.put(teleporterItem.id, userItem.id)
                        HabboServer.habboGame.itemManager.roomTeleportLinks.put(teleporterItem.id, 0)
                    }
                    userItem.furnishing.interactionType.name.startsWith("WIRED_") -> {
                        insertAndGetGeneratedKey("INSERT INTO `items_wired` (`item_id`, `delay`, `items`, `message`, `options`, `extradata`) VALUES (:item_id, :delay, :items, :message, :options, :extradata)",
                                mapOf(
                                        "item_id" to userItem.id,
                                        "delay" to 0,
                                        "items" to "",
                                        "message" to "",
                                        "options" to "",
                                        "extradata" to ""
                                )
                        )
                    }
                    userItem.furnishing.interactionType == InteractionType.DIMMER -> {
                        insertAndGetGeneratedKey("INSERT INTO `items_dimmer` (`item_id`, `enabled`, `current_preset`, `preset_one`, `preset_two`, `preset_three`) VALUES (:item_id, :enabled, :current_preset, :preset_one, :preset_two, :preset_three)",
                                mapOf(
                                        "item_id" to userItem.id,
                                        "enabled" to false,
                                        "current_preset" to 1,
                                        "preset_one" to "#000000,255,0",
                                        "preset_two" to "#000000,255,0",
                                        "preset_three" to "#000000,255,0"
                                )
                        )
                    }
                }
            }
        }

        habboSession.habboInventory.addItems(userItems)
        habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_OK, catalogItem, userItems)

        if (catalogItem.costCredits > 0) habboSession.userInformation.credits -= catalogItem.costCredits * totalAmountToPurchase
        if (catalogItem.costPixels > 0) habboSession.userInformation.pixels -= catalogItem.costPixels * totalAmountToPurchase
        if (catalogItem.costVip > 0) habboSession.userInformation.vipPoints -= catalogItem.costVip * totalAmountToPurchase

        habboSession.updateAllCurrencies()

        if (!catalogItem.badge.isEmpty()) habboSession.habboBadge.addBadge(catalogItem.badge)

        if (catalogItem.limited) {
            // send new data to everyone logged in
            HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated }.forEach {
                it.sendHabboResponse(Outgoing.CATALOG_OFFER, catalogItem)
            }
            // and save new limited sell to database
            CatalogDao.updateLimitedSells(catalogItem)
        }
    }

    fun redeemVoucher(habboSession: HabboSession, voucherCode: String) {
        // todo
        if (voucherCode == "full" && habboSession.hasPermission("acc_catalog_voucher_full")) {
            habboSession.userInformation.credits = Int.MAX_VALUE
            habboSession.userInformation.pixels = Int.MAX_VALUE
            if (habboSession.userInformation.vip) habboSession.userInformation.vipPoints = Int.MAX_VALUE

            habboSession.updateAllCurrencies()

            habboSession.sendHabboResponse(Outgoing.CATALOG_VOUCHER_REDEEMED, "", "")

            return
        }
        // todo: add a voucher table and redeem
        habboSession.sendHabboResponse(Outgoing.CATALOG_VOUCHER_REDEEM_ERROR, CatalogVoucherRedeemErrorResponse.CatalogVoucherRedeemError.NOT_VALID)
    }

    private fun totalFreeAmount(amount: Int): Int = blackBoxMath1(amount) + blackBoxMath2(amount) + blackBoxMath3(amount)

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

    private fun getRandomRecyclerLevel(random: Random): Int {
        HabboServer.habboConfig.recyclerConfig.odds.entries.filter { it.key != 1 }.filter {
            recyclerRewards.containsKey(it.key)
        }.sortedByDescending { it.key }.forEach {
            if (Utils.randInt(1..it.value, random) == it.value) return it.key
        }

        return 1
    }

    fun getRandomRecyclerReward(): Furnishing? {
        val random = SecureRandom()
        val level = getRandomRecyclerLevel(random)

        return HabboServer.habboGame.itemManager.furnishings[recyclerRewards[level]!!.random(random)]
    }

    companion object {
        const val FREE_AMOUNT = 6
    }
}