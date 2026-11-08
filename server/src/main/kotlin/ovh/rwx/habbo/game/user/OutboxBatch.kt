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

/**
 * Adia o agendamento do drain da fila de saída das sessões até o fim de um bloco síncrono (o tick de uma sala).
 *
 * Sem isto, a primeira mensagem para cada sessão agenda uma tarefa no event loop dela (e, se o loop está parado,
 * paga uma syscall de wakeup) A CADA rajada que o loop já terminou de drenar — num quarto de 500 usuários com dezenas
 * de falas por tick isso eram dezenas de milhares de wakeups por tick. Dentro de [collect] as mensagens só
 * entram na fila da sessão; ao sair do bloco cada sessão tocada agenda UM drain, que escreve tudo com um flush.
 * A ordem por sessão não muda; o custo é no máximo a duração do bloco de latência extra.
 *
 * O bloco tem de ser síncrono (sem suspender): o estado é por thread.
 */
internal object OutboxBatch {
    /** Estado reutilizado por thread: o tick roda dezenas de vezes por segundo por sala, então nada é alocado por bloco. */
    class State {
        val pending = ArrayList<HabboSession>(256)
        var active = false
    }

    private val current: ThreadLocal<State> = ThreadLocal.withInitial { State() }

    /** Sessão com mensagens enfileiradas que ainda precisam de um drain agendado ao fim do bloco. */
    fun defer(session: HabboSession): Boolean {
        val state = current.get()
        if (!state.active) return false
        state.pending.add(session)
        return true
    }

    inline fun <T> collect(block: () -> T): T {
        if (isActive()) return block()

        val state = begin()
        try {
            return block()
        } finally {
            end(state)
        }
    }

    fun isActive(): Boolean = current.get().active

    @PublishedApi
    internal fun begin(): State = current.get().also { it.active = true }

    @PublishedApi
    internal fun end(state: State) {
        // desativa antes de agendar: um drain síncrono que reenfileire não pode voltar a adiar para esta lista
        state.active = false
        val pending = state.pending
        try {
            for (i in 0 until pending.size) pending[i].scheduleOutboxDrain()
        } finally {
            pending.clear()
        }
    }
}
