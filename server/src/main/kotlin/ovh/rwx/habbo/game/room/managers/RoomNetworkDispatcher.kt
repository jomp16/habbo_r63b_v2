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
        broadcastModern(roomSessions { !it.isPreShuffle && it != excludeSession }, outgoing, args)
    }

    fun sendResponseR63A(outgoing: OutgoingR63A, vararg args: Any?, excludeSession: HabboSession? = null) {
        broadcastR63A(roomSessions { it.isPreShuffle && it != excludeSession }, outgoing, args)
    }

    /**
     * Envia a mesma mensagem lógica só para [recipients] (ex.: quem alcança um chat pela distância): a resposta é
     * serializada UMA vez por release e clonada por sessão, em vez de reexecutar o `@Response` para cada ouvinte.
     */
    fun sendResponseToSessions(
        recipients: Collection<HabboSession>,
        outgoing: Outgoing?,
        outgoingR63A: OutgoingR63A?,
        vararg args: Any?
    ) {
        if (outgoing != null) broadcastModern(recipients.filter { !it.isPreShuffle }, outgoing, args)
        if (outgoingR63A != null) broadcastR63A(recipients.filter { it.isPreShuffle }, outgoingR63A, args)
    }

    private fun broadcastModern(sessions: List<HabboSession>, outgoing: Outgoing, args: Array<out Any?>) {
        prototypeBroadcast(
            sessions,
            // Serializa a lógica pesada (mapas, strings, iterações) APENAS UMA VEZ por release
            { prototypeSession -> HabboServer.habboHandler.invokeResponse(prototypeSession, outgoing, *args) }
        ) { session, headerId, protocolEra, payload ->
            HabboResponse.shared(headerId, payload, outgoing = outgoing, protocolEra = protocolEra, habboVersion = session.habboVersion)
        }
    }

    private fun broadcastR63A(sessions: List<HabboSession>, outgoing: OutgoingR63A, args: Array<out Any?>) {
        prototypeBroadcast(
            sessions,
            { prototypeSession -> HabboServer.habboHandler.invokeResponse(prototypeSession, outgoing, *args) }
        ) { session, headerId, protocolEra, payload ->
            HabboResponse.shared(headerId, payload, outgoingR63A = outgoing, protocolEra = protocolEra, habboVersion = session.habboVersion)
        }
    }

    // Uma passada só, sem as 3 listas intermediárias (filterIsInstance/map/filter) que eram alocadas a CADA broadcast
    private fun roomSessions(filter: (HabboSession) -> Boolean): List<HabboSession> {
        val entities = room.userManager.entities.values
        val sessions = ArrayList<HabboSession>(entities.size)

        for (entity in entities) {
            if (entity is RoomUser) {
                val session = entity.habboSession
                if (filter(session)) sessions.add(session)
            }
        }

        return sessions
    }

    /** Agrupa por release; no caso comum (uma release só) devolve a própria lista, sem HashMap por broadcast. */
    private fun groupByRelease(sessions: List<HabboSession>): Collection<List<HabboSession>> {
        if (sessions.isEmpty()) return emptyList()

        val first = sessions[0].release
        if (sessions.all { it.release == first }) return listOf(sessions)

        return sessions.groupBy { it.release }.values
    }

    /**
     * Mecânica única de broadcast do quarto: agrupa por release (o wire temporal pode
     * diferir entre clientes mistos — vale para as duas eras), serializa UM protótipo por
     * grupo, extrai o payload binário puro e clona o pacote levemente para cada sessão
     * do grupo — O(N) por grupo em vez de O(N²).
     */
    private fun prototypeBroadcast(
        sessions: List<HabboSession>,
        serialize: (HabboSession) -> HabboResponse?,
        clone: (session: HabboSession, headerId: Int, protocolEra: ProtocolEra, payload: ByteArray) -> HabboResponse
    ) {
        for (releaseSessions in groupByRelease(sessions)) {
            val prototypeResponse = serialize(releaseSessions.first()) ?: continue

            val payload = ByteArray(prototypeResponse.byteBuf.readableBytes())
            prototypeResponse.byteBuf.getBytes(prototypeResponse.byteBuf.readerIndex(), payload)
            val headerId = prototypeResponse.headerId
            val protocolEra = prototypeResponse.protocolEra
            prototypeResponse.close() // Libera a memória do Netty do protótipo

            // o payload é compartilhado (somente leitura) por todos os ouvintes: cada um recebe só um objeto leve
            for (session in releaseSessions) {
                session.sendHabboResponse(clone(session, headerId, protocolEra, payload))
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