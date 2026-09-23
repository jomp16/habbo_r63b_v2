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

package ovh.rwx.habbo.game.snowwar.enums

enum class SnowWarOfferType(
    val offerId: Int,
    val localizationId: String,
    val priceInCredits: Int,
    val priceInActivityPoints: Int = 0,
    val activityPointType: Int = 0
) {
    TOKENS_10(1, "GET_SNOWWAR_TOKENS", 3),
    TOKENS_100(2, "GET_SNOWWAR_TOKENS2", 10),
    TOKENS_300(3, "GET_SNOWWAR_TOKENS3", 20);

    companion object {
        val defaultOffers: List<ovh.rwx.habbo.communication.outgoing.gamecenter.SnowWarGameTokenOfferData> by lazy {
            entries.map {
                ovh.rwx.habbo.communication.outgoing.gamecenter.SnowWarGameTokenOfferData(
                    offerId = it.offerId,
                    localizationId = it.localizationId,
                    priceInCredits = it.priceInCredits,
                    priceInActivityPoints = it.priceInActivityPoints,
                    activityPointType = it.activityPointType
                )
            }
        }

        fun fromId(id: Int): SnowWarOfferType? {
            return entries.firstOrNull { it.offerId == id }
        }
    }
}
