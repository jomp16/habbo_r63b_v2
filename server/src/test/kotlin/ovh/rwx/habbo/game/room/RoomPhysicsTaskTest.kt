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

package ovh.rwx.habbo.game.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ovh.rwx.habbo.game.item.wired.WiredHarness
import ovh.rwx.habbo.util.Vector2

class RoomPhysicsTaskTest : WiredHarness() {

    @Test
    fun `avatar anda rigorosamente a cada 500ms (1 quadrado por major tick)`() {
        initGamemap()

        // Avatar inicia em (2, 2)
        val user = roomUser(1, 2, 2)
        room.roomGamemap.addRoomEntity(user, user.currentVector3.vector2)
        room.userManager.entities[user.virtualID] = user

        // Comanda movimento para (5, 2) - 3 tiles de distância para o leste
        user.moveTo(Vector2(5, 2))

        // Sub-ticks 1 a 9 (50ms a 450ms): UserMoveTask é drenada no 1º tick (highFrequency),
        // mas o avatar NÃO dá nenhum passo durante os sub-ticks menores
        repeat(9) { room.tick() }
        assertEquals(Vector2(2, 2), user.currentVector3.vector2)
        assertEquals("Objetivo já foi registrado no 1º tick", Vector2(5, 2), user.objectiveVector2)

        // Tick 10 (1º major tick = 500ms): avatar planeja o 1º passo para (3, 2)
        room.tick()
        assertEquals("Avatar ainda está fisicamente em (2, 2)", Vector2(2, 2), user.currentVector3.vector2)
        assertEquals("Passo planejado para (3, 2)", Vector2(3, 2), user.nextStepVector?.vector2)

        // Sub-ticks 11 a 19 (550ms a 950ms): nenhum passo é comitado
        repeat(9) { room.tick() }
        assertEquals(Vector2(2, 2), user.currentVector3.vector2)

        // Tick 20 (2º major tick = 1000ms): comita o passo no tile (3, 2) e planeja para (4, 2)
        room.tick()
        assertEquals("Avatar dá o passo para (3, 2) aos 1000ms", Vector2(3, 2), user.currentVector3.vector2)
        assertEquals("Passo planejado para (4, 2)", Vector2(4, 2), user.nextStepVector?.vector2)

        // Ticks 21 a 30 (3º major tick = 1500ms): comita o passo no tile (4, 2) e planeja para (5, 2)
        repeat(10) { room.tick() }
        assertEquals("Avatar dá o passo para (4, 2) aos 1500ms", Vector2(4, 2), user.currentVector3.vector2)

        // Ticks 31 a 40 (4º major tick = 2000ms): chega ao destino final (5, 2) e para de andar
        repeat(10) { room.tick() }
        assertEquals("Avatar atinge o destino final (5, 2) aos 2000ms", Vector2(5, 2), user.currentVector3.vector2)
        assertNull("Avatar não tem mais próximo passo planejado", user.nextStepVector)
        assertNull("Avatar encerrou o objetivo", user.objectiveVector2)
    }

    @Test
    fun `tarefas de alta frequencia rodam em 50ms e tarefas comuns aguardam o major tick de 500ms`() {
        var highFreqRanAt = -1
        var majorRanAt = -1

        val highFreqTask = object : IRoomTask {
            override val highFrequency = true
            override fun executeTask(room: Room) {
                highFreqRanAt = room.tickCounter
            }
        }

        val majorTask = object : IRoomTask {
            override val highFrequency = false
            override fun executeTask(room: Room) {
                majorRanAt = room.tickCounter
            }
        }

        room.addTask(highFreqTask)
        room.addTask(majorTask)

        // Sub-tick 1 (50ms): tarefa de alta frequência executa imediatamente
        room.tick()
        assertEquals(1, highFreqRanAt)
        assertEquals("Tarefa major não deve rodar no 1º sub-tick", -1, majorRanAt)

        // Ticks 2 a 9 (100ms a 450ms): majorTask continua na fila
        repeat(8) { room.tick() }
        assertEquals(-1, majorRanAt)

        // Tick 10 (500ms, tickCounter == 0): majorTask é executada
        room.tick()
        assertEquals(0, majorRanAt)
    }

    @Test
    fun `redirecionamento de movimento no meio do caminho atualiza a rota no proximo tick de 500ms`() {
        initGamemap()

        val user = roomUser(1, 2, 2)
        room.roomGamemap.addRoomEntity(user, user.currentVector3.vector2)
        room.userManager.entities[user.virtualID] = user

        // Começa andando em direção a (6, 2)
        user.moveTo(Vector2(6, 2))
        repeat(20) { room.tick() } // 1000ms: chega em (3, 2)
        assertEquals(Vector2(3, 2), user.currentVector3.vector2)

        // Usuário muda de ideia e clica para andar para o sul (3, 5)
        user.moveTo(Vector2(3, 5))
        room.tick() // 1º sub-tick drena a UserMoveTask (highFrequency) imediatamente
        assertEquals(Vector2(3, 5), user.objectiveVector2)

        // Próximo ciclo major: comita o passo já em voo para (4, 2) e recalcula rota para (3, 5)
        repeat(9) { room.tick() }
        assertEquals("Conclui o passo que já estava em voo para (4, 2)", Vector2(4, 2), user.currentVector3.vector2)

        // Ciclo seguinte (10 ticks = 500ms): curva em direção ao novo objetivo (3, 5)
        repeat(10) { room.tick() }
        assertEquals(Vector2(4, 3), user.currentVector3.vector2)
    }
}
