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

package ovh.rwx.habbo.communication.incoming.room

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.stuff.PostItData
import ovh.rwx.habbo.game.user.HabboSession
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class RoomSavePostItHandler {
    @Handler(Incoming.ROOM_SAVE_POST_IT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null || !habboSession.currentRoom!!.userManager.hasRights(habboSession)) return
        val itemId = habboRequest.readInt()
        val roomItem = habboSession.currentRoom!!.itemManager.items[itemId] ?: return

        if (roomItem.furnishing.interactionType != InteractionType.POST_IT) return
        val color = habboRequest.readUTF().trim().uppercase(Locale.getDefault())
        val text = habboRequest.readUTF().trim()

        if (color != "FFFF33" && color != "FF9CFF" && color != "9CCEFF" && color != "9CFF9C" || text.length > Byte.MAX_VALUE) return
        val extraData = "$color $text"

        if (roomItem.extraData == extraData) return

        // Só o primeiro texto do recado conta (editar um recado já escrito não pontua)
        val firstText = text.isNotEmpty() && PostItData.parse(roomItem.extraData).text.isBlank()

        roomItem.extraData = extraData
        roomItem.update(updateDb = true, updateClient = true)

        val ownerId = habboSession.currentRoom!!.roomData.ownerId
        if (firstText && ownerId != habboSession.userInformation.id) {
            val writerId = habboSession.userInformation.id
            // O dono pode estar offline: o progresso vai ao banco, fora da thread de rede
            HabboServer.applicationScope.launch(Dispatchers.IO) {
                val achievements = HabboServer.habboGame.achievementManager
                achievements.progress(habboSession, "ACH_NotesLeft", 1, accumulate = true)
                achievements.progress(
                    HabboServer.habboSessionManager.getHabboSessionById(ownerId),
                    ownerId,
                    "ACH_NotesReceived",
                    1,
                    accumulate = true
                )
            }
        }
    }
}