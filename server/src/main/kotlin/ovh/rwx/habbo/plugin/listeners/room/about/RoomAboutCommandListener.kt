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

package ovh.rwx.habbo.plugin.listeners.room.about

import org.apache.commons.lang3.time.DurationFormatUtils
import ovh.rwx.habbo.BuildConfig
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.misc.NotificationType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.plugin.event.events.room.annotation.Command
import ovh.rwx.habbo.util.Utils
import ovh.rwx.utils.plugin.api.PluginListener
import java.lang.management.ManagementFactory
import java.time.ZoneId

@Suppress("unused", "UNUSED_PARAMETER")
class RoomAboutCommandListener : PluginListener() {
    @Command(["about", "info", "online", "server"])
    fun about(room: Room, roomUser: RoomUser, args: List<String>) {
        roomUser.habboSession?.sendNotification(NotificationType.MOTD_ALERT,
                BuildConfig.NAME +
                        "\n\n" +
                        "Version: ${BuildConfig.VERSION}" +
                        "\n\n" +
                        "Built at: ${HabboServer.DATE_TIME_FORMATTER_WITH_HOURS.format(BuildConfig.BUILD_INSTANT.atZone(ZoneId.systemDefault()).toLocalDateTime())}" +
                        "\n\n" +
                        "Git commit hash: ${BuildConfig.GIT_COMMIT_FULL}. Dirty: ${BuildConfig.GIT_IS_DIRTY}" +
                        "\n\n" +
                        "Written in Java and Kotlin by jomp16 and Lucas." +
                        "\n\n" +
                        "Licensed under GPLv3 and LGPLv3." +
                        "\n\n" +
                        "Source code at: https://git.rwx.ovh/Habbo/habbo_r63b_v2" +
                        "\n\n" +
                        "- Uptime: ${DurationFormatUtils.formatDurationWords(ManagementFactory.getRuntimeMXBean().uptime, true, false)}." +
                        "\n" +
                        "- RAM usage: ${Utils.ramUsageString}." +
                        "\n" +
                        "- Total memory: ${Utils.humanReadableByteCount(Runtime.getRuntime().totalMemory(), true)}." +
                        "\n" +
                        "- Processor cores: ${Runtime.getRuntime().availableProcessors()}." +
                        "\n" +
                        "- Online users: ${HabboServer.habboSessionManager.habboSessions.size}." +
                        "\n" +
                        "- Loaded rooms: ${HabboServer.habboGame.roomManager.roomTaskManager.rooms.size}." +
                        "\n" +
                        "- Catalog pages: ${HabboServer.habboGame.catalogManager.catalogPages.size}." +
                        "\n" +
                        "- Catalog items: ${HabboServer.habboGame.catalogManager.catalogItems.size}." +
                        "\n" +
                        "- Furnis: ${HabboServer.habboGame.itemManager.furnishings.size}." +
                        "\n" +
                        "- Available releases: ${HabboServer.habboHandler.releases.size}." +
                        "\n" +
                        "- Current logged release: ${roomUser.habboSession.release}." +
                        "\n" +
                        "- Loaded plugins jar: ${HabboServer.pluginManager.pluginsJar.size}." +
                        "\n" +
                        "- Loaded plugin listeners: ${HabboServer.pluginManager.pluginsListener.size}." +
                        "\n\n\n" +
                        "Have fun!"
        )
    }
}