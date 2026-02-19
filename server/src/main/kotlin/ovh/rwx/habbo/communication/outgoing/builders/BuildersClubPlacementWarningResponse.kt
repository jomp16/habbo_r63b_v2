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

package ovh.rwx.habbo.communication.outgoing.builders

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class BuildersClubPlacementWarningResponse {
    @Response(Outgoing.BUILDERS_PLACE_ITEM_WARNING)
    fun response(habboResponse: HabboResponse, data: BuildersClubPlacementData) {
        habboResponse.apply {
            writeInt(data.warningType)
            writeInt(data.pageId)
            writeInt(data.offerId)
            writeUTF(data.extraParam)

            if (data.isWall) {
                writeUTF(data.wallLocation)
            } else {
                writeInt(data.x)
                writeInt(data.y)
                writeInt(data.direction)
            }
        }
    }

    data class BuildersClubPlacementData(
        val warningType: Int,
        val pageId: Int = 0,
        val offerId: Int = 0,
        val extraParam: String = "",
        val x: Int = 0,
        val y: Int = 0,
        val direction: Int = 0,
        val wallLocation: String = "",
        val isWall: Boolean = false
    )
}
