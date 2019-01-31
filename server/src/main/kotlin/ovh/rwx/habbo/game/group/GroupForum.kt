/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.group

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize

class GroupForum(private val group: Group) : IHabboResponseSerialize {
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(group.groupData.id)
            writeUTF(group.groupData.name)
            writeUTF(group.groupData.description)
            writeUTF(group.groupData.badge)
            writeInt(0) // todo: total threads
            writeInt(0) // todo: leaderboard score
            writeInt(0) // TODO: keep a count of all messages (threads+replies)
            writeInt(0) //unread messages
            writeInt(0) //last message id
            writeInt(0) //last message author id
            writeUTF("") //last message author name
            writeInt(0) //last message time
        }
    }
}
