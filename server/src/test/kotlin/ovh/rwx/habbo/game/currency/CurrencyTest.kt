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

import org.junit.Assert.*
import org.junit.Test
import ovh.rwx.habbo.game.user.information.UserCurrencies
import ovh.rwx.habbo.util.ActivityPointType
import java.time.LocalDateTime

class CurrencyTest {
    @Test
    fun `carga inicial nao suja nada e tipo desconhecido do enum e preservado`() {
        var notified = 0
        val currencies = UserCurrencies(mapOf(0 to 10, 999 to 4)) { notified++ }

        assertEquals(10, currencies[ActivityPointType.PIXELS])
        assertEquals(4, currencies[999])
        assertEquals(0, notified)
        assertTrue(currencies.drainDirty().isEmpty())
    }

    @Test
    fun `alterar uma moeda suja so ela e avisa o dono`() {
        var notified = 0
        val currencies = UserCurrencies(mapOf(0 to 10, 5 to 3)) { notified++ }

        currencies.add(ActivityPointType.PIXELS, -4)

        assertEquals(6, currencies[ActivityPointType.PIXELS])
        assertEquals(1, notified)
        assertEquals(mapOf(0 to 6), currencies.drainDirty())
        assertTrue(currencies.drainDirty().isEmpty())
    }

    @Test
    fun `valor igual nao suja`() {
        val currencies = UserCurrencies(mapOf(0 to 10))

        currencies[ActivityPointType.PIXELS] = 10
        currencies.add(ActivityPointType.PIXELS, 0)

        assertTrue(currencies.drainDirty().isEmpty())
    }

    @Test
    fun `saldo satura em zero e em Int MAX`() {
        val currencies = UserCurrencies(mapOf(0 to 5, 5 to Int.MAX_VALUE - 1))

        currencies.add(0, -50)
        currencies.add(5, 100)

        assertEquals(0, currencies[0])
        assertEquals(Int.MAX_VALUE, currencies[5])
    }

    @Test
    fun `gravacao que falhou devolve os tipos para a lista de sujos`() {
        var notified = 0
        val currencies = UserCurrencies(mapOf(0 to 1)) { notified++ }
        currencies.add(0, 1)
        val drained = currencies.drainDirty()

        currencies.restoreDirty(drained.keys)

        assertEquals(2, notified)
        assertEquals(mapOf(0 to 2), currencies.drainDirty())
    }

    @Test
    fun `janela de disponibilidade do tipo respeita inicio fim e enabled`() {
        val diamonds = ActivityPointTypeInfo(5, "DIAMONDS", LocalDateTime.of(2012, 1, 13, 0, 0), null, true)

        assertFalse(diamonds.isAvailableAt(LocalDateTime.of(2012, 1, 12, 23, 59)))
        assertTrue(diamonds.isAvailableAt(LocalDateTime.of(2012, 1, 13, 0, 0)))
        assertFalse(diamonds.copy(enabled = false).isAvailableAt(LocalDateTime.of(2026, 1, 1, 0, 0)))
        assertFalse(
            diamonds.copy(availableUntil = LocalDateTime.of(2013, 1, 1, 0, 0))
                .isAvailableAt(LocalDateTime.of(2013, 1, 1, 0, 0))
        )
    }

    @Test
    fun `preco gratis e preco com pontos`() {
        assertTrue(Price(0, 0, 0).isFree)
        assertFalse(Price(0, 4, 3).isFree)
        assertTrue(BalanceCheck(false, false, 0).ok)
        assertFalse(BalanceCheck(false, true, 4).ok)
    }
}
