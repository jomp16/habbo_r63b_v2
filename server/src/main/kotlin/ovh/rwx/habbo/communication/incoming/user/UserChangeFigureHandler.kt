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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.room.RoomUpdateUserData
import ovh.rwx.habbo.communication.outgoing.user.UserUpdateFigureData
import ovh.rwx.habbo.game.user.HabboSession
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class UserChangeFigureHandler {
    @Handler(Incoming.USER_CHANGE_FIGURE)
    @HandlerR63A(IncomingR63A.USER_CHANGE_FIGURE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val gender = habboRequest.readUTF().uppercase(Locale.getDefault())
        val figure = habboRequest.readUTF()

        if (figure == habboSession.userInformation.figure && gender == habboSession.userInformation.gender) return

        if (HabboServer.habboGame.figureManager.isValidFigure(figure, gender, habboSession)) {
            habboSession.userInformation.figure = figure
            habboSession.userInformation.gender = gender

            // ACH_AvatarLooks: mudar visual
            HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_AvatarLooks", 1, accumulate = true)
        }

        val currentFigure = habboSession.userInformation.figure
        val currentGender = habboSession.userInformation.gender

        if (habboSession.habboVersion.isVersionBefore(2015, 12, 1)) {
            habboSession.sendResponse(
                Outgoing.USER_UPDATE,
                OutgoingR63A.USER_UPDATE,
                RoomUpdateUserData(
                    virtualId = -1,
                    figure = currentFigure,
                    gender = currentGender,
                    motto = habboSession.userInformation.motto,
                    achievementScore = habboSession.userStats.achievementScore
                )
            )
        } else {
            habboSession.sendHabboResponse(
                Outgoing.USER_UPDATE_FIGURE,
                UserUpdateFigureData(
                    figure = currentFigure,
                    gender = currentGender,
                )
            )
        }

        habboSession.roomUser?.let { roomUser ->
            habboSession.currentRoom?.sendResponse(
                Outgoing.USER_UPDATE,
                OutgoingR63A.USER_UPDATE,
                RoomUpdateUserData(
                    virtualId = roomUser.virtualID,
                    figure = currentFigure,
                    gender = currentGender,
                    motto = habboSession.userInformation.motto,
                    achievementScore = habboSession.userStats.achievementScore
                )
            )
        }
    }
}