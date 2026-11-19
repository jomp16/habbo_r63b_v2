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

package ovh.rwx.fastfood.game

import io.netty.channel.Channel
import ovh.rwx.fastfood.communication.FastFoodHandler
import ovh.rwx.fastfood.communication.outgoing.FFOutgoing
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.user.HabboSession

/**
 * A conexão de um cliente de Fast Food (TCP do Flash ou WebSocket do navegador). Sem threads próprias: os pacotes
 * chegam no event loop do Netty (FastFoodHandler.handle) e as respostas saem de quem as manda (o event loop ou a
 * thread das partidas). Cada resposta já vai serializada num payload compartilhado: nada pooled fica preso se o
 * canal fechar no meio do caminho.
 */
class FastFoodSession(val channel: Channel) : AutoCloseable {
    var habboSession: HabboSession? = null
    val authenticated: Boolean
        get() = habboSession != null

    fun sendHabboResponse(ffOutgoing: FFOutgoing, vararg args: Any?) {
        if (!channel.isActive) return
        HabboServer.fastFoodHandler.serialize(ffOutgoing, *args)?.let { send(ffOutgoing, it) }
    }

    /** Manda um payload já serializado (FastFoodHandler.serialize), o mesmo para vários destinatários. */
    fun send(ffOutgoing: FFOutgoing, payload: ByteArray) {
        if (!channel.isActive) return
        channel.writeAndFlush(HabboResponse.shared(ffOutgoing.headerId, payload, habboVersion = FastFoodHandler.VERSION))
    }

    override fun close() {
        FastFoodGameManager.leave(this)
        habboSession?.fastFoodSession = null
        habboSession = null
    }
}
