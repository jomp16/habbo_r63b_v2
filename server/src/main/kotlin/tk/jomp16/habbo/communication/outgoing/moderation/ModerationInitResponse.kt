/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.communication.outgoing.moderation

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class ModerationInitResponse {
    @Response(Outgoing.MODERATION_INIT)
    fun handle(habboResponse: HabboResponse) {
        habboResponse.apply {
            // todo: tickets
            writeInt(0)

            // todo: user presets
            writeInt(0)

            // todo: user actions presets, but it's useless on Habbo.swf
            writeInt(0)

            writeBoolean(true) // Ticket right
            writeBoolean(true) // Chatlogs
            writeBoolean(true) // User actions alert etc
            writeBoolean(true) // Kick users
            writeBoolean(true) // Ban users
            writeBoolean(true) // Caution etc
            writeBoolean(true) // ?

            // todo: room presets
            writeInt(0)
        }
    }
}