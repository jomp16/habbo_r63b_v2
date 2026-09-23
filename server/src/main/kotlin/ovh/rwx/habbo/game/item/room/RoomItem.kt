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

package ovh.rwx.habbo.game.item.room

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.room.RoomItemPlacedData
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.*
import ovh.rwx.habbo.game.item.wired.trigger.FurniTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerBotReachesFurni
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerWalksOffFurni
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerWalksOnFurni
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomBot
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.util.Direction
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.io.Serializable

data class RoomItem(
    val id: Int,
    var userId: Int,
    var roomId: Int,
    val itemName: String,
    var extraData: String,
    var position: Vector3,
    var rotation: Int,
    var wallPosition: String,
    val limited: Boolean,
    val buildersClub: Boolean,
) : IHabboResponseSerialize, Serializable {
    var magicRemove: Boolean = false
    private val limitedItemData: LimitedItemData? by lazy { if (limited) ItemDao.getLimitedData(id) else null }
    val wiredData: WiredData? by lazy { ItemDao.getWiredData(id) }
    val furnishing: Furnishing
        get() = HabboServer.habboGame.itemManager.furnishings[itemName]!!
    val room: Room
        get() = HabboServer.habboGame.roomManager.rooms[roomId]!!
    val height: Double
        get() {
            // Se for multi-height (mais de 1 valor no banco), usamos a lista do banco baseada no estado atual
            if (furnishing.stackMultiple && furnishing.stackHeight.isNotEmpty()) {
                // Se extraData estiver vazio ou for texto inválido, assume 0
                val mode = extraData.toIntOrNull() ?: 0

                // Garante que o index está dentro do limite da array (0 até size-1)
                val safeIndex = mode.coerceIn(furnishing.stackHeight.indices)

                return furnishing.stackHeight[safeIndex]
            }

            if (!furnishing.canStack && !(furnishing.canSit || furnishing.canLay || furnishing.walkable)) return 0.toDouble()

            // Se não for multi-height, PRIORIZAMOS o Z do SWF.
            // Caso o SWF falhe ao carregar (null), usamos a base do banco ou 0.0
            return furnishing.swfDimensions?.z ?: furnishing.stackHeight.firstOrNull() ?: 0.0
        }
    val totalHeight: Double
        get() = position.z + height
    val affectedTiles: List<Vector2>
        get() = HabboServer.habboGame.itemManager.getAffectedTiles(
            position.x,
            position.y,
            rotation,
            furnishing.width,
            furnishing.length
        )
    private var ticks: Int = 0
    private var currentTick: Int = 0
    val interactingUsers: MutableMap<Int, RoomEntity> by lazy { HashMap<Int, RoomEntity>() }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        if (furnishing.type == ItemType.FLOOR) {
            serializeFloorItem(habboResponse)
        } else {
            serializeWallItem(habboResponse)
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        serializeHabboResponse(habboResponse, *params)
    }

    private fun serializeFloorItem(habboResponse: HabboResponse) {
        habboResponse.apply {
            writeInt(id)
            writeInt(furnishing.spriteId)
            writeInt(position.x)
            writeInt(position.y)
            writeInt(rotation)
            writeUTF(position.z.toString())

            // Altura (sizeZ) adicionada em 01/12/2015 (BUILD 201512012203)
            if (isVersionAtLeast(2015, 12, 1)) {
                writeUTF(height.toString())
            }

            HabboServer.habboGame.itemManager.writeExtradata(
                habboResponse,
                extraData,
                furnishing,
                limitedItemData,
                magicRemove,
            )

            writeInt(-1) // rentals / expiry

            // Usage Policy virou Int em 13/01/2012; antes era Boolean (desde 12/05/2011)
            if (isVersionAtLeast(2012, 1, 13)) {
                writeInt(if (furnishing.interactionModesCount > 1) 1 else 0)
            } else if (isVersionAtLeast(2011, 5, 12)) {
                writeBoolean(furnishing.interactionModesCount > 1)
            }

            // Owner ID presente a partir de 21/09/2011 (RELEASE63-36096)
            if (isVersionAtLeast(2011, 9, 21)) {
                writeInt(if (buildersClub) -12345678 else userId)
            }

            // Se o spriteId for negativo (< 0), o Flash Player em TODAS as versões (de 2010 até WIN63 2026)
            // espera ler uma String adicional (staticClass / customType) para instanciar o móvel diretamente
            // pelo nome textual da classe sem consultar o furnidata.xml numérico (usado pela Sulake para
            // anúncios e itens dinâmicos). Se não enviarmos essa String quando spriteId < 0, o parser do Flash
            // tenta ler os bytes seguintes como comprimento de string e quebra com EOFError, sumindo com os itens.
            if (furnishing.spriteId < 0) {
                writeUTF("") // staticClass (customType)
            }
        }
    }

    private fun serializeWallItem(habboResponse: HabboResponse) {
        habboResponse.apply {
            writeUTF(id.toString())
            writeInt(furnishing.spriteId)
            writeUTF(wallPosition)
            writeUTF(if (furnishing.interactionType == InteractionType.POST_IT) extraData.split(' ')[0] else extraData)

            if (isVersionAtLeast(2013, 2, 13)) {
                writeInt(-1) // secondsToExpiration
            }

            // Usage Policy (era Boolean em 2011, virou Int em 2012)
            if (isVersionAtLeast(2012, 1, 13)) {
                writeInt(if (furnishing.interactionModesCount > 1) 1 else 0)
            } else if (isVersionAtLeast(2011, 5, 12)) {
                writeBoolean(furnishing.interactionModesCount > 1)
            }

            // Owner ID
            if (isVersionAtLeast(2011, 9, 21)) {
                writeInt(if (buildersClub) -12345678 else userId)
            }
        }
    }

    fun update(updateDb: Boolean, updateClient: Boolean) {
        if (updateClient) {
            when (furnishing.type) {
                ItemType.WALL -> {
                    room.sendResponse(Outgoing.ROOM_WALL_ITEM_UPDATE, OutgoingR63A.ROOM_WALL_ITEM_UPDATE, this)
                }

                else -> {
                    room.sendResponse(Outgoing.ROOM_FLOOR_ITEM_UPDATE, OutgoingR63A.ROOM_FLOOR_ITEM_UPDATE, this)
                }
            }
        }

        if (updateDb) room.itemManager.addItemToSave(this)
    }

    fun addToRoom(room: Room, updateDb: Boolean, updateClient: Boolean, userName: String) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (furnishing.type) {
            ItemType.FLOOR -> {
                if (updateDb) room.itemManager.addItemToSave(this)
                if (updateClient) {
                    room.sendResponse(
                        Outgoing.ROOM_ITEM_ADDED,
                        OutgoingR63A.ROOM_ITEM_ADDED,
                        RoomItemPlacedData(this, userName)
                    )
                }
            }

            ItemType.WALL -> {
                if (updateDb) room.itemManager.addItemToSave(this)
                if (updateClient) {
                    room.sendResponse(
                        Outgoing.ROOM_WALL_ITEM_ADDED,
                        OutgoingR63A.ROOM_WALL_ITEM_ADDED,
                        RoomItemPlacedData(this, userName)
                    )
                }
            }

            else -> {}
        }
    }

    fun requestTicks(ticks1: Int) {
        if (currentTick == 0 || ticks1 == 0) {
            // Multiplicamos por 10 porque passaremos a contabilizar a cada 50ms (1 ciclo = 500ms = 10 ticks)
            ticks = ticks1 * 10
            currentTick = 0
        }
    }

    fun processTick() {
        // Se o item solicitou um tempo de espera (ex: porta fechar após 2 ciclos)
        if (ticks > 0) {
            if (++currentTick >= ticks) {

                // IMPORTANTE: Zeramos os ciclos ANTES de chamar o interactor.
                // Isso permite que o interactor chame requestTicks() novamente
                // se quiser criar um looping contínuo.
                ticks = 0
                currentTick = 0

                furnishing.interactor?.processTick(room, this)
            }
        }
    }

    fun onEntityWalksOn(roomEntity: RoomEntity, handleInteractor: Boolean) {
        if (handleInteractor) furnishing.interactor?.onUserWalksOn(room, roomEntity, this)

        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerWalksOnFurni::class,
            roomEntity,
            FurniTriggerData(this)
        )

        if (roomEntity is RoomBot) {
            room.itemManager.wiredHandler.triggerWired(
                WiredTriggerBotReachesFurni::class,
                roomEntity,
                FurniTriggerData(this)
            )
        }
    }

    fun onEntityWalksOff(roomEntity: RoomEntity, handleInteractor: Boolean) {
        if (handleInteractor) furnishing.interactor?.onUserWalksOff(room, roomEntity, this)

        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerWalksOffFurni::class,
            roomEntity,
            FurniTriggerData(this)
        )
    }

    fun canClose(): Boolean {
        var closeable = true

        affectedTiles.forEach {
            val roomEntities = room.roomGamemap.getEntitiesFromVector2(it)

            if (closeable) closeable = roomEntities.isEmpty()
        }

        return closeable
    }

    private fun getFrontRotation(front: Vector2) = Direction.calculate(front.x, front.y, position.x, position.y)

    fun getFrontRotation(): Int = when (rotation) {
        2 -> 6
        6 -> 2
        0 -> 4
        else -> 0
    }

    fun getFrontPosition(): Vector2 {
        var x = position.x
        var y = position.y

        when (rotation) {
            0 -> y--
            2 -> x++
            4 -> y++
            6 -> x--
        }

        return Vector2(x, y)
    }

    fun getBehindPosition(): Vector2 {
        var x = position.x
        var y = position.y

        when (rotation) {
            0 -> y++
            2 -> x--
            4 -> y--
            6 -> x++
        }

        return Vector2(x, y)
    }

    fun isTouching(pos: Vector3, rotation: Int, z: Double = (-1).toDouble()) = isTouching(pos, rotation, false, z)

    private fun isTouching(vector3: Vector3, rotation: Int, ignoreItemRotation: Boolean, z: Double): Boolean {
        if (z != (-1).toDouble() && z - vector3.z > 3.toDouble()) return false

        if (ignoreItemRotation) return !(rotation != -1 && rotation != getFrontRotation(vector3.vector2)) && (position.x == vector3.x && position.y == vector3.y ||
                vector3.x == position.x && vector3.y == position.y + 1 ||
                vector3.x == position.x - 1 && vector3.y == position.y + 1 ||
                vector3.x == position.x - 1 && vector3.y == position.y ||
                vector3.x == position.x + 1 && vector3.y == position.y + 1 ||
                vector3.x == position.x && vector3.y == position.y - 1 ||
                vector3.x == position.x + 1 && vector3.y == position.y - 1 ||
                vector3.x == position.x + 1 && vector3.y == position.y ||
                vector3.x == position.x - 1 && vector3.y == position.y - 1)

        return when {
            rotation != -1 && rotation != getFrontRotation() -> false
            position.x == vector3.x && position.y == vector3.y -> true
            else -> if (rotation == 2 || rotation == 6) vector3.x == (if (rotation == 6) position.x + 1 else position.x - 1) && vector3.y >= position.y && vector3.y < position.y + furnishing.width
            else vector3.y == (if (rotation == 0) position.y + 1 else position.y - 1) && vector3.x >= position.x && vector3.x < position.x + furnishing.length
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RoomItem) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
