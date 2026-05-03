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

package ovh.rwx.habbo.game.room.games

import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.games.banzai.BattleBanzaiGame
import ovh.rwx.habbo.game.room.user.RoomEntity

class RoomGameManager(val room: Room) {
    val games = mutableMapOf<RoomGameType, RoomGame>()

    fun getGame(gameType: RoomGameType) = games[gameType]

    fun registerGame(gameType: RoomGameType) {
        if (games.containsKey(gameType)) return

        val roomGame = when (gameType) {
            RoomGameType.BATTLE_BANZAI -> BattleBanzaiGame(room)
            else -> null
        }

        if (roomGame == null) return

        games[gameType] = roomGame
    }

    fun onUserWalksOn(roomEntity: RoomEntity, roomItem: RoomItem) {
        getGameForItem(roomItem)?.onUserWalksOn(roomEntity, roomItem)
    }

    fun onUserWalkOff(roomEntity: RoomEntity, roomItem: RoomItem) {
        getGameForItem(roomItem)?.onUserWalkOff(roomEntity, roomItem)
    }

    fun handleInteraction(roomEntity: RoomEntity, roomItem: RoomItem, state: Int = 0) {
        getGameForItem(roomItem)?.handleInteraction(roomEntity, roomItem, state)
    }

    fun onEntityLeaveRoom(roomEntity: RoomEntity) {
        games.values.forEach { it.onEntityLeaveRoom(roomEntity) }
    }

    fun getGameForItem(roomItem: RoomItem): RoomGame? {
        return when {
            roomItem.furnishing.interactionType.name.startsWith("BATTLE_BANZAI") -> games[RoomGameType.BATTLE_BANZAI]
            roomItem.furnishing.interactionType.name.startsWith("FREEZE") -> games[RoomGameType.FREEZE]
            else -> null
        }
    }

    fun tick() {
        games.values.forEach { it.tick() }
    }
}

enum class RoomGameType {
    BATTLE_BANZAI,
    FREEZE,
}
