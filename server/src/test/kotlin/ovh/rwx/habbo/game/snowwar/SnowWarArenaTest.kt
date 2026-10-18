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

package ovh.rwx.habbo.game.snowwar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.game.snowwar.bot.SnowWarBotData
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTeam
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTrajectory
import java.util.concurrent.ConcurrentHashMap

/**
 * Replay headless da simulação através do seam da arena (tick/queueEvent/checksum) —
 * sem sessions, sem executor, sem config, sem HabboServer. É a test surface do
 * SnowWarArena; desync de checksum vira falha de build, não crash de cliente.
 */
class SnowWarArenaTest {

    private fun newArena(): SnowWarArena {
        val users = ConcurrentHashMap<Int, SnowWarUser>()
        users[-1001] = SnowWarUser(session = null, team = SnowWarTeam.BLUE, botData = SnowWarBotData(-1001, "BotAzul", "", "M", ""))
        users[-1002] = SnowWarUser(session = null, team = SnowWarTeam.RED, botData = SnowWarBotData(-1002, "BotVermelho", "", "F", ""))

        val arena = SnowWarArena(users)
        arena.setupField(
            SnowWarArenaData(
                id = 1,
                name = "test_arena",
                width = 10,
                height = 10,
                // chão '0' (z=0): a bola nasce a z=3000 — em tile '1' (z=3200) ela colidiria
                // com o chão no próprio subturn de spawn (física pré-existente da AS3)
                heightMap = (0 until 10).joinToString("\r") { "0000000000" },
                fuseObjects = emptyList(),
                blueSpawns = listOf(2 to 2),
                redSpawns = listOf(7 to 7)
            )
        )
        arena.resetTurns()
        return arena
    }

    @Test
    fun testEmptyArenaStopsTicking() {
        val arena = SnowWarArena(ConcurrentHashMap())

        assertNull(arena.tick())
    }

    @Test
    fun testTickAdvancesTurnAndChecksumIsStable() {
        val arena = newArena()

        val status = arena.tick()!!

        assertEquals(0, status.turn)
        // checksum é função pura do estado: recomputar no mesmo turno dá o mesmo valor
        assertEquals(arena.calculateChecksum(0), status.checksum)
        assertEquals(status.checksum, arena.lastCompletedChecksum)
    }

    @Test
    fun testQueuedSnowballIsSentFirstTurnAndAppliedNextTurn() {
        val arena = newArena()
        val bot = arena.users.values.first()
        val ballId = arena.nextObjectId.getAndIncrement()
        arena.queueEvent(
            0,
            CreateSnowballGameEvent(
                snowBallGameObjectId = ballId,
                humanGameObjectId = bot.objectId,
                targetX = bot.currentLocationX + 16000,
                targetY = bot.currentLocationY,
                trajectory = SnowWarTrajectory.LONG_LOB
            )
        )

        // Turno 0: o evento vai no subTurnEvents (broadcast), mas a física aplica só no próximo turno
        val status1 = arena.tick()!!
        assertTrue(arena.snowballs.isEmpty())
        assertTrue(status1.subTurns.any { sub -> sub.any { it is CreateSnowballGameEvent } })

        // Turno 1: evento deferido aplicado, bola física existe na arena
        arena.tick()
        assertEquals(1, arena.snowballs.size)
    }
}
