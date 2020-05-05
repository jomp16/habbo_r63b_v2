/*
 * Copyright (C) 2015-2020 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.incoming.guide

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class GuideOpenToolHandler {
    @Handler(Incoming.GUIDE_TOOL_OPEN)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        // todo: code Guide Tools this
        val duty = habboRequest.readBoolean()
        val tourRequests = habboRequest.readBoolean() // if you want to receive tour requests
        val helperRequests = habboRequest.readBoolean() // if you want to receive helper requests
        val bullyReports = habboRequest.readBoolean() // if you want to receive bully requests

        // todo: remove hardcoded value
        val guidesOnDuty = 0
        val helpersOnDuty = 0
        val guardiansOnDuty = 0

        habboSession.sendHabboResponse(Outgoing.GUIDE_TOOLS, duty, guidesOnDuty, helpersOnDuty, guardiansOnDuty)
    }
}