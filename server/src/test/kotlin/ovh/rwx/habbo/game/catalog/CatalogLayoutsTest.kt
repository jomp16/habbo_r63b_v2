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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Regressão: abrir a página do HC (`club_buy`, que só tem `ctlg_header_img`) com teaser/special preenchidos derrubava
 * o update receiver do client 20111005 (`findChildByName(...).tags` em null). Também cobre a escolha do layout que a
 * release tem (antes duplicada entre resolveLegacyPresentation e resolveR63APresentation).
 */
class CatalogLayoutsTest {
    private val start = LocalDateTime.of(2009, 6, 16, 13, 27)
    private val end = LocalDateTime.of(2014, 1, 30, 16, 19)
    private val all = setOf("ctlg_header_img", "ctlg_teaserimg_1", "ctlg_special_img")

    private fun window(code: String, from: LocalDateTime, until: LocalDateTime, images: Set<String>, texts: Int = 3) =
        CatalogLayoutWindow(code, from, until, images, texts)

    private val table = CatalogLayoutTable(
        listOf(
            window("default_3x3", start, end, all),
            window("club_buy", start, end, setOf("ctlg_header_img")),
            window("vip_buy", LocalDateTime.of(2011, 11, 25, 11, 40), end, setOf("ctlg_header_img", "ctlg_teaserimg_1")),
            window("pets2", start, end, setOf("ctlg_header_img", "ctlg_teaserimg_1"), 4),
            window("spaces", start, end, setOf("ctlg_header_img")),
            window("spaces_new", LocalDateTime.of(2011, 4, 5, 13, 10), end, setOf("ctlg_header_img", "ctlg_teaserimg_1")),
            window("presents", start, end, setOf("ctlg_header_img"), 2),
        ),
        mapOf("pets" to "pets2", "spaces_new" to "spaces", "vip_buy" to "club_buy")
    )

    private val images = listOf("header", "teaser", "special")

    @Test
    fun `club_buy mantem so o header`() {
        val layout = table.resolve(LocalDateTime.of(2011, 10, 5, 12, 50), "club_buy")!!
        assertEquals(listOf("header", "", ""), layout.pruneImages(images))
    }

    @Test
    fun `layout sem imagens esvazia tudo e com todas mantem tudo`() {
        assertEquals(images, table.resolve(LocalDateTime.of(2011, 10, 5, 12, 50), "default_3x3")!!.pruneImages(images))
        assertEquals(listOf("", "", ""), ResolvedCatalogLayout("loading", emptySet(), 3).pruneImages(images))
    }

    @Test
    fun `a imagem e associada por posicao e nao por quantidade`() {
        // club2: header, teaser_1 e teaser_2 (sem special): a 3a posicao (special_img) some.
        val club2 = ResolvedCatalogLayout("club2", setOf("ctlg_header_img", "ctlg_teaserimg_1", "ctlg_teaserimg_2"), 4)
        assertEquals(listOf("header", "teaser", ""), club2.pruneImages(images))
    }

    @Test
    fun `vip_buy cai em club_buy antes de existir`() {
        assertEquals("club_buy", table.resolve(LocalDateTime.of(2011, 10, 5, 12, 50), "vip_buy")!!.layoutCode)
        assertEquals("vip_buy", table.resolve(LocalDateTime.of(2011, 12, 13, 9, 5), "vip_buy")!!.layoutCode)
    }

    @Test
    fun `layout novo cai no anterior e desconhecido cai em default_3x3`() {
        assertEquals("spaces", table.resolve(LocalDateTime.of(2011, 1, 1, 0, 0), "spaces_new")!!.layoutCode)
        assertEquals("spaces_new", table.resolve(LocalDateTime.of(2011, 6, 1, 0, 0), "spaces_new")!!.layoutCode)
        assertEquals("pets2", table.resolve(LocalDateTime.of(2010, 1, 1, 0, 0), "pets")!!.layoutCode)
        assertEquals("default_3x3", table.resolve(LocalDateTime.of(2010, 1, 1, 0, 0), "category")!!.layoutCode)
    }

    @Test
    fun `fora da faixa da tabela devolve null para a pagina ir como esta`() {
        assertNull(table.resolve(start.minusMinutes(1), "default_3x3"))
        assertNull(table.resolve(end, "default_3x3"))
        assertNotNull(table.resolve(end.minusMinutes(1), "default_3x3"))
        assertFalse(CatalogLayoutTable(emptyList(), emptyMap()).covers(start))
    }

    @Test
    fun `fallback em ciclo nao trava`() {
        val cyclic = CatalogLayoutTable(listOf(window("default_3x3", start, end, all)), mapOf("a" to "b", "b" to "a"))
        assertEquals("default_3x3", cyclic.resolve(start, "a")!!.layoutCode)
    }

    @Test
    fun `quantidade de textos define quais campos da pagina vao`() {
        fun texts(count: Int) = ResolvedCatalogLayout("x", emptySet(), count).pickTexts("t1", "t2", "details", "teaser")
        assertEquals(listOf("t1"), texts(1))
        assertEquals(listOf("t1", "details"), texts(2))
        assertEquals(listOf("t1", "details", "teaser"), texts(3))
        assertEquals(listOf("t1", "t2", "details", "teaser"), texts(4))
        assertTrue(texts(0).isEmpty())
    }
}
