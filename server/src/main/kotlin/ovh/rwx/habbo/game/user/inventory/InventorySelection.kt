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

package ovh.rwx.habbo.game.user.inventory

/**
 * Seleção pura de itens do inventário a partir de ids vindos do cliente (nunca confiáveis): só o que a sessão
 * realmente possui pode ser removido/apagado/consumido.
 */
object InventorySelection {
    /**
     * Seleção "tudo ou nada": devolve os itens na ordem do pedido, ou null se algum id se repete, não pertence ao
     * inventário ([owned]) ou reprova no [predicate]. Lista vazia também é null (nada a consumir).
     */
    fun <T> select(owned: Map<Int, T>, requested: List<Int>, predicate: (T) -> Boolean = { true }): List<T>? {
        if (requested.isEmpty() || requested.distinct().size != requested.size) return null

        val selected = ArrayList<T>(requested.size)
        for (id in requested) {
            val item = owned[id] ?: return null
            if (!predicate(item)) return null
            selected += item
        }

        return selected
    }
}
