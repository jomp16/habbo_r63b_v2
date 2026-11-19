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

package ovh.rwx.fastfood.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FastFoodLobbyTest {
    private val lobby = FastFoodLobby<String>(capacity = 3, waitTicks = 100)

    @Test
    fun aLonePlayerWaitsThenPlaysWithBots() {
        lobby.join("ana", now = 0)
        assertNull(lobby.takeReady(now = 99))
        assertEquals(listOf("ana"), lobby.takeReady(now = 100))
        assertTrue(lobby.isEmpty())
    }

    @Test
    fun aFullTableStartsAtOnce() {
        lobby.join("ana", 0)
        lobby.join("bia", 10)
        assertNull(lobby.takeReady(20))
        lobby.join("caio", 20)
        assertEquals(listOf("ana", "bia", "caio"), lobby.takeReady(20))
    }

    @Test
    fun whoeverIsLeftOverOpensANewWait() {
        listOf("ana", "bia", "caio", "duda").forEach { lobby.join(it, 50) }
        assertEquals(listOf("ana", "bia", "caio"), lobby.takeReady(50))
        // duda não leva a espera vencida de quem saiu: espera a dela inteira
        assertNull(lobby.takeReady(149))
        assertEquals(listOf("duda"), lobby.takeReady(150))
    }

    @Test
    fun theWaitStartsWithTheFirstPlayerNotTheLast() {
        lobby.join("ana", 0)
        lobby.join("bia", 90)
        assertEquals(listOf("ana", "bia"), lobby.takeReady(100))
    }

    @Test
    fun leavingAndJoiningTwice() {
        assertTrue(lobby.join("ana", 0))
        assertFalse(lobby.join("ana", 5))
        lobby.leave("ana")
        assertNull(lobby.takeReady(1000))
        // quem chega depois de a fila esvaziar abre uma espera nova
        lobby.join("bia", 1000)
        assertNull(lobby.takeReady(1050))
    }
}
