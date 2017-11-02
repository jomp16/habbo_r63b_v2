/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.plugin.listeners.room.about

import org.apache.commons.lang3.time.DurationFormatUtils
import tk.jomp16.habbo.BuildConfig
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.misc.NotificationType
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.plugin.event.events.room.annotation.Command
import tk.jomp16.habbo.util.Utils
import tk.jomp16.utils.plugin.api.PluginListener
import java.lang.management.ManagementFactory

@Suppress("unused", "UNUSED_PARAMETER")
class RoomAboutCommandListener : PluginListener() {
    @Command(["about", "info", "online", "server"])
    fun about(room: Room, roomUser: RoomUser, args: List<String>) {
        roomUser.habboSession?.sendNotification(NotificationType.MOTD_ALERT,
                BuildConfig.NAME +
                        "\n\n" +
                        "Version: ${BuildConfig.VERSION}" +
                        "\n\n" +
                        "Written in Java and Kotlin by jomp16 and Lucas." +
                        "\n\n" +
                        "Licensed under GPLv3 and LGPLv3." +
                        "\n\n" +
                        "Source code at: https://git.jomp16.tk/Habbo/habbo_r63b_v2" +
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