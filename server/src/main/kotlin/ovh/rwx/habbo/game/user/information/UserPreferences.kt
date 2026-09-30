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

package ovh.rwx.habbo.game.user.information

import ovh.rwx.habbo.database.writebehind.AbstractDirtyEntity

class UserPreferences(
    var id: Int,
    volume: String,
    preferOldChat: Boolean,
    ignoreRoomInvite: Boolean,
    disableCameraFollow: Boolean,
    navigatorX: Int,
    navigatorY: Int,
    navigatorWidth: Int,
    navigatorHeight: Int,
    hideInRoom: Boolean,
    blockNewFriends: Boolean,
    chatColor: Int,
    friendBarOpen: Boolean,
    friendStreamEnabled: Boolean,
) : AbstractDirtyEntity() {
    var volume: String = volume; set(v) { if (field != v) { field = v; markDirty() } }
    var preferOldChat: Boolean = preferOldChat; set(v) { if (field != v) { field = v; markDirty() } }
    var ignoreRoomInvite: Boolean = ignoreRoomInvite; set(v) { if (field != v) { field = v; markDirty() } }
    var disableCameraFollow: Boolean = disableCameraFollow; set(v) { if (field != v) { field = v; markDirty() } }
    var navigatorX: Int = navigatorX; set(v) { if (field != v) { field = v; markDirty() } }
    var navigatorY: Int = navigatorY; set(v) { if (field != v) { field = v; markDirty() } }
    var navigatorWidth: Int = navigatorWidth; set(v) { if (field != v) { field = v; markDirty() } }
    var navigatorHeight: Int = navigatorHeight; set(v) { if (field != v) { field = v; markDirty() } }
    var hideInRoom: Boolean = hideInRoom; set(v) { if (field != v) { field = v; markDirty() } }
    var blockNewFriends: Boolean = blockNewFriends; set(v) { if (field != v) { field = v; markDirty() } }
    var chatColor: Int = chatColor; set(v) { if (field != v) { field = v; markDirty() } }
    var friendBarOpen: Boolean = friendBarOpen; set(v) { if (field != v) { field = v; markDirty() } }
    var friendStreamEnabled: Boolean = friendStreamEnabled; set(v) { if (field != v) { field = v; markDirty() } }

    override fun flush() {
        ovh.rwx.habbo.database.user.UserPreferencesDao.savePreferences(this)
        markClean()
    }
}