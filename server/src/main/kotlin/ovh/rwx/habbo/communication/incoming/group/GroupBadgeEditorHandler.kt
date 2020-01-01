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

package ovh.rwx.habbo.communication.incoming.group

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class GroupBadgeEditorHandler {
    @Handler(Incoming.GROUP_BADGE_EDITOR)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboSession.sendHabboResponse(Outgoing.GROUP_BADGE_EDITOR,
                HabboServer.habboGame.groupManager.groupBadgesBases,
                HabboServer.habboGame.groupManager.groupBadgesSymbols,
                HabboServer.habboGame.groupManager.groupBaseColors,
                HabboServer.habboGame.groupManager.groupBadgeSymbolColors,
                HabboServer.habboGame.groupManager.groupBadgeBackgroundColors)
    }
}