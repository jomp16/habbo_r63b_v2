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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class UserChangeFigureHandler {
    @Handler(Incoming.USER_CHANGE_FIGURE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val gender = habboRequest.readUTF().toUpperCase()
        val figure = habboRequest.readUTF()

        if (figure == habboSession.userInformation.figure) return

        if (!HabboServer.habboGame.antiMutantManager.isValidFigureSet(figure, gender, habboSession.habboSubscription.validUserSubscription)) {
            habboSession.sendNotification("Trying to script it eh?")

            return
        }

        habboSession.userInformation.figure = figure
        habboSession.userInformation.gender = gender

        habboSession.sendHabboResponse(Outgoing.USER_UPDATE_FIGURE, figure, gender)

        habboSession.currentRoom?.sendHabboResponse(Outgoing.USER_UPDATE, habboSession.roomUser!!.virtualID, figure, gender, habboSession.userInformation.motto, habboSession.userStats.achievementScore)
    }
}