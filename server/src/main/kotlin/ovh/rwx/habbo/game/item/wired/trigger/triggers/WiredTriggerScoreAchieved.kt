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
import ovh.rwx.habbo.game.room.games.GameTeam

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_SCORE_ACHIEVED)
class WiredTriggerScoreAchieved(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    private var gameTeam = GameTeam.NONE
    private var scoreRequirement = 0
    private var triggered = false

    init {
        setData()
    }

    override fun code() = WiredTriggerType.SCORE_ACHIEVED.code

    override fun setData() {
        roomItem.wiredData?.let {
            scoreRequirement = it.options.getOrElse(0) { 0 }
            gameTeam = GameTeam.fromColor(it.options.getOrElse(1) { GameTeam.NONE.color })
        }
    }

    override fun onTrigger(wiredContext: WiredContext, data: Any?): Boolean {
        val args = data as? List<*> ?: return false

        val team = args[0] as? GameTeam ?: GameTeam.NONE
        val score = args[1] as? Int ?: 0

        if (triggered) return false

        var scoreAchieved = score >= scoreRequirement

        if (gameTeam != GameTeam.NONE) {
            scoreAchieved = scoreAchieved && gameTeam == team
        }

        if (scoreAchieved) {
            triggered = true
        }

        return scoreAchieved
    }

    override fun resetTriggered() {
        triggered = false
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0), "")
        }
    }
}
