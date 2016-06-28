/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.communication.outgoing.catalog

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing

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

            (0..7).forEach { i -> writeInt(i) }

            writeInt(11)

            (0..10).forEach { i -> writeInt(i) }

            writeInt(HabboServer.habboGame.itemManager.oldGiftWrapper.size)

            HabboServer.habboGame.itemManager.oldGiftWrapper.forEach { writeInt(it.spriteId) }
        }
    }
}