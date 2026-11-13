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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RecyclerRewardsTest {
    private val odds = mapOf(1 to 1, 2 to 5, 3 to 10)
    private val rewards = mapOf(1 to listOf("a1", "a2"), 2 to listOf("b1"), 3 to listOf("c1"))

    @Test
    fun `todos os niveis sao alcancaveis`() {
        val random = Random(42)
        val levels = (1..5000).mapNotNull { RecyclerRewards.drawLevel(rewards, odds, random) }.toSet()

        assertEquals(setOf(1, 2, 3), levels)
    }

    @Test
    fun `dentro do nivel o sorteio e uniforme (todos saem)`() {
        val random = Random(7)
        val names = (1..2000).mapNotNull { RecyclerRewards.draw(rewards, odds, random) }.toSet()

        assertTrue(names.containsAll(listOf("a1", "a2", "b1", "c1")))
    }

    @Test
    fun `nivel vazio e pulado`() {
        val random = Random(1)
        val r = mapOf(1 to listOf("a"), 2 to emptyList(), 3 to listOf("c"))

        repeat(3000) { assertTrue(RecyclerRewards.drawLevel(r, odds, random) != 2) }
    }

    @Test
    fun `nunca devolve null havendo algum premio, mesmo sem nivel 1`() {
        val random = Random(3)
        val r = mapOf(2 to listOf("b"), 3 to emptyList())

        repeat(1000) { assertEquals("b", RecyclerRewards.draw(r, odds, random)) }
        assertNotNull(RecyclerRewards.draw(rewards, odds, random))
    }

    @Test
    fun `sem premios devolve null`() {
        assertNull(RecyclerRewards.draw(emptyMap(), odds, Random(0)))
        assertNull(RecyclerRewards.draw(mapOf(1 to emptyList()), odds, Random(0)))
    }
}
