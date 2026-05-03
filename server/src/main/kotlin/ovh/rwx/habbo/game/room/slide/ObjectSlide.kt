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

package ovh.rwx.habbo.game.room.slide

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.wired.WiredFurniMove
import ovh.rwx.habbo.game.item.wired.WiredMoveEntry
import ovh.rwx.habbo.game.item.wired.WiredUserMove
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Vector3

data class ObjectSlide(
    val source: Vector3,
    val target: Vector3,
    val rollerId: Int = 0, // ID do mobi que causou o movimento (0 se não houver)
    val items: List<SlideItem> = emptyList(),
    val user: SlideUser? = null
) {
    fun toWiredMoveEntries(animationTime: Int = 500): List<WiredMoveEntry> {
        val entries = mutableListOf<WiredMoveEntry>()

        // 1. Converte todos os mobis (usando 'this' implicitamente)
        items.forEach { item ->
            entries.add(
                WiredFurniMove(
                    furniId = item.id,
                    sourceX = source.x,
                    sourceY = source.y,
                    sourceZ = item.zSrc,
                    targetX = target.x,
                    targetY = target.y,
                    targetZ = item.zTgt,
                    animationTime = animationTime,
                    rotation = 0
                )
            )
        }

        // 2. Converte o usuário (se houver)
        user?.let { u ->
            entries.add(
                WiredUserMove(
                    userIndex = u.virtualId,
                    sourceX = source.x,
                    sourceY = source.y,
                    sourceZ = u.zSrc,
                    targetX = target.x,
                    targetY = target.y,
                    targetZ = u.zTgt,
                    sliding = u.type == SlideType.SLIDE,
                    animationTime = animationTime,
                    bodyDirection = 0,
                    headDirection = 0
                )
            )
        }

        return entries
    }

    companion object {
        fun createItemSlide(source: Vector3, target: Vector3, rollerId: Int, itemId: Int): ObjectSlide {
            return ObjectSlide(
                source = source,
                target = target,
                rollerId = rollerId,
                items = listOf(SlideItem(itemId, source.z, target.z))
            )
        }

        fun createUserSlide(source: Vector3, target: Vector3, rollerId: Int, virtualId: Int): ObjectSlide {
            return ObjectSlide(
                source = source,
                target = target,
                rollerId = rollerId,
                user = SlideUser(virtualId, source.z, target.z)
            )
        }
    }
}

data class SlideItem(val id: Int, val zSrc: Double, val zTgt: Double)
data class SlideUser(val virtualId: Int, val zSrc: Double, val zTgt: Double, val type: SlideType = SlideType.SLIDE)

enum class SlideType(val value: Int) {
    MOVE(1),  // "mv"
    SLIDE(2)  // "sld"
}

fun Room.flushWiredMovements(movements: List<WiredMoveEntry>) {
    if (movements.isEmpty()) return

    this.userManager.entities.values.forEach { entity ->
        val habboSession = (entity as? RoomUser)?.habboSession ?: return@forEach

        val responseMethod = if (habboSession.release == "R63A") "DISABLED" else
            HabboServer.habboHandler.getOverrideMethodForHeader(Outgoing.WIRED_MOVEMENT, habboSession.release)

        if (responseMethod != "DISABLED") {
            habboSession.sendHabboResponse(Outgoing.WIRED_MOVEMENT, movements)
        } else {
            // Clientes antigos precisam receber os slides individualmente
            // Conversão reversa para clientes legados (R63A/R63B Flash)
            movements.forEach { move ->
                when (move) {
                    is WiredFurniMove -> {
                        val slide = ObjectSlide(
                            source = Vector3(move.sourceX, move.sourceY, move.sourceZ),
                            target = Vector3(move.targetX, move.targetY, move.targetZ),
                            rollerId = -2, // Assinatura genérica de Wired para o legado
                            items = listOf(SlideItem(move.furniId, move.sourceZ, move.targetZ))
                        )
                        if (habboSession.release == "R63A") {
                            habboSession.sendHabboResponse(OutgoingR63A.ROOM_OBJECT_SLIDE, slide)
                        } else {
                            habboSession.sendHabboResponse(Outgoing.ROOM_OBJECT_SLIDE, slide)
                        }

                        // O legado não gira mobis no Slide. Se rotacionou, enviamos um Update à força.
                        val item = this.itemManager.items[move.furniId]
                        if (item != null && item.rotation != move.rotation) {
                            item.rotation = move.rotation
                            item.update(updateDb = false, updateClient = true)
                        }
                    }

                    is WiredUserMove -> {
                        val slide = ObjectSlide(
                            source = Vector3(move.sourceX, move.sourceY, move.sourceZ),
                            target = Vector3(move.targetX, move.targetY, move.targetZ),
                            rollerId = -2,
                            user = SlideUser(
                                virtualId = move.userIndex,
                                zSrc = move.sourceZ,
                                zTgt = move.targetZ,
                                type = if (move.sliding) SlideType.SLIDE else SlideType.MOVE
                            )
                        )
                        if (habboSession.release == "R63A") {
                            habboSession.sendHabboResponse(OutgoingR63A.ROOM_OBJECT_SLIDE, slide)
                        } else {
                            habboSession.sendHabboResponse(Outgoing.ROOM_OBJECT_SLIDE, slide)
                        }

                        // O legado precisa do pacote UserUpdate para girar a cabeça/corpo no lugar
                        val roomEntity = this.userManager.entities[move.userIndex]
                        if (roomEntity != null && (roomEntity.bodyRotation != move.bodyDirection || roomEntity.headRotation != move.headDirection)) {
                            roomEntity.bodyRotation = move.bodyDirection
                            roomEntity.headRotation = move.headDirection
                            roomEntity.updateNeeded = true
                        }
                    }

                    else -> {
                        // Ignoramos WallItemMove e UserDirection no legado se não forem suportados
                    }
                }
            }
        }
    }
}