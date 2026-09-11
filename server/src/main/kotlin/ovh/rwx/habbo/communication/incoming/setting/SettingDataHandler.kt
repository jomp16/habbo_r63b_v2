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

package ovh.rwx.habbo.communication.incoming.setting

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.setting.UserSettingsData
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class SettingDataHandler {
    @Handler(Incoming.USER_SETTINGS)
    @HandlerR63A(IncomingR63A.USER_SETTINGS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val tmp = habboSession.userPreferences.volume.split(',').map(String::toInt)

        habboSession.sendResponse(
            Outgoing.USER_SETTINGS,
            OutgoingR63A.USER_SETTINGS,
            UserSettingsData(
                systemVolume = tmp[0],
                furniVolume = tmp.getOrElse(1) { 100 },
                musicVolume = tmp.getOrElse(2) { 100 },
                preferOldChat = habboSession.userPreferences.preferOldChat,
                ignoreRoomInvite = habboSession.userPreferences.ignoreRoomInvite,
                disableCameraFollow = habboSession.userPreferences.disableCameraFollow,
                friendBarOpen = habboSession.userPreferences.friendBarOpen,
                chatColor = habboSession.userPreferences.chatColor
            )
        )
    }
}