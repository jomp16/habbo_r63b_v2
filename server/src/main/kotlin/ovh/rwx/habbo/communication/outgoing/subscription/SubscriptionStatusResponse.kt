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

package ovh.rwx.habbo.communication.outgoing.subscription

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

@Suppress("unused", "UNUSED_PARAMETER")
class SubscriptionStatusResponse {
    @Response(Outgoing.SUBSCRIPTION_STATUS)
    fun response(
        habboResponse: HabboResponse,
        clubType: String,
        active: Boolean,
        days: Int,
        months: Int,
        elapsedDays: Int,
        minutes: Int
    ) {
        habboResponse.apply {
            writeUTF(clubType)
            writeInt(days) // days left
            writeInt(if (active) 1 else 0) // active
            writeInt(months) // months left
            writeInt(1) // from request
            writeBoolean(active)
            writeBoolean(active)
            writeInt(elapsedDays) // hc elapsed
            writeInt(elapsedDays) // vip elapsed
            writeInt(minutes) // minutes left
            writeInt(-1) // ???????
        }
    }

    @ResponseR63A(OutgoingR63A.SUBSCRIPTION_STATUS)
    fun responseR63A(
        habboResponse: HabboResponse,
        clubType: String,
        active: Boolean,
        days: Int,
        months: Int,
        elapsedDays: Int,
        minutes: Int
    ) {
        habboResponse.apply {
            writeUTF(clubType) // productName ("habbo_club" ou "habbo_vip")
            writeInt(days) // daysLeft
            writeInt(elapsedDays) // pastClubDays (Dias já passados de HC)
            writeInt(months) // periods (Meses restantes)
            writeInt(1) // When set to 2, the Habbo club dialogue opens.

            if (isVersionAtLeast(2009, 10, 15)) {
                writeBoolean(active) // hasEverBeenMember
            }

            if (isVersionAtLeast(2010, 4, 7)) {
                writeBoolean(active) // isVIP
            }

            if (isVersionAtLeast(2010, 4, 27)) {
                writeInt(elapsedDays) // pastClubDays (Dias já passados de HC)
                writeInt(elapsedDays) // pastVipDays (Dias já passados de VIP)
            }

            if (isVersionAtLeast(2010, 11, 12) && isVersionBefore(2011, 8, 14)) {
                writeBoolean(false) // isShowBasicPromo
                writeInt(10) // regular price
                writeInt(9) // price with discount
            }

            if (isVersionAtLeast(2011, 8, 17)) {
                writeInt(minutes) // minutes left
            }
        }
    }
}