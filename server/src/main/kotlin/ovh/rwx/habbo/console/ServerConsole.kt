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

package ovh.rwx.habbo.console

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.snowwar.SnowWarArenaMaps
import ovh.rwx.habbo.game.snowwar.enums.SnowWarFieldType
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class ServerConsole(private val server: HabboServer) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun startConsoleReader() {
        Thread({
            try {
                val reader = BufferedReader(InputStreamReader(System.`in`))
                while (server.started) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) {
                        val result = executeCommand(trimmed)
                        if (result.isNotBlank()) {
                            println(result)
                        }
                    }
                }
            } catch (e: Exception) {
                log.debug("Console reader ended: ${e.message}")
            }
        }, "console-reader").apply { isDaemon = true }.start()
    }

    fun executeCommand(input: String): String {
        val parts = input.trim().split("\\s+".toRegex())
        val cmd = parts[0].lowercase()
        val arg = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" ") else ""

        return when (cmd) {
            "arena", "setarena", "snowwar_arena", "snowwar" -> {
                handleArenaCommand(arg)
            }
            "status", "stats" -> {
                val sessions = server.habboSessionManager.habboSessions.values
                val authed = sessions.count { it.authenticated && !it.handshaking }
                val rooms = server.habboGame.roomManager.rooms.values.count { it.running }
                val arenas = SnowWarArenaMaps.arenas.size
                val forced = server.habboGame.snowWarManager.forcedArenaId?.let { id ->
                    val name = SnowWarFieldType.fromId(id).fieldName
                    "[$id] $name"
                } ?: (server.habboConfig.gameConfig.snowwar.forcedArenaId.takeIf { it > 0 }?.let { id ->
                    val name = SnowWarFieldType.fromId(id).fieldName
                    "[$id] $name (config)"
                } ?: "Random")
                """
                === Server Status ===
                Online users: $authed
                Active rooms: $rooms
                SnowWar arenas loaded: $arenas
                SnowWar forced arena: $forced
                """.trimIndent()
            }
            "reload" -> {
                when (arg.lowercase()) {
                    "arena", "arenas", "snowwar" -> {
                        SnowWarArenaMaps.loadArenas()
                        "[SnowWar] Reloaded ${SnowWarArenaMaps.arenas.size} arenas from database."
                    }
                    "catalog", "catalogue" -> {
                        server.habboGame.catalogManager.load()
                        "[Catalog] Reloaded catalog."
                    }
                    "items" -> {
                        server.habboGame.itemManager.load()
                        "[Items] Reloaded items."
                    }
                    else -> "Usage: reload <arenas|catalog|items>"
                }
            }
            "help", "?" -> {
                """
                === Console Commands ===
                arena [id|name|random|reload] : View or force SnowWar arena
                status                        : View server status
                reload <arenas|catalog|items> : Reload server components
                stop / exit                   : Gracefully stop the server
                help                          : Show this help menu
                """.trimIndent()
            }
            "stop", "exit", "shutdown" -> {
                Thread {
                    server.close()
                    exitProcess(0)
                }.start()
                "Stopping server..."
            }
            else -> "Unknown command: '$input'. Type 'help' for available commands."
        }
    }

    fun handleArenaCommand(arg: String): String {
        val arenas = SnowWarArenaMaps.arenas.values.sortedBy { it.id }
        val snowWarMgr = server.habboGame.snowWarManager

        if (arg.isBlank() || arg.equals("list", ignoreCase = true)) {
            val sb = StringBuilder()
            sb.appendLine("=== SnowWar Arenas ===")
            if (arenas.isEmpty()) {
                sb.appendLine("No arenas loaded from database!")
            } else {
                for (a in arenas) {
                    val fieldType = SnowWarFieldType.entries.firstOrNull { it.id == a.id }
                    val enumName = fieldType?.name ?: "UNKNOWN"
                    sb.appendLine("  [${a.id}] ${a.name} ($enumName) - ${a.fuseObjects.size} items, ${a.blueSpawns.size} blue spawns, ${a.redSpawns.size} red spawns")
                }
            }
            val current = snowWarMgr.forcedArenaId?.let { id ->
                val name = arenas.firstOrNull { it.id == id }?.name ?: SnowWarFieldType.fromId(id).fieldName
                "Forced to: [$id] $name"
            } ?: (server.habboConfig.gameConfig.snowwar.forcedArenaId.takeIf { it > 0 }?.let { id ->
                val name = arenas.firstOrNull { it.id == id }?.name ?: SnowWarFieldType.fromId(id).fieldName
                "Forced by config: [$id] $name"
            } ?: "Random")
            sb.appendLine("Current Arena Selection: $current")
            sb.append("Usage: arena <id|name|random|reload>")
            return sb.toString()
        }

        if (arg.equals("reload", ignoreCase = true)) {
            SnowWarArenaMaps.loadArenas()
            return "[SnowWar] Reloaded ${SnowWarArenaMaps.arenas.size} arenas from database."
        }

        if (arg.equals("random", ignoreCase = true) || arg == "0" || arg.equals("clear", ignoreCase = true) || arg.equals("reset", ignoreCase = true)) {
            snowWarMgr.forcedArenaId = null
            return "[SnowWar] Forced arena cleared. Arenas will now be chosen randomly."
        }

        val target = SnowWarFieldType.fromQuery(arg)
            ?: arenas.firstOrNull { it.name.lowercase().contains(arg.lowercase()) || it.id.toString() == arg }?.let { SnowWarFieldType.fromId(it.id) }

        if (target != null) {
            snowWarMgr.forcedArenaId = target.id
            val arenaData = SnowWarArenaMaps.getArena(target.id)
            val arenaName = arenaData?.name ?: target.fieldName
            return "[SnowWar] Forced arena set to: [${target.id}] $arenaName (${target.name})"
        } else {
            return "[SnowWar] Arena '$arg' not found. Available IDs: ${arenas.map { "${it.id} (${it.name})" }.joinToString(", ")}"
        }
    }
}
