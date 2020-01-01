/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.incoming.user

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.database.wardrobe.WardrobeDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.wardrobe.Wardrobe

@Suppress("unused", "UNUSED_PARAMETER")
class UserWardrobeSaveHandler {
    @Handler(Incoming.USER_WARDROBE_SAVE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val slotId = habboRequest.readInt()

        if (slotId > 10) return
        val figure = habboRequest.readUTF()
        val gender = habboRequest.readUTF()
        val wardrobeToRemove = habboSession.userInformation.wardrobes.firstOrNull { it.slotId == slotId }
        val newWardrobe: Wardrobe = if (wardrobeToRemove != null) {
            habboSession.userInformation.wardrobes.remove(wardrobeToRemove)

            WardrobeDao.updateWardrobe(wardrobeToRemove.id, slotId, figure, gender)
        } else {
            WardrobeDao.createWardrobe(habboSession.userInformation.id, slotId, figure, gender)
        }

        habboSession.userInformation.wardrobes.add(newWardrobe)
    }
}