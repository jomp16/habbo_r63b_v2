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

package ovh.rwx.fastfood.communication.outgoing.game

import ovh.rwx.fastfood.communication.FFResponse
import ovh.rwx.fastfood.communication.outgoing.FFOutgoing
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.database.fastfood.FastFoodPowerUpOffer

@Suppress("unused", "UNUSED_PARAMETER")
class GamePowerUpsResponse {
    @FFResponse(FFOutgoing.POWER_UPS_INVENTORY)
    fun responsePowerUpsInventory(habboResponse: HabboResponse, inventory: Map<Int, Int>) {
        habboResponse.apply {
            writeInt(inventory.size)
            inventory.forEach { (type, amount) ->
                writeInt(type)
                writeInt(amount)
            }
        }
    }

    /** As ofertas do "Comprar mais" (parser §_-Es§ → §_-FP§: id, tipo, quantidade, preço); a compra manda o id. */
    @FFResponse(FFOutgoing.POWER_UPS_PRICE)
    fun responsePowerUpsPrice(habboResponse: HabboResponse, offers: List<FastFoodPowerUpOffer>) {
        habboResponse.apply {
            writeInt(offers.size)
            offers.forEach { offer ->
                writeUTF(offer.id)
                writeUTF(offer.type)
                writeInt(offer.amount)
                writeInt(offer.priceCredits)
            }
        }
    }
}
