/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize

data class CatalogPage(
        val id: Int,
        private val parentId: Int,
        val name: String,
        private val codename: String,
        private val iconImage: Int,
        val visible: Boolean,
        val enabled: Boolean,
        val minRank: Int,
        val clubOnly: Boolean,
        private val orderNum: Int,
        val pageLayout: String,
        val pageHeadline: String,
        val pageTeaser: String,
        val pageSpecial: String,
        val pageText1: String,
        val pageText2: String,
        val pageTextDetails: String,
        val pageTextTeaser: String,
        val pageLinkDescription: String,
        val pageLinkPagename: String
) : IHabboResponseSerialize {
    val catalogItems: List<CatalogItem>
        get() = HabboServer.habboGame.catalogManager.catalogItems.filter { it.pageId == id }.sortedBy { it.id }.sortedBy { it.orderNum }
    private val offerItems: List<CatalogItem>
        get() = catalogItems.filter { it.offerId != -1 }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            val rank = params[0] as Int
            val club = params[1] as Boolean

            writeBoolean(visible)
            writeInt(iconImage)
            writeInt(if (pageLayout == "category") -1 else id)
            writeUTF(codename)

            if (HabboServer.habboConfig.catalogConfig.showHowManyItemsInTitle && pageLayout != "category" && parentId != -1) writeUTF("$name (${catalogItems.size})")
            else writeUTF(name)

            writeInt(offerItems.size)
            offerItems.forEach {
                writeInt(it.offerId)
            }
            val childCatalogPages = HabboServer.habboGame.catalogManager.catalogPages.filter { it.parentId == id && it.enabled && it.visible && it.minRank <= rank && if (it.clubOnly) club else true }.sortedBy { it.orderNum }
            writeInt(childCatalogPages.size)

            childCatalogPages.forEach {
                it.serializeHabboResponse(habboResponse, rank, club)
            }

            if (id == -1 || id == -2) {
                writeBoolean(false)
                writeUTF(if (id == -1) "NORMAL" else "BUILDERS_CLUB")
            }
        }
    }
}