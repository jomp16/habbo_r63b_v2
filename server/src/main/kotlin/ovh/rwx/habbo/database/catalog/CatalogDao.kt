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

package ovh.rwx.habbo.database.catalog

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ovh.rwx.habbo.game.catalog.CatalogClubOffer
import ovh.rwx.habbo.game.catalog.CatalogDeal
import ovh.rwx.habbo.game.catalog.CatalogItem
import ovh.rwx.habbo.game.catalog.CatalogPage
import ovh.rwx.habbo.game.user.subscription.ClubType
import java.util.concurrent.atomic.AtomicInteger
import ovh.rwx.habbo.database.*

private val jsonMapper = jacksonObjectMapper()

object CatalogDao {
    fun getCatalogPages(): List<CatalogPage> = db {
        query<CatalogPageDto>("SELECT * FROM `catalog_pages` WHERE `id` != -1 AND `id` != -2")
            .map { it.toDomain() }
    }

    fun getCatalogItems(): List<CatalogItem> = db {
        query<CatalogItemDto>("SELECT * FROM `catalog_items`")
            .map { it.toDomain() }
    }

    fun getCatalogClubOffers(): List<CatalogClubOffer> = db {
        query<CatalogClubOfferDto>("SELECT * FROM `catalog_club_offers`")
            .map { it.toDomain() }
    }

    fun getCatalogDeals(): List<CatalogDeal> = db {
        query<CatalogDealDto>("SELECT * FROM `catalog_deals`")
            .map { it.toDomain() }
    }

    fun getRecyclerRewards(): List<Pair<Int, String>> = db {
        query<RecyclerRewardDto>("SELECT * FROM `catalog_recycler`")
            .map { it.level to it.itemName.trim() }
    }

    fun updateLimitedSells(catalogItem: CatalogItem) {
        db {
            update(
                "UPDATE `catalog_items` SET `limited_sells` = :limited_sells WHERE `id` = :id",
                mapOf(
                    "limited_sells" to catalogItem.limitedSells.get(),
                    "id" to catalogItem.id
                )
            )
        }
    }
}

data class CatalogPageDto(
    val id: Int,
    val parentId: Int,
    val name: String,
    val codeName: String,
    val iconImage: Int,
    val visible: Boolean,
    val enabled: Boolean,
    val minRank: Int,
    val clubOnly: Boolean,
    val orderNum: Int,
    val pageLayout: String,
    val pageHeadline: String,
    val pageTeaser: String,
    val pageSpecial: String,
    val pageText1: String,
    val pageText2: String,
    val pageTextDetails: String,
    val pageTextTeaser: String,
    val pageLinkDescription: String,
    val pageLinkPagename: String,
    val customData: String
) {
    fun toDomain(): CatalogPage = CatalogPage(
        id, parentId, name.trim(), codeName.trim(), iconImage, visible, enabled, minRank, clubOnly, orderNum,
        pageLayout.trim(), pageHeadline.trim(), pageTeaser.trim(), pageSpecial.trim(), pageText1.trim(),
        pageText2.trim(), pageTextDetails.trim(), pageTextTeaser.trim(), pageLinkDescription.trim(),
        pageLinkPagename.trim(), jsonMapper.readValue(customData)
    )
}

data class CatalogItemDto(
    val id: Int,
    val pageId: Int,
    val itemName: String,
    val orderNum: Int,
    val dealId: Int? = 0,
    val catalogName: String,
    val badge: String,
    val costCredits: Int,
    val costPixels: Int,
    val costVip: Int,
    val amount: Int,
    val clubOnly: Boolean,
    val limitedSells: Int = 0,
    val limitedStack: Int = 0,
    val offerActive: Boolean,
    val extraData: String
) {
    fun toDomain(): CatalogItem = CatalogItem(
        id, pageId, itemName.trim(), orderNum, dealId ?: 0, catalogName.trim(), badge.trim(),
        costCredits, costPixels, costVip, amount, clubOnly, AtomicInteger(limitedSells),
        limitedStack, offerActive, jsonMapper.readValue(extraData)
    )
}

data class CatalogClubOfferDto(
    val id: Int,
    val itemId: Int,
    val name: String,
    val clubType: String,
    val months: Int,
    val credits: Int,
    val points: Int,
    val pointsType: Int,
    val giftable: Boolean,
    val itemsLimit: Int
) {
    fun toDomain() = CatalogClubOffer(
        id, itemId, name.trim(), ClubType.valueOf(clubType.uppercase()),
        months, credits, points, pointsType, giftable, itemsLimit
    )
}

data class CatalogDealDto(
    val id: Int,
    val itemName: String,
    val amount: String
) {
    fun toDomain() = CatalogDeal(
        id,
        itemName.split(',').map(String::trim),
        amount.split(',').map(String::toInt)
    )
}

data class RecyclerRewardDto(val level: Int, val itemName: String)
