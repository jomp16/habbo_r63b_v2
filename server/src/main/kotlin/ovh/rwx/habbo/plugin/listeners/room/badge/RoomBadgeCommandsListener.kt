/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.plugin.listeners.room.badge

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.wired.WiredRewardNotificationResponse
import ovh.rwx.habbo.database.badge.BadgeDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.ChatType
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.plugin.event.events.room.annotation.Command
import ovh.rwx.utils.plugin.api.PluginListener

@Suppress("unused", "UNUSED_PARAMETER")
class RoomBadgeCommandsListener : PluginListener() {
    @Command(["givebadge"], permissionName = "cmd_givebadge")
    fun giveBadge(room: Room, roomUser: RoomUser, args: List<String>) {
        if (args.size <= 1) {
            roomUser.chat(roomUser.virtualID, "Excepted params: badge_name or badge_name username", 0, ChatType.WHISPER, true)

            return
        }

        if (roomUser.habboSession == null) return
        val badgeCode = args[1].toUpperCase()
        val username = if (args.size >= 3) args[2] else null

        if (username == null) {
            // give badge to user calling command
            giveBadge(roomUser.habboSession, badgeCode)
        } else {
            val userHabboSession = HabboServer.habboSessionManager.getHabboSessionByUsername(username)

            if (userHabboSession != null) {
                giveBadge(userHabboSession, badgeCode)

                return
            }
            val userId = UserInformationDao.getUserInformationByUsername(username)?.id

            if (userId != null) {
                val badges = BadgeDao.getBadges(userId)

                if (!badges.any { it.value.code == badgeCode }) BadgeDao.addBadge(userId, badgeCode, 0)

                return
            }

            roomUser.chat(roomUser.virtualID, "We couldn't find the user $username!", 0, ChatType.WHISPER, true)
        }
    }

    @Command(["removebadge"], permissionName = "cmd_removebadge")
    fun removeBadge(room: Room, roomUser: RoomUser, args: List<String>) {
        if (args.isEmpty()) {
            roomUser.chat(roomUser.virtualID, "Excepted params: badge_name or badge_name username", 0, ChatType.WHISPER, true)

            return
        }

        if (roomUser.habboSession == null) return
        val badgeCode = args[1].toUpperCase()
        val username = if (args.size >= 3) args[2] else null

        if (username == null) {
            // give badge to user calling command
            removeBadge(roomUser.habboSession, badgeCode)
        } else {
            val userHabboSession = HabboServer.habboSessionManager.getHabboSessionByUsername(username)

            if (userHabboSession != null) {
                removeBadge(userHabboSession, badgeCode)

                return
            }
            val userId = UserInformationDao.getUserInformationByUsername(username)?.id

            if (userId != null) {
                val badges = BadgeDao.getBadges(userId)

                if (badges.any { it.value.code == badgeCode }) BadgeDao.removeBadge(badges.values.find { it.code == badgeCode }!!.id)

                return
            }

            roomUser.chat(roomUser.virtualID, "We couldn't find the user $username!", 0, ChatType.WHISPER, true)
        }
    }

    private fun giveBadge(habboSession: HabboSession, badgeCode: String) {
        if (!habboSession.habboBadge.badges.containsKey(badgeCode)) {
            habboSession.habboBadge.addBadge(badgeCode)

            habboSession.sendHabboResponse(Outgoing.WIRED_REWARD_NOTIFICATION, WiredRewardNotificationResponse.WiredRewardNotification.BADGE_REWARDED)
        }
    }

    private fun removeBadge(habboSession: HabboSession, badgeCode: String) {
        if (habboSession.habboBadge.badges.containsKey(badgeCode)) habboSession.habboBadge.removeBadge(badgeCode)
    }
}