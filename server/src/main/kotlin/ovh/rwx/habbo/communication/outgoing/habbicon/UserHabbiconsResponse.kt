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

package ovh.rwx.habbo.communication.outgoing.habbicon

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.habbicon.UserHabbicon

/**
 * Payload estruturado com a lista de Habbicons pertencentes ao usuário (USER_HABBICONS).
 *
 * @param userHabbicons Lista de instâncias [UserHabbicon] do usuário.
 * @param recentHabbiconIds Lista com os IDs de Habbicons usados recentemente.
 */
data class UserHabbiconsData(
    val userHabbicons: List<UserHabbicon>,
    val recentHabbiconIds: List<Int> = emptyList(),
)

@Suppress("unused", "UNUSED_PARAMETER")
class UserHabbiconsResponse {
    @Response(Outgoing.USER_HABBICONS)
    fun response(habboResponse: HabboResponse, data: UserHabbiconsData) {
        habboResponse.writeInt(data.userHabbicons.size)
        data.userHabbicons.forEach {
            habboResponse.writeInt(it.habbiconId)
            habboResponse.writeInt(it.state)
        }
        habboResponse.writeInt(data.recentHabbiconIds.size)
        data.recentHabbiconIds.forEach(habboResponse::writeInt)
    }
}
