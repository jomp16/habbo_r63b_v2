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

package ovh.rwx.habbo.game.chest

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.chest.ChestDao
import ovh.rwx.habbo.database.chest.ChestLogItemsData
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession

object ChestAuditLog {
    private val log = LoggerFactory.getLogger(javaClass)

    fun logTransaction(
        room: Room,
        chest: ChestData,
        habboSession: HabboSession,
        withdrawFurniCount: Int = 0,
        depositFurniCount: Int = 0,
        withdrawCoinsCount: Int = 0,
        depositCoinsCount: Int = 0,
        itemsData: ChestLogItemsData? = null,
    ) {
        runCatching {
            ChestDao.insertChestLog(
                chestItemId = chest.itemId,
                roomId = room.roomData.id,
                userId = habboSession.userInformation.id,
                username = habboSession.userInformation.username,
                withdrawFurniCount = withdrawFurniCount,
                depositFurniCount = depositFurniCount,
                withdrawCoinsCount = withdrawCoinsCount,
                depositCoinsCount = depositCoinsCount,
                itemsData = itemsData,
            )
        }.onFailure { log.warn("Failed to insert chest log for chest {}", chest.itemId, it) }
    }
}
