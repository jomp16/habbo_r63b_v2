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
import ovh.rwx.habbo.game.item.wired.trigger.TimerTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_CLOCK_COUNTER)
class WiredTriggerClockCounter(room: Room, roomItem: RoomItem) : WiredTrigger<TimerTriggerData>(room, roomItem) {
    // O tempo alvo convertido totalmente para a unidade de "meios-segundos" (0.5s)
    private var targetTimeHalfSeconds = 0

    override val requiresItems = true

    init {
        setData()
    }

    override fun code() = WiredTriggerType.CLOCK_REACH_TIME.code

    override fun setData() {
        roomItem.wiredData?.let {
            val seconds = it.options.getOrElse(0) { 0 }
            val minutes = it.options.getOrElse(1) { 0 }
            val fractionalSeconds = it.options.getOrElse(2) { 0 }

            // Converte tudo para a base de 0.5s.
            // Se fractionalSeconds for maior que 0, adicionamos 1 (que equivale a 500ms)
            val extraHalfSecond = if (fractionalSeconds > 0) 1 else 0

            targetTimeHalfSeconds = (minutes * 120) + (seconds * 2) + extraHalfSecond
        }
    }

    override fun onTrigger(wiredContext: WiredContext, data: TimerTriggerData): Boolean {
        // Recebemos o Par (ID do Cronômetro, Tempo Atual em 0.5s)
        val tickingClockId = data.furniId
        val currentClockTimeHalfSeconds = data.time

        // REGRA 1: O cronômetro que apitou foi selecionado neste Wired?
        val selectedClocks = wiredContext.getEffectiveFurnis(this)
        if (selectedClocks.find { it.id == tickingClockId } == null) {
            return false // Ignora, foi um cronômetro de outro jogo na mesma sala
        }

        // REGRA 2: O tempo é o exato configurado?
        return currentClockTimeHalfSeconds == targetTimeHalfSeconds
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            // Padrão do client para os 3 ints (segundos, minutos, fração)
            return WiredData(0, 0, emptyList(), "", listOf(0, 0, 0), "")
        }
    }
}
