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

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.interactors.TimerItemInteractor
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.games.RoomGameClockAdjustMode

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_ADJUST_CLOCK)
class WiredEffectAdjustClock(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {

    // Convertemos tudo para meios-segundos (0.5s) para simplificar a matemática
    private var durationHalfSeconds = 0
    private var mode: RoomGameClockAdjustMode = RoomGameClockAdjustMode.INCREASE

    init {
        setData()
    }

    override fun code() = WiredEffectType.ADJUST_CLOCK.code

    override val requiresItems = true

    override fun setData() {
        roomItem.wiredData?.let {
            val seconds = it.options.getOrElse(0) { 0 }
            val minutes = it.options.getOrElse(1) { 0 }
            val fractionalSeconds = it.options.getOrElse(2) { 0 }
            mode = RoomGameClockAdjustMode.fromCode(it.options.getOrElse(3) { 0 })

            // Converte tempo para base de 0.5s
            val extraHalfSecond = if (fractionalSeconds > 0) 1 else 0
            durationHalfSeconds = (minutes * 120) + (seconds * 2) + extraHalfSecond
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val targetItems = wiredContext.getEffectiveFurnis(this)

        targetItems.forEach { targetItem ->
            val interactor = targetItem.furnishing.interactor

            // Se o interactor do mobi for do tipo TimerItemInteractor, usamos a função centralizada
            if (interactor is TimerItemInteractor) {
                interactor.adjustClock(targetItem, durationHalfSeconds, mode)
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, 0, 0, 0), "")
        }
    }
}
