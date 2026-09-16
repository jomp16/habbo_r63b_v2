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

data class SubscriptionStatusData(
    val clubType: String,
    val active: Boolean,
    val days: Int,
    val months: Int,
    val elapsedDays: Int,
    val minutes: Int
)

@Suppress("unused", "UNUSED_PARAMETER")
class SubscriptionStatusResponse {
    @Response(Outgoing.SUBSCRIPTION_STATUS)
    @ResponseR63A(OutgoingR63A.SUBSCRIPTION_STATUS)
    fun response(habboResponse: HabboResponse, data: SubscriptionStatusData) {
        habboResponse.apply {
            // 1. Dados Fundamentais (Presente desde a RELEASE34)
            writeUTF(data.clubType) // productName ("habbo_club" ou "habbo_vip")
            writeInt(data.days) // daysToPeriodEnd
            writeInt(if (isVersionBefore(2010, 4, 27) && data.active) 1 else data.elapsedDays) // memberPeriods
            writeInt(data.months) // periodsSubscribedAhead
            writeInt(1) // responseType (quando 2, abre a janela de compra do clube)

            // 2. hasEverBeenMember (Adicionado na R39, com rollback na R38)
            val hasEverBeenMemberSupport = isVersionAtLeast(2009, 10, 15) && habboVersion.majorVersion != 38
            if (hasEverBeenMemberSupport) {
                writeBoolean(data.active)
            }

            // 3. isVIP (Adicionado na R49, com rollback na R48)
            val isVipSupport = isVersionAtLeast(2010, 4, 7) && habboVersion.majorVersion != 48
            if (isVipSupport) {
                writeBoolean(data.active)
            }

            // 4. pastClubDays e pastVipDays (Adicionados na RELEASE50)
            if (isVersionAtLeast(2010, 4, 27)) {
                writeInt(data.elapsedDays) // pastClubDays
                writeInt(data.elapsedDays) // pastVipDays
            }

            // 5. Promoção temporária de assinatura (RELEASE62 até meados da R63)
            if (isVersionBetween(2010, 11, 12, 2011, 8, 14)) {
                writeBoolean(false) // isShowBasicPromo
                writeInt(10) // regular price
                writeInt(9) // price with discount
            }

            // 6. minutesUntilExpiration (Adicionado na build 201108172310 da R63)
            if (isVersionAtLeast(2011, 8, 17)) {
                writeInt(data.minutes)
            }

            // 7. Campo extra introduzido no PRODUCTION moderno / R63B (201512012203+)
            if (isVersionAtLeast(2015, 12, 1)) {
                writeInt(-1) // minutesSinceLastModified / status flag
            }
        }
    }
}