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

package ovh.rwx.habbo.communication.outgoing.catalog.recycler

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogRecyclerStatusResponse {
    @Response(Outgoing.CATALOG_RECYCLER_STATUS)
    @ResponseR63A(OutgoingR63A.CATALOG_RECYCLER_STATUS)
    fun response(habboResponse: HabboResponse, recyclerResultResponse: CatalogRecyclerStatus, timeInSeconds: Int) {
        habboResponse.apply {
            writeInt(recyclerResultResponse.code)
            writeInt(timeInSeconds)
        }
    }

    enum class CatalogRecyclerStatus(val code: Int) {
        OPEN(1),
        CLOSED(2),
        CLOSED_WITH_TIME(3)
    }
}