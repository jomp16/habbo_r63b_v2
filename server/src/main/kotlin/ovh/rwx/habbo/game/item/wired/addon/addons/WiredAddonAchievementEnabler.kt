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

package ovh.rwx.habbo.game.item.wired.addon.addons

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.addon.WiredAddon
import ovh.rwx.habbo.game.item.wired.addon.WiredAddonType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_ACHIEVEMENT_ENABLER)
class WiredAddonAchievementEnabler(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.ACHIEVEMENT_ENABLER

    override fun onAddon(wiredContext: WiredContext) {
        val message = roomItem.wiredData?.message ?: ""
        wiredContext.placeholders["achievement_enabler"] = message

        val user = (wiredContext.triggererUser as? RoomUser) ?: return
        val achievements = message.split("\r\n", "\n", ",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (ach in achievements) {
            try {
                HabboServer.habboGame.achievementManager.progress(user.habboSession, ach, 1, accumulate = true)
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
