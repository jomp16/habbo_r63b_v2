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

package ovh.rwx.habbo.game.user.ignore

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.user.HabboSession

/** Alvo de um pedido de ignorar/designorar; [userId] = 0 quando não foi possível resolver o usuário. */
data class IgnoreTarget(val userId: Int, val username: String)

/** Lê o alvo do pedido (id nas builds WIN63 2025-12-10+, nome nas demais) e o resolve para um usuário. */
object IgnoreTargetReader {
    /** O 2º texto opcional (balão de chat silenciado) existe só em 2012-04-02 <= data < 2013-05-22 e é descartado. */
    fun read(habboSession: HabboSession, habboRequest: HabboRequest, ignoring: Boolean): IgnoreTarget {
        if (habboSession.isVersionAtLeast(2025, 12, 10)) {
            val userId = habboRequest.readInt()
            val userInformation = UserInformationDao.getUserInformationById(userId)
                ?: return IgnoreTarget(0, "")

            return IgnoreTarget(userInformation.id, userInformation.username)
        }

        val name = habboRequest.readUTF()

        if (ignoring && habboSession.isVersionAtLeast(2012, 4, 2) && habboSession.isVersionBefore(2013, 5, 22)) {
            habboRequest.readUTF()
        }

        if (name.isBlank()) return IgnoreTarget(0, name)

        if (!ignoring) {
            // designorar: o nome pode ter mudado desde que foi ignorado, a lista própria é a fonte
            habboSession.habboIgnores.snapshot().find { it.username.equals(name, ignoreCase = true) }
                ?.let { return IgnoreTarget(it.userId, it.username) }
        }

        val userInformation = UserInformationDao.getUserInformationByUsername(name) ?: return IgnoreTarget(0, name)

        return IgnoreTarget(userInformation.id, userInformation.username)
    }
}
