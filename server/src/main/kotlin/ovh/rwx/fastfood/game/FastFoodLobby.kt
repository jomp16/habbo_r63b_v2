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

/**
 * A fila de quem espera uma partida ("aguardando jogadores" no cliente). O primeiro a entrar abre a espera: a
 * partida sai quando a mesa enche (capacity) ou quando a espera vence, com quem houver; os bots completam o resto.
 * Quem sobra de uma mesa cheia abre uma espera nova.
 *
 * Sem sincronização: só a thread do FastFoodGameManager mexe nela. O tempo é o tick do manager.
 */
class FastFoodLobby<T>(private val capacity: Int, private val waitTicks: Long) {
    private val waiting = LinkedHashSet<T>()
    private var deadline = 0L

    val size get() = waiting.size

    fun isEmpty() = waiting.isEmpty()

    operator fun contains(player: T) = player in waiting

    /** Entra na fila; false se já estava. */
    fun join(player: T, now: Long): Boolean {
        if (waiting.isEmpty()) deadline = now + waitTicks
        return waiting.add(player)
    }

    fun leave(player: T) = waiting.remove(player)

    /** Os jogadores da próxima partida, se ela já pode sair (mesa cheia ou espera vencida); null se ainda espera. */
    fun takeReady(now: Long): List<T>? {
        if (waiting.isEmpty() || (waiting.size < capacity && now < deadline)) return null
        val table = waiting.take(capacity)
        waiting.removeAll(table.toSet())
        if (waiting.isNotEmpty()) deadline = now + waitTicks
        return table
    }
}
