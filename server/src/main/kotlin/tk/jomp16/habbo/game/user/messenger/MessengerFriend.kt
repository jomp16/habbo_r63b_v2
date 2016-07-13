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

package tk.jomp16.habbo.game.user.messenger

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.IHabboResponseSerialize
import tk.jomp16.habbo.database.information.UserInformationDao
import tk.jomp16.habbo.database.information.UserStatsDao
import tk.jomp16.habbo.game.user.HabboSession
import java.time.format.DateTimeFormatter

data class MessengerFriend(
        val userId: Int
                          ) : IHabboResponseSerialize {
    val habboSession: HabboSession?
        get() = HabboServer.habboSessionManager.getHabboSessionById(userId)

    val online: Boolean
        get() = habboSession != null // todo: add appear offline here

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            val userInformation = UserInformationDao.getUserInformationById(userId) ?: return

            writeInt(userInformation.id)
            writeUTF(userInformation.username)
            writeInt(if (online) 1 else 0)
            writeBoolean(online)
            writeBoolean(online && habboSession?.currentRoom != null)
            writeUTF(userInformation.figure)
            writeInt(0) // todo: add ability to add friend on custom category
            writeUTF(userInformation.motto)
            writeUTF(if (online) "" else UserStatsDao.getUserStats(userId).lastOnline.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
            writeUTF(userInformation.realname)
            writeBoolean(true) // allows offline messaging
            writeBoolean(false) // ?
            writeBoolean(false) // uses phone
            writeShort(0) // todo relationship type
        }
    }

    fun serializeHabboResponseSearch(habboResponse: HabboResponse) {
        habboResponse.apply {
            val userInformation = UserInformationDao.getUserInformationById(userId) ?: return

            writeInt(userInformation.id)
            writeUTF(userInformation.username)
            writeUTF(userInformation.motto)
            writeBoolean(online)
            writeBoolean(false)
            writeUTF("")
            writeInt(0) // ?
            writeUTF(userInformation.figure)
            writeUTF(if (online) "" else UserStatsDao.getUserStats(userId).lastOnline.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
        }
    }
}