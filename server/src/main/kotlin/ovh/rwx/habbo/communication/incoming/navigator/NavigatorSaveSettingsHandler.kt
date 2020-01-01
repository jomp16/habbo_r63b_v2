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

package ovh.rwx.habbo.communication.incoming.navigator

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorSaveSettingsHandler {
    @Handler(Incoming.NAVIGATOR_SAVE_SETTINGS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val x = habboRequest.readInt()
        val y = habboRequest.readInt()
        val width = habboRequest.readInt()
        val height = habboRequest.readInt()

        habboSession.userPreferences.navigatorX = x
        habboSession.userPreferences.navigatorY = y
        habboSession.userPreferences.navigatorWidth = width
        habboSession.userPreferences.navigatorHeight = height
    }
}