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

package ovh.rwx.habbo.game.catalog

object CatalogDiscount {
    const val BULK_DISCOUNT_BATCH_SIZE = 6

    /**
     * Calcula a quantidade de mobílias gratuitas concedidas na compra em lote (bulk discount).
     *
     * Fórmula oficial do Habbo:
     * - A cada lote de 6 itens, ganha 1 grátis (ex: 6 -> 1 grátis, 12 -> 2 grátis).
     * - Bônus intermediário se o resto do lote for 5 (falta apenas 1 para o próximo lote).
     * - Bônus adicionais de patamares para compras grandes (>= 40 itens e >= 99 itens).
     */
    fun calculateFreeAmount(amount: Int): Int {
        if (amount < BULK_DISCOUNT_BATCH_SIZE) return 0

        val batchDiscount = amount / BULK_DISCOUNT_BATCH_SIZE

        val remainderBonus = if (amount % BULK_DISCOUNT_BATCH_SIZE == BULK_DISCOUNT_BATCH_SIZE - 1) 1 else 0
        val intermediateBonus = if (batchDiscount >= 1) remainderBonus + (batchDiscount - 1) else 0

        val tierBonus = (if (amount >= 40) 1 else 0) + (if (amount >= 99) 1 else 0)

        return batchDiscount + intermediateBonus + tierBonus
    }

    /**
     * Retorna a quantidade líquida a pagar após aplicar o desconto em lote.
     */
    fun calculateTotalToPay(amount: Int): Int =
        (amount - calculateFreeAmount(amount)).coerceAtLeast(1)
}
