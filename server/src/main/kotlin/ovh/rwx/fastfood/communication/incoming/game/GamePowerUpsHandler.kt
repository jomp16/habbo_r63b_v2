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

package ovh.rwx.fastfood.communication.incoming.game

import ovh.rwx.fastfood.communication.FFHandler
import ovh.rwx.fastfood.communication.incoming.FFIncoming
import ovh.rwx.fastfood.communication.outgoing.FFOutgoing
import ovh.rwx.fastfood.game.FastFoodPowerUpInventory
import ovh.rwx.fastfood.game.FastFoodPowerUpShop
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.communication.HabboRequest

@Suppress("unused", "UNUSED_PARAMETER")
class GamePowerUpsHandler {
    /** O estoque de turbos do lobby (o "x N" ao lado de cada um). */
    @FFHandler(FFIncoming.GET_POWER_UPS)
    fun handleGetPowerUps(fastFoodSession: FastFoodSession, habboRequest: HabboRequest) {
        val habboSession = fastFoodSession.habboSession ?: return
        fastFoodSession.sendHabboResponse(FFOutgoing.POWER_UPS_INVENTORY, FastFoodPowerUpInventory.of(habboSession).snapshot())
    }

    @FFHandler(FFIncoming.GET_POWER_UPS_PRICE)
    fun handleGetPowerUpsPrice(fastFoodSession: FastFoodSession, habboRequest: HabboRequest) {
        if (!fastFoodSession.authenticated) return
        fastFoodSession.sendHabboResponse(FFOutgoing.POWER_UPS_PRICE, FastFoodPowerUpShop.offers)
    }
}
