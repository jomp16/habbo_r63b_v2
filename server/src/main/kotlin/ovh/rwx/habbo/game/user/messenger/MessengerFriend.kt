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

package ovh.rwx.habbo.game.user.messenger

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.database.user.UserStatsDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.information.UserInformation

data class MessengerFriend(val id: Int, val userId: Int, var relationship: MessengerRelationship = MessengerRelationship.NONE) : IHabboResponseSerialize {
    val habboSession: HabboSession?
        get() = HabboServer.habboSessionManager.getHabboSessionById(userId)
    val online: Boolean
        get() = userId < 0 || userId == UserInformationDao.serverConsoleUserInformation.id || habboSession != null // todo: add appear offline here
    val userInformation: UserInformation?
        get() = UserInformationDao.getUserInformationById(userId)

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(userId)

            if (userId > 0) {
                userInformation?.let {
                    writeUTF(it.username)
                    writeInt(1)
                    writeBoolean(online)
                    writeBoolean(online && habboSession?.currentRoom != null)
                    writeUTF(it.figure)
                    writeInt(0) // todo: add ability to add friend on custom category
                    writeUTF(it.motto)
                    writeUTF(if (online) "" else UserStatsDao.getUserStats(userId).lastOnline.format(HabboServer.DATE_TIME_FORMATTER_WITH_HOURS))
                    writeUTF(it.realname)
                    writeBoolean(true) // allows offline messaging
                    writeBoolean(false) // useless
                    writeBoolean(false) // uses phone
                    writeShort(relationship.type) // relationship type
                }
            } else {
                // todo: group, this is a stub
                writeUTF("GRUPO TESTE #1")
                writeInt(1)
                writeBoolean(true)
                writeBoolean(false)
                writeUTF("b0513s48104")
                writeInt(1)
                writeUTF("")
                writeUTF("")
                writeUTF("")
                writeBoolean(true) // do not allow offline messaging
                writeBoolean(false) // useless
                writeBoolean(false) // uses phone
                writeShort(0)
            }
        }
    }

    fun serializeHabboResponseSearch(habboResponse: HabboResponse) {
        habboResponse.apply {
            userInformation?.let {
                writeInt(it.id)
                writeUTF(it.username)
                writeUTF(it.motto)
                writeBoolean(online)
                writeBoolean(false)
                writeUTF("")
                writeInt(0) // ?
                writeUTF(it.figure)
                writeUTF(if (online) "" else UserStatsDao.getUserStats(userId).lastOnline.format(HabboServer.DATE_TIME_FORMATTER_WITH_HOURS))
            }
        }
    }
}