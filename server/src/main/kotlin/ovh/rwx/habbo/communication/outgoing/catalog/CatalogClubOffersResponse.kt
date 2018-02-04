/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.catalog

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.catalog.CatalogClubOffer
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogClubOffersResponse {
    @Response(Outgoing.CATALOG_HABBO_CLUB_PAGE)
    fun response(habboResponse: HabboResponse, windowId: Int, clubOffers: Collection<CatalogClubOffer>) {
        habboResponse.apply {
            writeInt(clubOffers.size)

            clubOffers.forEach {
                val now = LocalDateTime.now()
                val localDateTime = now.plusMonths(it.months.toLong())

                writeInt(it.itemId)
                writeUTF(it.name)
                writeBoolean(false) // useless
                writeInt(it.credits) // credits
                writeInt(it.points) // points
                writeInt(it.pointsType) // type: 0 - pixel, > 0 - vip points (5 = diamonds)
                writeBoolean(true)
                writeInt(ChronoUnit.MONTHS.between(now, localDateTime).toInt())
                writeInt(ChronoUnit.DAYS.between(now, localDateTime).toInt())
                writeBoolean(it.giftable)
                writeInt(ChronoUnit.DAYS.between(now, localDateTime).toInt())
                writeInt(localDateTime.year)
                writeInt(localDateTime.monthValue)
                writeInt(localDateTime.dayOfMonth)
            }

            writeInt(windowId)
        }
    }
}