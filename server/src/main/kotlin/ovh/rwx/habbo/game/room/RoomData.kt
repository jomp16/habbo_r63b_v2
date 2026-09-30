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

package ovh.rwx.habbo.game.room

import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.database.writebehind.AbstractDirtyEntity

class RoomData(
    val id: Int,
    val roomType: RoomType,
    name: String,
    val ownerId: Int,
    description: String,
    category: Int,
    state: RoomState,
    tradeState: Int,
    usersMax: Int,
    modelName: String,
    score: Int,
    tags: List<String>,
    password: String,
    wallpaper: String,
    floor: String,
    landscape: String,
    hideWall: Boolean,
    wallThick: Int,
    wallHeight: Int,
    floorThick: Int,
    muteSettings: Int,
    banSettings: Int,
    kickSettings: Int,
    chatType: Int,
    chatBalloon: Int,
    chatSpeed: Int,
    chatMaxDistance: Int,
    chatFloodProtection: Int,
    allowPets: Boolean,
    allowPetsEat: Boolean,
    allowWalkThrough: Boolean,
    groupId: Int,
    allowNavigatorDynamicCats: Boolean = true,
    leaveOnDoorTileEnabled: Boolean = true,
    idleSleepEnabled: Boolean = true,
    idleSleepTimeoutSeconds: Int = 1200,
    idleAutokickEnabled: Boolean = false,
    idleAutokickTimeoutSeconds: Int = 1800,
    muteAllPets: Boolean = false
) : AbstractDirtyEntity() {
    var name: String = name; set(v) { if (field != v) { field = v; markDirty() } }
    var description: String = description; set(v) { if (field != v) { field = v; markDirty() } }
    var category: Int = category; set(v) { if (field != v) { field = v; markDirty() } }
    var state: RoomState = state; set(v) { if (field != v) { field = v; markDirty() } }
    var tradeState: Int = tradeState; set(v) { if (field != v) { field = v; markDirty() } }
    var usersMax: Int = usersMax; set(v) { if (field != v) { field = v; markDirty() } }
    var modelName: String = modelName; set(v) { if (field != v) { field = v; markDirty() } }
    var score: Int = score; set(v) { if (field != v) { field = v; markDirty() } }
    var tags: List<String> = tags; set(v) { if (field != v) { field = v; markDirty() } }
    var password: String = password; set(v) { if (field != v) { field = v; markDirty() } }
    var wallpaper: String = wallpaper; set(v) { if (field != v) { field = v; markDirty() } }
    var floor: String = floor; set(v) { if (field != v) { field = v; markDirty() } }
    var landscape: String = landscape; set(v) { if (field != v) { field = v; markDirty() } }
    var hideWall: Boolean = hideWall; set(v) { if (field != v) { field = v; markDirty() } }
    var wallThick: Int = wallThick; set(v) { if (field != v) { field = v; markDirty() } }
    var wallHeight: Int = wallHeight; set(v) { if (field != v) { field = v; markDirty() } }
    var floorThick: Int = floorThick; set(v) { if (field != v) { field = v; markDirty() } }
    var muteSettings: Int = muteSettings; set(v) { if (field != v) { field = v; markDirty() } }
    var banSettings: Int = banSettings; set(v) { if (field != v) { field = v; markDirty() } }
    var kickSettings: Int = kickSettings; set(v) { if (field != v) { field = v; markDirty() } }
    var chatType: Int = chatType; set(v) { if (field != v) { field = v; markDirty() } }
    var chatBalloon: Int = chatBalloon; set(v) { if (field != v) { field = v; markDirty() } }
    var chatSpeed: Int = chatSpeed; set(v) { if (field != v) { field = v; markDirty() } }
    var chatMaxDistance: Int = chatMaxDistance; set(v) { if (field != v) { field = v; markDirty() } }
    var chatFloodProtection: Int = chatFloodProtection; set(v) { if (field != v) { field = v; markDirty() } }
    var allowPets: Boolean = allowPets; set(v) { if (field != v) { field = v; markDirty() } }
    var allowPetsEat: Boolean = allowPetsEat; set(v) { if (field != v) { field = v; markDirty() } }
    var allowWalkThrough: Boolean = allowWalkThrough; set(v) { if (field != v) { field = v; markDirty() } }
    var groupId: Int = groupId; set(v) { if (field != v) { field = v; markDirty() } }
    var allowNavigatorDynamicCats: Boolean = allowNavigatorDynamicCats; set(v) { if (field != v) { field = v; markDirty() } }
    var leaveOnDoorTileEnabled: Boolean = leaveOnDoorTileEnabled; set(v) { if (field != v) { field = v; markDirty() } }
    var idleSleepEnabled: Boolean = idleSleepEnabled; set(v) { if (field != v) { field = v; markDirty() } }
    var idleSleepTimeoutSeconds: Int = idleSleepTimeoutSeconds; set(v) { if (field != v) { field = v; markDirty() } }
    var idleAutokickEnabled: Boolean = idleAutokickEnabled; set(v) { if (field != v) { field = v; markDirty() } }
    var idleAutokickTimeoutSeconds: Int = idleAutokickTimeoutSeconds; set(v) { if (field != v) { field = v; markDirty() } }
    var muteAllPets: Boolean = muteAllPets; set(v) { if (field != v) { field = v; markDirty() } }

    val ownerName: String by lazy { UserInformationDao.getUserInformationById(ownerId)?.username ?: "null" }

    override fun flush() {
        ovh.rwx.habbo.database.room.RoomDao.updateRoomData(this)
        markClean()
    }

    companion object {
        fun createPrivate(
            id: Int,
            userId: Int,
            name: String,
            description: String,
            model: String,
            category: Int,
            maxUsers: Int,
            tradeSettings: Int
        ): RoomData = RoomData(
            id = id,
            roomType = RoomType.PRIVATE,
            name = name,
            ownerId = userId,
            description = description,
            category = category,
            state = RoomState.OPEN,
            tradeState = tradeSettings,
            usersMax = maxUsers,
            modelName = model,
            score = 0,
            tags = emptyList(),
            password = "",
            wallpaper = "0.0",
            floor = "0.0",
            landscape = "0.0",
            hideWall = false,
            wallThick = 0,
            wallHeight = 0,
            floorThick = 0,
            muteSettings = 1,
            banSettings = 1,
            kickSettings = 1,
            chatType = 0,
            chatBalloon = 0,
            chatSpeed = 1,
            chatMaxDistance = 50,
            chatFloodProtection = 2,
            allowPets = true,
            allowPetsEat = false,
            allowWalkThrough = false,
            groupId = 0,
            allowNavigatorDynamicCats = true,
            leaveOnDoorTileEnabled = true,
            idleSleepEnabled = true,
            idleSleepTimeoutSeconds = 1200,
            idleAutokickEnabled = false,
            idleAutokickTimeoutSeconds = 1800,
            muteAllPets = false
        )
    }
}