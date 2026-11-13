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

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.room.RoomIgnoreUserResultData
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.ignore.IgnoreResult
import ovh.rwx.habbo.game.user.ignore.IgnoreTargetReader

@Suppress("unused", "UNUSED_PARAMETER")
class RoomIgnoreUserHandler {
    @Handler(Incoming.ROOM_IGNORE_USER)
    @HandlerR63A(IncomingR63A.ROOM_IGNORE_USER)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val target = IgnoreTargetReader.read(habboSession, habboRequest, ignoring = true)
        val result = if (target.userId == 0) IgnoreResult.REFUSED
        else habboSession.habboIgnores.add(target.userId, target.username)

        // exatamente um resultado por pedido, na ordem (o cliente casa com a fila de pedidos pendentes)
        habboSession.sendResponse(
            Outgoing.ROOM_IGNORE_USER_RESULT,
            OutgoingR63A.ROOM_IGNORE_USER_RESULT,
            RoomIgnoreUserResultData(result.code, target.userId, target.username)
        )
    }
}
