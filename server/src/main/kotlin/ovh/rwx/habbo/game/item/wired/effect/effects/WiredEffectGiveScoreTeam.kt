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
import ovh.rwx.habbo.game.room.games.GameTeam

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_GIVE_SCORE_TEAM)
class WiredEffectGiveScoreTeam(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var gameTeam = GameTeam.NONE
    private var score: Int = 0

    init {
        setData()
    }

    override fun code() = WiredEffectType.GIVE_SCORE_TO_PREDEFINED_TEAM.code
    override val requiresItems = false

    override fun setData() {
        roomItem.wiredData?.let {
            gameTeam = GameTeam.fromColor(it.options.getOrElse(0) { GameTeam.NONE.color })
            score = it.options.getOrElse(1) { 0 }
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        room.gameManager.games.values.forEach { game ->
            game.addScore(gameTeam, score)
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, 0), "")
        }
    }
}
