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

package ovh.rwx.habbo.communication.outgoing.wired

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.item.wired.*

@Suppress("unused", "UNUSED_PARAMETER")
class WiredMovementResponse {
    @Response(Outgoing.WIRED_MOVEMENT)
    fun response(habboResponse: HabboResponse, movements: List<WiredMoveEntry>) {
        habboResponse.apply {
            writeInt(movements.size) // Quantidade total de movimentos no pacote

            for (move in movements) {
                when (move) {
                    is WiredUserMove -> {
                        writeInt(0) // Tipo: User Move
                        writeInt(move.sourceX)
                        writeInt(move.sourceY)
                        writeInt(move.targetX)
                        writeInt(move.targetY)
                        writeUTF(move.sourceZ.toString())
                        writeUTF(move.targetZ.toString())
                        writeInt(move.userIndex)
                        writeInt(if (move.sliding) 1 else 0)
                        writeInt(move.animationTime)
                        writeInt(move.bodyDirection)
                        writeInt(move.headDirection)
                    }

                    is WiredFurniMove -> {
                        writeInt(1) // Tipo: Furni Move
                        writeInt(move.sourceX)
                        writeInt(move.sourceY)
                        writeInt(move.targetX)
                        writeInt(move.targetY)
                        writeUTF(move.sourceZ.toString())
                        writeUTF(move.targetZ.toString())
                        writeInt(move.furniId)
                        writeInt(move.animationTime)
                        writeInt(move.rotation)
                    }

                    is WiredWallItemMove -> {
                        writeInt(2) // Tipo: Wall Item Move
                        writeInt(move.itemId)
                        writeBoolean(move.isDirectionRight)
                        writeInt(move.oldWallX)
                        writeInt(move.oldWallY)
                        writeInt(move.oldOffsetX)
                        writeInt(move.oldOffsetY)
                        writeInt(move.newWallX)
                        writeInt(move.newWallY)
                        writeInt(move.newOffsetX)
                        writeInt(move.newOffsetY)
                        writeInt(move.animationTime)
                    }

                    is WiredUserDirection -> {
                        writeInt(3) // Tipo: User Direction Update
                        writeInt(move.userIndex)
                        writeInt(move.bodyDirection)
                        writeInt(move.headDirection)
                    }
                }
            }
        }
    }
}