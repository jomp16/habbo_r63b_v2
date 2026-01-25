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
import ovh.rwx.habbo.game.room.user.RoomUser

class RoomGameManager(private val room: Room) {
    private val games = mutableMapOf<String, RoomGame>()

    fun registerGame(gameType: String, game: RoomGame) {
        games[gameType] = game
    }

    fun onUserWalksOn(roomUser: RoomUser, roomItem: RoomItem) {
        getGameForItem(roomItem)?.onUserWalksOn(roomUser, roomItem)
    }

    fun onUserWalkOff(roomUser: RoomUser, roomItem: RoomItem) {
        getGameForItem(roomItem)?.onUserWalkOff(roomUser, roomItem)
    }

    fun handleInteraction(roomUser: RoomUser, roomItem: RoomItem, state: Int = 0) {
        getGameForItem(roomItem)?.handleInteraction(roomUser, roomItem, state)
    }

    private fun getGameForItem(roomItem: RoomItem): RoomGame? {
        return when {
            roomItem.furnishing.interactionType.name.startsWith("BATTLE_BANZAI") -> games["banzai"]
            roomItem.furnishing.interactionType.name.startsWith("FREEZE") -> games["freeze"]
            else -> null
        }
    }

    fun tick() {
        games.values.forEach { it.tick() }
    }
}
