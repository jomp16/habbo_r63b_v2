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

package ovh.rwx.habbo.communication.outgoing.user

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.currency.ActivityPointTypes
import ovh.rwx.habbo.game.user.information.UserCurrencies

@Suppress("unused", "UNUSED_PARAMETER")
class UserActivityPointsResponse {
    @Response(Outgoing.ACTIVITY_POINTS_BALANCE)
    @ResponseR63A(OutgoingR63A.ACTIVITY_POINTS_BALANCE)
    fun response(habboResponse: HabboResponse, currencies: UserCurrencies) {
        // Só os tipos que o client dessa release conhece: um tipo desconhecido pode quebrar a UI dele (ver ActivityPointTypes)
        val supported = ActivityPointTypes.supportedBalances(habboResponse.habboVersion, currencies.snapshot())

        habboResponse.apply {
            writeInt(supported.size)

            supported.forEach { (type, amount) ->
                writeInt(type)
                writeInt(amount)
            }
        }
    }
}

