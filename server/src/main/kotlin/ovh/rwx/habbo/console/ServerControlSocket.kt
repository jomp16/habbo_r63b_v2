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

import ovh.rwx.habbo.HabboServer
import java.io.BufferedWriter
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.SocketChannel as UnixSocketChannel
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import java.nio.file.Path

class ServerControlSocket(
    private val server: HabboServer,
    private val console: ServerConsole,
    private val socketPath: Path = Path.of(System.getProperty("habbo.tui.socket") ?: "/tmp/habbo-r63b.sock")
) : AutoCloseable {
    private var controlSocket: UnixSocketChannel? = null
    private var controlWriter: BufferedWriter? = null

    fun start() {
        Thread({
            while (controlSocket == null && server.started) {
                try {
                    val socket = UnixSocketChannel.open(java.net.StandardProtocolFamily.UNIX)
                    socket.connect(UnixDomainSocketAddress.of(socketPath))
                    controlSocket = socket
                    controlWriter = Channels.newWriter(socket, StandardCharsets.UTF_8.newEncoder(), -1).buffered()
                    controlWriter?.append("HELLO\tCONTROL\n")?.flush()
                    handleControlClient(socket)
                } catch (e: Exception) {
                    Thread.sleep(250)
                }
            }
        }, "tui-control-acceptor").apply { isDaemon = true }.start()

        server.serverScheduledExecutor.scheduleWithFixedDelay(
            { broadcastControlStats() },
            0,
            1,
            TimeUnit.SECONDS
        )
    }

    private fun handleControlClient(socket: UnixSocketChannel) {
        Thread({
            socket.use { client ->
                val reader = Channels.newReader(client, StandardCharsets.UTF_8.newDecoder(), -1).buffered()
                reader.lineSequence().forEach { command ->
                    if (command.startsWith("CMD\t")) {
                        controlWriter?.append(commandResult(command.removePrefix("CMD\t")))?.append('\n')?.flush()
                    }
                }
            }
            controlSocket = null
        }, "tui-control-client").apply { isDaemon = true }.start()
    }

    private fun broadcastControlStats() {
        val stats = controlStatsLine()
        controlWriter?.append(stats)?.append('\n')?.flush()
    }

    private fun controlStatsLine(): String {
        val sessions = server.habboSessionManager.habboSessions.values
        val online = sessions.count { it.authenticated && !it.handshaking }
        val rooms = server.habboGame.roomManager.rooms.values.count { it.running }
        return "STATS\tusers=$online\trooms_loaded=$rooms"
    }

    private fun commandResult(command: String): String {
        val name = command.trim()
        if (name.isBlank()) return "RESULT\t$command\tComando inválido"
        val output = console.executeCommand(name).replace("\n", " | ")
        return "RESULT\t$command\t$output"
    }

    override fun close() {
        controlWriter?.close()
        controlSocket?.close()
    }
}
