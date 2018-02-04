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

package ovh.rwx.habbo.game.landing

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize

data class LandingPromo(
        val id: Int,
        private val header: String,
        private val body: String,
        private val button: String,
        private val showButton: Boolean,
        private val buttonLink: String,
        private val image: String
) : IHabboResponseSerialize {
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(id) // index
            writeUTF(header) // title
            writeUTF(body) // body
            writeUTF(button) // button name
            writeInt(if (showButton) 1 else 0) // show button
            writeUTF(buttonLink) // button action
            writeUTF(image) // promo image
        }
    }
}