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

import ovh.rwx.habbo.database.fastfood.FastFoodDao
import ovh.rwx.habbo.game.user.HabboSession

/**
 * O estoque de turbos de um usuário (fastfood_power_ups), em memória enquanto a sessão do hotel existir: compras
 * (event loop do Netty) e turbos usados (thread das partidas) mexem nele, então todo acesso é sincronizado. Cada
 * mudança grava a quantidade nova pelo write-behind (WriteKey.user: as gravações saem em ordem).
 */
class FastFoodPowerUpInventory internal constructor(
    private val amounts: MutableMap<FastFoodPowerUp, Int>,
    private val save: (FastFoodPowerUp, Int) -> Unit
) {
    companion object {
        /** Quantos de cada tipo uma partida libera, no máximo ("Podem ser usados TRÊS ... por jogo"). */
        const val MAX_PER_GAME = 3

        /** O presente de quem joga pela primeira vez: 3 mísseis e 5 escudos. */
        val STARTER = mapOf(FastFoodPowerUp.MISSILE to 3, FastFoodPowerUp.SHIELD to 5)

        /**
         * O estoque a partir das linhas do banco (tipo → quantidade). Sem linha nenhuma o usuário nunca jogou: ganha
         * o [STARTER] e TODOS os tipos são gravados (a presença das linhas marca que o presente já foi dado).
         */
        internal fun fromRows(rows: Map<String, Int>): Pair<Map<FastFoodPowerUp, Int>, Boolean> {
            val firstTime = rows.isEmpty()
            val amounts = FastFoodPowerUp.entries.associateWith { type ->
                if (firstTime) STARTER[type] ?: 0 else (rows[type.code] ?: 0).coerceAtLeast(0)
            }
            return amounts to firstTime
        }

        /** O estoque do usuário da sessão, carregado do banco na primeira vez. */
        fun of(habboSession: HabboSession): FastFoodPowerUpInventory {
            habboSession.fastFoodPowerUps?.let { return it }
            synchronized(habboSession) {
                habboSession.fastFoodPowerUps?.let { return it }
                val userId = habboSession.userInformation.id
                val (amounts, firstTime) = fromRows(FastFoodDao.getPowerUps(userId))
                val save = { type: FastFoodPowerUp, amount: Int -> FastFoodDao.savePowerUp(userId, type.code, amount) }
                if (firstTime) amounts.forEach { (type, amount) -> save(type, amount) }
                return FastFoodPowerUpInventory(amounts.toMutableMap(), save).also { habboSession.fastFoodPowerUps = it }
            }
        }
    }

    @Synchronized
    fun amount(type: FastFoodPowerUp): Int = amounts.getValue(type)

    /** O estoque no formato do POWER_UPS_INVENTORY (tipo do wire → quantidade). */
    @Synchronized
    fun snapshot(): Map<Int, Int> = FastFoodPowerUp.entries.associate { it.wire to amounts.getValue(it) }

    /** Quantos de cada tipo uma partida libera: o estoque, até [MAX_PER_GAME]. */
    @Synchronized
    fun allowance(): Map<FastFoodPowerUp, Int> = FastFoodPowerUp.entries.associateWith { minOf(MAX_PER_GAME, amounts.getValue(it)) }

    @Synchronized
    fun add(type: FastFoodPowerUp, amount: Int) {
        if (amount <= 0) return
        val total = amounts.getValue(type) + amount
        amounts[type] = total
        save(type, total)
    }

    /** Gasta um turbo do estoque; false se não tem. */
    @Synchronized
    fun take(type: FastFoodPowerUp): Boolean {
        val current = amounts.getValue(type)
        if (current <= 0) return false
        amounts[type] = current - 1
        save(type, current - 1)
        return true
    }
}
