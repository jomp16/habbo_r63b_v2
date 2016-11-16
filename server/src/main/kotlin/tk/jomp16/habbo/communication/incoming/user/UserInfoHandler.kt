/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.communication.incoming.user

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.QueuedHabboResponse
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class UserInfoHandler {
    @Handler(Incoming.INFO_RETRIEVE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        val queuedHabboResponse = QueuedHabboResponse()

        queuedHabboResponse += Outgoing.USER_OBJECT to arrayOf(
                habboSession.userInformation.id,
                habboSession.userInformation.username,
                habboSession.userInformation.figure,
                habboSession.userInformation.gender,
                habboSession.userInformation.motto,
                habboSession.userStats.respect,
                habboSession.userStats.dailyRespectPoints,
                habboSession.userStats.dailyPetRespectPoints,
                habboSession.userStats.lastOnline,
                false // todo can change name
        )

        queuedHabboResponse += Outgoing.USER_PERKS to arrayOf(arrayOf(
                Triple("USE_GUIDE_TOOL", "", true),
                Triple("JUDGE_CHAT_REVIEWS", "", true),
                Triple("CITIZEN", "", true),
                Triple("TRADE", "", true),
                Triple("BUILDER_AT_WORK", "", false),
                Triple("NAVIGATOR_PHASE_ONE_2014", "", false),
                Triple("CAMERA", "", false),
                Triple("NAVIGATOR_PHASE_TWO_2014", "", true),
                Triple("MOUSE_ZOOM", "", true),
                Triple("NAVIGATOR_ROOM_THUMBNAIL_CAMERA", "", false)
        ))

        habboSession.sendQueuedHabboResponse(queuedHabboResponse)
    }
}