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

package ovh.rwx.habbo.communication.outgoing.moderation

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.information.UserStats
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@Suppress("unused", "UNUSED_PARAMETER")
class ModerationUserInfoResponse {
    @Response(Outgoing.MODERATION_USER_INFO)
    fun response(habboResponse: HabboResponse, userInformation: UserInformation, userStats: UserStats) {
        habboResponse.apply {
            writeInt(userInformation.id)
            writeUTF(userInformation.username)
            writeUTF(userInformation.figure)
            writeInt(0) // todo: account created
            writeInt(TimeUnit.SECONDS.toMinutes(Math.ceil(Instant.now().epochSecond.toDouble() - userStats.lastOnline.atZone(ZoneId.systemDefault()).toEpochSecond().toDouble()).toLong()).toInt())
            writeBoolean(HabboServer.habboSessionManager.getHabboSessionById(userInformation.id) != null)
            writeInt(0) // todo: cfhs created
            writeInt(0) // todo: cfhs abusive
            writeInt(0) // todo: cautions
            writeInt(0) // todo: bans
            writeInt(0) // todo: trading locked
            writeUTF("") // todo: trading unban
            writeUTF("") // todo: last purchase
            writeInt(0)
            writeInt(0) // todo: bans by id, probably
            writeUTF(userInformation.email)
            writeUTF("") // todo: user classification
            // more data available:
            // string -- last sanction time
            // int -- unknown
        }
    }
}