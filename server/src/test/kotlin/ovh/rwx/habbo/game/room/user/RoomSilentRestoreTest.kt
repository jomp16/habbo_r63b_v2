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

package ovh.rwx.habbo.game.room.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomData
import ovh.rwx.habbo.game.room.RoomState
import ovh.rwx.habbo.game.room.RoomType
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.util.Vector3

class RoomSilentRestoreTest {
    private class TestHumanoid(
        room: Room,
        virtualID: Int,
        pos: Vector3
    ) : RoomHumanoid(room, virtualID, pos, 2, 2) {
        override fun serializeHabboResponse(habboResponse: ovh.rwx.habbo.communication.HabboResponse, vararg params: Any) {}
    }

    @Test
    fun `restoreSilentState sets all fields without broadcasting and resets suppressBroadcast`() {
        val roomData = RoomData(
            id = 1,
            roomType = RoomType.PRIVATE,
            name = "Test",
            ownerId = 1,
            description = "",
            category = 0,
            state = RoomState.OPEN,
            tradeState = 0,
            usersMax = 25,
            modelName = "model_a",
            score = 0,
            tags = emptyList(),
            password = "",
            wallpaper = "0.0",
            floor = "0.0",
            landscape = "0.0",
            hideWall = false,
            wallThick = 0,
            wallHeight = -1,
            floorThick = 0,
            muteSettings = 0,
            banSettings = 0,
            kickSettings = 0,
            chatType = 0,
            chatBalloon = 0,
            chatSpeed = 0,
            chatMaxDistance = 50,
            chatFloodProtection = 0,
            allowPets = false,
            allowPetsEat = false,
            allowWalkThrough = true,
            groupId = 0,
            virtual = true
        )
        val model = RoomModel("model_a", 1, Vector3(0, 0, 0.0), 2, listOf("xx", "00"), false)
        val room = Room(roomData, model)
        val humanoid = TestHumanoid(room, virtualID = 5, pos = Vector3(1, 1, 0.0))

        humanoid.restoreSilentState(
            idle = true,
            danceId = 2,
            handItem = 7,
            effectId = 15,
            statuses = mapOf("sit" to "1.0")
        )

        assertFalse(humanoid.suppressBroadcast)
        assertFalse(humanoid.pendingJoin)
        assertFalse(humanoid.updateNeeded)
        assertTrue(humanoid.idle)
        assertEquals(2, humanoid.danceId)
        assertEquals(7, humanoid.handItem)
        assertEquals(15, humanoid.effect?.effectId)
        assertEquals("1.0", humanoid.statusMap["sit"]?.second)

        humanoid.restoreSilentState(
            idle = false,
            danceId = 0,
            handItem = 0,
            effectId = 0,
            statuses = mapOf("mv" to "2,3,0.0", "sit" to "1.0")
        )
        assertFalse(humanoid.statusMap.containsKey("mv"))
        assertEquals("1.0", humanoid.statusMap["sit"]?.second)
        assertTrue(humanoid.updateNeeded)
    }
}
