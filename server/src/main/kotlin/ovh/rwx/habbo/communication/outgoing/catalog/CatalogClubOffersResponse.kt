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

package ovh.rwx.habbo.communication.outgoing.catalog

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.catalog.CatalogClubOffer
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogClubOffersResponse {
    @Response(Outgoing.CATALOG_HABBO_CLUB_PAGE)
    fun response(habboResponse: HabboResponse, windowId: Int, clubOffers: Collection<CatalogClubOffer>, lastExpireOffer: LocalDateTime) {
        habboResponse.apply {
            writeInt(clubOffers.size)

            clubOffers.forEach {
                val localDateTime = lastExpireOffer.plusMonths(it.months.toLong())

                writeInt(it.itemId)
                writeUTF(it.name)

                // evolucao do offer no wire (habbo-timeline; AS3 confere): booleano lido e descartado desde 2013-02-26;
                // preco em 3 campos desde 2012-10-02 (antes, 1 Int); `upgrade` ate 2011-12-18; `months` desde 2011-12-19
                if (isVersionAtLeast(2013, 2, 26)) {
                    writeBoolean(false) // o cliente le e descarta (AS3 2013: `k.readBoolean();` sem atribuir)
                }

                if (isVersionAtLeast(2012, 10, 2)) {
                    writeInt(it.credits) // credits
                    writeInt(it.points) // points
                    writeInt(it.pointsType) // type: 0 - pixel, > 0 - vip points (5 = diamonds)
                } else {
                    writeInt(it.credits) // price
                }

                if (isVersionBefore(2011, 12, 19)) {
                    writeBoolean(false) // upgrade
                }

                writeBoolean(true)

                if (isVersionAtLeast(2011, 12, 19)) {
                    writeInt(ChronoUnit.MONTHS.between(lastExpireOffer, localDateTime).toInt())
                }

                writeInt(ChronoUnit.DAYS.between(lastExpireOffer, localDateTime).toInt())
                // giftable nasce entre RELEASE63-201406121205 e RELEASE63-201407091214;
                // antes disso o offer do wire tem só 2 bools (AS3: _SafeStr_2838@0522 = 13 reads)
                if (isVersionAtLeast(2014, 7, 9)) {
                    writeBoolean(it.giftable)
                }
                // daysLeftAfterPurchase: total de dias de HC restantes após a compra (hoje → nova expiração),
                // na mesma base `periods * 31 + days` do SUBSCRIPTION_STATUS ("antes" vs "depois" no confirm)
                writeInt(ChronoUnit.DAYS.between(LocalDateTime.now(), localDateTime).toInt().coerceAtLeast(0))
                writeInt(localDateTime.year)
                writeInt(localDateTime.monthValue)
                writeInt(localDateTime.dayOfMonth)
            }

            // source/windowId: desde 2011-12-19
            if (isVersionAtLeast(2011, 12, 19)) {
                writeInt(windowId)
            }
        }
    }
}