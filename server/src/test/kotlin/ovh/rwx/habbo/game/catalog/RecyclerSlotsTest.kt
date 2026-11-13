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

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RecyclerSlotsTest {
    @Test
    fun fixedFiveBeforeConfigurable() {
        // antes do corte o config é ignorado: o client tem 5 fixo
        assertEquals(5, RecyclerSlots.forBuildDate(LocalDate.of(2011, 8, 11), 8))
        assertEquals(5, RecyclerSlots.forBuildDate(LocalDate.of(2011, 10, 5), 8))
        assertEquals(5, RecyclerSlots.forBuildDate(LocalDate.of(2012, 4, 13), 8))
    }

    @Test
    fun configurableFromFirstConfigRelease() {
        assertEquals(8, RecyclerSlots.forBuildDate(LocalDate.of(2012, 4, 16), 8))
        assertEquals(8, RecyclerSlots.forBuildDate(LocalDate.of(2013, 5, 16), 8))
        assertEquals(8, RecyclerSlots.forBuildDate(LocalDate.of(2026, 5, 18), 8))
        // segue o que o hotel serve no external_variables
        assertEquals(5, RecyclerSlots.forBuildDate(LocalDate.of(2026, 5, 18), 5))
    }
}
