/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.communication.outgoing.catalog

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.catalog.CatalogManager

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogOfferConfigurationResponse {
    @Response(Outgoing.CATALOG_OFFER_CONFIGURATION)
    fun response(habboResponse: HabboResponse) {
        habboResponse.apply {
            writeInt(100) // Most you can get.
            writeInt(CatalogManager.FREE_AMOUNT) // each n items
            writeInt(1) // win one item for free
            writeInt(1)
            writeInt(2)
            writeInt(40)
            writeInt(99)
        }
    }
}