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

package tk.jomp16.habbo.plugin.listeners.room

import net.engio.mbassy.listener.Handler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.plugin.event.events.room.RoomUserChatEvent
import tk.jomp16.habbo.plugin.event.events.room.annotation.Command
import tk.jomp16.utils.plugin.api.PluginListener
import tk.jomp16.utils.plugin.event.events.PluginListenerAddedEvent
import tk.jomp16.utils.plugin.event.events.PluginListenerRemovedEvent
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.regex.Pattern

class RoomCommandsManagerListener : PluginListener() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val lookup = MethodHandles.lookup()
    private val commandsEvents: MutableSet<PluginCommandRegister> = mutableSetOf()

    override fun onCreate() {
        HabboServer.pluginManager.pluginsListener.map { it.second }.forEach {
            handlePluginListenerAdded(PluginListenerAddedEvent(it))
        }
    }

    @Handler
    fun handlePluginListenerAdded(pluginListenerAddedEvent: PluginListenerAddedEvent) {
        log.trace("Searching commands for class ${pluginListenerAddedEvent.pluginListener.javaClass.simpleName}")

        searchCommands(pluginListenerAddedEvent.pluginListener).let {
            commandsEvents += it

            log.trace("Found ${it.map { it.commands.size }.sum()} commands for class ${pluginListenerAddedEvent.pluginListener.javaClass.simpleName}")
        }
    }

    @Handler
    fun handlePluginListenerRemoved(pluginListenerRemovedEvent: PluginListenerRemovedEvent) {
        log.trace("Removing commands for class ${pluginListenerRemovedEvent.pluginListener.javaClass.simpleName}")

        commandsEvents.filter { it.pluginListener == pluginListenerRemovedEvent.pluginListener }.let {
            commandsEvents -= it

            log.trace("Removed ${it.map { it.commands.size }.sum()} commands for class ${pluginListenerRemovedEvent.pluginListener.javaClass.simpleName}")
        }
    }

    @Handler
    fun handleRoomUserChatEvent(roomUserChatEvent: RoomUserChatEvent) {
        if (roomUserChatEvent.message.isEmpty() || !roomUserChatEvent.message.startsWith(':')) return

        val args = parseLine(roomUserChatEvent.message.substring(1).trim())

        if (args == null || args.isEmpty()) return

        val commands = commandsEvents.filter { it.commands.contains(args[0]) }.filterNotNull()

        if (commands.isEmpty()) roomUserChatEvent.roomUser.chat(roomUserChatEvent.roomUser.virtualID, roomUserChatEvent.message, roomUserChatEvent.bubble, roomUserChatEvent.type, true)

        commands.forEach {
            if (it.permissionName.isNotEmpty() && !roomUserChatEvent.roomUser.habboSession!!.hasPermission(it.permissionName)) {
                roomUserChatEvent.roomUser.habboSession.sendNotification("You have no permission to run this command!")

                return@forEach
            }

            if (roomUserChatEvent.roomUser.habboSession!!.userInformation.rank < it.rank) {
                roomUserChatEvent.roomUser.habboSession.sendNotification("You have a rank less than the required!")

                return@forEach
            }

            val messageWithoutCommand = if (roomUserChatEvent.message.length == args[0].length) "" else roomUserChatEvent.message.substring(args[0].length).trim()

            try {
                it.methodHandle.invokeWithArguments(it.pluginListener, roomUserChatEvent.roomUser, messageWithoutCommand, args.drop(1))
            } catch (e: Exception) {
                log.error("Error when trying to invoke command ${args[0]}!", e)
            }
        }
    }

    private fun searchCommands(pluginListener: PluginListener): List<PluginCommandRegister> {
        val commands: MutableList<PluginCommandRegister> = mutableListOf()

        pluginListener.javaClass.declaredMethods.forEach {
            val commandAnnotation = it.getAnnotation(Command::class.java) ?: return@forEach

            it.isAccessible = true

            commands += PluginCommandRegister(pluginListener, commandAnnotation.commands, commandAnnotation.rank, commandAnnotation.permissionName, lookup.unreflect(it))
        }

        return commands
    }

    private fun parseLine(message: String?): List<String>? {
        if (message == null || message.isEmpty()) {
            return null
        }

        val args: MutableList<String> = mutableListOf()

        val matcher = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'").matcher(message)

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Add double-quoted string without the quotes
                args.add(matcher.group(1))
            } else if (matcher.group(2) != null) {
                // Add single-quoted string without the quotes
                args.add(matcher.group(2))
            } else {
                // Add unquoted word
                args.add(matcher.group())
            }
        }

        return args
    }

    internal class PluginCommandRegister(
            val pluginListener: PluginListener,
            val commands: Array<String>,
            val rank: Int = 0,
            val permissionName: String,
            val methodHandle: MethodHandle
    )
}