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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemInteractor
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.slide.ObjectSlide
import ovh.rwx.habbo.game.room.slide.SlideItem
import ovh.rwx.habbo.util.Vector3

@Suppress("unused")
class RollerItemInteractor : ItemInteractor() {
    override val interactionType = listOf(InteractionType.ROLLER)

    override fun onCycle(room: Room, roomItem: RoomItem) {
        super.onCycle(room, roomItem)
        val frontVector2 = roomItem.getFrontPosition()
        val frontHeight = room.roomGamemap.getAbsoluteHeight(frontVector2)
        val frontVector3 = Vector3(frontVector2, frontHeight)
        var reCycle = true

        if (!room.roomGamemap.isBlocked(frontVector2)) {
            // 1. Moving players (Processamento individual pois geralmente há apenas 1)
            room.roomGamemap.getUsersFromVector2(roomItem.position.vector2).filter { !it.walking }.forEach {
                val oldPos = it.currentVector3.copy()

                if (it.moveTo(frontVector2, rollerId = roomItem.id)) {
                    val userSlide = ObjectSlide.createUserSlide(oldPos, frontVector3, roomItem.id, it.virtualID)
                    room.sendHabboResponse(Outgoing.ROOM_OBJECT_SLIDE, userSlide)
                    room.sendHabboResponse(OutgoingR63A.ROOM_OBJECT_SLIDE, userSlide)
                    reCycle = false
                }
            }

            // 2. Moving items (Agrupamento da pilha)
            room.roomGamemap.getItemsFromVector2(roomItem.position.vector2)
                .filter { it.id != roomItem.id && it.position.z > roomItem.position.z }
                .let { itemsAtPos ->
                    val roomItems = if (itemsAtPos.size > 10) itemsAtPos.take(10) else itemsAtPos
                    val slideItems = mutableListOf<SlideItem>()

                    roomItems.forEach { itemToMove ->
                        val zSrc = itemToMove.position.z // Guardamos o Z antes de mover

                        // Chamamos o setFloorItem com sendSlide = false para evitar pacotes duplicados
                        if (room.itemManager.setFloorItem(itemToMove, frontVector2, itemToMove.rotation, null)) {
                            // Adicionamos à lista para o pacote único
                            slideItems.add(SlideItem(itemToMove.id, zSrc, itemToMove.position.z))
                            reCycle = false
                        }
                    }

                    // Se a lista não estiver vazia, enviamos o "Bundle" (pacote agrupado)
                    if (slideItems.isNotEmpty()) {
                        val batchSlide = ObjectSlide(
                            source = roomItem.position, // Origem: Onde o roller está
                            target = frontVector3,      // Destino: Para onde o roller aponta
                            rollerId = roomItem.id,
                            items = slideItems
                        )
                        room.sendHabboResponse(Outgoing.ROOM_OBJECT_SLIDE, batchSlide)
                        room.sendHabboResponse(OutgoingR63A.ROOM_OBJECT_SLIDE, batchSlide)
                    }
                }
        }

        if (reCycle) roomItem.requestCycles(HabboServer.habboConfig.timerConfig.roller)
    }
}