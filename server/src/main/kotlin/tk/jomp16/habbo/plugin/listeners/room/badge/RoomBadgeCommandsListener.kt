package tk.jomp16.habbo.plugin.listeners.room.badge

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.database.badge.BadgeDao
import tk.jomp16.habbo.database.information.UserInformationDao
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.tasks.ChatType
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.plugin.event.events.room.annotation.Command
import tk.jomp16.utils.plugin.api.PluginListener

@Suppress("unused", "UNUSED_PARAMETER")
class RoomBadgeCommandsListener : PluginListener() {
    @Command(arrayOf("givebadge"), permissionName = "cmd_givebadge")
    fun giveBadge(room: Room, roomUser: RoomUser, args: List<String>) {
        if (args.isEmpty()) {
            roomUser.chat(roomUser.virtualID, "Excepted params: badge_name or badge_name username", 0, ChatType.WHISPER, true)

            return
        }

        val badgeCode = args[1].toUpperCase()
        val username = if (args.size >= 3) args[2] else null

        if (username == null && roomUser.habboSession != null) {
            // give badge to user calling command
            giveBadge(roomUser.habboSession, badgeCode)
        } else if (username != null) {
            val userHabboSession = HabboServer.habboSessionManager.getHabboSessionByUsername(username)

            if (userHabboSession != null) {
                giveBadge(userHabboSession, badgeCode)

                return
            }

            val userId = UserInformationDao.getUserInformationByUsername(username)?.id

            if (userId != null) {
                val badges = BadgeDao.getBadges(userId)

                if (!badges.any { it.code == badgeCode }) BadgeDao.addBadge(userId, badgeCode, 0)

                return
            }

            roomUser.chat(roomUser.virtualID, "We couldn't find the user $username!", 0, ChatType.WHISPER, true)
        }
    }

    @Command(arrayOf("removebadge"), permissionName = "cmd_removebadge")
    fun removeBadge(room: Room, roomUser: RoomUser, args: List<String>) {
        if (args.isEmpty()) {
            roomUser.chat(roomUser.virtualID, "Excepted params: badge_name or badge_name username", 0, ChatType.WHISPER, true)

            return
        }

        val badgeCode = args[1].toUpperCase()
        val username = if (args.size >= 3) args[2] else null

        if (username == null && roomUser.habboSession != null) {
            // give badge to user calling command
            giveBadge(roomUser.habboSession, badgeCode)
        } else if (username != null) {
            val userHabboSession = HabboServer.habboSessionManager.getHabboSessionByUsername(username)

            if (userHabboSession != null) {
                removeBadge(userHabboSession, badgeCode)

                return
            }

            val userId = UserInformationDao.getUserInformationByUsername(username)?.id

            if (userId != null) {
                val badges = BadgeDao.getBadges(userId)

                if (badges.any { it.code == badgeCode }) BadgeDao.removeBadge(badges.find { it.code == badgeCode }!!.id)

                return
            }

            roomUser.chat(roomUser.virtualID, "We couldn't find the user $username!", 0, ChatType.WHISPER, true)
        }
    }

    private fun giveBadge(habboSession: HabboSession, badgeCode: String) {
        if (!habboSession.habboBadge.badges.containsKey(badgeCode)) habboSession.habboBadge.addBadge(badgeCode)
    }

    private fun removeBadge(habboSession: HabboSession, badgeCode: String) {
        if (habboSession.habboBadge.badges.containsKey(badgeCode)) habboSession.habboBadge.removeBadge(badgeCode)
    }
}