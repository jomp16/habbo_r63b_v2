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

package ovh.rwx.habbo.game.item.wired.effect.effects

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUserEffect
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_TELEPORT_TO_ROOM)
class WiredEffectTeleportToRoom(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var targetRoomId: Int = 0
    private var targetX: Int = 0
    private var targetY: Int = 0

    init {
        setData()
    }

    override fun code() = WiredEffectType.TELEPORT_TO_ROOM.code
    override val requiresItems = false

    override fun setData() {
        roomItem.wiredData?.let {
            targetRoomId = it.options.getOrElse(0) { 0 }
            targetX = it.options.getOrElse(1) { 0 }
            targetY = it.options.getOrElse(2) { 0 }
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val targets = wiredContext.getEffectiveUsers(this)

        targets.forEach { user ->
            if (user.habboSession == null) return@forEach

            val targetRoom = HabboServer.habboGame.roomManager.rooms[targetRoomId]
            if (targetRoom == null) return@forEach

            val targetPos = Vector2(targetX, targetY)

            if (!targetRoom.roomGamemap.isBlocked(targetPos, ignoreUsers = true)) {
                val z = targetRoom.roomGamemap.getAbsoluteHeight(targetPos.x, targetPos.y)
                val position = Vector3(targetPos, z)

                user.stopWalking()

                targetRoom.roomGamemap.updateRoomUserMovement(
                    user,
                    user.currentVector3.vector2,
                    targetPos
                )

                user.effect = RoomUserEffect(4, 5)
                user.headRotation = 0
                user.bodyRotation = 0
                user.currentVector3 = position

                targetRoom.userManager.users[user.virtualID] = user

                // todo
                /*user.habboSession.sendHabboResponse(
                    Outgoing.ROOM_ENTRY_INFO,
                    targetRoom.roomData.id,
                    targetRoom.roomData.name,
                    targetRoom.roomData.description,
                    targetRoom.roomData.ownerName,
                    targetRoom.roomData.modelName,
                    targetRoom.roomData.state.code,
                    targetRoom.roomData.usersNow,
                    targetRoom.roomData.usersMax,
                    targetRoom.roomData.tags,
                    targetRoom.roomData.categoryId,
                    targetRoom.roomData.tradeSettings,
                    targetRoom.roomData.score,
                    targetRoom.roomData.allowPets,
                    targetRoom.roomData.allowPetsEating,
                    targetRoom.roomData.allowWalkThru,
                    0,
                    0,
                    0
                )*/
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, 0, 0), "")
        }
    }
}
