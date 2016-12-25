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

package tk.jomp16.habbo.game.item.interactors

import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.ItemInteractor
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser
import java.util.*

class MannequinFurniInteractor : ItemInteractor() {
    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomUser, roomItem, hasRights, request)

        if (roomUser == null || roomUser.habboSession == null) return

        val mannequinDataArray = roomItem.extraData.split(7.toChar())
        val mannequinFigureArray = mannequinDataArray[1].split('.').toTypedArray()
        val userFigureArray = roomUser.habboSession.userInformation.figure.split('.')

        val figures = LinkedHashMap<String, String>()

        mannequinFigureArray.forEach { figure ->
            userFigureArray.forEach { figure1 ->
                val key = figure.split('-')[0]
                val key1 = figure1.split('-')[0]

                if (key1 == key) figures.put(key, figure)
                else if (!figures.containsKey(key1)) figures.put(key1, figure1)
            }
        }

        val builder = StringBuilder()

        figures.values.forEach { s -> builder.append(s).append('.') }

        roomUser.habboSession.userInformation.figure = builder.toString().substring(0, builder.toString().lastIndexOf('.'))
        roomUser.habboSession.userInformation.gender = mannequinDataArray[0].toUpperCase()

        roomUser.habboSession.sendHabboResponse(Outgoing.USER_UPDATE_FIGURE, roomUser.habboSession.userInformation.figure, roomUser.habboSession.userInformation.gender)

        room.sendHabboResponse(Outgoing.USER_UPDATE, roomUser.virtualID, roomUser.habboSession.userInformation.figure, roomUser.habboSession.userInformation.gender, roomUser.habboSession.userInformation.motto, roomUser.habboSession.userStats.achievementScore)

        roomUser.habboSession.habboMessenger.notifyFriends()
    }
}
