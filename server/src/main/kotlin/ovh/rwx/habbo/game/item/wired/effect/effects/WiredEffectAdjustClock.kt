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
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.games.RoomGameClockAdjustMode
import ovh.rwx.habbo.game.room.games.RoomGameType
import java.time.Duration

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_ADJUST_CLOCK)
class WiredEffectAdjustClock(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var duration = Duration.ZERO
    private var mode: RoomGameClockAdjustMode = RoomGameClockAdjustMode.INCREASE

    init {
        setData()
    }

    override fun code() = WiredEffectType.ADJUST_CLOCK.code
    override val requiresItems = false

    override fun setData() {
        roomItem.wiredData?.let {
            val seconds = it.options.getOrElse(0) { 0 }
            val minutes = it.options.getOrElse(1) { 0 }
            val fractionalSeconds = it.options.getOrElse(2) { 0 }
            mode = RoomGameClockAdjustMode.fromCode(it.options.getOrElse(3) { 0 })

            duration = Duration
                .ofMinutes(minutes.toLong())
                .plusSeconds(seconds.toLong())
                .plusMillis(if (fractionalSeconds > 0) fractionalSeconds * 500.toLong() else 0)
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val game = room.gameManager.getGame(RoomGameType.BATTLE_BANZAI) ?: return

        game.adjustClock(duration, mode)
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, 0, 0, 0), "")
        }
    }
}
