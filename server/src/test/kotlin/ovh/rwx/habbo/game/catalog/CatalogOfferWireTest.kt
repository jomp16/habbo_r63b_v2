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

import org.junit.Assert.assertEquals
import org.junit.Test
import ovh.rwx.habbo.communication.HabboVersion

/**
 * Os campos da oferta por build, como o AS3 de cada uma lê (`habbo-timeline CatalogPageMessageEvent incoming`).
 * A 201112130905 é a do relatório "Failed to parse incoming message using CatalogPageMessageParser": o emulador mandava
 * o boolean bundlePurchaseAllowed (só lido desde 201201302302) e um boolean isUnique no lugar da expiração do produto.
 */
class CatalogOfferWireTest {
    private fun wire(release: String) = CatalogOfferWire(HabboVersion.parse(release))

    private fun describe(w: CatalogOfferWire) =
        "giftable=${w.giftable} badge=${w.badgeProduct} bundle=${w.bundlePurchaseAllowed} unique=${w.productUniqueFlag} exp=${w.productExpiration}"

    @Test
    fun `201110051250 - oferta so com id, nome, preco, produtos e clubLevel`() {
        assertEquals(
            "giftable=false badge=false bundle=false unique=false exp=true",
            describe(wire("RELEASE63-201110051250-692472661"))
        )
    }

    @Test
    fun `201112130905 - com giftable, sem emblema, sem bundle e sem isUnique (a do relatorio)`() {
        assertEquals(
            "giftable=true badge=false bundle=false unique=false exp=true",
            describe(wire("RELEASE63-201112130905-713192101"))
        )
    }

    @Test
    fun `201201302302 - bundlePurchaseAllowed e emblema, ainda sem isUnique`() {
        assertEquals(
            "giftable=true badge=true bundle=true unique=false exp=true",
            describe(wire("RELEASE63-201201302302-96159305"))
        )
    }

    @Test
    fun `201204021456 - isUnique e expiracao`() {
        assertEquals(
            "giftable=true badge=true bundle=true unique=true exp=true",
            describe(wire("RELEASE63-201204021456-654375178"))
        )
    }

    @Test
    fun `201305221112 e AIR - a expiracao saiu em 2013-01-30`() {
        assertEquals(
            "giftable=true badge=true bundle=true unique=true exp=false",
            describe(wire("RELEASE63-201305221112-286302280"))
        )
        assertEquals(
            "giftable=true badge=true bundle=true unique=true exp=false",
            describe(wire("WIN63-202610271147-806594867"))
        )
    }

    @Test
    fun `201111082302 introduz o giftable e a 201201242303 o emblema (as vizinhas nao)`() {
        assertEquals(false, wire("RELEASE63-201111072302-301115772").giftable)
        assertEquals(true, wire("RELEASE63-201111082302-671329261").giftable)
        assertEquals(false, wire("RELEASE63-201201241118-652943702").badgeProduct)
    }
}
