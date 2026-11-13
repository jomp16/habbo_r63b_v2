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

package ovh.rwx.habbo.game.user.ignore

/** Linha da lista de ignorados: id e username ATUAL do usuário ignorado. */
data class IgnoredUser(val userId: Int, val username: String)

/** Código enviado ao cliente em ROOM_IGNORE_USER_RESULT (um por pedido de ignorar/designorar). */
enum class IgnoreResult(val code: Int) {
    /** Nada mudou (recusado): o cliente só descarta o pedido pendente. */
    REFUSED(0),
    ADDED(1),

    /** Adicionado, e o cliente descarta o ignorado mais ANTIGO (limite atingido). */
    ADDED_DROPPED_OLDEST(2),
    REMOVED(3)
}

/** Resultado de [IgnoreList.add]; [droppedUserId] é o mais antigo removido pelo limite (resultado 2). */
data class IgnoreAddOutcome(val result: IgnoreResult, val droppedUserId: Int? = null)

/**
 * Lista ordenada (mais antigo primeiro) de ignorados, sem banco: só a decisão de adicionar/remover/limite.
 * Escritas são sincronizadas (threads de handler); [isIgnoring] lê um snapshot imutável e é seguro para as threads de
 * sala, sem lock.
 */
class IgnoreList(private val maxIgnores: Int = MAX_IGNORES) {
    private val entries = LinkedHashMap<Int, String>()

    @Volatile
    private var idsSnapshot: Set<Int> = emptySet()

    @Volatile
    private var entriesSnapshot: List<IgnoredUser> = emptyList()

    val isEmpty: Boolean get() = idsSnapshot.isEmpty()

    /** Ordenada do mais antigo para o mais recente (a mesma ordem que o cliente mantém). */
    fun snapshot(): List<IgnoredUser> = entriesSnapshot

    fun isIgnoring(userId: Int): Boolean = userId in idsSnapshot

    @Synchronized
    fun load(ignores: List<IgnoredUser>) {
        entries.clear()
        ignores.forEach { entries[it.userId] = it.username }
        publish()
    }

    @Synchronized
    fun add(ownerId: Int, userId: Int, username: String): IgnoreAddOutcome {
        if (userId == ownerId || entries.containsKey(userId)) return IgnoreAddOutcome(IgnoreResult.REFUSED)

        var dropped: Int? = null
        if (entries.size >= maxIgnores) {
            dropped = entries.keys.first()
            entries.remove(dropped)
        }

        entries[userId] = username
        publish()

        return if (dropped == null) IgnoreAddOutcome(IgnoreResult.ADDED)
        else IgnoreAddOutcome(IgnoreResult.ADDED_DROPPED_OLDEST, dropped)
    }

    @Synchronized
    fun remove(userId: Int): IgnoreResult {
        if (entries.remove(userId) == null) return IgnoreResult.REFUSED

        publish()

        return IgnoreResult.REMOVED
    }

    private fun publish() {
        idsSnapshot = entries.keys.toSet()
        entriesSnapshot = entries.map { IgnoredUser(it.key, it.value) }
    }

    companion object {
        /** Limite clássico da lista de ignorados. */
        const val MAX_IGNORES = 100
    }
}
