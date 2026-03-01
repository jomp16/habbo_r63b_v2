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

package ovh.rwx.habbo.game.item.wired.effect.effects

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredFurniMove
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.model.SquareState
import ovh.rwx.habbo.util.Direction
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
    override val requiresItems = true
    override val allowedFurniSources = listOf(
        WiredFurniSource.SELECTED_ITEMS,
        WiredFurniSource.TRIGGERING_ITEM,
        WiredFurniSource.SELECTOR_ITEMS
    )

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

                val visualRotation = if (item.furnishing.allowedDirections.size == 4) {
                    (newDirection.code / 2) * 2
                } else {
                    newDirection.code
                }

                if (turnBehavior != TurnBehavior.WAIT) {
                    val oldPos = item.position.copy() // Salva posição original

                    if (room.setFloorItem(item, item.position.vector2, visualRotation, null)) {
                        // Adiciona ao acumulador do ciclo
                        wiredContext.batchedMovements.add(
                            WiredFurniMove(
                                furniId = item.id,
                                sourceX = oldPos.x,
                                sourceY = oldPos.y,
                                sourceZ = oldPos.z,
                                targetX = item.position.x,
                                targetY = item.position.y,
                                targetZ = item.position.z,
                                rotation = item.rotation
                            )
                        )
                    }
                }
            } else {
                // Caminho livre! Mover.
                val visualRotation = if (item.furnishing.allowedDirections.size == 4) {
                    (currentDirection.code / 2) * 2
                } else {
                    currentDirection.code
                }

                val oldPos = item.position.copy() // Salva posição original

                if (room.setFloorItem(item, nextPosition, visualRotation, null)) {
                    // Adiciona ao acumulador do ciclo
                    wiredContext.batchedMovements.add(
                        WiredFurniMove(
                            furniId = item.id,
                            sourceX = oldPos.x,
                            sourceY = oldPos.y,
                            sourceZ = oldPos.z,
                            targetX = item.position.x,
                            targetY = item.position.y,
                            targetZ = item.position.z,
                            rotation = item.rotation
                        )
                    )
                }
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
        val (dx, dy) = direction.getOffset()
        return Vector2(currentPos.x + dx, currentPos.y + dy)
    }

    private fun handleBlockedMovement(currentDirection: Direction): Direction {
        return when (turnBehavior) {
            TurnBehavior.WAIT -> currentDirection
            TurnBehavior.TURN_RIGHT_45 -> currentDirection.turnRight45()
            TurnBehavior.TURN_RIGHT_90 -> currentDirection.turnRight90()
            TurnBehavior.TURN_LEFT_45 -> currentDirection.turnLeft45()
            TurnBehavior.TURN_LEFT_90 -> currentDirection.turnLeft90()
            TurnBehavior.TURN_AROUND -> currentDirection.turnAround()
            TurnBehavior.RANDOM_DIRECTION -> Direction.fromCode((0..7).random())
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