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
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_GIVE_SCORE)
class WiredEffectGiveScore(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var score: Int = 0

    init {
        setData()
    }

    override fun code() = WiredEffectType.GIVE_SCORE.code
    override val requiresItems = false

    override fun setData() {
        roomItem.wiredData?.let {
            score = it.options.getOrElse(0) { 0 }
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val targets = wiredContext.getEffectiveUsers(this)

        targets.forEach { user ->
            (user as? RoomUser)?.habboSession?.let { session ->
                // todo
                /*val userStats = HabboServer.habboGame.userManager.getUserStats(session.userInformation.id)
                userStats?.let {
                    it.score += score
                    HabboServer.habboGame.userManager.saveUserStats(it)
                    
                    session.sendHabboResponse(
                        Outgoing.GUILD_SCORE_UPDATE,
                        it.score
                    )
                }*/
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0), "")
        }
    }
}
