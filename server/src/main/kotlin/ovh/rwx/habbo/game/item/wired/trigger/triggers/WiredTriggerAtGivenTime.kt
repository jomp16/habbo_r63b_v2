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

package ovh.rwx.habbo.game.item.wired.trigger.triggers

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_AT_GIVEN_TIME)
class WiredTriggerAtGivenTime(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    private var targetTime = 1 // Padrão: 1 tick de 500ms (0.5s)
    private var hasTriggered = false

    override val requiresItems = true

    init {
        setData()
    }

    override fun code() = WiredTriggerType.TRIGGER_ONCE.code

    override fun setData() {
        roomItem.wiredData?.let {
            // O cliente envia em intervalos de 0.5s (que equivalem perfeitamente ao nosso Major Tick de 500ms)
            // Trivia Habbo: Limites entre 0.5s (1) e 600s (1200)
            targetTime = it.options.getOrElse(0) { 1 }.coerceIn(1, 1200)
        }
    }

    override fun onTrigger(wiredContext: WiredContext, data: Any?): Boolean {
        // O roomTimer é incrementado a cada 500ms pelo seu Major Tick no RoomTask
        val currentTime = room.roomTimer.get()

        // Se o timer da sala for menor que o alvo, significa que o quarto acabou de ser carregado
        // ou o "WIRED Effect: Timer Reset" zerou o roomTimer. Nesse caso, rearmamos o gatilho.
        if (currentTime < targetTime) {
            hasTriggered = false
            return false
        }

        // Se alcançou/passou o tempo e ainda não disparou neste ciclo, nós disparamos.
        if (!hasTriggered) {
            hasTriggered = true
            return true
        }

        return false
    }

    fun resetTimer() {
        hasTriggered = false
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(1), "") // Configuração inicial padrão para 0.5s
        }
    }
}