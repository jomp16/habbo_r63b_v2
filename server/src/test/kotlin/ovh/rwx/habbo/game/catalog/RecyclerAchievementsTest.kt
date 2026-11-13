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
import org.junit.Test

class RecyclerAchievementsTest {
    private val prizes = setOf("p1", "p2")
    private val names8 = List(8) { "x$it" }

    private fun progress(names: List<String>, level: Int, populated: Set<Int>) =
        RecyclerAchievements.progressFor(names, prizes, level, populated)

    @Test
    fun `conta os itens reciclados`() {
        assertEquals(8, progress(names8, 1, setOf(1, 2, 3))[RecyclerAchievements.RECYCLED_ITEMS])
        assertEquals(5, progress(names8.take(5), 1, setOf(1, 2, 3))[RecyclerAchievements.RECYCLED_ITEMS])
    }

    @Test
    fun `ecoprizes conta so os premios e omite zero`() {
        val withPrizes = progress(listOf("p1", "x", "p2", "p1"), 1, setOf(1))
        assertEquals(3, withPrizes[RecyclerAchievements.RECYCLE_ECOPRIZES])

        assertFalse(progress(names8, 1, setOf(1)).containsKey(RecyclerAchievements.RECYCLE_ECOPRIZES))
    }

    @Test
    fun `raro so do nivel 3 para cima`() {
        assertFalse(progress(names8, 1, setOf(1, 2, 3, 4)).containsKey(RecyclerAchievements.RECYCLER_RARE_PRIZE))
        assertFalse(progress(names8, 2, setOf(1, 2, 3, 4)).containsKey(RecyclerAchievements.RECYCLER_RARE_PRIZE))
        assertEquals(1, progress(names8, 3, setOf(1, 2, 3, 4))[RecyclerAchievements.RECYCLER_RARE_PRIZE])
        assertEquals(1, progress(names8, 5, setOf(1, 5))[RecyclerAchievements.RECYCLER_RARE_PRIZE])
    }

    @Test
    fun `grande premio so no maior nivel povoado`() {
        assertFalse(progress(names8, 3, setOf(1, 2, 3, 4, 5)).containsKey(RecyclerAchievements.RECYCLER_GRAND_PRIZE))
        assertEquals(1, progress(names8, 5, setOf(1, 2, 3, 4, 5))[RecyclerAchievements.RECYCLER_GRAND_PRIZE])
        // nível 5 vazio: o topo é o 4
        assertEquals(1, progress(names8, 4, setOf(1, 2, 3, 4))[RecyclerAchievements.RECYCLER_GRAND_PRIZE])
        assertFalse(progress(names8, 3, setOf(1, 2, 3, 4)).containsKey(RecyclerAchievements.RECYCLER_GRAND_PRIZE))
    }

    @Test
    fun `so o comum povoado nao e grande premio`() {
        assertFalse(progress(names8, 1, setOf(1)).containsKey(RecyclerAchievements.RECYCLER_GRAND_PRIZE))
    }

    @Test
    fun `nao inclui o abridor de caixa`() {
        assertFalse(progress(names8, 5, setOf(1, 5)).containsKey(RecyclerAchievements.RECYCLE_BOX_OPENER))
    }
}
