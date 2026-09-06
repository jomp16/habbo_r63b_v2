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

package ovh.rwx.habbo.communication.outgoing.chest

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.chest.ChestDao

@Suppress("unused", "UNUSED_PARAMETER")
class ChestTransactionLogDetailsResponse {
    @Response(Outgoing.CHEST_TRANSACTION_LOG_DETAILS)
    fun response(habboResponse: HabboResponse, log: ChestDao.ChestLog) {
        habboResponse.apply {
            ChestTransactionLogListResponse.serializeTransactionInfo(this, log)

            // chestIds
            writeInt(1)
            writeInt(log.chestItemId)

            val deposited = log.itemsData?.deposited ?: emptyList()
            val withdrawn = log.itemsData?.withdrawn ?: emptyList()

            // depositedFurnis (ChestItemType + amount)
            writeInt(deposited.size)
            deposited.forEach { item ->
                writeBoolean(item.isWallItem)
                writeInt(item.typeId)
                writeUTF(item.legacyPosterId)
                writeInt(item.count)
            }

            // withdrawnFurnis (ChestItemType + amount)
            writeInt(withdrawn.size)
            withdrawn.forEach { item ->
                writeBoolean(item.isWallItem)
                writeInt(item.typeId)
                writeUTF(item.legacyPosterId)
                writeInt(item.count)
            }

            // isIncompleteData: o client mostra "Mais mobis não listados" caso haja contagem não coberta pelo detalhamento
            val depositedCount = deposited.sumOf { it.count }
            val withdrawnCount = withdrawn.sumOf { it.count }
            val isIncomplete = (log.depositFurniCount > depositedCount) || (log.withdrawFurniCount > withdrawnCount)
            writeBoolean(isIncomplete)
        }
    }
}
