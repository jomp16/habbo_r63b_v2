/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.database.catalog

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.catalog.CatalogClubOffer
import tk.jomp16.habbo.game.catalog.CatalogDeal
import tk.jomp16.habbo.game.catalog.CatalogItem
import tk.jomp16.habbo.game.catalog.CatalogPage
import java.util.concurrent.atomic.AtomicInteger

object CatalogDao {
    fun getCatalogPages() = HabboServer.database {
        select("SELECT * FROM catalog_pages") {
            CatalogPage(
                    it.int("id"),
                    it.int("parent_id"),
                    it.string("caption"),
                    it.string("code_name"),
                    it.int("icon_image"),
                    it.boolean("visible"),
                    it.boolean("enabled"),
                    it.int("min_rank"),
                    it.boolean("club_only"),
                    it.int("order_num"),
                    it.string("page_layout"),
                    it.string("page_headline"),
                    it.string("page_teaser"),
                    it.string("page_special"),
                    it.string("page_text1"),
                    it.string("page_text2"),
                    it.string("page_text_details"),
                    it.string("page_text_teaser"),
                    it.string("page_link_description"),
                    it.string("page_link_pagename")
            )
        }
    }

    fun getCatalogItems() = HabboServer.database {
        select("SELECT * FROM catalog_items") {
            CatalogItem(
                    it.int("id"),
                    it.int("page_id"),
                    it.string("item_name"),
                    it.int("deal_id"),
                    it.string("catalog_name"),
                    it.string("badge"),
                    it.int("cost_credits"),
                    it.int("cost_pixels"),
                    it.int("cost_vip"),
                    it.int("amount"),
                    it.boolean("club_only"),
                    AtomicInteger(it.int("limited_sells")),
                    it.int("limited_stack"),
                    it.boolean("offer_active")
            )
        }
    }

    fun getCatalogClubOffers() = HabboServer.database {
        select("SELECT * FROM catalog_club_offers") {
            CatalogClubOffer(
                    it.int("id"),
                    it.int("item_id"),
                    it.string("name"),
                    it.int("months"),
                    it.int("credits"),
                    it.int("points"),
                    it.int("points_type"),
                    it.boolean("giftable")
            )
        }
    }

    fun getCatalogDeals() = HabboServer.database {
        select("SELECT * FROM catalog_deals") {
            CatalogDeal(
                    it.int("id"),
                    it.string("item_names").split(','),
                    it.string("amounts").split(',').map(String::toInt)
            )
        }
    }
}