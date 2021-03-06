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

package ovh.rwx.habbo.communication.outgoing.gamecenter

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class GameCenterLoadGameResponse {
    @Response(Outgoing.GAME_CENTER_LOAD_GAME)
    fun response(habboResponse: HabboResponse, gameId: Int, clientId: String, swfURL: String, quality: String, scaleMode: String, framesPerSecond: Int, flashMajorVersion: Int, flashMinorVersion: Int, params: Map<String, String>) {
        habboResponse.apply {
            writeInt(gameId)
            writeUTF(clientId)
            writeUTF(swfURL)
            writeUTF(quality)
            writeUTF(scaleMode)
            writeInt(framesPerSecond)
            writeInt(flashMajorVersion)
            writeInt(flashMinorVersion)
            writeInt(params.size)

            params.forEach { (key, value) ->
                writeUTF(key)
                writeUTF(value)
            }
        }
    }
}