/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.incoming.room

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomFloorPlanSaveHandler {
    @Handler(Incoming.FLOOR_PLAN_SAVE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null || !habboSession.currentRoom!!.hasRights(habboSession, true)) return

        val heightmap = habboRequest.readUTF()
        val doorX = habboRequest.readInt()
        val doorY = habboRequest.readInt()
        val doorDir = habboRequest.readInt()
        val wallThickness = habboRequest.readInt()
        val floorThickness = habboRequest.readInt()
        val wallHeight = habboRequest.readInt()

        if (heightmap.isEmpty() || heightmap.length < 2) return
        if (wallThickness < -2 || wallThickness > 1) {
            habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.FLOOR_PLAN_EDITOR_ERROR, "errors", "(%%%general%%%): %%%invalid_wall_thickness%%%")
            return
        }
        if (floorThickness < -2 || floorThickness > 1) {
            habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.FLOOR_PLAN_EDITOR_ERROR, "errors", "(%%%general%%%): %%%invalid_floor_thickness%%%")
            return
        }
        if (wallHeight < -1 || wallHeight > 15) {
            habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.FLOOR_PLAN_EDITOR_ERROR, "errors", "(%%%general%%%): %%%invalid_walls_fixed_height%%%")
            return
        }

        // TODO: FINISH IMPLEMENTATION
    }
}