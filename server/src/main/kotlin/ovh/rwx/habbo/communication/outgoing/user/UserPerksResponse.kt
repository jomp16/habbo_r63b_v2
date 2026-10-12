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

package ovh.rwx.habbo.communication.outgoing.user

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing

class UserPerksResponse {
    @Response(Outgoing.USER_PERKS)
    fun response(habboResponse: HabboResponse, perksData: Array<Triple<String, String, Boolean>>) {
        habboResponse.apply {
            writeInt(perksData.size)

            // Até 2013-05-31 o PerkAllowancesMessageParser lê (code, isAllowed, errorMessage);
            // de 2013-06-03 em diante Sulake trocou para (code, errorMessage, isAllowed)
            val oldEra = isVersionBefore(2013, 6, 3)

            perksData.forEach { (name, requirement, enabled) ->
                if (oldEra) {
                    writeUTF(name)
                    writeBoolean(enabled)
                    writeUTF(requirement)
                } else {
                    writeUTF(name)
                    writeUTF(requirement)
                    writeBoolean(enabled)
                }
            }
        }
    }
}