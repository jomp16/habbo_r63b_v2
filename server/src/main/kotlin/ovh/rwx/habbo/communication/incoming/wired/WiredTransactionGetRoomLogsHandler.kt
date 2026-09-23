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

package ovh.rwx.habbo.communication.incoming.wired

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.chest.ChestTransactionLogListData
import ovh.rwx.habbo.database.chest.ChestDao
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class WiredTransactionGetRoomLogsHandler {
    @Handler(Incoming.WIRED_TRANSACTION_GET_ROOM_LOGS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return

        val pageSize = habboRequest.readInt()
        val page = habboRequest.readInt()

        val (total, logs) = ChestDao.getRoomLogs(room.roomData.id, pageSize, page)

        habboSession.sendHabboResponse(
            Outgoing.CHEST_TRANSACTION_LOG_LIST,
            ChestTransactionLogListData(
                logListType = 1,
                logListId = room.roomData.id.toLong(),
                totalLogs = total,
                currentPage = page,
                pageSize = pageSize,
                logs = logs,
            ),
        )
    }
}
