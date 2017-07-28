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

package tk.jomp16.habbo.communication.incoming.user

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.database.wardrobe.WardrobeDao
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.game.user.wardrobe.Wardrobe

@Suppress("unused", "UNUSED_PARAMETER")
class UserWardrobeSaveHandler {
    @Handler(Incoming.USER_WARDROBE_SAVE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return
        val slotId = habboRequest.readInt()

        if (slotId > 10) return
        val figure = habboRequest.readUTF()
        val gender = habboRequest.readUTF()
        val wardrobeToRemove = habboSession.userInformation.wardrobes.filter { it.slotId == slotId }.firstOrNull()
        val newWardrobe: Wardrobe = if (wardrobeToRemove != null) {
            habboSession.userInformation.wardrobes.remove(wardrobeToRemove)

            WardrobeDao.updateWardrobe(wardrobeToRemove.id, slotId, figure, gender)
        } else {
            WardrobeDao.createWardrobe(habboSession.userInformation.id, slotId, figure, gender)
        }

        habboSession.userInformation.wardrobes.add(newWardrobe)
    }
}