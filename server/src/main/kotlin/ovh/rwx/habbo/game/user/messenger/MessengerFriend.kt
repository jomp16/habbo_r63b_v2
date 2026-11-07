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

package ovh.rwx.habbo.game.user.messenger

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
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
        val username: String
        val gender: Int
        val isOnline: Boolean
        val followingAllowed: Boolean
        val figure: String
        val categoryId: Int
        val motto: String
        val lastAccess: String
        val realName: String
        val relType: Int

        if (userId > 0) {
            val info = userInformation ?: return
            username = info.username
            gender = if (info.gender == "M") 0 else 1
            isOnline = online
            followingAllowed = isOnline && habboSession?.currentRoom != null
            figure = info.figure
            categoryId = 0 // todo: add ability to add friend on custom category
            motto = info.motto
            lastAccess = if (isOnline) "" else UserStatsDao.getLastOnline(userId).format(HabboServer.DATE_TIME_FORMATTER_WITH_HOURS)
            realName = info.realname
            relType = relationship.type
        } else {
            // Grupo / stub legado
            username = "GRUPO TESTE #1"
            gender = 1
            isOnline = true
            followingAllowed = false
            figure = "b0513s48104"
            categoryId = 1
            motto = ""
            lastAccess = ""
            realName = ""
            relType = 0
        }

        habboResponse.apply {
            writeInt(userId)
            writeUTF(username)
            writeInt(gender)
            writeBoolean(isOnline)
            writeBoolean(followingAllowed)
            writeUTF(figure)
            writeInt(categoryId)
            writeUTF(motto)

            // 1) lastAccess: exibido até 2012-02-21 (removido no BUILD 201202211117)
            if (isVersionBefore(2012, 2, 21)) {
                writeUTF(lastAccess)
            }

            // 2) realName: adicionado em 2009-11-13 (BUILD 200911131255)
            if (isVersionAtLeast(2009, 11, 13)) {
                writeUTF(realName)
            }

            // 3) facebookId: adicionado em 2011-05-09 (BUILD 201105092307)
            if (isVersionAtLeast(2011, 5, 9)) {
                writeUTF("") // facebookId
            }

            // 4) persistedMessageUser (allows offline messaging): adicionado em 2012-04-18 (BUILD 201204181558)
            if (isVersionAtLeast(2012, 4, 18)) {
                writeBoolean(true)
            }

            // 5) vipMember & pocketHabboUser: adicionados em 2012-11-05 (BUILD 201211051241)
            if (isVersionAtLeast(2012, 11, 5)) {
                writeBoolean(false) // vipMember
                writeBoolean(false) // pocketHabboUser
            }

            // 6) relationshipStatus: adicionado em 2013-02-04 (BUILD 201302042303)
            if (isVersionAtLeast(2013, 2, 4)) {
                writeShort(relType)
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        serializeHabboResponse(habboResponse, *params)
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