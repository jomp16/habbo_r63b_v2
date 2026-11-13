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

package ovh.rwx.habbo.game.user.information

import ovh.rwx.habbo.util.ActivityPointType
import java.util.concurrent.ConcurrentHashMap

/**
 * Saldos de moedas alternativas (activity points) de um usuário, indexados pelo código do tipo.
 *
 * A chave é o código inteiro, e não o enum [ActivityPointType]: um tipo que o enum não conhece (linha antiga ou de
 * outra release no banco) continua sendo carregado e regravado em vez de sumir.
 * Cada alteração marca só o tipo alterado como sujo ([drainDirty]) e avisa o dono via [onChanged], para que o
 * write-behind grave apenas o que mudou.
 */
class UserCurrencies(initial: Map<Int, Int> = emptyMap(), private val onChanged: () -> Unit = {}) {
    private val balances = ConcurrentHashMap<Int, Int>(initial)
    private val dirty = ConcurrentHashMap.newKeySet<Int>()

    operator fun get(type: Int): Int = balances[type] ?: 0

    operator fun get(type: ActivityPointType): Int = get(type.code)

    fun has(type: Int, amount: Int): Boolean = amount <= 0 || get(type) >= amount

    /** Define o saldo (limitado a [0, Int.MAX_VALUE]). */
    operator fun set(type: Int, value: Int) {
        val clamped = value.coerceAtLeast(0)
        val previous = balances.put(type, clamped)
        if (previous != clamped) markDirty(type)
    }

    operator fun set(type: ActivityPointType, value: Int) = set(type.code, value)

    /** Soma [delta] (negativo debita), saturando em 0 e Int.MAX_VALUE. */
    fun add(type: Int, delta: Int) {
        if (delta == 0) return
        val changed = booleanArrayOf(false)
        balances.compute(type) { _, current ->
            val old = current ?: 0
            val updated = (old.toLong() + delta).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
            if (updated != old || current == null) changed[0] = true
            updated
        }
        if (changed[0]) markDirty(type)
    }

    fun add(type: ActivityPointType, delta: Int) = add(type.code, delta)

    /** Cópia imutável dos saldos, ordenada por código (ordem estável nos pacotes). */
    fun snapshot(): Map<Int, Int> = balances.toSortedMap()

    /** Retira e devolve os saldos alterados desde a última chamada. */
    fun drainDirty(): Map<Int, Int> {
        val result = LinkedHashMap<Int, Int>()
        for (type in dirty.toList()) {
            dirty.remove(type)
            result[type] = get(type)
        }
        return result
    }

    /** Devolve tipos à lista de sujos (uma gravação que falhou precisa ser refeita). */
    fun restoreDirty(types: Collection<Int>) {
        if (types.isEmpty()) return
        dirty.addAll(types)
        onChanged()
    }

    private fun markDirty(type: Int) {
        dirty.add(type)
        onChanged()
    }
}
