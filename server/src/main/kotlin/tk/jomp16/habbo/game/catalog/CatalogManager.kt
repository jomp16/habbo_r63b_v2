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
import tk.jomp16.habbo.database.catalog.CatalogDao
import tk.jomp16.habbo.game.room.tasks.ChatType
import tk.jomp16.habbo.game.user.HabboSession
import java.util.*

class CatalogManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val catalogPages: MutableList<CatalogPage> = ArrayList()
    val catalogItems: MutableList<CatalogItem> = ArrayList()
    val catalogClubOffers: MutableList<CatalogClubOffer> = ArrayList()

    init {
        log.info("Loading catalog...")

        catalogPages += CatalogDao.getCatalogPages()
        catalogItems += CatalogDao.getCatalogItems()
        catalogClubOffers += CatalogDao.getCatalogClubOffers()

        log.info("Loaded {} catalog pages!", catalogPages.size)
        log.info("Loaded {} catalog items!", catalogItems.size)
        log.info("Loaded {} club offers!", catalogClubOffers.size)
    }

    // todo: add gift support
    fun purchase(habboSession: HabboSession, catalogPage: CatalogPage, catalogItem: CatalogItem, extraData: String,
                 amount: Int) {
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

        habboSession.roomUser?.chat((catalogItem.costCredits * totalPrice).toString(), 0, ChatType.CHAT)
    }
}