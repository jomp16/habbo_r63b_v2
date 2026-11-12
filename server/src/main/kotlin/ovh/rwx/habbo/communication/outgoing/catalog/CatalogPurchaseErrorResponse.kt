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
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPurchaseErrorResponse {
    @Response(Outgoing.CATALOG_PURCHASE_ERROR)
    @ResponseR63A(OutgoingR63A.CATALOG_PURCHASE_ERROR)
    fun response(habboResponse: HabboResponse, errorCode: Int) {
        habboResponse.apply {
            // PurchaseErrorMessageParser: parse vazio de 2009 (estático 65) até o pós-shuffle (md5 a5c6bea2);
            // lê errorCode desde 201210081113 (md5 233f804a). O estático 296 é o PurchaseNotAllowed.
            if (isVersionAtLeast(2012, 10, 8)) {
                writeInt(errorCode)
            }
        }
    }
}