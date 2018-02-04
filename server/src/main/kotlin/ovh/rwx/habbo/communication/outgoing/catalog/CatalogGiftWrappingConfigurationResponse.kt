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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogGiftWrappingConfigurationResponse {
    @Response(Outgoing.CATALOG_GIFT_WRAPPING)
    fun response(habboResponse: HabboResponse) {
        habboResponse.apply {
            writeBoolean(true) // enabled modern gifts
            writeInt(1) // cost
            writeInt(HabboServer.habboGame.itemManager.newGiftWrapper.size)

            HabboServer.habboGame.itemManager.newGiftWrapper.forEach { writeInt(it.spriteId) }

            writeInt(8)

            repeat(8) { writeInt(it) }

            writeInt(11)

            repeat(11) { writeInt(it) }

            writeInt(HabboServer.habboGame.itemManager.oldGiftWrapper.size)

            HabboServer.habboGame.itemManager.oldGiftWrapper.forEach { writeInt(it.spriteId) }
        }
    }
}