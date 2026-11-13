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

package ovh.rwx.habbo.game.catalog

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogVoucherRedeemErrorResponse
import ovh.rwx.habbo.game.currency.CurrencyService
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.ActivityPointType

class CatalogVoucherService {

    fun redeemVoucher(habboSession: HabboSession, voucherCode: String) {
        if (voucherCode == "full" && habboSession.hasPermission("acc_catalog_voucher_full")) {
            // Concede o máximo (grant satura em Int.MAX_VALUE) e reenvia os saldos
            val points = mutableMapOf(ActivityPointType.PIXELS.code to Int.MAX_VALUE)
            if (habboSession.userInformation.vip) points[ActivityPointType.DIAMONDS.code] = Int.MAX_VALUE
            CurrencyService.grantAll(habboSession, Int.MAX_VALUE, points)

            habboSession.sendResponse(Outgoing.CATALOG_VOUCHER_REDEEMED, OutgoingR63A.CATALOG_VOUCHER_REDEEMED, "", "")
            return
        }

        // todo: add a voucher table and redeem
        habboSession.sendResponse(
            Outgoing.CATALOG_VOUCHER_REDEEM_ERROR,
            OutgoingR63A.CATALOG_VOUCHER_REDEEM_ERROR,
            CatalogVoucherRedeemErrorResponse.CatalogVoucherRedeemError.NOT_VALID
        )
    }
}
