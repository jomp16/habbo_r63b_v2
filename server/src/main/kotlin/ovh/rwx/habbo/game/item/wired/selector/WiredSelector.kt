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
import ovh.rwx.habbo.util.Vector2

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

    fun getNeighborhoodOffsets(options: List<Int>, radius: Int = 10): List<Vector2> {
        if (options.size < 4) {
            val list = mutableListOf<Vector2>()
            for (dx in -1..1) {
                for (dy in -1..1) {
                    list.add(Vector2(dx, dy))
                }
            }
            return list
        }

        val spiralParams = options.subList(3, options.size)
        val bitmask = mutableListOf<Boolean>()
        for (intVal in spiralParams) {
            for (b in 0 until 32) {
                bitmask.add((intVal and (1 shl b)) != 0)
            }
        }

        val side = radius * 2 + 1
        val totalTiles = side * side
        val activeOffsets = mutableListOf<Vector2>()
        var rank = 0
        var cx = 0
        var cy = 0
        var stepSize = 1
        val directions = listOf(Vector2(1, 0), Vector2(0, -1), Vector2(-1, 0), Vector2(0, 1))
        var dirIdx = 0

        while (rank < totalTiles) {
            for (sideRepeat in 0 until 2) {
                for (step in 0 until stepSize) {
                    if (rank < bitmask.size && bitmask[rank]) {
                        activeOffsets.add(Vector2(cx, cy))
                    }
                    val dir = directions[dirIdx]
                    cx += dir.x
                    cy += dir.y
                    rank++
                    if (rank >= totalTiles) break
                }
                if (rank >= totalTiles) break
                dirIdx = (dirIdx + 1) % 4
            }
            stepSize++
        }
        return activeOffsets
    }

    fun compareVariableValue(varValue: Any?, operator: Int, refValue: Double): Boolean {
        if (varValue == null) return false
        val num = (varValue as? Number)?.toDouble()
            ?: varValue.toString().toDoubleOrNull()

        if (num != null) {
            return when (operator) {
                0 -> num < refValue
                1 -> Math.abs(num - refValue) < 0.0001
                2 -> num > refValue
                3 -> num <= refValue
                4 -> Math.abs(num - refValue) >= 0.0001
                5 -> num >= refValue
                else -> false
            }
        }

        val str = varValue.toString()
        val refStr = if (refValue % 1.0 == 0.0) refValue.toLong().toString() else refValue.toString()
        return when (operator) {
            1 -> str.equals(refStr, ignoreCase = true)
            4 -> !str.equals(refStr, ignoreCase = true)
            else -> false
        }
    }
}