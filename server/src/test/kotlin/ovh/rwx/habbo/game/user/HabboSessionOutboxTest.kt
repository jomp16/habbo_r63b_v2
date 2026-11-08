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

package ovh.rwx.habbo.game.user

import io.netty.channel.embedded.EmbeddedChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.HabboVersion
import java.time.LocalDateTime

/**
 * Fila de saída por sessão (`HabboSession.sendHabboResponse`): a ordem de envio tem de ser preservada exatamente
 * (o RC4 de saída é um fluxo contínuo) e nenhum buffer pode vazar se o canal fechar antes do drain.
 */
class HabboSessionOutboxTest {
    private fun response(headerId: Int) = HabboResponse(headerId, habboVersion = HabboVersion(LocalDateTime.now(), 63))

    @Test
    fun `preserva a ordem de envio e so escreve quando o event loop drena`() {
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)

        repeat(500) { session.sendHabboResponse(response(it)) }

        // produtor só enfileira: nada foi para o canal antes de o event loop rodar a tarefa de drain
        assertTrue(channel.outboundMessages().isEmpty())

        channel.runPendingTasks()

        val sent = ArrayList<HabboResponse>()
        while (true) sent += (channel.readOutbound<HabboResponse>() ?: break)

        assertEquals((0 until 500).toList(), sent.map { it.headerId })
        sent.forEach { it.close() }
    }

    @Test
    fun `agenda um drain por rajada e um novo depois que o anterior rodou`() {
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)

        session.sendHabboResponse(response(1))
        session.sendHabboResponse(response(2))
        channel.runPendingTasks()
        session.sendHabboResponse(response(3)) // depois do primeiro drain: precisa agendar de novo
        channel.runPendingTasks()

        val ids = ArrayList<Int>()
        while (true) {
            val r = channel.readOutbound<HabboResponse>() ?: break
            ids += r.headerId
            r.close()
        }

        assertEquals(listOf(1, 2, 3), ids)
    }

    @Test
    fun `canal fechado antes do drain libera os buffers e nao escreve nada`() {
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)
        val queued = (1..20).map { response(it) }

        queued.forEach { session.sendHabboResponse(it) }
        // closeForcibly NÃO roda as tarefas pendentes (o EmbeddedChannel.close() roda antes de fechar, o que
        // faria o drain escrever com o canal ainda ativo): simula o fechamento ocorrer antes do drain
        channel.unsafe().closeForcibly()
        channel.runPendingTasks()

        assertNull(channel.readOutbound<HabboResponse>())
        queued.forEach { assertEquals("buffer vazou: header ${it.headerId}", 0, it.byteBuf.refCnt()) }
    }

    private fun drain(channel: EmbeddedChannel): List<Int> {
        val ids = ArrayList<Int>()
        while (true) {
            val r = channel.readOutbound<HabboResponse>() ?: break
            ids += r.headerId
            r.close()
        }
        return ids
    }

    @Test
    fun `dentro de um OutboxBatch o drain so e agendado ao fim do bloco e a ordem e mantida`() {
        val channelA = EmbeddedChannel()
        val channelB = EmbeddedChannel()
        val a = HabboSession(channelA)
        val b = HabboSession(channelB)

        OutboxBatch.collect {
            repeat(50) {
                a.sendHabboResponse(response(it))
                b.sendHabboResponse(response(1000 + it))
            }

            // nenhum drain foi agendado ainda: rodar as tarefas pendentes do loop não escreve nada
            channelA.runPendingTasks()
            channelB.runPendingTasks()
            assertTrue(channelA.outboundMessages().isEmpty())
            assertTrue(channelB.outboundMessages().isEmpty())
        }

        // ao sair do bloco cada sessão agendou UM drain, que escreve tudo em ordem
        channelA.runPendingTasks()
        channelB.runPendingTasks()
        assertEquals((0 until 50).toList(), drain(channelA))
        assertEquals((1000 until 1050).toList(), drain(channelB))
    }

    @Test
    fun `OutboxBatch aninhado nao agenda antes do bloco externo terminar`() {
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)

        OutboxBatch.collect {
            session.sendHabboResponse(response(1))
            OutboxBatch.collect { session.sendHabboResponse(response(2)) }
            channel.runPendingTasks()
            assertTrue("o bloco interno não pode drenar", channel.outboundMessages().isEmpty())
        }

        channel.runPendingTasks()
        assertEquals(listOf(1, 2), drain(channel))
    }

    @Test
    fun `OutboxBatch entrega o que foi enfileirado mesmo se o bloco lancar excecao`() {
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)

        try {
            OutboxBatch.collect {
                session.sendHabboResponse(response(7))
                error("falha no meio do tick")
            }
        } catch (_: IllegalStateException) {
        }

        channel.runPendingTasks()
        assertEquals(listOf(7), drain(channel))
        assertTrue("o estado por thread tem de ser limpo", !OutboxBatch.isActive())
    }

    @Test
    fun `fora de um batch o envio agenda o drain na hora`() {
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)

        session.sendHabboResponse(response(5))
        channel.runPendingTasks()

        assertEquals(listOf(5), drain(channel))
    }

    @Test
    fun `enviar com o canal ja fechado libera o buffer na hora`() {
        val channel = EmbeddedChannel()
        val session = HabboSession(channel)
        channel.close()

        val late = response(99)
        session.sendHabboResponse(late)

        assertEquals(0, late.byteBuf.refCnt())
    }
}
