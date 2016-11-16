/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.game.user.information

class UserPreferences(
        var id: Int,
        var volume: String,
        var preferOldChat: Boolean,
        var ignoreRoomInvite: Boolean,
        var disableCameraFollow: Boolean,
        var navigatorX: Int,
        var navigatorY: Int,
        var navigatorWidth: Int,
        var navigatorHeight: Int,
        var hideInRoom: Boolean,
        var blockNewFriends: Boolean,
        var chatColor: Int,
        var friendBarOpen: Boolean
)