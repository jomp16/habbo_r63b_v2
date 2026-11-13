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

import ovh.rwx.habbo.database.user.UserIgnoresDao
import ovh.rwx.habbo.game.user.HabboSession

/** Lista de ignorados da sessão: memória na frente, persistência em write-behind. */
class HabboIgnores(private val habboSession: HabboSession) {
    private val list = IgnoreList()

    internal fun load() {
        list.load(UserIgnoresDao.getIgnores(habboSession.userInformation.id))
    }

    /** Ordenada do mais antigo para o mais recente. */
    fun snapshot(): List<IgnoredUser> = list.snapshot()

    /** Barato e seguro para as threads de sala (usado no loop de chat). */
    fun isIgnoring(userId: Int): Boolean = list.isIgnoring(userId)

    fun add(userId: Int, username: String): IgnoreResult {
        val ownerId = habboSession.userInformation.id
        val outcome = list.add(ownerId, userId, username)

        if (outcome.result != IgnoreResult.REFUSED) {
            outcome.droppedUserId?.let { UserIgnoresDao.removeIgnore(ownerId, it) }
            UserIgnoresDao.addIgnore(ownerId, userId)
        }

        return outcome.result
    }

    fun remove(userId: Int): IgnoreResult {
        val result = list.remove(userId)

        if (result == IgnoreResult.REMOVED) UserIgnoresDao.removeIgnore(habboSession.userInformation.id, userId)

        return result
    }
}
