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

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemInteractor
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.effect.effects.WiredEffectControlClock
import ovh.rwx.habbo.game.item.wired.trigger.TimerTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerClockCounter
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.games.RoomGameClockAdjustMode
import ovh.rwx.habbo.game.room.user.RoomUser
import java.util.concurrent.ConcurrentHashMap

class TimerItemInteractor : ItemInteractor() {

    override val interactionType = listOf(
        InteractionType.BATTLE_BANZAI_COUNTER, InteractionType.ES_COUNTER, InteractionType.FBALL_COUNTER,
        InteractionType.UP_COUNTER
    )

    // O estado agora é mapeado pelo ID único do item. O Interactor continua stateless!
    private val itemStates = ConcurrentHashMap<Int, TimerState>()

    // Classe de dados simples para reter o estado de cada relógio físico
    private class TimerState {
        var running = false
        var timeHalfSeconds = 0
        var tickCounter = 0
        var configuredHalfSeconds = 60
    }

    private fun getState(roomItem: RoomItem): TimerState {
        return itemStates.getOrPut(roomItem.id) {
            val state = TimerState()
            // Ao carregar o item pela primeira vez, a memória é o que está no extraData (DB)
            val initialSecs = roomItem.extraData.toIntOrNull() ?: 30
            state.configuredHalfSeconds = initialSecs * 2
            state
        }
    }

    override fun onRemove(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        // ESSENCIAL: Previne fugas de memória (memory leaks) limpando o estado quando o item é recolhido
        itemStates.remove(roomItem.id)
    }

    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        if (!hasRights) return

        val state = getState(roomItem)
        val isCountingUp = roomItem.furnishing.interactionType == InteractionType.UP_COUNTER

        when (request) {
            1 -> { // Play / Pause
                state.running = !state.running
                if (state.running) {
                    var currentSecs = roomItem.extraData.toIntOrNull() ?: 0

                    if (!isCountingUp && currentSecs <= 0) {
                        currentSecs = state.configuredHalfSeconds / 2
                        roomItem.extraData = currentSecs.toString()
                        roomItem.update(updateDb = false, updateClient = true)
                    }

                    state.timeHalfSeconds = currentSecs * 2
                    state.tickCounter = 0

                    roomItem.requestCycles(1)
                    room.gameManager.getGameForItem(roomItem)?.start()
                } else {
                    room.gameManager.getGameForItem(roomItem)?.pause()
                }
            }

            2 -> { // Botão SET (Salva a configuração)
                if (!state.running) {
                    var currentSeconds = roomItem.extraData.toIntOrNull() ?: 0

                    if (isCountingUp) {
                        currentSeconds = 0
                    } else {
                        currentSeconds = ((currentSeconds / 30) * 30) + 30
                        if (currentSeconds > 300) currentSeconds = 30
                    }

                    // SALVA NA MEMÓRIA do Interactor
                    state.configuredHalfSeconds = currentSeconds * 2

                    state.timeHalfSeconds = state.configuredHalfSeconds
                    roomItem.extraData = currentSeconds.toString()
                    roomItem.update(updateDb = true, updateClient = true)
                }
            }
        }
    }

    override fun onCycle(room: Room, roomItem: RoomItem) {
        val state = itemStates[roomItem.id] ?: return
        if (!state.running) return

        val isCountingUp = roomItem.furnishing.interactionType == InteractionType.UP_COUNTER

        // Cada ciclo de 500ms equivale a 1 unidade de timeHalfSeconds
        if (isCountingUp) {
            state.timeHalfSeconds++
        } else {
            state.timeHalfSeconds--
        }

        // 1. Notifica Wireds (essencial para gatilhos de tempo)
        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerClockCounter::class,
            null,
            TimerTriggerData(roomItem.id, state.timeHalfSeconds)
        )

        // 2. Atualiza o visual a cada 1 segundo real (a cada 2 chamadas de 500ms)
        if (state.timeHalfSeconds % 2 == 0) {
            val secondsToDisplay = state.timeHalfSeconds / 2
            roomItem.extraData = secondsToDisplay.toString()
            roomItem.update(updateDb = false, updateClient = true)

            if (!isCountingUp && secondsToDisplay <= 0) {
                state.running = false
                room.gameManager.getGameForItem(roomItem)?.stop()
                return
            }
        }

        // Mantém o item "acordado" para o próximo Major Tick
        roomItem.requestCycles(1)
    }

    // Método exposto para receber comandos do WiredEffectControlClock
    fun applyWiredAction(room: Room, roomItem: RoomItem, action: WiredEffectControlClock.Action) {
        val state = getState(roomItem)
        val game = room.gameManager.getGameForItem(roomItem)

        when (action) {
            WiredEffectControlClock.Action.START -> {
                if (!state.running) {
                    state.running = true
                    roomItem.requestCycles(1)
                    game?.start()
                }
            }

            WiredEffectControlClock.Action.PAUSE -> {
                if (state.running) {
                    state.running = false
                    game?.pause()
                }
            }

            WiredEffectControlClock.Action.STOP -> {
                state.running = false
                state.timeHalfSeconds = 0
                roomItem.extraData = "0"
                roomItem.update(updateDb = false, updateClient = true)
                game?.stop()
            }

            WiredEffectControlClock.Action.RESET -> {
                val resetSeconds = game?.configuredTime ?: 0
                state.timeHalfSeconds = resetSeconds * 2
                roomItem.extraData = resetSeconds.toString()
                roomItem.update(updateDb = false, updateClient = true)
            }

            WiredEffectControlClock.Action.RESTART -> {
                if (!state.running) {
                    val resetSeconds = game?.configuredTime ?: 0
                    state.timeHalfSeconds = resetSeconds * 2
                    roomItem.extraData = resetSeconds.toString()
                    roomItem.update(updateDb = false, updateClient = true)

                    state.running = true
                    roomItem.requestCycles(1)
                    game?.start()
                }
            }
        }
    }

    fun adjustClock(roomItem: RoomItem, durationHalfSeconds: Int, mode: RoomGameClockAdjustMode) {
        val state = getState(roomItem)

        // 1. Calcula o novo tempo baseado no modo (Increase/Decrease/Set)
        val newHalfSeconds = when (mode) {
            RoomGameClockAdjustMode.INCREASE -> state.timeHalfSeconds + durationHalfSeconds
            RoomGameClockAdjustMode.DECREASE -> (state.timeHalfSeconds - durationHalfSeconds).coerceAtLeast(0)
            RoomGameClockAdjustMode.SET -> durationHalfSeconds
        }

        // 2. Aplica a mudança no estado interno do Interactor
        state.timeHalfSeconds = newHalfSeconds

        // 3. Atualiza o visual (extraData) para o cliente
        val secondsToDisplay = state.timeHalfSeconds / 2
        roomItem.extraData = secondsToDisplay.toString()

        // Se o jogo não estiver rodando, salvamos como o novo "tempo padrão" do mobi
        roomItem.update(updateDb = !state.running, updateClient = true)

        // 4. Se o tempo zerou enquanto o relógio estava rodando, encerra o jogo associado
        if (state.running && state.timeHalfSeconds <= 0) {
            state.running = false
            roomItem.room.gameManager.getGameForItem(roomItem)?.stop()
        }
    }
}
