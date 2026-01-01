/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.item.wired.effect.effects

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.model.SquareState
import ovh.rwx.habbo.util.Utils
import ovh.rwx.habbo.util.Vector2

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_MOVE_TO_DIRECTION)
class WiredEffectMoveToDirection(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var startDirection: Direction = Direction.NORTH
    private var turnBehavior: TurnBehavior = TurnBehavior.WAIT
    private var blockUserMovement: Boolean = false

    // Store current direction for each item
    private val itemDirections = mutableMapOf<Int, Direction>()

    init {
        setData()
    }

    override fun code() = WiredEffectType.MOVE_TO_DIRECTION.code
    override fun requiresItems() = true

    override fun setData() {
        roomItem.wiredData?.let {
            startDirection = Direction.fromCode(it.options.getOrElse(0) { 0 })
            turnBehavior = TurnBehavior.fromCode(it.options.getOrElse(1) { 0 })
            blockUserMovement = it.options.getOrElse(2) { 0 } == 1
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val targets = wiredContext.getEffectiveFurnis(this)
        if (targets.isEmpty()) return

        targets.forEach { item ->
            if (!itemDirections.containsKey(item.id)) {
                itemDirections[item.id] = startDirection
            }

            val currentDirection = itemDirections[item.id]!!
            val nextPosition = getNextPosition(item.position.vector2, currentDirection)

            // 1. Pegamos a largura e altura do item
            val width = item.furnishing.width
            val height = item.furnishing.height

            // 2. Calculamos quais quadrados ele ocuparia SE se movesse para nextPosition
            // Nota: Mantemos a item.rotation atual, pois o wired move sem girar o corpo (exceto colisão)
            val affectedTiles = HabboServer.habboGame.itemManager.getAffectedTiles(
                nextPosition.x,
                nextPosition.y,
                item.rotation,
                width,
                height
            )

            // 3. Verificamos se ALGUM desses quadrados está bloqueado
            // Usamos o seu método isPositionBlocked corrigido (do turno anterior)
            val isBlocked = affectedTiles.any { tile -> isPositionBlocked(tile) }

            if (isBlocked) {
                // TODO: Acionar Wired Trigger: Collision (Triggers quando o mobi bate na parede/mobis)
                // Exemplo: wiredHandler.onEvent(WiredTriggerType.COLLISION, item)

                // Lógica de Colisão (Girar/Rebater)
                val newDirection = handleBlockedMovement(currentDirection)
                itemDirections[item.id] = newDirection

                val visualRotation = if (item.furnishing.interactionModesCount == 4) {
                    (newDirection.code / 2) * 2
                } else {
                    newDirection.code
                }

                if (turnBehavior != TurnBehavior.WAIT) {
                    // Aqui mandamos rollerId = -1 ou 0 dependendo da sua lógica de giro no lugar
                    // Para girar no lugar, geralmente é um movimento instantâneo (-1)
                    room.setFloorItem(item, item.position.vector2, visualRotation, null, rollerId = -1)
                }
            } else {
                // Caminho livre! Mover.
                // rollerId = -2 parece ser um código interno seu para wired,
                // ou use 0 se seguir a lógica que discutimos antes (animação)
                room.setFloorItem(item, nextPosition, item.rotation, null, rollerId = 0)
            }
        }
    }

    private fun isPositionBlocked(position: Vector2): Boolean {
        // 1. Verificação de Limites do Mapa
        if (!room.roomGamemap.grid.isInside(position.x, position.y)) {
            return true
        }

        // 2. Verificação de Bloqueio Físico (Paredes, Itens)
        // Passamos ignoreUsers=true pois vamos tratar usuários separadamente na configuração do Wired
        if (room.roomGamemap.isBlocked(position, ignoreUsers = true)) {

            // Se o quadrado está FECHADO (parede ou buraco), bloqueia sempre.
            // Isso impede que a lógica de empilhar abaixo libere entrada em paredes.
            if (room.roomModel.squareStates[position.x][position.y] == SquareState.CLOSED) {
                return true
            }

            // Se está bloqueado, mas o item que está lá PERMITE empilhar (cannotStackItem == false),
            // então para o Wired isso NÃO é um bloqueio.
            // Se cannotStackItem for true, aí sim bloqueia.
            if (room.roomGamemap.cannotStackItem[position.x][position.y]) {
                return true
            }
        }

        // 3. Verificação de Colisão com Usuários (Se o Wired não estiver configurado para "Atravessar")
        if (blockUserMovement) {
            // OTIMIZAÇÃO: Usar o lookup do gamemap é O(1), enquanto values.any é O(n)
            val usersOnTile = room.roomGamemap.getUsersFromVector2(position)
            if (usersOnTile.isNotEmpty()) {
                return true
            }
        }

        return false
    }

    private fun getNextPosition(currentPos: Vector2, direction: Direction): Vector2 {
        return when (direction) {
            Direction.NORTH -> Vector2(currentPos.x, currentPos.y - 1)
            Direction.NORTH_EAST -> Vector2(currentPos.x + 1, currentPos.y - 1)
            Direction.EAST -> Vector2(currentPos.x + 1, currentPos.y)
            Direction.SOUTH_EAST -> Vector2(currentPos.x + 1, currentPos.y + 1)
            Direction.SOUTH -> Vector2(currentPos.x, currentPos.y + 1)
            Direction.SOUTH_WEST -> Vector2(currentPos.x - 1, currentPos.y + 1)
            Direction.WEST -> Vector2(currentPos.x - 1, currentPos.y)
            Direction.NORTH_WEST -> Vector2(currentPos.x - 1, currentPos.y - 1)
        }
    }

    private fun handleBlockedMovement(currentDirection: Direction): Direction {
        return when (turnBehavior) {
            TurnBehavior.WAIT -> currentDirection
            TurnBehavior.TURN_RIGHT_45 -> currentDirection.turnRight45()
            TurnBehavior.TURN_RIGHT_90 -> currentDirection.turnRight90()
            TurnBehavior.TURN_LEFT_45 -> currentDirection.turnLeft45()
            TurnBehavior.TURN_LEFT_90 -> currentDirection.turnLeft90()
            TurnBehavior.TURN_AROUND -> currentDirection.turnAround()
            TurnBehavior.RANDOM_DIRECTION -> Direction.fromCode(Utils.randInt(0..7))
        }
    }

    private enum class Direction(val code: Int) {
        NORTH(0),
        NORTH_EAST(1),
        EAST(2),
        SOUTH_EAST(3),
        SOUTH(4),
        SOUTH_WEST(5),
        WEST(6),
        NORTH_WEST(7);

        fun turnRight45() = fromCode((code + 1) % 8)
        fun turnRight90() = fromCode((code + 2) % 8)
        fun turnLeft45() = fromCode(if (code - 1 < 0) 7 else code - 1)
        fun turnLeft90() = fromCode(if (code - 2 < 0) code + 6 else code - 2)
        fun turnAround() = fromCode((code + 4) % 8)

        companion object {
            fun fromCode(code: Int) = values().find { it.code == code } ?: NORTH
        }
    }

    private enum class TurnBehavior(val code: Int) {
        WAIT(0),
        TURN_RIGHT_45(1),
        TURN_RIGHT_90(2),
        TURN_LEFT_45(3),
        TURN_LEFT_90(4),
        TURN_AROUND(5),
        RANDOM_DIRECTION(6);

        companion object {
            fun fromCode(code: Int) = values().find { it.code == code } ?: WAIT
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, 0, 0), "")
        }
    }
}