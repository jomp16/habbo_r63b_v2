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

package ovh.rwx.habbo.database.fastfood

import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.writebehind.WriteKey

/** Turbos do Fast Food: o estoque de cada usuário e as ofertas da loja do lobby (V34). */
object FastFoodDao {
    /** O estoque do usuário por tipo (o nome do cliente); vazio = nunca jogou. */
    fun getPowerUps(userId: Int): Map<String, Int> = db {
        query<PowerUpRow>(
            "SELECT `type`, `amount` FROM `fastfood_power_ups` WHERE `user_id` = :userId",
            mapOf("userId" to userId)
        )
    }.associate { it.type to it.amount }

    /** Grava a quantidade (absoluta) de um tipo; as gravações do mesmo usuário saem na ordem em que entraram. */
    fun savePowerUp(userId: Int, type: String, amount: Int) {
        WriteBehindManager.queue(WriteKey.user(userId)) {
            db {
                update(
                    "INSERT INTO `fastfood_power_ups` (`user_id`, `type`, `amount`) VALUES (:userId, :type, :amount) " +
                        "ON DUPLICATE KEY UPDATE `amount` = VALUES(`amount`)",
                    mapOf("userId" to userId, "type" to type, "amount" to amount)
                )
            }
        }
    }

    fun getOffers(): List<FastFoodPowerUpOffer> = db {
        query<FastFoodPowerUpOffer>(
            "SELECT `id`, `type`, `amount`, `price_credits` FROM `fastfood_power_up_offers` ORDER BY `type`, `position`, `id`"
        )
    }

    data class PowerUpRow(val type: String, val amount: Int)
}

/** Uma oferta do "Comprar mais": [amount] turbos do [type] por [priceCredits] créditos; o cliente compra pelo [id]. */
data class FastFoodPowerUpOffer(
    val id: String,
    val type: String,
    val amount: Int,
    val priceCredits: Int
)
