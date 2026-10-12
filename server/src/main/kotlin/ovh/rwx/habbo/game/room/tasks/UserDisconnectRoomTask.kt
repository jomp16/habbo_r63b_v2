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

package ovh.rwx.habbo.game.room.tasks

import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Cleanup de disconnect serializado na thread do quarto.
 *
 * O removeEntity não pode rodar inline na thread do Netty por dois motivos:
 * 1. `habboSession.roomUser` só é atribuído dentro do [UserJoinRoomTask]; se o socket morrer
 *    na janela entre o addUser e o próximo tick (500ms), o close() chamava
 *    removeEntity(null) — no-op — e a entidade ficava pra sempre no mapa (fantasma,
 *    bloqueando tile da porta e o re-login).
 * 2. Correr em paralelo com o join task corrompia o estado (remove durante o join).
 *
 * Buscar pela sessão e não pelo roomUser resolve ambos: se o join task já rodou, a
 * entidade é encontrada e removida com broadcast; se não rodou, o join task aborta
 * sozinho no check de canal morto.
 */
class UserDisconnectRoomTask(
    private val habboSession: HabboSession
) : IRoomTask {
    override fun executeTask(room: Room) {
        val roomUser = room.userManager.entities.values
                .filterIsInstance<RoomUser>()
                .find { it.habboSession === habboSession }
                ?: return

        room.userManager.removeEntity(roomUser, notifyClient = false, kickNotification = false)
    }
}
