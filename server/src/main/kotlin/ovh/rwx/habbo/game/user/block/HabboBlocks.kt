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

package ovh.rwx.habbo.game.user.block

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.user.UserBlocksDao
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Bloqueios da sessão: quem EU bloqueei ([blocked]) e quem me bloqueou ([blockedBy]). Memória na frente, persistência
 * em write-behind. O bloqueio é por usuário: vale mesmo que um dos lados entre depois com um cliente antigo.
 */
class HabboBlocks(private val habboSession: HabboSession) : BlockView {
    private val blocked = BlockList()

    @Volatile
    private var blockedBy: Set<Int> = emptySet()

    internal fun load() {
        val userId = habboSession.userInformation.id

        blocked.load(UserBlocksDao.getBlockedIds(userId))
        blockedBy = UserBlocksDao.getBlockedByIds(userId).toSet()
    }

    /** Ids que bloqueei, do mais antigo para o mais recente. */
    fun snapshot(): Set<Int> = blocked.snapshot()

    /** Barato e seguro para as threads de sala. */
    override fun isBlocking(userId: Int): Boolean = blocked.isBlocked(userId)

    override fun isBlockedBy(userId: Int): Boolean = userId in blockedBy

    fun block(userId: Int): BlockResult {
        val ownerId = habboSession.userInformation.id
        val result = blocked.block(ownerId, userId)

        if (result == BlockResult.BLOCKED) {
            UserBlocksDao.addBlock(ownerId, userId)
            HabboServer.habboSessionManager.getHabboSessionById(userId)?.habboBlocks?.updateBlockedBy(ownerId, true)
        }

        return result
    }

    fun unblock(userId: Int): BlockResult {
        val ownerId = habboSession.userInformation.id
        val result = blocked.unblock(userId)

        if (result == BlockResult.UNBLOCKED) {
            UserBlocksDao.removeBlock(ownerId, userId)
            HabboServer.habboSessionManager.getHabboSessionById(userId)?.habboBlocks?.updateBlockedBy(ownerId, false)
        }

        return result
    }

    @Synchronized
    private fun updateBlockedBy(userId: Int, isBlocked: Boolean) {
        blockedBy = if (isBlocked) blockedBy + userId else blockedBy - userId
    }
}

/** Verdadeiro se um dos dois bloqueou o outro (as ações entre eles são recusadas). */
fun HabboSession.isBlockedEitherWay(otherUserId: Int): Boolean =
    BlockPolicy.isBlockedEitherWay(habboBlocks, otherUserId)

/** Remove os quartos cujo dono bloqueou esta sessão (buscas/listas do navegador). Barato: lookup em set. */
fun HabboSession.withoutBlockedOwners(rooms: List<ovh.rwx.habbo.game.room.Room>): List<ovh.rwx.habbo.game.room.Room> =
    rooms.filterNot { BlockPolicy.hidesRoomOwner(habboBlocks, it.roomData.ownerId) }
