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

package ovh.rwx.habbo.communication.outgoing.room

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.slide.ObjectSlide

@Suppress("unused", "UNUSED_PARAMETER")
class RoomObjectSlideResponse {
    @Response(Outgoing.ROOM_OBJECT_SLIDE)
    @ResponseR63A(OutgoingR63A.ROOM_OBJECT_SLIDE)
    fun response(habboResponse: HabboResponse, slide: ObjectSlide) {
        habboResponse.apply {
            // Coordenadas base
            writeInt(slide.source.x)
            writeInt(slide.source.y)
            writeInt(slide.target.x)
            writeInt(slide.target.y)

            // Bloco de Móveis (Furniture)
            writeInt(slide.items.size)
            for (item in slide.items) {
                writeInt(item.id)
                writeUTF(item.zSrc.toString())
                writeUTF(item.zTgt.toString())
            }

            // O ID do Roller/Mobi causador
            writeInt(slide.rollerId)

            // Bloco de Usuário (Avatar)
            if (slide.user != null) {
                writeInt(slide.user.type.value)
                writeInt(slide.user.virtualId)
                writeUTF(slide.user.zSrc.toString())
                writeUTF(slide.user.zTgt.toString())
            } else {
                writeInt(0) // Indica que nenhum usuário se moveu
            }
        }
    }
}