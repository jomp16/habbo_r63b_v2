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

package ovh.rwx.habbo.plugin.listeners.room.about

import org.apache.commons.lang3.time.DurationFormatUtils
import ovh.rwx.habbo.BuildConfig
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.misc.NotificationType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.plugin.api.PluginListener
import ovh.rwx.habbo.plugin.event.events.room.annotation.Command
import ovh.rwx.habbo.util.Utils
import java.lang.management.ManagementFactory
import java.time.ZoneId

@Suppress("unused", "UNUSED_PARAMETER")
class RoomAboutCommandListener : PluginListener() {
    @Command(["about", "info", "online", "server"])
    fun about(room: Room, roomUser: RoomUser, args: List<String>) {
        roomUser.habboSession.sendNotification(
            NotificationType.MOTD_ALERT,
            """${BuildConfig.NAME}

Version: ${BuildConfig.VERSION}

Built at: ${
                HabboServer.DATE_TIME_FORMATTER_WITH_HOURS.format(
                    BuildConfig.BUILD_INSTANT.atZone(ZoneId.systemDefault()).toLocalDateTime()
                )
            }

Git commit hash: ${BuildConfig.GIT_COMMIT_FULL}.

Written in Java and Kotlin by jomp16 and Lucas.

Licensed under GPLv3 and LGPLv3.

Source code at: https://git.rwx.ovh/Habbo/habbo_r63b_v2

- Uptime: ${DurationFormatUtils.formatDurationWords(ManagementFactory.getRuntimeMXBean().uptime, true, false)}.
- RAM usage: ${Utils.ramUsageString}.
- Total memory: ${Utils.humanReadableByteCount(Runtime.getRuntime().totalMemory(), true)}.
- Processor cores: ${Runtime.getRuntime().availableProcessors()}.
- Online users: ${HabboServer.habboSessionManager.habboSessions.size}.
- Loaded rooms: ${HabboServer.habboGame.roomManager.roomTaskManager.rooms.size}.
- Catalog pages: ${HabboServer.habboGame.catalogManager.catalogPages.size}.
- Catalog items: ${HabboServer.habboGame.catalogManager.catalogItems.size}.
- Furnis: ${HabboServer.habboGame.itemManager.furnishings.size}.
- Available releases: ${HabboServer.habboHandler.releases.size}.
- Current logged release: ${roomUser.habboSession.release}.
- Loaded plugins jar: ${HabboServer.pluginManager.pluginsJar.size}.
- Loaded plugin listeners: ${HabboServer.pluginManager.pluginsListener.size}.


Have fun!"""
        )
    }
}