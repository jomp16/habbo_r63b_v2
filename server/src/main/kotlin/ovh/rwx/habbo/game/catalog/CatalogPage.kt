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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.isVersionBetween

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
    val pageLinkPagename: String,
    val customData: Map<String, Any>
) : IHabboResponseSerialize {
    val catalogItems: List<CatalogItem>
        get() {
            val indexed = HabboServer.habboGame.catalogManager.getItemsForPage(id)
            if (indexed.isNotEmpty()) return indexed
            return HabboServer.habboGame.catalogManager.catalogItems.filter { it.pageId == id }
                .sortedBy { it.id }
                .sortedBy { it.orderNum }
        }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            val rank = params[0] as Int
            val club = params[1] as Boolean

            // Evolução do CatalogIndexMessageEvent (nó = NodeData):
            //  - 2011-08-17..2012-02-08: bool + int color + int icon + int pageId + 1 string + filhos
            //  - 2012-02-08..2014-01-30: idem, porém 2 strings (pageName + localization)
            //  - >= 2014-01-30: bool + int icon + int pageId + 2 strings + LOOP[Int] itemIds + filhos
            writeBoolean(visible)
            if (isVersionBetween(2011, 8, 17, 2014, 1, 30)) {
                writeInt(0) // icon color (era 2011-08-17..2014-01-30)
            }
            writeInt(iconImage)
            writeInt(if (isVersionAtLeast(2012, 2, 8) && pageLayout == "category") -1 else id)

            val displayName = if (HabboServer.habboConfig.catalogConfig.showHowManyItemsInTitle && pageLayout != "category" && (parentId != -1 && parentId != -2)) {
                "$name (${catalogItems.size})"
            } else {
                name
            }
            if (isVersionAtLeast(2012, 2, 8)) {
                writeUTF(codename)
                writeUTF(displayName)
            } else {
                writeUTF(displayName)
            }

            if (isVersionAtLeast(2014, 1, 30)) {
                writeInt(catalogItems.size)
                catalogItems.forEach {
                    writeInt(it.id)
                }
            }
            // Catálogo antigo (<2014-01-30, sem abas): root achatado — pastas nível 1 viram cópias com parentId=-1
            val childCatalogPages: List<CatalogPage> =
                if (isVersionBefore(2014, 1, 30) && id == -1) {
                    val level1Nodes = HabboServer.habboGame.catalogManager.catalogPages.filter {
                        it.parentId == -1 && it.enabled && it.visible && it.minRank <= rank && if (it.clubOnly) club else true
                    }
                    val level1Ids = level1Nodes.map { it.id }
                    val level2Nodes = HabboServer.habboGame.catalogManager.catalogPages.filter { page ->
                        level1Ids.contains(page.parentId) && page.enabled && page.visible && page.minRank <= rank && if (page.clubOnly) club else true
                    }
                    val parentIdsThatAreFolders = level2Nodes.map { it.parentId }.distinct()
                    val orphansLevel1 = level1Nodes.filter { !parentIdsThatAreFolders.contains(it.id) }
                    (orphansLevel1 + level2Nodes)
                        .sortedWith(compareBy({ if (it.parentId == -1) it.id else it.parentId }, { it.orderNum }))
                        .map { if (it.parentId == -1) it else it.copy(parentId = -1) }
                } else {
                    HabboServer.habboGame.catalogManager.catalogPages.filter { it.parentId == id && it.enabled && it.visible && it.minRank <= rank && if (it.clubOnly) club else true }
                        .sortedBy { it.orderNum }
                }
            writeInt(childCatalogPages.size)

            childCatalogPages.forEach {
                it.serializeHabboResponse(habboResponse, rank, club)
            }

            if (id == -1 || id == -2) {
                // newAdditionsAvailable nasce em 2011-06-13; catalogType em 2014-01-30
                if (isVersionAtLeast(2011, 6, 13)) {
                    writeBoolean(false)
                }
                if (isVersionAtLeast(2014, 1, 30)) {
                    writeUTF(if (id == -1) "NORMAL" else "BUILDERS_CLUB")
                }
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        // Unificado: R63A (ancorado em 2011-09-30 no decoder) cai nos predicados da era preshuffle
        serializeHabboResponse(habboResponse, *params)
    }

    companion object {
        fun createRoot(id: Int, codename: String = "root"): CatalogPage = CatalogPage(
            id = id,
            parentId = 0,
            name = "",
            codename = codename,
            iconImage = 0,
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
    }
}