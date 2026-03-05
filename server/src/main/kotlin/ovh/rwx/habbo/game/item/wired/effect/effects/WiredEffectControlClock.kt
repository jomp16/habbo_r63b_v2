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

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_CONTROL_CLOCK)
class WiredEffectControlClock(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var action: Action = Action.START

    init {
        setData()
    }

    override fun code() = WiredEffectType.CONTROL_CLOCK.code

    override val requiresItems = true

    override fun setData() {
        roomItem.wiredData?.let {
            action = Action.fromCode(it.options.getOrElse(0) { Action.START.code })
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val targetItems = wiredContext.getEffectiveFurnis(this)

        targetItems.forEach { targetItem ->
            // Checamos se o item selecionado é realmente um cronômetro e possui o Interactor de tempo
            val interactor = targetItem.furnishing.interactor as? TimerItemInteractor

            // Se for um cronômetro válido, aplicamos a ação diretamente nele
            interactor?.applyWiredAction(room, targetItem, action)
        }
    }

    enum class Action(val code: Int) {
        START(0),
        STOP(1),
        RESET(2),
        PAUSE(3),
        RESTART(4);

        companion object {
            fun fromCode(code: Int) = entries.find { it.code == code } ?: START
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0), "")
        }
    }
}