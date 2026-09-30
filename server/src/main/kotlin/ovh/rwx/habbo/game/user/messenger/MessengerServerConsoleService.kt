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

package ovh.rwx.habbo.game.user.messenger

import org.apache.commons.lang3.time.DurationFormatUtils
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.urlUserAgent
import ovh.rwx.habbo.util.Utils
import java.io.File
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

object MessengerServerConsoleService {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isServerConsole(userId: Int, habboSession: HabboSession): Boolean =
        userId == UserInformationDao.serverConsoleUserInformation.id && habboSession.hasPermission("acc_server_console")

    fun execute(
        habboSession: HabboSession,
        message: String,
        sendResponse: (String) -> Unit
    ) {
        val args = message.split(' ')
        if (args.isEmpty()) return

        habboSession.scriptEngine.put("habboSession", habboSession)
        habboSession.scriptEngine.put("habboServer", HabboServer)
        habboSession.scriptEngine.put("habboGame", HabboServer.habboGame)
        habboSession.scriptEngine.put("room", habboSession.currentRoom)
        habboSession.scriptEngine.put("roomUser", habboSession.roomUser)

        when {
            args[0] == "load" && args.size >= 2 -> {
                try {
                    val jsOutput =
                        habboSession.scriptEngine.eval(InputStreamReader(urlUserAgent(args[1]).inputStream))?.toString()
                            ?: "null"
                    sendResponse(jsOutput)
                } catch (e: Exception) {
                    log.error("Error executing script from URL in Server Console: {}", args[1], e)
                    sendResponse("Error: ${e.message}")
                }
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
                        val pluginJar = File("plugins").walk().firstOrNull { it.nameWithoutExtension.contains(pluginName) }
                        if (pluginJar != null) {
                            val result = if (HabboServer.pluginManager.addPluginJar(pluginJar)) "Done!" else "Failed"
                            sendResponse(result)
                        } else {
                            sendResponse("Plugin '$pluginName' not found in plugins folder")
                        }
                    }

                    "unload" -> {
                        val result =
                            if (HabboServer.pluginManager.removePluginJarByName(pluginName)) "Done!" else "Failed"
                        sendResponse(result)
                    }

                    else -> sendResponse("Usage: plugin <load|unload> <pluginName>")
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
                try {
                    val args1 = message.split("(?<!\\\\),".toRegex())
                    val header = args1[0].substring(2).toInt()
                    val habboResponse = HabboResponse(headerId = header, habboVersion = habboSession.habboVersion)

                    habboResponse.apply {
                        args1.drop(1).forEach {
                            val type = it.substring(0, 1)
                            val param = it.substring(2)

                            when (type) {
                                "s" -> writeUTF(param.replace("\\,", ","))
                                "i" -> writeInt(param.toInt())
                                "sh" -> writeShort(param.toInt())
                                "b" -> writeBoolean(param.toBoolean())
                                "d" -> writeDouble(param.toDouble())
                                "v" -> writeByte(param.toInt())
                            }
                        }
                    }

                    habboSession.sendHabboResponse(habboResponse)
                    sendResponse("Done!")
                } catch (e: Exception) {
                    sendResponse("Error parsing header: ${e.message}")
                }
            }

            else -> {
                try {
                    val jsOutput = habboSession.scriptEngine.eval(message)?.toString() ?: "null"
                    sendResponse(jsOutput)
                } catch (e: Exception) {
                    sendResponse("JS Error: ${e.message}")
                }
            }
        }
    }
}
