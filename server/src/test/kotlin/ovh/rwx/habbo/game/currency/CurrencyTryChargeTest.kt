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

package ovh.rwx.habbo.game.currency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Parte pura de [CurrencyService.tryCharge]/[CurrencyService.refund] (carteira em valores simples). */
class CurrencyTryChargeTest {
    private class FakeWallet(override var credits: Int, var points: Int = 0) : CurrencyService.Wallet {
        override fun points(type: Int): Int = points
        override fun addPoints(type: Int, delta: Int) {
            points += delta
        }
    }

    @Test
    fun `debita so quando o saldo cobre`() {
        val wallet = FakeWallet(credits = 10, points = 5)
        val lock = Any()

        val ok = CurrencyService.tryDebit(lock, wallet, Price(4, 5, 2), 2)
        assertTrue(ok.ok)
        assertEquals(2, wallet.credits)
        assertEquals(1, wallet.points)

        val fail = CurrencyService.tryDebit(lock, wallet, Price(4, 5, 2), 1)
        assertFalse(fail.ok)
        assertTrue(fail.notEnoughCredits)
        assertTrue(fail.notEnoughPoints)
        assertEquals(2, wallet.credits)
        assertEquals(1, wallet.points)
    }

    @Test
    fun `refund devolve exatamente o debitado`() {
        val wallet = FakeWallet(credits = 10, points = 5)
        val lock = Any()
        val price = Price(3, 5, 1)

        CurrencyService.tryDebit(lock, wallet, price, 2)
        CurrencyService.applyRefund(lock, wallet, price, 2)

        assertEquals(10, wallet.credits)
        assertEquals(5, wallet.points)
    }

    @Test
    fun `grant soma creditos e pontos e satura em Int MAX`() {
        val wallet = FakeWallet(credits = 5, points = 1)
        val lock = Any()

        CurrencyService.applyGrant(lock, wallet, 10, mapOf(5 to 3))
        assertEquals(15, wallet.credits)
        assertEquals(4, wallet.points)

        CurrencyService.applyGrant(lock, wallet, Int.MAX_VALUE, emptyMap())
        assertEquals(Int.MAX_VALUE, wallet.credits)
    }

    @Test
    fun `grant e tryDebit concorrentes nao perdem atualizacao nem ficam negativos`() {
        val wallet = FakeWallet(credits = 0)
        val lock = Any()
        val debited = AtomicInteger()
        val granted = AtomicInteger()

        val workers = List(8) { index ->
            Thread {
                repeat(500) {
                    if (index % 2 == 0) {
                        CurrencyService.applyGrant(lock, wallet, 3, emptyMap())
                        granted.addAndGet(3)
                    } else if (CurrencyService.tryDebit(lock, wallet, Price(2, 0, 0), 1).ok) {
                        debited.addAndGet(2)
                    }
                    assertTrue(synchronized(lock) { wallet.credits } >= 0)
                }
            }.also { it.start() }
        }
        workers.forEach { it.join() }

        assertEquals(granted.get() - debited.get(), wallet.credits)
        assertTrue(wallet.credits >= 0)
    }

    @Test
    fun `tryCharge concorrente nunca fica negativo e debita no maximo saldo dividido pelo preco`() {
        val wallet = FakeWallet(credits = 1000)
        val lock = Any()
        val successes = AtomicInteger()

        val workers = List(16) {
            Thread {
                repeat(200) {
                    if (CurrencyService.tryDebit(lock, wallet, Price(7, 0, 0), 1).ok) successes.incrementAndGet()
                }
            }.also { it.start() }
        }
        workers.forEach { it.join() }

        assertEquals(1000 / 7, successes.get())
        assertEquals(1000 - (1000 / 7) * 7, wallet.credits)
        assertTrue(wallet.credits >= 0)
    }
}
