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

package ovh.rwx.habbo.communication.outgoing.gamecenter

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class SnowWarGameTokensResponse {
    @Response(Outgoing.SNOWWAR_GAME_TOKENS)
    fun response(
        habboResponse: HabboResponse,
        offers: List<SnowWarGameTokenOfferData>
    ) {
        habboResponse.apply {
            writeInt(offers.size)
            for (offer in offers) {
                writeInt(offer.offerId)
                writeUTF(offer.localizationId)
                writeInt(offer.priceInCredits)
                writeInt(offer.priceInActivityPoints)
                writeInt(offer.activityPointType)
            }
        }
    }
}

data class SnowWarGameTokenOfferData(
    val offerId: Int,
    val localizationId: String,
    val priceInCredits: Int,
    val priceInActivityPoints: Int = 0,
    val activityPointType: Int = 0
)
