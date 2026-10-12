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
import ovh.rwx.fastfood.game.PowerUpEventData
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.communication.HabboRequest

@Suppress("unused", "UNUSED_PARAMETER")
class GamePurchasePowerUpHandler {
    // ids do cliente: 0 = bigparachute, 1 = missile, 2 = shield (constantes §_-DQ§)
    private val powerUpPrices = mapOf(
        "bigparachute" to 1,
        "missile" to 1,
        "shield" to 1
    )
    private val typeIds = mapOf(
        "bigparachute" to 0,
        "missile" to 1,
        "shield" to 2
    )

    @FFHandler(FFIncoming.PURCHASE_POWER_UP)
    fun handle(fastFoodSession: FastFoodSession, habboRequest: HabboRequest) {
        if (!fastFoodSession.authenticated) return

        val habboSession = fastFoodSession.habboSession ?: return
        val powerUpId = habboRequest.readUTF()

        val price = powerUpPrices[powerUpId] ?: return
        val typeId = typeIds[powerUpId] ?: return

        val userInformation = habboSession.userInformation
        if (userInformation.credits < price) return
        userInformation.credits -= price

        fastFoodSession.sendHabboResponse(FFOutgoing.USER_CREDITS, userInformation.credits)
        fastFoodSession.sendHabboResponse(FFOutgoing.POWER_UP_GAINED, PowerUpEventData(count = 3, type = typeId))
    }
}
