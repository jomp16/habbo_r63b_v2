/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

package ovh.rwx.fastfood.communication.incoming.game

import ovh.rwx.fastfood.communication.FFHandler
import ovh.rwx.fastfood.communication.incoming.FFIncoming
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.communication.HabboRequest

@Suppress("unused", "UNUSED_PARAMETER")
class GameExecuteActionHandler {
    @FFHandler(FFIncoming.EXECUTE_ACTION)
    fun handle(fastFoodSession: FastFoodSession, habboRequest: HabboRequest) {
        if (!fastFoodSession.authenticated) return
        val action = habboRequest.readInt()

        when (action) {
            0 -> {
                // drop food
            }
            1 -> {
                // open parachute
            }
            2 -> {
                // open big parachute
            }
            3 -> {
                // launch missile
            }
            4 -> {
                // shield food
            }
        }
    }
}