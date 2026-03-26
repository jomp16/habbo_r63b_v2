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

package ovh.rwx.habbo.communication.incoming.messenger

import org.apache.commons.lang3.time.DurationFormatUtils
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerChatErrorResponse
import ovh.rwx.habbo.database.messenger.MessengerDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.urlUserAgent
import ovh.rwx.habbo.util.Utils
import java.io.File
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerChatHandler {
    private fun isServerConsole(userId: Int, habboSession: HabboSession) =
        userId == UserInformationDao.serverConsoleUserInformation.id && habboSession.hasPermission("acc_server_console")

    private fun handleServerConsole(
        habboSession: HabboSession,
        userId: Int,
        message: String,
        sendResponse: (String) -> Unit
    ) {
        val args = message.split(' ')

        if (args.isNotEmpty()) {
            habboSession.scriptEngine.put("habboSession", habboSession)
            habboSession.scriptEngine.put("habboServer", HabboServer)
            habboSession.scriptEngine.put("habboGame", HabboServer.habboGame)
            habboSession.scriptEngine.put("room", habboSession.currentRoom)
            habboSession.scriptEngine.put("roomUser", habboSession.roomUser)

            when {
                args[0] == "load" && args.size >= 2 -> {
                    val jsOutput =
                        habboSession.scriptEngine.eval(InputStreamReader(urlUserAgent(args[1]).inputStream))?.toString()
                            ?: "null"
                    sendResponse(jsOutput)
                }

                args[0] == "ram" -> sendResponse(Utils.ramUsageString)
                args[0] == "uptime" -> {
                    val uptime = DurationFormatUtils.formatDurationWords(
                        ManagementFactory.getRuntimeMXBean().uptime,
                        true,
                        false
                    ) + " up!"
                    sendResponse(uptime)
                }

                args[0] == "plugin" && args.size >= 3 -> {
                    val pluginName = args[2].trim()
                    when (args[1]) {
                        "load" -> {
                            File("plugins").walk().firstOrNull { it.nameWithoutExtension.contains(pluginName) }?.let {
                                val result = if (HabboServer.pluginManager.addPluginJar(it)) "Done!" else "Failed"
                                sendResponse(result)
                            }
                        }

                        "unload" -> {
                            val result =
                                if (HabboServer.pluginManager.removePluginJarByName(pluginName)) "Done!" else "Failed"
                            sendResponse(result)
                        }
                    }
                }

                message == "reload_handlers" -> {
                    HabboServer.habboHandler.load()
                    HabboServer.serverScheduledExecutor.schedule({ sendResponse("Done!") }, 1, TimeUnit.SECONDS)
                }

                message == "habbo_version" -> {
                    sendResponse(habboSession.habboVersion.toString())
                }

                message.startsWith("h:") -> {
                    val args1 = message.split("(?<!\\\\),".toRegex())
                    val header = args1[0].substring(2).toInt()
                    val habboResponse =
                        HabboResponse(headerId = header, habboVersion = habboSession.habboVersion)

                    habboResponse.apply {
                        args1.drop(1).forEach {
                            val type = it.substring(0, 1)
                            val param = it.substring(2)

                            when (type) {
                                "u" -> writeUTF(param.replace("\\,", ","))
                                "i" -> writeInt(param.toInt())
                                "s" -> writeShort(param.toInt())
                                "b" -> writeBoolean(param.toBoolean())
                                "d" -> writeDouble(param.toDouble())
                                "v" -> writeByte(param.toInt())
                            }
                        }
                    }

                    habboSession.sendHabboResponse(habboResponse)
                    sendResponse("Done!")
                }

                else -> {
                    val jsOutput = habboSession.scriptEngine.eval(message)?.toString() ?: "null"
                    sendResponse(jsOutput)
                }
            }
        }
    }

    @Handler(Incoming.MESSENGER_CHAT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.habboMessenger.initialized) return
        val userId = habboRequest.readInt()
        val message = habboRequest.readUTF().trim()

        if (message.isBlank()) return

        if (!habboSession.habboMessenger.friends.containsKey(userId) || userId == UserInformationDao.serverConsoleUserInformation.id && !habboSession.hasPermission(
                "acc_server_console"
            )
        ) {
            habboSession.sendHabboResponse(
                Outgoing.MESSENGER_CHAT_ERROR,
                MessengerChatErrorResponse.MessengerChatError.NOT_FRIENDS,
                userId,
                message
            )

            return
        }

        if (isServerConsole(userId, habboSession)) {
            handleServerConsole(habboSession, userId, message) { responseMessage ->
                habboSession.sendHabboResponse(
                    Outgoing.MESSENGER_CHAT,
                    userId,
                    responseMessage,
                    0,
                    UserInformationDao.serverConsoleUserInformation.id,
                    UserInformationDao.serverConsoleUserInformation.username,
                    UserInformationDao.serverConsoleUserInformation.figure
                )
            }
            return
        }

        if (userId < 0) {
            if (userId == -1) habboSession.sendHabboResponse(
                Outgoing.MESSENGER_CHAT,
                userId,
                message,
                0,
                habboSession.userInformation.id,
                habboSession.userInformation.username,
                habboSession.userInformation.figure
            )
        } else {
            val messengerBuddy = habboSession.habboMessenger.friends[userId] ?: return

            if (!messengerBuddy.online) {
                MessengerDao.addOfflineMessage(habboSession.userInformation.id, userId, message)

                return
            }
            val friendHabboSession = messengerBuddy.habboSession ?: return

            friendHabboSession.sendHabboResponse(
                Outgoing.MESSENGER_CHAT,
                habboSession.userInformation.id,
                message,
                0,
                UserInformationDao.serverConsoleUserInformation.id,
                UserInformationDao.serverConsoleUserInformation.username,
                UserInformationDao.serverConsoleUserInformation.figure
            )
        }
    }

    @HandlerR63A(IncomingR63A.MESSENGER_CHAT)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.habboMessenger.initialized) return
        val userId = habboRequest.readInt()
        val message = habboRequest.readUTF().trim()

        if (message.isBlank()) return

        if (!habboSession.habboMessenger.friends.containsKey(userId) || userId == UserInformationDao.serverConsoleUserInformation.id && !habboSession.hasPermission(
                "acc_server_console"
            )
        ) {
            habboSession.sendHabboResponse(
                OutgoingR63A.MESSENGER_CHAT_ERROR,
                MessengerChatErrorResponse.MessengerChatError.NOT_FRIENDS,
                userId,
                message
            )

            return
        }

        if (isServerConsole(userId, habboSession)) {
            handleServerConsole(habboSession, userId, message) { responseMessage ->
                habboSession.sendHabboResponse(OutgoingR63A.MESSENGER_CHAT, userId, responseMessage)
            }
            return
        }

        if (userId < 0) {
            if (userId == -1) habboSession.sendHabboResponse(
                OutgoingR63A.MESSENGER_CHAT,
                userId,
                message,
            )
        } else {
            val messengerBuddy = habboSession.habboMessenger.friends[userId] ?: return

            if (!messengerBuddy.online) {
                MessengerDao.addOfflineMessage(habboSession.userInformation.id, userId, message)

                return
            }
            val friendHabboSession = messengerBuddy.habboSession ?: return

            friendHabboSession.sendHabboResponse(
                OutgoingR63A.MESSENGER_CHAT,
                habboSession.userInformation.id,
                message,
            )
        }
    }
}