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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/** Numeração LTD sem buracos nem repetição: o contador só avança quando a seção crítica termina com sucesso. */
class CatalogLimitedReservationTest {
    private fun ltd(total: Int, sells: Int = 0) = CatalogItem(
        id = 1, pageId = 1, itemName = "x", orderNum = 0, dealId = 0, catalogName = "x", badge = "",
        costCredits = 1, pricePoints = 0, pointType = 0, amount = 1, clubOnly = false,
        limitedSells = AtomicInteger(sells), limitedTotal = total, offerActive = true, extraData = emptyMap()
    )

    @Test
    fun `sucesso avanca o contador e os numeros sao contiguos`() {
        val item = ltd(10)
        val seen = mutableListOf<IntRange>()

        assertEquals(LimitedOutcome.COMMITTED, item.withLimitedReservation(3) { seen += it; true })
        assertEquals(LimitedOutcome.COMMITTED, item.withLimitedReservation(2) { seen += it; true })

        assertEquals(listOf(1..3, 4..5), seen)
        assertEquals(5, item.limitedSells.get())
    }

    @Test
    fun `falha ou recusa nao avanca e o proximo recebe os mesmos numeros`() {
        val item = ltd(10)

        assertEquals(LimitedOutcome.DECLINED, item.withLimitedReservation(2) { false })
        assertEquals(0, item.limitedSells.get())

        try {
            item.withLimitedReservation(2) { error("insert falhou") }
        } catch (_: IllegalStateException) {
        }
        assertEquals(0, item.limitedSells.get())

        var got: IntRange? = null
        item.withLimitedReservation(2) { got = it; true }
        assertEquals(1..2, got)
    }

    @Test
    fun `alem do estoque esgota sem chamar o bloco nem mexer no contador`() {
        val item = ltd(5, sells = 4)
        var called = false

        assertEquals(LimitedOutcome.SOLD_OUT, item.withLimitedReservation(2) { called = true; true })

        assertTrue(!called)
        assertEquals(4, item.limitedSells.get())
        assertEquals(LimitedOutcome.COMMITTED, item.withLimitedReservation(1) { true })
        assertEquals(LimitedOutcome.SOLD_OUT, item.withLimitedReservation(1) { true })
    }

    @Test
    fun `oferta nao limitada nunca reserva`() {
        assertEquals(LimitedOutcome.SOLD_OUT, ltd(0).withLimitedReservation(1) { true })
    }

    @Test
    fun `estresse concorrente com falhas aleatorias gera exatamente 1 a N sem buracos nem repeticao`() {
        val total = 1000
        val item = ltd(total)
        val delivered = ConcurrentLinkedQueue<Int>()
        val threads = 16
        val start = CountDownLatch(1)

        val workers = List(threads) { t ->
            Thread {
                val random = Random(t)
                start.await()
                var soldOut = false
                while (!soldOut) {
                    val failMode = random.nextInt(4) // 0 = recusa, 1 = lança, resto = sucesso
                    try {
                        val outcome = item.withLimitedReservation(1) { range ->
                            when (failMode) {
                                0 -> false
                                1 -> error("falha")
                                else -> { range.forEach { delivered += it }; true }
                            }
                        }
                        if (outcome == LimitedOutcome.SOLD_OUT) soldOut = true
                    } catch (_: IllegalStateException) {
                    }
                }
            }.also { it.start() }
        }
        start.countDown()
        workers.forEach { it.join() }

        assertEquals(total, item.limitedSells.get())
        assertEquals((1..total).toList(), delivered.sorted())
    }
}
