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

package ovh.rwx.habbo.communication.outgoing.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.catalog.CatalogClubOffer
import ovh.rwx.habbo.game.catalog.CatalogPage
import ovh.rwx.habbo.game.user.subscription.ClubType

data class CatalogPageData(
    val catalogPage: CatalogPage,
    val category: String = "NORMAL",
    val chosenOfferId: Int = -1
)

data class CatalogFrontPageItem(
    val position: Int,
    val itemName: String,
    val itemPromoImage: String,
    val type: Int = 0,
    val cataloguePageLocation: String = "",
    val productOfferId: Int = -1,
    val expiration: Int = -1
) {
    fun serialize(response: HabboResponse) {
        response.apply {
            writeInt(position)
            writeUTF(itemName)
            writeUTF(itemPromoImage)
            writeInt(type)
            when (type) {
                0 -> writeUTF(cataloguePageLocation)
                1 -> writeInt(productOfferId)
                2 -> writeUTF(cataloguePageLocation)
                else -> writeUTF("")
            }
            writeInt(expiration)
        }
    }
}

data class CatalogPagePresentation(
    val layoutCode: String,
    val images: List<String>,
    val texts: List<String>,
    val frontPageItems: List<CatalogFrontPageItem> = emptyList()
)

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPageResponse {
    @Response(Outgoing.CATALOG_PAGE)
    @ResponseR63A(OutgoingR63A.CATALOG_PAGE)
    fun response(habboResponse: HabboResponse, data: CatalogPageData) {
        val page = data.catalogPage
        val presentation = resolvePresentation(habboResponse, page)

        habboResponse.apply {
            // Campo #1: pageId
            writeInt(page.id)

            // Campo #2: catalogType (Modern >= 2016)
            if (isVersionAtLeast(2016, 7, 26)) {
                writeUTF(data.category)
            }

            // Campo #3: layoutCode
            writeUTF(presentation.layoutCode)

            // Campo #4: localization (images & texts)
            writeInt(presentation.images.size)
            presentation.images.forEach { writeUTF(it) }

            writeInt(presentation.texts.size)
            presentation.texts.forEach { writeUTF(it) }

            // Campo #5: offers
            val isBuildersClub = isVersionAtLeast(2016, 7, 26) && page.pageLayout.startsWith("builders_club_frontpage")
            if (isBuildersClub) {
                val buildersOffers = HabboServer.habboGame.catalogManager.catalogClubOffers
                    .filter { it.clubType == ClubType.BUILDERS_CLUB }
                    .sortedBy { it.credits }
                writeInt(buildersOffers.size)
                buildersOffers.forEach { serializeBuildersClubOffer(this, it) }
            } else {
                writeInt(page.catalogItems.size)
                page.catalogItems.forEach { serialize(it) }
            }

            // Campo #6: offerId (chosenOfferId) (Adicionado na build 201109301501 / R63A)
            if (isVersionAtLeast(2010, 11, 12)) {
                writeInt(data.chosenOfferId)
            }

            // Campo #7: acceptSeasonCurrencyAsCredits (Modern >= 2012)
            if (isVersionAtLeast(2012, 3, 29)) {
                writeBoolean(false)
            }

            // Campo #8: frontPageItems (Modern >= 2016)
            if (isVersionAtLeast(2016, 7, 26)) {
                writeInt(presentation.frontPageItems.size)
                presentation.frontPageItems.forEach { it.serialize(this) }
            }
        }
    }

    companion object {
        private val NEWLINE_SPLIT_REGEX = Regex("[\\r\\n]+")

        fun resolvePresentation(habboResponse: HabboResponse, page: CatalogPage): CatalogPagePresentation {
            return if (habboResponse.isVersionAtLeast(2016, 7, 26)) {
                resolveModernPresentation(page)
            } else {
                resolveR63APresentation(page)
            }
        }

        fun resolveModernPresentation(page: CatalogPage): CatalogPagePresentation {
            val layout = when (page.pageLayout) {
                "frontpage", "frontpage4" -> "frontpage4"
                "spaces_new" -> "spaces_new"
                "builders_club_frontpage_normal", "builders_club_frontpage" -> "builders_club_frontpage"
                "empty" -> ""
                else -> page.pageLayout
            }

            val images = when (page.pageLayout) {
                "frontpage", "frontpage4", "vip_buy", "pets", "pets2", "pets3",
                "guild_frontpage", "badge_display", "recycler", "recycler_info", "roomads" ->
                    listOf(page.pageHeadline, page.pageTeaser)

                "spaces_new", "trophies", "recycler_prizes", "marketplace_own_items", "marketplace" ->
                    listOf(page.pageHeadline)

                "guild_custom_furni" ->
                    listOf(page.pageHeadline, "", "")

                "builders_club_frontpage_normal", "builders_club_frontpage", "empty" ->
                    emptyList()

                else ->
                    listOf(page.pageHeadline, page.pageTeaser, page.pageSpecial)
            }

            val texts = when (page.pageLayout) {
                "frontpage", "frontpage4" ->
                    listOf(page.pageText1, page.pageText2)

                "vip_buy", "marketplace_own_items", "marketplace", "empty" ->
                    emptyList()

                "builders_club_frontpage_normal", "builders_club_frontpage", "spaces_new",
                "recycler", "recycler_prizes" ->
                    listOf(page.pageText1)

                "pets", "pets2", "pets3" ->
                    listOf(page.pageText1, page.pageText2, page.pageTextDetails, page.pageTextTeaser)

                "guild_frontpage", "guild_custom_furni" ->
                    listOf(page.pageText1, page.pageTextDetails, page.pageText2)

                "badge_display", "recycler_info" ->
                    listOf(page.pageText1, page.pageText2, page.pageTextDetails)

                "trophies", "roomads" ->
                    listOf(page.pageText1, page.pageTextDetails)

                else ->
                    listOf(page.pageText1, page.pageTextDetails, page.pageTextTeaser)
            }

            val frontPageItems = if (page.pageLayout == "frontpage" || page.pageLayout == "frontpage4") {
                parseFrontPageItems(page.pageTextDetails)
            } else {
                emptyList()
            }

            return CatalogPagePresentation(layout, images, texts, frontPageItems)
        }

        fun resolveR63APresentation(page: CatalogPage): CatalogPagePresentation {
            if (page.pageLayout == "frontpage" || page.pageLayout == "frontpage4") {
                val custom = page.customData
                val color1and2 = custom.getOrDefault("color_ctlg_txt1_ctlg_txt2", "#FAF8CC").toString()
                val color3 = custom.getOrDefault("color_ctlg_txt3", "#FAF8CC").toString()

                val texts = listOf(
                    custom.getOrDefault("ctlg_txt1", "").toString(),
                    custom.getOrDefault("ctlg_txt2", "").toString(),
                    custom.getOrDefault("ctlg_txt3", "").toString(),
                    custom.getOrDefault("ctlg_txt4", "").toString(),
                    custom.getOrDefault("ctlg_txt5", "").toString(),
                    custom.getOrDefault("ctlg_txt6", "").toString(),
                    custom.getOrDefault("ctlg_txt3_link", "").toString(),
                    color1and2,
                    color3,
                    custom.getOrDefault("ctlg_txt7", "").toString(),
                    custom.getOrDefault("ctlg_txt7_link", "").toString()
                )
                return CatalogPagePresentation(
                    layoutCode = "frontpage3",
                    images = listOf(page.pageHeadline, page.pageSpecial),
                    texts = texts
                )
            }

            return CatalogPagePresentation(
                layoutCode = page.pageLayout,
                images = listOf(page.pageHeadline, page.pageTeaser, page.pageSpecial),
                texts = listOf(page.pageText1, page.pageTextDetails, page.pageTextTeaser)
            )
        }

        private fun parseFrontPageItems(pageTextDetails: String): List<CatalogFrontPageItem> {
            if (pageTextDetails.isBlank()) return emptyList()
            return pageTextDetails.split('-').mapIndexed { index, section ->
                val lines = section.split(NEWLINE_SPLIT_REGEX).filterNot(String::isEmpty)
                CatalogFrontPageItem(
                    position = index + 1,
                    itemName = lines.getOrElse(0) { "" },
                    itemPromoImage = lines.getOrElse(1) { "" },
                    type = 0,
                    cataloguePageLocation = lines.getOrElse(2) { "" },
                    expiration = -1
                )
            }
        }

        private fun serializeBuildersClubOffer(response: HabboResponse, offer: CatalogClubOffer) {
            response.apply {
                writeInt(offer.itemId)
                writeUTF(offer.name)
                writeBoolean(false) // isRent
                writeInt(offer.credits)
                writeInt(offer.points)
                writeInt(offer.pointsType)
                if (isVersionAtLeast(2024, 1, 22)) {
                    writeInt(0) // priceInSilver
                }
                writeBoolean(true) // giftable
                writeInt(0) // items
                writeInt(0) // club only
                writeBoolean(true)
                writeBoolean(false) // is pet
                writeUTF("")
            }
        }
    }
}