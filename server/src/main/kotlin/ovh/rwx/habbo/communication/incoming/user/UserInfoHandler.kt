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

package ovh.rwx.habbo.communication.incoming.user

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class UserInfoHandler {
    @Handler(Incoming.INFO_RETRIEVE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        habboSession.sendHabboResponse(Outgoing.USER_OBJECT,
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

        habboSession.sendHabboResponse(Outgoing.USER_PERKS,
                arrayOf(
                        Triple("CITIZEN", "", true),
                        Triple("CALL_ON_HELPERS", "", true),
                        Triple("NAVIGATOR_PHASE_TWO_2014", "", true),
                        Triple("USE_GUIDE_TOOL", "", true),
                        Triple("BUILDER_AT_WORK", "", false),
                        Triple("NAVIGATOR_ROOM_THUMBNAIL_CAMERA", "", true),
                        Triple("TRADE", "", true),
                        Triple("HABBO_CLUB_OFFER_BETA", "", true),
                        Triple("JUDGE_CHAT_REVIEWS", "", true),
                        Triple("MOUSE_ZOOM", "", true),
                        Triple("VOTE_IN_COMPETITIONS", "", true),
                        Triple("CAMERA", "", true)
                )
        )
    }
}