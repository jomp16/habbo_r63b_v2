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

package ovh.rwx.habbo.game.room

import ovh.rwx.habbo.database.user.UserInformationDao

data class RoomData(
        val id: Int,
        val roomType: RoomType,
        var name: String,
        val ownerId: Int,
        var description: String,
        var category: Int,
        var state: RoomState,
        var tradeState: Int,
        var usersMax: Int,
        var modelName: String,
        var score: Int,
        var tags: List<String>,
        var password: String,
        var wallpaper: String,
        var floor: String,
        var landscape: String,
        var hideWall: Boolean,
        var wallThick: Int,
        var wallHeight: Int,
        var floorThick: Int,
        var muteSettings: Int,
        var banSettings: Int,
        var kickSettings: Int,
        var chatType: Int,
        var chatBalloon: Int,
        var chatSpeed: Int,
        var chatMaxDistance: Int,
        var chatFloodProtection: Int,
        var allowPets: Boolean,
        var allowPetsEat: Boolean,
        var allowWalkThrough: Boolean,
        var groupId: Int
) {
    val ownerName: String by lazy { UserInformationDao.getUserInformationById(ownerId)?.username ?: "null" }
}