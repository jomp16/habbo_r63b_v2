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

package ovh.rwx.habbo.game.item.stuff

import ovh.rwx.habbo.communication.HabboResponse

class LimitedStuffData(private val value: String, private val limitedNumber: Int, private val limitedTotal: Int) :
    StuffData(FORMAT_KEY_LEGACY_WITH_LIMITED, roomExtra = 1) {
    override val legacyValue: String = value

    override fun writePayload(habboResponse: HabboResponse) {
        habboResponse.writeUTF(value)
        habboResponse.writeInt(limitedNumber)
        habboResponse.writeInt(limitedTotal)
    }
}