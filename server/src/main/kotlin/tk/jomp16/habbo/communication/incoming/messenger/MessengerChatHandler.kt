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

package tk.jomp16.habbo.communication.incoming.messenger

import org.apache.commons.lang3.time.DurationFormatUtils
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.communication.outgoing.messenger.MessengerChatErrorResponse
import tk.jomp16.habbo.database.information.UserInformationDao
import tk.jomp16.habbo.database.messenger.MessengerDao
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.kotlin.urlUserAgent
import tk.jomp16.habbo.util.Utils
import java.io.File
import java.io.InputStreamReader
import java.lang.management.ManagementFactory

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerChatHandler {
    @Handler(Incoming.MESSENGER_CHAT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initialized) return
        val userId = habboRequest.readInt()
        val message = habboRequest.readUTF().trim()

        if (message.isBlank()) return

        if (!habboSession.habboMessenger.friends.containsKey(userId) || userId == UserInformationDao.serverConsoleUserInformation.id && !habboSession.hasPermission("acc_server_console")) {
            habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT_ERROR, MessengerChatErrorResponse.MessengerChatError.NOT_FRIENDS, userId, message)

            return
        }

        if (userId == UserInformationDao.serverConsoleUserInformation.id && habboSession.hasPermission("acc_server_console")) {
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

                    habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, userId, jsOutput, 0, 0, "", "")
                } else if (args[0] == "ram") {
                    habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, userId, Utils.ramUsageString, 0, 0, "", "")
                } else if (args[0] == "uptime") {
                    habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, userId, DurationFormatUtils.formatDurationWords(ManagementFactory.getRuntimeMXBean().uptime, true, false) + " up!", 0, 0, "", "")
                } else if (args[0] == "plugin") {
                    if (args.size < 3) return
                    val pluginName = args[2].trim()

                    when (args[1]) {
                        "load" -> {
                            File("plugins").walk().filter { it.nameWithoutExtension.contains(pluginName) }.firstOrNull()?.let {
                                val message1 = if (HabboServer.pluginManager.addPluginJar(it)) "Done!" else "Failed"

                                habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, userId, message1, 0, 0, "", "")
                            }
                        }
                        "unload" -> {
                            val message1 = if (HabboServer.pluginManager.removePluginJarByName(pluginName)) "Done!" else "Failed"

                            habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, userId, message1, 0, 0, "", "")
                        }
                    }
                } else if (message.startsWith("h:")) {
                    // one line response messages
                    val args1 = message.split("(?<!\\\\),".toRegex())
                    val header = args1[0].substring(2).toInt()
                    val habboResponse = HabboResponse(header)

                    habboResponse.apply {
                        args1.drop(1).forEach {
                            val type = it.substring(0, 1)
                            val param = it.substring(2)

                            when (type) {
                                "u" -> {
                                    // string
                                    writeUTF(param.replace("\\,", ","))
                                }
                                "i" -> {
                                    // int
                                    writeInt(param.toInt())
                                }
                                "s" -> {
                                    // short
                                    writeShort(param.toInt())
                                }
                                "b" -> {
                                    // boolean
                                    writeBoolean(param.toBoolean())
                                }
                                "d" -> {
                                    // double
                                    writeDouble(param.toDouble())
                                }
                                "v" -> {
                                    // bytes
                                    writeByte(param.toInt())
                                }
                            }
                        }
                    }

                    habboSession.sendHabboResponse(habboResponse)

                    habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, userId, "Done!", 0, 0, "", "")
                } else {
                    val jsOutput = habboSession.scriptEngine.eval(message)?.toString() ?: "null"

                    habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, userId, jsOutput, 0, 0, "", "")
                }
            }

            return
        }

        if (userId < 0) {
            if (userId == -1) habboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, userId, message, 0, habboSession.userInformation.id, habboSession.userInformation.username, habboSession.userInformation.figure)
        } else {
            val messengerBuddy = habboSession.habboMessenger.friends[userId] ?: return

            if (!messengerBuddy.online) {
                MessengerDao.addOfflineMessage(habboSession.userInformation.id, userId, message)

                return
            }
            val friendHabboSession = messengerBuddy.habboSession ?: return

            friendHabboSession.sendHabboResponse(Outgoing.MESSENGER_CHAT, habboSession.userInformation.id, message, 0, 0, "", "")
        }
    }
}