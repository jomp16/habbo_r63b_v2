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

package tk.jomp16.habbo.communication.incoming.moderation

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")
class ModerationCallForHelpHandler {
    @Handler(Incoming.MODERATION_CALL_FOR_HELP)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        // [716 ][null] -- [0]%olá mundo! aeeeeeeeeeeeeeeeeeeeeeeee[0][0][0][3]����[0][0][0][1][0][0][0][0]
        // [716 ][null] -- [0]3eu sou jomp16![13][13]mas tem alguém dizendo ser jomp16![0][0][0][17]����[0][0][0][1][0][0][0][0]
        val message = habboRequest.readUTF().trim()
        val topicId = habboRequest.readInt()
        val reportedUserId = habboRequest.readInt()
        val roomId = habboRequest.readInt()
        val chatlogs = habboRequest.readInt() // ???

        // structure users envolved in the help:
        // int - user id
        // string - chat -- ???
    }
}