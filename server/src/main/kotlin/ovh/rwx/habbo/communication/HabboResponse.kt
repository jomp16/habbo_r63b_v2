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
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.util.ReferenceCountUtil

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.encryption.decoder.HabboVl64

@Suppress("unused")
class HabboResponse(
    val headerId: Int,
    val outgoing: Outgoing? = null,
    val keepCopy: Boolean = false,
    val outgoingR63A: OutgoingR63A? = null,
    val protocolEra: ProtocolEra = ProtocolEra.SHUFFLED,
    val habboVersion: HabboVersion,
    /**
     * Payload já serializado e COMPARTILHADO entre os destinatários de um broadcast (ver [shared]). Quando não nulo,
     * a resposta não aloca buffer pooled nem stream: é só um objeto leve, e o encoder copia o payload direto para o
     * seu buffer de saída. Antes cada ouvinte recebia um clone com buffer próprio (alocado + copiado + liberado):
     * ~21% da CPU de um quarto com 500 usuários.
     */
    private val sharedPayload: ByteArray? = null,
) : AutoCloseable {
    /** Se true, o encoder envia este pacote SEM o RC4 de saída — mesmo para sessões com
     *  [ovh.rwx.habbo.game.user.HabboSession.rc4EncryptionOut] ativo. É o caso da própria
     *  resposta SecretKey, que criptografa o handshake: o cliente só instancia o cipher 2
     *  DEPOIS de parseá-la. Decisão tomada no momento da criação (dentro do @Response),
     *  imune à corrida coroutine→event-loop do write. */
    var plaintext: Boolean = false
    // Modo compartilhado: buffer/stream vazios e imutáveis (EMPTY_BUFFER lança em qualquer escrita: falha rápido)
    private val _byteBuf: ByteBuf = if (sharedPayload == null) PooledByteBufAllocator.DEFAULT.buffer() else Unpooled.EMPTY_BUFFER
    private val byteBufOutputStream: ByteBufOutputStream = if (sharedPayload == null) ByteBufOutputStream(_byteBuf) else EMPTY_STREAM
    // Só existe com o debug de pacotes ligado: antes cada resposta (uma por destinatário num broadcast) alocava um
    // StringBuilder inútil — 54% de todos os byte[] alocados sob carga.
    private val debugString: StringBuilder? = if (debugEnabled) StringBuilder() else null
    val byteBuf: ByteBuf
        get() = if (keepCopy) _byteBuf.duplicate() else _byteBuf

    /** Tamanho do corpo (sem length nem header), seja pelo buffer próprio ou pelo payload compartilhado. */
    val payloadLength: Int
        get() = sharedPayload?.size ?: _byteBuf.writerIndex()

    /** Copia o corpo para [out]: do payload compartilhado ou do buffer próprio (consome-o, como o `writeBytes` antigo). */
    fun writePayloadTo(out: ByteBuf) {
        if (sharedPayload != null) out.writeBytes(sharedPayload) else out.writeBytes(byteBuf)
    }

    fun writeUTF(s: String, breakChar: Int = 2) {
        if (debugEnabled) {
            val debugStr = s.replace("\n", "\\n").replace("\r", "\\r")
            debugString?.append("{s:\"$debugStr\"}")
        }
        if (outgoingR63A != null && protocolEra == ProtocolEra.WEDGIE_PRE_SHUFFLE) {
            // Pré-shuffle (WedgieDecoder.readString): bytes até o terminador, decodificados com
            // readUTFBytes = UTF-8 (writeBytes/data é Latin-1 e vira triângulo no cliente)
            byteBufOutputStream.write(s.toByteArray(Charsets.UTF_8))
            byteBufOutputStream.writeByte(breakChar)
        } else {
            byteBufOutputStream.writeUTF(s)
        }
    }

    fun writeUTFWithoutBreak(s: String) {
        if (debugEnabled) {
            val debugStr = s.replace("\n", "\\n").replace("\r", "\\r")
            debugString?.append("{s:\"$debugStr\"}")
        }
        if (outgoingR63A != null && protocolEra == ProtocolEra.WEDGIE_PRE_SHUFFLE) {
            // Pré-shuffle (WedgieDecoder.readString): bytes até o terminador, decodificados com
            // readUTFBytes = UTF-8 (writeBytes/data é Latin-1 e vira triângulo no cliente)
            byteBufOutputStream.write(s.toByteArray(Charsets.UTF_8))
        }
    }

    fun writeShort(i: Int) {
        if (debugEnabled) {
            debugString?.append("{sh:\"$i\"}")
        }
        byteBufOutputStream.writeShort(i)
    }

    fun writeInt(i: Int) {
        if (debugEnabled) {
            debugString?.append("{i:$i}")
        }
        if (outgoingR63A != null && protocolEra == ProtocolEra.WEDGIE_PRE_SHUFFLE) {
            HabboVl64.encodeBytes(i)?.let {
                byteBufOutputStream.write(it)
            }
        } else {
            byteBufOutputStream.writeInt(i)
        }
    }

    fun writeDouble(d: Double) {
        if (debugEnabled) {
            debugString?.append("{dl:$d}")
        }
        byteBufOutputStream.writeDouble(d)
    }

    fun writeFloat(d: Float) {
        if (debugEnabled) {
            debugString?.append("{f:$d}")
        }
        byteBufOutputStream.writeFloat(d)
    }

    fun writeLong(l: Long) {
        if (debugEnabled) {
            debugString?.append("{l:$l}")
        }
        byteBufOutputStream.writeLong(l)
    }

    fun writeBoolean(b: Boolean) {
        if (debugEnabled) {
            debugString?.append("{b:$b}")
        }
        if (outgoingR63A != null && protocolEra == ProtocolEra.WEDGIE_PRE_SHUFFLE) {
            byteBufOutputStream.writeByte((if (b) 73 else 72))
        } else {
            byteBufOutputStream.writeBoolean(b)
        }
    }

    fun writeByte(b: Int) {
        if (debugEnabled) {
            debugString?.append("{by:$b}")
        }
        byteBufOutputStream.writeByte(b)
    }

    fun serialize(habboResponseSerialize: IHabboResponseSerialize, vararg params: Any = arrayOf()) {
        if (outgoingR63A != null) {
            habboResponseSerialize.serializeHabboResponseR63A(this, *params)
        } else {
            habboResponseSerialize.serializeHabboResponse(this, *params)
        }
    }

    /**
     * Layout do R63A explícito, para os métodos `responseR63A` usados como override_method em clients pós-shuffle que
     * ainda lêem o formato antigo (RELEASE63-201110051250): neles a resposta sai pelo enum Outgoing, `outgoingR63A` é nulo e
     * `serialize` escolheria o layout moderno.
     */
    fun serializeR63A(habboResponseSerialize: IHabboResponseSerialize, vararg params: Any = arrayOf()) {
        habboResponseSerialize.serializeHabboResponseR63A(this, *params)
    }

    override fun toString(): String {
        return debugString?.toString() ?: ""
    }

    override fun close() {
        // modo compartilhado: nada a liberar (o payload é um ByteArray comum, o buffer é o EMPTY_BUFFER global)
        if (sharedPayload != null) return

        byteBufOutputStream.close()

        _byteBuf.readerIndex(_byteBuf.readableBytes())
        _byteBuf.discardSomeReadBytes()

        ReferenceCountUtil.release(_byteBuf)
    }

    companion object {
        private val EMPTY_STREAM = ByteBufOutputStream(Unpooled.EMPTY_BUFFER)

        /**
         * Resposta leve que reutiliza um [payload] já serializado (broadcast: o mesmo corpo para todos os ouvintes
         * de uma release). O array NÃO pode ser alterado depois de compartilhado.
         */
        fun shared(
            headerId: Int,
            payload: ByteArray,
            outgoing: Outgoing? = null,
            outgoingR63A: OutgoingR63A? = null,
            protocolEra: ProtocolEra = ProtocolEra.SHUFFLED,
            habboVersion: HabboVersion,
        ): HabboResponse = HabboResponse(
            headerId = headerId,
            outgoing = outgoing,
            outgoingR63A = outgoingR63A,
            protocolEra = protocolEra,
            habboVersion = habboVersion,
            sharedPayload = payload,
        )

        /** Flag de debug de pacotes — copiada do config no boot (HabboServer.start) para
         *  desacoplar o write path do singleton (e permitir teste headless do writer). */
        @JvmField
        var debugEnabled: Boolean = false
    }
}