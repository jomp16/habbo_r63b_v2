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

package ovh.rwx.habbo.game.room.managers

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

class RoomNetworkDispatcher(private val room: Room) {
    fun sendResponse(habboResponse: HabboResponse) {
        room.userManager.entities.values.filterIsInstance<RoomUser>()
            .forEach { it.habboSession.sendHabboResponse(habboResponse) }
    }

    fun sendResponseModern(outgoing: Outgoing, vararg args: Any?) {
        val sessions = room.userManager.entities.values
            .filterIsInstance<RoomUser>()
            .map { it.habboSession }
            .filter { it.release != "R63A" }

        if (sessions.isEmpty()) return

        // Agrupa por versão para suportar clientes mistos sem quebrar o Header ID
        val groupedSessions = sessions.groupBy { it.release }

        for ((_, releaseSessions) in groupedSessions) {
            val prototypeSession = releaseSessions.first()

            // 1. Serializa a lógica pesada (mapas, strings, iterações) APENAS UMA VEZ
            val prototypeResponse =
                HabboServer.habboHandler.invokeResponse(prototypeSession, outgoing, *args) ?: continue

            // 2. Extrai o payload binário puro
            val readableBytes = prototypeResponse.byteBuf.readableBytes()
            val payload = ByteArray(readableBytes)
            prototypeResponse.byteBuf.getBytes(prototypeResponse.byteBuf.readerIndex(), payload)
            prototypeResponse.close() // Libera a memória do Netty do protótipo

            // 3. Clona o pacote levemente para todos da mesma versão (O(N) ao invés de O(N²))
            for (session in releaseSessions) {
                val clonedResponse = HabboResponse(
                    prototypeResponse.headerId,
                    outgoing = outgoing,
                    habboVersion = session.habboVersion
                )
                clonedResponse.byteBuf.writeBytes(payload)
                session.sendHabboResponse(clonedResponse)
            }
        }
    }

    fun sendResponseR63A(outgoing: OutgoingR63A, vararg args: Any?) {
        val sessions = room.userManager.entities.values
            .filterIsInstance<RoomUser>()
            .map { it.habboSession }
            .filter { it.release == "R63A" }

        if (sessions.isEmpty()) return

        val prototypeSession = sessions.first()
        val prototypeResponse = HabboServer.habboHandler.invokeResponse(prototypeSession, outgoing, *args) ?: return

        val readableBytes = prototypeResponse.byteBuf.readableBytes()
        val payload = ByteArray(readableBytes)
        prototypeResponse.byteBuf.getBytes(prototypeResponse.byteBuf.readerIndex(), payload)
        prototypeResponse.close()

        for (session in sessions) {
            val clonedResponse = HabboResponse(
                prototypeResponse.headerId,
                outgoingR63A = outgoing,
                r63ANewEncoding = prototypeResponse.r63ANewEncoding,
                habboVersion = session.habboVersion
            )
            clonedResponse.byteBuf.writeBytes(payload)
            session.sendHabboResponse(clonedResponse)
        }
    }

    /**
     * Envia a resposta correta para cada sessão do quarto de acordo com a release do cliente.
     * Evita if/else nos handlers/tasks de quarto quando há dois enum variants para a mesma mensagem lógica.
     */
    fun sendResponse(outgoing: Outgoing?, outgoingR63A: OutgoingR63A?, vararg args: Any?) {
        if (outgoing != null) sendResponseModern(outgoing, *args)
        if (outgoingR63A != null) sendResponseR63A(outgoingR63A, *args)
    }
}