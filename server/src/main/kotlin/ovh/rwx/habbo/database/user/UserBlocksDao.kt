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

object UserBlocksDao {
    // toda operação sobre os bloqueios de um dono usa a MESMA chave (bloquear/desbloquear em sequência mantém a ordem)
    private fun ownerKey(userId: Int) = WriteKey("user-blocks", userId.toLong())

    /** Ids que o usuário bloqueou, do mais antigo para o mais recente. */
    fun getBlockedIds(userId: Int): List<Int> = db {
        query<Int>("sql/users/blocks/select_user_blocks.sql", mapOf("user_id" to userId))
    }

    /** Ids dos usuários que bloquearam este usuário. */
    fun getBlockedByIds(userId: Int): List<Int> = db {
        query<Int>("sql/users/blocks/select_blocked_by.sql", mapOf("user_id" to userId))
    }

    fun addBlock(userId: Int, blockedUserId: Int) = WriteBehindManager.queue(ownerKey(userId)) {
        db {
            update(
                "sql/users/blocks/insert_user_block.sql",
                mapOf("user_id" to userId, "blocked_user_id" to blockedUserId)
            )
        }
    }

    fun removeBlock(userId: Int, blockedUserId: Int) = WriteBehindManager.queue(ownerKey(userId)) {
        db {
            update(
                "sql/users/blocks/delete_user_block.sql",
                mapOf("user_id" to userId, "blocked_user_id" to blockedUserId)
            )
        }
    }
}
