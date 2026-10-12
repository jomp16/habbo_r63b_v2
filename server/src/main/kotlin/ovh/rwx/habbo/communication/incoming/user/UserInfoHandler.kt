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

package ovh.rwx.habbo.communication.incoming.user

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.user.UserObjectData
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class UserInfoHandler {
    @Handler(Incoming.INFO_RETRIEVE)
    @HandlerR63A(IncomingR63A.USER_INFO_RETRIEVE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboSession.sendResponse(
            Outgoing.USER_OBJECT,
            OutgoingR63A.USER_OBJECT,
            UserObjectData(
                userInformation = habboSession.userInformation,
                userStats = habboSession.userStats,
                userPreferences = habboSession.userPreferences,
                // todo can change name
                canChangeName = false,
            ),
        )

        // USER_PERKS nasce 2012-05-03 (RELEASE63-201205032302); ordem antiga dos campos até 2013-06-03
        if (habboSession.isVersionAtLeast(2012, 5, 3)) {
            habboSession.sendHabboResponse(
                Outgoing.USER_PERKS,
                arrayOf(
                    Triple("CITIZEN", "", true),
                    // Sem SAFE_CHAT o cliente (RoomChatInputView) exibe o safety_quiz_overlay e bloqueia o chat;
                    // FULL_CHAT evita o citizenship_overlay e o filtro de palavras do safe chat
                    Triple("SAFE_CHAT", "", true),
                    Triple("FULL_CHAT", "", true),
                    Triple("CALL_ON_HELPERS", "", true),
                    Triple("NAVIGATOR_PHASE_ONE_2014", "", true),
                    Triple("NAVIGATOR_PHASE_TWO_2014", "", true),
                    Triple("USE_GUIDE_TOOL", "", true),
                    Triple("GIVE_GUIDE_TOURS", "", true),
                    // Perks beta/experimental ficam desativados (comportamento histórico do hotel)
                    Triple("HEIGHTMAP_EDITOR_BETA", "", false),
                    Triple("EXPERIMENTAL_CHAT_BETA", "", false),
                    Triple("EXPERIMENTAL_TOOLBAR", "", false),
                    Triple("NEW_UI", "", false),
                    Triple("BUILDER_AT_WORK", "", false),
                    Triple("NAVIGATOR_ROOM_THUMBNAIL_CAMERA", "", true),
                    Triple("TRADE", "", true),
                    Triple("HABBO_CLUB_OFFER_BETA", "", true),
                    Triple("JUDGE_CHAT_REVIEWS", "", true),
                    Triple("MOUSE_ZOOM", "", true),
                    Triple("VOTE_IN_COMPETITIONS", "", true),
                    Triple("CAMERA", "", true),
                )
            )
        }
    }
}