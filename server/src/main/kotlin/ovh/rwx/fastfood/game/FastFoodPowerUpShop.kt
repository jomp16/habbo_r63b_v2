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
import ovh.rwx.habbo.database.fastfood.FastFoodPowerUpOffer
import ovh.rwx.habbo.game.currency.CurrencyService
import ovh.rwx.habbo.game.currency.Price
import ovh.rwx.habbo.game.user.HabboSession

/** A loja de turbos do lobby: as ofertas de fastfood_power_up_offers (lidas uma vez) e a compra. */
object FastFoodPowerUpShop {
    val offers: List<FastFoodPowerUpOffer> by lazy {
        FastFoodDao.getOffers().filter { FastFoodPowerUp.fromCode(it.type) != null && it.amount > 0 && it.priceCredits >= 0 }
    }

    /**
     * Compra uma oferta: cobra os créditos e põe os turbos no estoque (gravado). Null se a oferta não existe ou falta
     * crédito: nada é cobrado nem entregue.
     */
    fun purchase(habboSession: HabboSession, offerId: String): Pair<FastFoodPowerUp, Int>? {
        val offer = offers.firstOrNull { it.id == offerId } ?: return null
        val type = FastFoodPowerUp.fromCode(offer.type) ?: return null
        val inventory = FastFoodPowerUpInventory.of(habboSession)
        if (!CurrencyService.tryCharge(habboSession, Price(offer.priceCredits, 0, 0)).ok) return null
        inventory.add(type, offer.amount)
        return type to offer.amount
    }
}
