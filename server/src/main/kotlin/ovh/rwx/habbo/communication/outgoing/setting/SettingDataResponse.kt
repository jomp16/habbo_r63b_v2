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

package ovh.rwx.habbo.communication.outgoing.setting

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

data class UserSettingsData(
    val systemVolume: Int,
    val furniVolume: Int = 100,
    val musicVolume: Int = 100,
    val preferOldChat: Boolean = false,
    val ignoreRoomInvite: Boolean = false,
    val disableCameraFollow: Boolean = false,
    val friendBarOpen: Boolean = false,
    val chatColor: Int = 0,
    val unknownR63A: Boolean = false
)

@Suppress("unused", "UNUSED_PARAMETER")
class SettingDataResponse {
    @Response(Outgoing.USER_SETTINGS)
    @ResponseR63A(OutgoingR63A.USER_SETTINGS)
    fun response(habboResponse: HabboResponse, data: UserSettingsData) {
        habboResponse.apply {
            if (isVersionBefore(2011, 9, 20)) {
                writeInt(data.systemVolume)
                writeBoolean(data.unknownR63A)
            } else {
                writeInt(data.systemVolume)
                writeInt(data.furniVolume)
                writeInt(data.musicVolume)
                writeBoolean(data.preferOldChat)
                writeBoolean(data.ignoreRoomInvite)
                writeBoolean(data.disableCameraFollow)
                writeInt(if (data.friendBarOpen) 1 else 0)
                writeInt(data.chatColor)

                if (isVersionAtLeast(2024, 5, 24)) {
                    writeBoolean(false) // wired menu button
                    writeBoolean(false) // wired inspect button
                    writeBoolean(false) // play test mode
                    if (isVersionAtLeast(2024, 5, 30)) writeInt(0) // useless
                    if (isVersionAtLeast(2024, 12, 12)) writeBoolean(false) // wired whisper disabled
                    if (isVersionAtLeast(2025, 9, 11)) writeBoolean(false) // show all notifications
                    if (isVersionAtLeast(2026, 2, 9)) writeUTF("illumina") // wiredUiStyle
                } else {
                    writeInt(0)
                }
            }
        }
    }
}