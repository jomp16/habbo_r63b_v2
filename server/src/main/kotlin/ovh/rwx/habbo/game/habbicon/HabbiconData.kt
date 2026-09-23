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

package ovh.rwx.habbo.game.habbicon

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize

data class HabbiconCollection(
    val id: Int,
    val name: String,
    val enabled: Boolean,
    val priceCredits: Int,
    val priceActivityPoints: Int,
    val activityPointType: Int,
    val rewardHabbiconId: Int?,
    val rewardState: Int,
    val habbicons: MutableList<Habbicon> = mutableListOf()
)

data class Habbicon(
    val id: Int,
    val collectionId: Int,
    val name: String,
    val enabled: Boolean,
    val purchasable: Boolean,
    val priceCredits: Int,
    val priceActivityPoints: Int,
    val activityPointType: Int
) : IHabboResponseSerialize {
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        val state = params.firstOrNull() as? Int ?: 0
        habboResponse.apply {
            writeInt(id)
            writeUTF(name)
            writeInt(collectionId)
            writeInt(state)
            writeInt(priceCredits)
            writeInt(priceActivityPoints)
            writeInt(activityPointType)
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        serializeHabboResponse(habboResponse, *params)
    }
}

data class UserHabbicon(
    val id: Int,
    val userId: Int,
    val habbiconId: Int,
    val state: Int
) {
    companion object {
        const val STATE_LOCKED = 0
        const val STATE_CLAIMABLE = 1
        const val STATE_OWNED = 2
        const val STATE_FAVORITE = 3
    }
}
