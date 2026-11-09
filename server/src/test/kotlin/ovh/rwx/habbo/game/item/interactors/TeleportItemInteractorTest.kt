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

package ovh.rwx.habbo.game.item.interactors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredHarness
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomData
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.game.room.tasks.UserMoveTask
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3

class TeleportItemInteractorTest : WiredHarness() {
    private val interactor = TeleportItemInteractor()

    @Before
    fun setUpTeleports() {
        furnishing("sf_tele", InteractionType.TELEPORT)
        itemManager.furniInteractor[InteractionType.TELEPORT] = interactor
    }

    @Test
    fun `rotina completa de teletransporte no mesmo quarto funciona com sucesso`() {
        // Teleporte 1 em (3, 3), rotação 0 (porta para o norte: front = (3, 2), lookDir = 4)
        val tele1 = item(59000, "sf_tele", 3, 3).apply {
            rotation = 0
        }
        // Teleporte 2 em (6, 6), rotação 2 (porta para o leste: front = (7, 6), lookDir = 6)
        val tele2 = item(59001, "sf_tele", 6, 6).apply {
            rotation = 2
        }

        initGamemap()

        // Mapeia links de teletransporte entre tele1 e tele2
        itemManager.teleportLinks[tele1.id] = tele2.id
        itemManager.teleportLinks[tele2.id] = tele1.id
        itemManager.roomTeleportLinks[tele1.id] = room.roomData.id
        itemManager.roomTeleportLinks[tele2.id] = room.roomData.id

        // Usuário em frente ao teleporte 1 (3, 2), virado para o teleporte (olhando para o sul = 4)
        val user = roomUser(1, 3, 2).apply {
            bodyRotation = 4
            headRotation = 4
        }
        room.roomGamemap.addRoomEntity(user, user.currentVector3.vector2)
        room.userManager.entities[user.virtualID] = user

        assertTrue("Usuário deve estar tocando a frente do tele1", tele1.isTouching(user.currentVector3, user.bodyRotation))

        // 1. Usuário clica no tele1
        interactor.onTrigger(room, user, tele1, hasRights = true, request = 0)

        // Tele1 abre a porta (extraData = 1), bloqueia caminhada externa e enfileira UserMoveTask na sala
        assertEquals("Porta do tele1 deve acender/abrir", "1", tele1.extraData)
        assertEquals("Usuário deve estar em interactingUsers[1] do tele1", user, tele1.interactingUsers[1])
        assertTrue("walkingBlocked deve estar true durante o teleporte", user.walkingBlocked)

        // 2. Avança os ticks pela física centralizada da sala (room.tick())
        // Ciclo 1 (10 ticks = 500ms): UserMoveTask é drenada e o avatar planeja o passo para o tele1 (3, 3)
        repeat(10) { room.tick() }
        assertEquals("Objetivo do usuário deve ser o tile do teleporte (3, 3)", Vector2(3, 3), user.objectiveVector2)
        assertTrue("ignoreBlocking deve estar true", user.ignoreBlocking)

        // Ciclo 2 (10 ticks = 500ms): Avatar dá o passo para dentro do tele1 (3, 3) e o tele1 o transporta para o tele2 (6, 6)
        repeat(10) { room.tick() }

        // Usuário deve ter sido transportado para o tele2!
        assertEquals("Tele1 deve ter liberado o usuário de interactingUsers[1]", null, tele1.interactingUsers[1])
        assertEquals("Usuário deve estar na posição do tele2 (6, 6)", Vector2(6, 6), user.currentVector3.vector2)
        assertEquals("Tele2 deve estar com o usuário em interactingUsers[2]", user, tele2.interactingUsers[2])
        assertEquals("Tele2 deve estar em transição (extraData = 2)", "2", tele2.extraData)

        // 3. Tele2 processa os ticks pela física da sala (2 ciclos = 20 ticks): ejeta o usuário para a frente do tele2 (7, 6)
        repeat(20) { room.tick() }

        // Rotação 2 do tele2 tem front em (7, 6)
        assertEquals("Tele2 deve abrir para saída do usuário (extraData = 1)", "1", tele2.extraData)
        assertFalse("walkingBlocked deve ser liberado para o usuário sair", user.walkingBlocked)

        // 4. Avatar caminha para fora do tele2 (2 ciclos = 20 ticks)
        repeat(20) { room.tick() }
        assertEquals("Usuário deve estar fora do teleporte na frente dele (7, 6)", tele2.getFrontPosition(), user.currentVector3.vector2)

        // 5. Teleportes fecham a porta (1 ciclo = 10 ticks de 50ms)
        repeat(10) { room.tick() }
        assertEquals("Tele2 deve voltar para estado fechado (extraData = 0)", "0", tele2.extraData)
        assertEquals("Tele1 deve voltar para estado fechado (extraData = 0)", "0", tele1.extraData)
    }

    @Test
    fun `rotina de teletransporte para outro quarto vincula targetTeleportId e teleportRoom na sessao`() {
        val room2 = Room(
            RoomData.createPrivate(
                id = 2, userId = 1, name = "test2", description = "",
                model = "model", category = 1, maxUsers = 10, tradeSettings = 0
            ),
            RoomModel("model", 1, Vector3(1, 1, 0.0), 2, (0 until 10).map { "0000000000" }, false)
        )
        roomManager.rooms[2] = room2

        // Tele1 no room 1 em (3, 3)
        val tele1 = item(59000, "sf_tele", 3, 3).apply { rotation = 0 }
        // Tele2 no room 2 em (5, 5)
        val tele2 = RoomItem(59001, 2, 1, "sf_tele", "0", Vector3(5, 5, 0.0), 0, "", false, false)
            .also { room2.itemManager.items[59001] = it }

        initGamemap()
        room2.roomGamemap = ovh.rwx.habbo.game.room.gamemap.RoomGamemap(room2)

        itemManager.teleportLinks[tele1.id] = tele2.id
        itemManager.teleportLinks[tele2.id] = tele1.id
        itemManager.roomTeleportLinks[tele1.id] = room.roomData.id
        itemManager.roomTeleportLinks[tele2.id] = room2.roomData.id

        val user = roomUser(1, 3, 2).apply {
            bodyRotation = 4
            headRotation = 4
        }
        room.roomGamemap.addRoomEntity(user, user.currentVector3.vector2)
        room.userManager.entities[user.virtualID] = user

        interactor.onTrigger(room, user, tele1, hasRights = true, request = 0)

        // 2 ciclos (20 ticks = 1000ms): usuário entra no tele1 e tele1 processa o teletransporte
        repeat(10) { room.tick() }
        repeat(10) { room.tick() }
        assertEquals(Vector2(3, 3), user.currentVector3.vector2)

        // Deve ter marcado a sessão para entrar no room2 pelo tele2
        assertTrue("Sessão deve estar com flag teleporting = true", user.habboSession.teleporting)
        assertEquals("Alvo de teleporte deve ser tele2", tele2.id, user.habboSession.targetTeleportId)
        assertEquals("Quarto de destino deve ser room2", room2, user.habboSession.teleportRoom)
        assertEquals("Tele1 deve estar no estado de transição (extraData = 2)", "2", tele1.extraData)
    }

    @Test
    fun `usuario clica no teleporte a distancia, caminha ate a frente e teletransporta automaticamente`() {
        val tele1 = item(59000, "sf_tele", 3, 3).apply { rotation = 0 }
        val tele2 = item(59001, "sf_tele", 6, 6).apply { rotation = 2 }

        initGamemap()

        itemManager.teleportLinks[tele1.id] = tele2.id
        itemManager.teleportLinks[tele2.id] = tele1.id
        itemManager.roomTeleportLinks[tele1.id] = room.roomData.id
        itemManager.roomTeleportLinks[tele2.id] = room.roomData.id

        // Usuário a 2 tiles de distância da porta: em (3, 0), olhando para o sul (4)
        val user = roomUser(1, 3, 0).apply {
            bodyRotation = 4
            headRotation = 4
        }
        room.roomGamemap.addRoomEntity(user, user.currentVector3.vector2)
        room.userManager.entities[user.virtualID] = user

        assertFalse(tele1.isTouching(user.currentVector3, user.bodyRotation))

        // Usuário clica no teleporte à distância
        interactor.onTrigger(room, user, tele1, hasRights = true, request = 0)

        // 1. Caminhada até (3, 2): de (3, 0) até (3, 2) são 3 major ticks (30 ticks de 50ms)
        repeat(30) { room.tick() }

        // Ao chegar em (3, 2), stopWalking dispara o onTrigger do tele1
        assertEquals("Usuário deve ter chegado à frente do teleporte (3, 2)", Vector2(3, 2), user.currentVector3.vector2)
        assertEquals("Porta do tele1 deve acender/abrir", "1", tele1.extraData)
        assertTrue("walkingBlocked deve estar true", user.walkingBlocked)

        // 2. Avatar entra no tele1 (3, 3) e é teletransportado para tele2 (6, 6) (30 ticks)
        repeat(30) { room.tick() }
        assertEquals("Usuário deve estar no tele2 (6, 6)", Vector2(6, 6), user.currentVector3.vector2)
        assertEquals("Tele2 deve estar com o usuário em interactingUsers[2]", user, tele2.interactingUsers[2])

        // 3. Tele2 processa ticks (20 ticks): ejeta o avatar para a frente (7, 6)
        repeat(20) { room.tick() }
        assertEquals("Tele2 deve abrir para saída do usuário (extraData = 1)", "1", tele2.extraData)
        assertFalse("walkingBlocked deve ser liberado para o usuário sair", user.walkingBlocked)

        // 4. Avatar caminha para fora do tele2 e portas fecham (20 ticks)
        repeat(20) { room.tick() }
        assertEquals("Usuário deve estar na frente do tele2 (7, 6)", tele2.getFrontPosition(), user.currentVector3.vector2)
        assertEquals("Tele1 deve estar fechado", "0", tele1.extraData)
        assertEquals("Tele2 deve estar fechado", "0", tele2.extraData)
    }
}
