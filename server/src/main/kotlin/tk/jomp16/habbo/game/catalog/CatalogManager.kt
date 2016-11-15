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

package tk.jomp16.habbo.game.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.catalog.CatalogDao
import tk.jomp16.habbo.database.item.ItemDao
import tk.jomp16.habbo.game.item.InteractionType
import tk.jomp16.habbo.game.item.user.UserItem
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.kotlin.batchInsertAndGetGeneratedKeys
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey
import java.util.*

class CatalogManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val catalogPages: MutableList<CatalogPage> = ArrayList()
    val catalogItems: MutableList<CatalogItem> = ArrayList()
    val catalogClubOffers: MutableList<CatalogClubOffer> = ArrayList()
    val catalogDeals: MutableList<CatalogDeal> = ArrayList()

    init {
        log.info("Loading catalog...")

        // catalog root
        catalogPages += CatalogPage(-1, 0, "", "root", 0, true, true, 1, false, 1, "", "", "", "", "", "", "", "", "", "")
        // catalog builders
        catalogPages += CatalogPage(-2, 0, "", "root", 0, true, true, 1, false, 1, "", "", "", "", "", "", "", "", "", "")

        catalogPages += CatalogDao.getCatalogPages()
        catalogItems += CatalogDao.getCatalogItems()
        catalogClubOffers += CatalogDao.getCatalogClubOffers()
        catalogDeals += CatalogDao.getCatalogDeals()

        log.info("Loaded {} catalog pages!", catalogPages.size - 2)
        log.info("Loaded {} catalog items!", catalogItems.size)
        log.info("Loaded {} club offers!", catalogClubOffers.size)
        log.info("Loaded {} catalog deals!", catalogDeals.size)
    }

    // todo: add gift support
    fun purchase(habboSession: HabboSession, catalogPage: CatalogPage, catalogItemId: Int, extraData: String, amount: Int) {
        /*val amountPurchase = if (catalogItem.amount > 1) catalogItem.amount else amount
        val totalCreditsCost = if (amount > 1) (catalogItem.costCredits * amount - Math.floor(
                (amount / 6).toDouble()) * catalogItem.costCredits).toInt() else catalogItem.costCredits

        val totalPixelsCost = if (amount > 1) (catalogItem.costPixels * amount - Math.floor(
                (amount / 6).toDouble()) * catalogItem.costPixels).toInt() else catalogItem.costPixels

        val totalVipCost = if (amount > 1) (catalogItem.costVip * amount - Math.floor(
                (amount / 6).toDouble()) * catalogItem.costVip).toInt() else catalogItem.costVip

        println(amountPurchase)
        println(totalCreditsCost)
        println(totalPixelsCost)
        println(totalVipCost)*/

        var totalPrice = amount

        if (amount >= 6) {
            // numbers like 11, 17 does not show correct total price discount
            // my workaround was checking if amount is an odd prime number, if yes, subtract one
            // But numbers like 39, 40, 41, 42, 43, 95, 99 also don't work properly
            // totalPrice -= Math.floor((amount.toDouble() / 6) * 2).toInt() - if (amount != 100 && (amount >= 40 || amount % 6 == 0)) 1 else if (amount < 40 && amount % 2 != 0) 2 else 0
            // totalPrice -= Math.floor((amount.toDouble() / 6) * 2).toInt() - if (amount == 40 || amount == 99) 2 else if (amount % 6 == 0) 1 else 0
            totalPrice -= Math.ceil((amount.toDouble() / 6) * 2).toInt() - 1
        }

        if (!catalogPage.enabled || !catalogPage.visible || habboSession.userInformation.rank < catalogPage.minRank || catalogPage.clubOnly && !habboSession.habboSubscription.validUserSubscription) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)
            return
        }

        if (catalogPage.pageLayout == "vip_buy") {
            // Habbo Club

            if (!catalogClubOffers.any { it.itemId == catalogItemId }) {
                habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)

                return
            }

            val clubOffer = catalogClubOffers.find { it.itemId == catalogItemId }

            if (clubOffer == null) {
                habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)

                return
            }

            if (habboSession.userInformation.credits < clubOffer.credits) return

            habboSession.userInformation.credits -= clubOffer.credits

            habboSession.habboSubscription.addOrExtend(clubOffer.months)

            habboSession.updateAllCurrencies()

            return
        }

        val catalogItem = catalogPage.catalogItems.find { it.id == catalogItemId }

        if (catalogItem == null || !catalogItem.offerActive || catalogItem.clubOnly && !habboSession.habboSubscription.validUserSubscription
                || catalogItem.limited && catalogItem.limitedSells.get() >= catalogItem.limitedStack) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)

            return
        }

        if (catalogItem.costCredits > 0 && habboSession.userInformation.credits < catalogItem.costCredits * totalPrice
                || catalogItem.costPixels > 0 && habboSession.userInformation.pixels < catalogItem.costPixels * totalPrice
                || catalogItem.costVip > 0 && habboSession.userInformation.vipPoints < catalogItem.costVip * totalPrice) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)

            return
        }

        // todo
        val furnishingToPurchase: MutableList<CatalogPurchaseData> = ArrayList()

        if (catalogItem.dealId > 0) {
            catalogItem.deal!!.furnishings.forEachIndexed { i, furnishing ->
                HabboServer.habboGame.itemManager.correctExtradataCatalog(habboSession, extraData, furnishing)?.let { extraData1 ->
                    (0..catalogItem.deal!!.amounts[i] - 1).forEach {
                        furnishingToPurchase += CatalogPurchaseData(furnishing, extraData1, if (catalogItem.limited) catalogItem.limitedSells.andIncrement else 0)

                        if (furnishing.interactionType == InteractionType.TELEPORT) furnishingToPurchase += furnishingToPurchase.last()
                    }
                }
            }
        } else {
            HabboServer.habboGame.itemManager.correctExtradataCatalog(habboSession, extraData, catalogItem.furnishing!!)?.let { extraData1 ->
                (0..catalogItem.amount - 1).forEach {
                    furnishingToPurchase += CatalogPurchaseData(catalogItem.furnishing!!, extraData1, if (catalogItem.limited) catalogItem.limitedSells.andIncrement else 0)

                    if (catalogItem.furnishing!!.interactionType == InteractionType.TELEPORT) furnishingToPurchase += furnishingToPurchase.last()
                }
            }
        }

        HabboServer.database {
            val ids = batchInsertAndGetGeneratedKeys("INSERT INTO items (user_id, base_item, extra_data, limited_id, wall_pos) VALUES (:user_id, :base_item, :extra_data, :limited_id, :wall_pos)",
                    furnishingToPurchase.map {
                        mapOf(
                                "user_id" to habboSession.userInformation.id,
                                "base_item" to it.furnishing.itemName,
                                "extra_data" to it.extraData,
                                "limited_id" to it.limitedId,
                                "wall_pos" to ""
                        )
                    }
            )

            if (furnishingToPurchase.filter { it.limitedId > 0 }.isNotEmpty()) {
                batchInsertAndGetGeneratedKeys("INSERT INTO items_limited (limited_num, limited_total) VALUES (:limited_num, :limited_total)",
                        furnishingToPurchase.filter { it.limitedId > 0 }.map {
                            mapOf(
                                    "limited_num" to it.limitedId,
                                    "limited_total" to catalogItem.limitedStack
                            )
                        }
                )
            }

            val userItems = ids.mapIndexed { i, itemId ->
                UserItem(
                        itemId,
                        habboSession.userInformation.id,
                        furnishingToPurchase[i].furnishing.itemName,
                        furnishingToPurchase[i].extraData,
                        furnishingToPurchase[i].limitedId,
                        ItemDao.getLimitedData(furnishingToPurchase[i].limitedId)
                )
            }

            val copyUserItems = userItems.toMutableList()

            habboSession.habboInventory.addItems(userItems)

            userItems.forEach { userItem ->
                if (!copyUserItems.contains(userItem)) return@forEach

                when {
                    userItem.furnishing.interactionType == InteractionType.TELEPORT -> {
                        val teleporterItem1 = userItems.find { it.furnishing == userItem.furnishing } ?: return@forEach

                        copyUserItems.remove(teleporterItem1)

                        batchInsertAndGetGeneratedKeys("INSERT INTO items_teleport (teleport_one_id, teleport_two_id) VALUES (:teleport_one_id, :teleport_two_id)",
                                listOf(
                                        mapOf(
                                                "teleport_one_id" to userItem.id,
                                                "teleport_two_id" to teleporterItem1.id
                                        ),
                                        mapOf(
                                                "teleport_two_id" to userItem.id,
                                                "teleport_one_id" to teleporterItem1.id
                                        )
                                )
                        )
                    }
                    userItem.furnishing.interactionType.name.startsWith("WIRED_")   -> {
                        insertAndGetGeneratedKey("INSERT INTO items_wired (item_id, extra1, extra2, extra3, extra4, extra5) VALUES (:item_id, :extra1, :extra2, :extra3, :extra4, :extra5)",
                                mapOf(
                                        "item_id" to userItem.id,
                                        "extra1" to "",
                                        "extra2" to "",
                                        "extra3" to "",
                                        "extra4" to "",
                                        "extra5" to ""
                                )
                        )
                    }
                    userItem.furnishing.interactionType == InteractionType.DIMMER   -> {
                        insertAndGetGeneratedKey("INSERT INTO items_dimmer (item_id, enabled, current_preset, preset_one, preset_two, preset_three) VALUES (:item_id, :enabled, :current_preset, :preset_one, :preset_two, :preset_three)",
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

            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_OK, catalogItem, furnishingToPurchase.map { it.furnishing })
        }

        if (catalogItem.costCredits > 0) habboSession.userInformation.credits -= catalogItem.costCredits * totalPrice
        if (catalogItem.costPixels > 0) habboSession.userInformation.pixels -= catalogItem.costPixels * totalPrice
        if (catalogItem.costVip > 0) habboSession.userInformation.vipPoints -= catalogItem.costVip * totalPrice

        habboSession.updateAllCurrencies()

        if (!catalogItem.badge.isEmpty()) {
            habboSession.habboBadge.addBadge(catalogItem.badge)
        }
    }

    fun redeemVoucher(habboSession: HabboSession, voucherCode: String) {
        // todo
        if (voucherCode == "full" && habboSession.hasPermission("cmd_catalog_voucher_full")) {
            habboSession.userInformation.credits = Int.MAX_VALUE
            habboSession.userInformation.pixels = Int.MAX_VALUE
            if (habboSession.userInformation.vip) habboSession.userInformation.vipPoints = Int.MAX_VALUE

            habboSession.updateAllCurrencies()

            habboSession.sendHabboResponse(Outgoing.CATALOG_VOUCHER_REDEEMED, "", "")

            return
        }

        // todo: add a voucher table and redeem
        habboSession.sendHabboResponse(Outgoing.CATALOG_VOUCHER_REDEEM_ERROR, 0)
    }
}