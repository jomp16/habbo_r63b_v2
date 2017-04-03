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

package tk.jomp16.habbo.communication.outgoing.user

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.information.UserInformation

@Suppress("unused", "UNUSED_PARAMETER")
class UserRelationshipsResponse {
    @Response(Outgoing.USER_RELATIONSHIPS)
    fun handle(habboResponse: HabboResponse, userId: Int, relationships: Collection<UserInformation>, loves: Int, likes: Int, hates: Int) {
        // todo: relationships

        habboResponse.apply {
            writeInt(userId)
            writeInt(relationships.size)

            relationships.forEach {
                writeInt(1) // todo: relationship type
                writeInt(loves) // todo: relationship type
                writeInt(it.id)
                writeUTF(it.username)
                writeUTF(it.figure)
            }
        }
    }
}