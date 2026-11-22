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

package ovh.rwx.habbo.game.user

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LoginStreakTest {
    private val today = LocalDate.of(2026, 11, 21)

    @Test
    fun `primeira entrada de todas conta o dia e nao continua sequencia`() {
        assertEquals(LoginDay(firstOfDay = true, streakContinues = false), LoginStreak.evaluate(null, today))
    }

    @Test
    fun `entrada ontem continua a sequencia`() {
        assertEquals(LoginDay(true, true), LoginStreak.evaluate(today.minusDays(1), today))
    }

    @Test
    fun `entrada hoje nao conta de novo`() {
        assertEquals(LoginDay(false, false), LoginStreak.evaluate(today, today))
    }

    @Test
    fun `pular um dia quebra a sequencia mas conta o dia`() {
        assertEquals(LoginDay(true, false), LoginStreak.evaluate(today.minusDays(2), today))
    }

    @Test
    fun `virada de mes e de ano continua a sequencia`() {
        assertEquals(LoginDay(true, true), LoginStreak.evaluate(LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)))
    }

    @Test
    fun `data futura no banco nao conta como novo dia`() {
        assertEquals(LoginDay(false, false), LoginStreak.evaluate(today.plusDays(1), today))
    }
}
