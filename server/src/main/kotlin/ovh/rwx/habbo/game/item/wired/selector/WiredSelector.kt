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

package ovh.rwx.habbo.game.item.wired.selector

import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItem
import ovh.rwx.habbo.game.room.Room

abstract class WiredSelector(room: Room, roomItem: RoomItem) : WiredItem(room, roomItem) {
    abstract fun onSelect(context: WiredContext)

    /**
     * Refina a lista de alvos baseada nas opções de Filtro e Inversão.
     * O tipo <T> permite que funcione para RoomItem ou RoomUser.
     */
    fun <T> refineTargets(
        contextTargets: MutableList<T>,
        currentSelection: List<T>,
        isFilter: Boolean,
        isInverse: Boolean,
        allPossibleTargets: () -> Collection<T> // Função para buscar "todos" só se precisar (Inverter)
    ) {
        // 1. Lógica de Adição ou Interseção (Filtro)
        if (!isFilter) {
            currentSelection.forEach { target ->
                if (!contextTargets.contains(target)) {
                    contextTargets.add(target)
                }
            }
        } else {
            // Mantém apenas o que já estava no contexto E está na seleção atual
            contextTargets.retainAll(currentSelection.toSet())
        }

        // 2. Lógica de Inversão
        if (isInverse) {
            if (contextTargets.isEmpty() && !isFilter) {
                // Caso perigoso: Inverter sem filtro prévio
                val inverted = allPossibleTargets().filter { it !in currentSelection }
                contextTargets.addAll(inverted.take(100)) // Limite de segurança
            } else {
                contextTargets.removeAll(currentSelection.toSet())
            }
        }
    }
}