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

package ovh.rwx.habbo.database.user

import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.writebehind.WriteKey
import ovh.rwx.habbo.game.user.ignore.IgnoredUser

object UserIgnoresDao {
    // a lista de um dono é ordenada (o mais antigo sai no limite): toda operação sobre ela usa a MESMA chave
    private fun ownerKey(userId: Int) = WriteKey("user-ignores", userId.toLong())

    /** Ignorados do usuário, do mais antigo para o mais recente, com o username atual. */
    fun getIgnores(userId: Int): List<IgnoredUser> = db {
        query<IgnoredUser>("sql/users/ignores/select_user_ignores.sql", mapOf("user_id" to userId))
    }

    fun addIgnore(userId: Int, ignoredUserId: Int) = WriteBehindManager.queue(ownerKey(userId)) {
        db {
            update(
                "sql/users/ignores/insert_user_ignore.sql",
                mapOf("user_id" to userId, "ignored_user_id" to ignoredUserId)
            )
        }
    }

    fun removeIgnore(userId: Int, ignoredUserId: Int) = WriteBehindManager.queue(ownerKey(userId)) {
        db {
            update(
                "sql/users/ignores/delete_user_ignore.sql",
                mapOf("user_id" to userId, "ignored_user_id" to ignoredUserId)
            )
        }
    }
}
