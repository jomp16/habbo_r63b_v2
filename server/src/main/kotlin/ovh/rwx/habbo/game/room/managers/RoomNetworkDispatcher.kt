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
import ovh.rwx.habbo.communication.ProtocolEra
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

class RoomNetworkDispatcher(private val room: Room) {
    fun sendResponse(habboResponse: HabboResponse) {
        room.userManager.entities.values.filterIsInstance<RoomUser>()
            .forEach { it.habboSession.sendHabboResponse(habboResponse) }
    }

    fun sendResponseModern(outgoing: Outgoing, vararg args: Any?, excludeSession: HabboSession? = null) {
        prototypeBroadcast(
            roomSessions { !it.isPreShuffle && it != excludeSession },
            // Serializa a lógica pesada (mapas, strings, iterações) APENAS UMA VEZ por release
            { prototypeSession -> HabboServer.habboHandler.invokeResponse(prototypeSession, outgoing, *args) }
        ) { session, headerId, protocolEra ->
            HabboResponse(headerId, outgoing = outgoing, protocolEra = protocolEra, habboVersion = session.habboVersion)
        }
    }

    fun sendResponseR63A(outgoing: OutgoingR63A, vararg args: Any?, excludeSession: HabboSession? = null) {
        prototypeBroadcast(
            roomSessions { it.isPreShuffle && it != excludeSession },
            { prototypeSession -> HabboServer.habboHandler.invokeResponse(prototypeSession, outgoing, *args) }
        ) { session, headerId, protocolEra ->
            HabboResponse(headerId, outgoingR63A = outgoing, protocolEra = protocolEra, habboVersion = session.habboVersion)
        }
    }

    private fun roomSessions(filter: (HabboSession) -> Boolean): List<HabboSession> =
        room.userManager.entities.values
            .filterIsInstance<RoomUser>()
            .map { it.habboSession }
            .filter(filter)

    /**
     * Mecânica única de broadcast do quarto: agrupa por release (o wire temporal pode
     * diferir entre clientes mistos — vale para as duas eras), serializa UM protótipo por
     * grupo, extrai o payload binário puro e clona o pacote levemente para cada sessão
     * do grupo — O(N) por grupo em vez de O(N²).
     */
    private fun prototypeBroadcast(
        sessions: List<HabboSession>,
        serialize: (HabboSession) -> HabboResponse?,
        clone: (session: HabboSession, headerId: Int, protocolEra: ProtocolEra) -> HabboResponse
    ) {
        for ((_, releaseSessions) in sessions.groupBy { it.release }) {
            val prototypeResponse = serialize(releaseSessions.first()) ?: continue

            val payload = ByteArray(prototypeResponse.byteBuf.readableBytes())
            prototypeResponse.byteBuf.getBytes(prototypeResponse.byteBuf.readerIndex(), payload)
            val headerId = prototypeResponse.headerId
            val protocolEra = prototypeResponse.protocolEra
            prototypeResponse.close() // Libera a memória do Netty do protótipo

            for (session in releaseSessions) {
                val clonedResponse = clone(session, headerId, protocolEra)
                clonedResponse.byteBuf.writeBytes(payload)
                session.sendHabboResponse(clonedResponse)
            }
        }
    }

    /**
     * Envia a resposta correta para cada sessão do quarto de acordo com a release do cliente.
     * Evita if/else nos handlers/tasks de quarto quando há dois enum variants para a mesma mensagem lógica.
     */
    fun sendResponse(outgoing: Outgoing?, outgoingR63A: OutgoingR63A?, vararg args: Any?, excludeSession: HabboSession? = null) {
        if (outgoing != null) sendResponseModern(outgoing, *args, excludeSession = excludeSession)
        if (outgoingR63A != null) sendResponseR63A(outgoingR63A, *args, excludeSession = excludeSession)
    }
}