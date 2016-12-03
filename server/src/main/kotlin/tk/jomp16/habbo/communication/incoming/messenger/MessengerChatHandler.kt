/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.communication.incoming.messenger

import org.apache.commons.lang3.time.DurationFormatUtils
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.messenger.MessengerDao
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.kotlin.urlUserAgent
import tk.jomp16.habbo.util.Utils
import java.io.InputStreamReader
import java.lang.management.ManagementFactory

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerChatHandler {
    @Handler(Incoming.MESSENGER_CHAT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initializedMessenger) return

        val userId = habboRequest.readInt()
        val message = habboRequest.readUTF()

        if (userId <= 0 || message.isBlank() || !habboSession.habboMessenger.friends.containsKey(userId)) return

        if (userId == Int.MAX_VALUE && habboSession.hasPermission("acc_server_console")) {
            // server console!

            val args = message.split(' ')

            if (args.isNotEmpty()) {
                habboSession.scriptEngine.put("habboSession", habboSession)
                habboSession.scriptEngine.put("habboServer", HabboServer)
                habboSession.scriptEngine.put("habboGame", HabboServer.habboGame)
                habboSession.scriptEngine.put("room", habboSession.currentRoom)
                habboSession.scriptEngine.put("roomUser", habboSession.roomUser)

                if (args[0] == "load" && args.size >= 2) {
                    val jsOutput = habboSession.scriptEngine.eval(InputStreamReader(urlUserAgent(args[1]).inputStream))?.toString() ?: "null"

                    habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, Int.MAX_VALUE, jsOutput, 0)
                } else if (args[0] == "ram") {
                    habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, Int.MAX_VALUE, Utils.ramUsageString, 0)
                } else if (args[0] == "uptime") {
                    habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, Int.MAX_VALUE, DurationFormatUtils.formatDurationWords(ManagementFactory.getRuntimeMXBean().uptime, true, false) + " up!", 0)
                } else {
                    val jsOutput = habboSession.scriptEngine.eval(message)?.toString() ?: "null"

                    habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, Int.MAX_VALUE, jsOutput, 0)
                }
            }

            return
        }

        val messengerBuddy = habboSession.habboMessenger.friends[userId] ?: return

        if (!messengerBuddy.online) {
            MessengerDao.addOfflineMessage(habboSession.userInformation.id, userId, message)

            return
        }

        val friendHabboSession = messengerBuddy.habboSession ?: return

        friendHabboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, habboSession.userInformation.id, message, 0)
    }
}