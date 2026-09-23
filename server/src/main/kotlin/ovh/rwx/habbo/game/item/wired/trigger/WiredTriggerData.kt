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

package ovh.rwx.habbo.game.item.wired.trigger

import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerUserPerformsAction
import ovh.rwx.habbo.game.room.games.GameTeam
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomUser

/**
 * Base sealed class para todos os tipos de dados de trigger.
 * Usa sealed classes para garantir type safety e exhaustiveness checking.
 */
sealed class WiredTriggerData

/**
 * Dados para triggers que recebem um RoomItem (ex: click furni, walks on/off)
 */
data class FurniTriggerData(
    val roomItem: RoomItem
) : WiredTriggerData()

/**
 * Dados para triggers que recebem um RoomUser (ex: click user)
 */
data class UserTriggerData(
    val roomUser: RoomUser
) : WiredTriggerData()

/**
 * Dados para triggers de colisão (avatar caught)
 */
data object CollisionTriggerData : WiredTriggerData()

/**
 * Dados para triggers de entrada/saída de sala
 */
data object RoomEventTriggerData : WiredTriggerData()

/**
 * Dados para triggers periódicos e de tempo
 */
data object PeriodicTriggerData : WiredTriggerData()

/**
 * Dados para triggers de estado (máquinas, portas, dados, etc)
 */
data class StateTriggerData(
    val roomItem: RoomItem
) : WiredTriggerData()

/**
 * Dados para triggers de ação do usuário (sentar, deitar, etc)
 */
data class UserActionTriggerData(
    val action: WiredTriggerUserPerformsAction.WiredUserAction,
    val danceId: Int = 0,
    val signId: Int = 0,
) : WiredTriggerData()

/**
 * Dados para triggers de jogo (score, game starts/ends)
 */
data class GameTriggerData(
    val team: GameTeam,
    val score: Int
) : WiredTriggerData()

/**
 * Dados para trigger de dizer algo (texto falado)
 */
data class SayTriggerData(
    val text: String
) : WiredTriggerData()

/**
 * Dados para trigger de colisão com tile
 */
data class TileTriggerData(
    val x: Int,
    val y: Int
) : WiredTriggerData()

/**
 * Dados para triggers de timer/contador
 */
data class TimerTriggerData(
    val furniId: Int,
    val time: Int
) : WiredTriggerData()

/**
 * Dados para trigger de variável alterada
 */
data class VariableTriggerData(
    val variableId: String,
    val oldValue: Any? = null,
    val newValue: Any? = null
) : WiredTriggerData()

/**
 * Dados para trigger de sinal recebido
 */
data class SignalTriggerData(
    val antennaId: Int = 0,
    val signal: Int = 0,
    val signalUsers: List<RoomEntity> = emptyList(),
    val signalFurnis: List<RoomItem> = emptyList(),
    val context: WiredContext? = null
) : WiredTriggerData()

data object EmptyTriggerData : WiredTriggerData()