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

package ovh.rwx.habbo.communication

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * Descarte de pacotes idênticos repetidos em rajada ([Handler.dedupeMs]). Guarda, por header, o ÚLTIMO pedido
 * atendido da sessão (payload e instante); só os handlers marcados passam por aqui, então o mapa tem no máximo um
 * item por header marcado. Os pacotes de uma sessão rodam em série (`HabboSession.packetDispatcher`), mas o mapa é
 * concorrente porque bots de stress e testes chamam o handler de outras threads.
 */
class RequestDedupe {
    private class Last(val payload: ByteArray, val atNanos: Long)

    private val last = ConcurrentHashMap<Int, Last>()

    /**
     * true se [payload] é igual ao do último pedido atendido de [headerId] e chegou há menos de [windowNanos]. O
     * instante só avança quando o pedido é atendido: um retry contínuo do cliente nunca fica sem resposta. Não mexe
     * no readerIndex do [payload].
     */
    fun isDuplicate(headerId: Int, payload: ByteBuf, windowNanos: Long, nowNanos: Long = System.nanoTime()): Boolean {
        if (windowNanos <= 0L) return false

        var duplicate = false
        last.compute(headerId) { _, previous ->
            if (previous != null && nowNanos - previous.atNanos < windowNanos && sameBytes(previous.payload, payload)) {
                duplicate = true
                previous
            } else {
                Last(ByteBufUtil.getBytes(payload), nowNanos)
            }
        }
        return duplicate
    }

    private fun sameBytes(bytes: ByteArray, buf: ByteBuf): Boolean {
        if (bytes.size != buf.readableBytes()) return false
        val start = buf.readerIndex()
        for (i in bytes.indices) if (bytes[i] != buf.getByte(start + i)) return false
        return true
    }
}
