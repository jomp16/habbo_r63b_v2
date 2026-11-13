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

package ovh.rwx.habbo.game.user.block

/** Resultado de bloquear/desbloquear; [REFUSED] = nada mudou (nada é enviado ao cliente). */
enum class BlockResult { REFUSED, BLOCKED, UNBLOCKED }

/**
 * Lista ordenada (mais antigo primeiro) de ids bloqueados, sem banco: só a decisão de bloquear/desbloquear.
 * Sem limite. Escritas são sincronizadas (threads de handler); [isBlocked] lê um snapshot imutável e é seguro para as
 * threads de sala, sem lock.
 */
class BlockList {
    private val ids = LinkedHashSet<Int>()

    @Volatile
    private var snapshot: Set<Int> = emptySet()

    fun snapshot(): Set<Int> = snapshot

    fun isBlocked(userId: Int): Boolean = userId in snapshot

    @Synchronized
    fun load(blockedIds: Collection<Int>) {
        ids.clear()
        ids.addAll(blockedIds)
        publish()
    }

    @Synchronized
    fun block(ownerId: Int, userId: Int): BlockResult {
        if (userId == ownerId || !ids.add(userId)) return BlockResult.REFUSED

        publish()

        return BlockResult.BLOCKED
    }

    @Synchronized
    fun unblock(userId: Int): BlockResult {
        if (!ids.remove(userId)) return BlockResult.REFUSED

        publish()

        return BlockResult.UNBLOCKED
    }

    private fun publish() {
        snapshot = LinkedHashSet(ids)
    }
}
