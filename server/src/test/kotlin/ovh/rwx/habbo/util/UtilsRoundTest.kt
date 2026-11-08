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

package ovh.rwx.habbo.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random

class UtilsRoundTest {
    private fun reference(value: Double, places: Int) = BigDecimal(value).setScale(places, RoundingMode.HALF_UP).toDouble()

    @Test
    fun `round e identico ao BigDecimal HALF_UP em valores aleatorios e de empate`() {
        val random = Random(42)
        repeat(500_000) {
            val places = random.nextInt(4)
            val value = when (it % 4) {
                0 -> random.nextDouble() * 20 - 5
                1 -> (random.nextInt(4000) - 500) / 100.0 // alturas "redondas" do jogo
                2 -> (random.nextInt(4000) - 500) / 1000.0 + 0.0005 // proximo de empate
                else -> (random.nextInt(2000) - 300) / 8.0 // empates exatos binarios (.125, .5 ...)
            }
            assertEquals("value=$value places=$places", reference(value, places), Utils.round(value, places), 0.0)
        }
    }

    @Test
    fun `round trata zero negativo e valores especiais`() {
        assertEquals(reference(0.0, 2), Utils.round(0.0, 2), 0.0)
        assertEquals(reference(-0.004, 2), Utils.round(-0.004, 2), 0.0)
        assertEquals(0.13, Utils.round(0.125, 2), 0.0)
        assertEquals(-0.13, Utils.round(-0.125, 2), 0.0)
        assertEquals(1.005.let { reference(it, 2) }, Utils.round(1.005, 2), 0.0)
    }
}
