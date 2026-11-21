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

package ovh.rwx.habbo.admin.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugGamemapTest {
    @Test
    fun alturaViraUmCaractere() {
        assertEquals('0', DebugGamemap.levelChar(0.0))
        assertEquals('9', DebugGamemap.levelChar(9.0))
        assertEquals('a', DebugGamemap.levelChar(10.0))
        assertEquals('z', DebugGamemap.levelChar(35.0))
        assertEquals('+', DebugGamemap.levelChar(36.0))
        assertEquals('-', DebugGamemap.levelChar(-1.0))
        assertEquals('3', DebugGamemap.levelChar(2.6)) // arredonda
        assertEquals('0', DebugGamemap.levelChar(0.4))
    }

    @Test
    fun estadoRespeitaAPrioridade() {
        assertEquals('A', DebugGamemap.stateChar(true, true, true, true, true, true))
        assertEquals('D', DebugGamemap.stateChar(false, true, false, true, true, true))
        assertEquals('#', DebugGamemap.stateChar(true, false, false, true, true, true))
        assertEquals('B', DebugGamemap.stateChar(false, false, false, true, true, true))
        assertEquals('E', DebugGamemap.stateChar(false, false, false, false, true, true))
        assertEquals('i', DebugGamemap.stateChar(false, false, false, false, false, true))
        assertEquals('.', DebugGamemap.stateChar(false, false, false, false, false, false))
    }

    @Test
    fun ferramentaEstaNoCatalogoComoLeitura() {
        val t = DebugToolset.tools.single { it.name == "room_gamemap" }
        assertEquals(DebugAccess.READ, t.access)
        assertEquals(listOf("room_id", "x", "y", "limit"), t.params.map { it.name })
        assertTrue(t.params.single { it.name == "room_id" }.required)
        assertTrue(DebugGamemap.LEGEND.contains("state:"))
    }
}
