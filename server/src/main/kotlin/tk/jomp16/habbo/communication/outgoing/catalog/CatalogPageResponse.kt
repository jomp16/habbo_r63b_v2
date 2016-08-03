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

package tk.jomp16.habbo.communication.outgoing.catalog

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.catalog.CatalogPage

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPageResponse {
    @Response(Outgoing.CATALOG_PAGE)
    fun response(habboResponse: HabboResponse, catalogPage: CatalogPage) {
        habboResponse.apply {
            writeInt(catalogPage.id)

            when (catalogPage.pageLayout) {
                "frontpage"               -> {
                    writeUTF("NORMAL")
                    writeUTF("frontpage4")
                    writeInt(2)
                    writeUTF(catalogPage.pageHeadline)
                    writeUTF(catalogPage.pageTeaser)
                    writeInt(2)
                    writeUTF(catalogPage.pageText1)
                    writeUTF(catalogPage.pageText2)
                    writeInt(0)
                    writeInt(-1)
                    writeBoolean(false)

                    // todo: figure out dis to add on database
                    writeInt(4) // count

                    writeInt(1) // id
                    writeUTF("New Rare Jellyfish Lamp!") // name
                    writeUTF("") // image
                    writeInt(0)
                    writeUTF("boat16jelly") // page link?
                    writeInt(-1) // page id?

                    writeInt(2)
                    writeUTF("Cozy Garden Bundle (ALL new furni)")
                    writeUTF("catalogue/feature_cata_hort_gardenbundle.png")
                    writeInt(0)
                    writeUTF("room_bundles_mobile")
                    writeInt(-1)

                    writeInt(3)
                    writeUTF("NEW: Karaoke Bar Bundle")
                    writeUTF("catalogue/feature_cata_hort_barbundle.png")
                    writeInt(0)
                    writeUTF("room_bundles_mobile")
                    writeInt(-1)

                    writeInt(4)
                    writeUTF("Classic Jetset Furni!")
                    writeUTF("catalogue/feature_cata_hort_partyboat16_b.png")
                    writeInt(0)
                    writeUTF("jetset")
                    writeInt(-1)
                }
                "vip_buy"                 -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(2)
                    writeUTF(catalogPage.pageHeadline)
                    writeUTF(catalogPage.pageTeaser)
                    writeInt(0)
                }
                "builders_club_frontpage_normal",
                "builders_club_frontpage" -> {
                    writeUTF(
                            if (catalogPage.pageLayout == "builders_club_frontpage_normal") "NORMAL" else "BUILDERS_CLUB")
                    writeUTF("builders_club_frontpage")
                    writeInt(0)
                    writeInt(1)
                    writeUTF(catalogPage.pageHeadline)
                    writeInt(3)
                    writeInt(8554)
                    writeUTF("builders_club_1_month")
                    writeUTF("")
                    writeInt(2560000)
                    writeInt(2560000)
                    writeInt(1024)
                    writeInt(0)
                    writeInt(0)
                    writeBoolean(false)
                    writeInt(8606)
                    writeUTF("builders_club_14_days")
                    writeUTF("")
                    writeInt(2560000)
                    writeInt(2560000)
                    writeInt(1024)
                    writeInt(0)
                    writeInt(0)
                    writeBoolean(false)
                    writeInt(8710)
                    writeUTF("builders_club_31_days")
                    writeUTF("")
                    writeInt(2560000)
                    writeInt(2560000)
                    writeInt(1024)
                    writeInt(0)
                    writeInt(0)
                    writeBoolean(false)
                }
                "pets",
                "pets2",
                "pets3"                   -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(2)
                    writeUTF(catalogPage.pageHeadline)
                    writeUTF(catalogPage.pageTeaser)
                    writeInt(4)
                    writeUTF(catalogPage.pageText1)
                    writeUTF(catalogPage.pageText2)
                    writeUTF(catalogPage.pageTextDetails)
                    writeUTF(catalogPage.pageTextTeaser)
                }
                "spaces_new"              -> {
                    writeUTF("NORMAL")
                    writeUTF("spaces_new")
                    writeInt(1)
                    writeUTF(catalogPage.pageHeadline)
                    writeInt(1)
                    writeUTF(catalogPage.pageText1)
                }
                "guild_frontpage"         -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(2)
                    writeUTF(catalogPage.pageHeadline)
                    writeUTF(catalogPage.pageTeaser)
                    writeInt(3)
                    writeUTF(catalogPage.pageText1)
                    writeUTF(catalogPage.pageTextDetails)
                    writeUTF(catalogPage.pageText2)
                }
                "guild_custom_furni"      -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(3)
                    writeUTF(catalogPage.pageHeadline)
                    writeUTF("")
                    writeUTF("")
                    writeInt(3)
                    writeUTF(catalogPage.pageText1)
                    writeUTF(catalogPage.pageTextDetails)
                    writeUTF(catalogPage.pageText2)
                }
                "badge_display"           -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(2)
                    writeUTF(catalogPage.pageHeadline)
                    writeUTF(catalogPage.pageTeaser)
                    writeInt(3)
                    writeUTF(catalogPage.pageText1)
                    writeUTF(catalogPage.pageText2)
                    writeUTF(catalogPage.pageTextDetails)
                }
                "trophies"                -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(1)
                    writeUTF(catalogPage.pageHeadline)
                    writeInt(2)
                    writeUTF(catalogPage.pageText1)
                    writeUTF(catalogPage.pageTextDetails)
                }
                "recycler"                -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(2)
                    writeUTF(catalogPage.pageHeadline)
                    writeUTF(catalogPage.pageTeaser)
                    writeInt(1)
                    writeUTF(catalogPage.pageText1)
                }
                "recycler_info"           -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(2)
                    writeUTF(catalogPage.pageHeadline)
                    writeUTF(catalogPage.pageTeaser)
                    writeInt(3)
                    writeUTF(catalogPage.pageText1)
                    writeUTF(catalogPage.pageText2)
                    writeUTF(catalogPage.pageTextDetails)
                }
                "recycler_prizes"         -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(1)
                    writeUTF(catalogPage.pageHeadline)
                    writeInt(1)
                    writeUTF(catalogPage.pageText1)
                }
                "marketplace_own_items",
                "marketplace"             -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(1)
                    writeUTF(catalogPage.pageHeadline)
                    writeInt(0)
                }
                else                      -> {
                    writeUTF("NORMAL")
                    writeUTF(catalogPage.pageLayout)
                    writeInt(3)
                    writeUTF(catalogPage.pageHeadline)
                    writeUTF(catalogPage.pageTeaser)
                    writeUTF(catalogPage.pageSpecial)
                    writeInt(3)
                    writeUTF(catalogPage.pageText1)
                    writeUTF(catalogPage.pageTextDetails)
                    writeUTF(catalogPage.pageTextTeaser)
                }
            }

            writeInt(catalogPage.catalogItems.size)

            catalogPage.catalogItems.forEach { serialize(it) }

            writeInt(-1)
            writeBoolean(false)
        }
    }
}