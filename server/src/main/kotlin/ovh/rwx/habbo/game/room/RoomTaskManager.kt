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

import java.util.concurrent.CopyOnWriteArraySet

class RoomTaskManager {
    val rooms: MutableSet<Room> = CopyOnWriteArraySet()

    // synchronized(room): quem chega enquanto OUTRA thread ainda está em startLoop() espera ela terminar, em vez de
    // sair na hora de um `rooms.add` falso e seguir com a sala ainda "não rodando" — aí `RoomUserManager.addUser`
    // descartava o usuário em silêncio (sem resposta), e ele ficava parado até o timeout de ociosidade. Com muitos
    // usuários entrando juntos numa sala fria (ex.: reconexão em massa), alguns eram perdidos assim. Início e parada
    // da mesma sala também não se intercalam mais. O monitor é o mesmo do `Room.initialize` (reentrante).
    fun addRoom(room: Room) {
        synchronized(room) {
            if (!rooms.add(room)) return
            room.startLoop()
        }
    }

    fun removeRoom(room: Room) {
        synchronized(room) {
            if (!rooms.remove(room)) return
            room.stopLoop()
        }
    }
}
