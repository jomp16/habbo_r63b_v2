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

package ovh.rwx.habbo.game.gamecenter

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailyPlaysTest {
    private val today = LocalDate.of(2026, 11, 19)

    @Test
    fun yesterdaysGamesDoNotCountToday() {
        assertEquals(7, DailyPlays(today, 7).on(today))
        assertEquals(0, DailyPlays(today.minusDays(1), 30).on(today))
    }

    @Test
    fun freeGamesLeftCountsDownToZeroAndNegativeLimitIsUnlimited() {
        assertEquals(30, DailyPlays.freeGamesLeft(30, 0))
        assertEquals(1, DailyPlays.freeGamesLeft(30, 29))
        assertEquals(0, DailyPlays.freeGamesLeft(30, 30))
        assertEquals(0, DailyPlays.freeGamesLeft(3, 9))
        assertEquals(-1, DailyPlays.freeGamesLeft(-1, 500))
    }
}
