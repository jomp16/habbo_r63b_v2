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

            // depositedFurnis (ChestItemType + amount) — detalhamento por tipo
            // quando houver; sem transações Wired o detalhe individual não existe.
            writeInt(0)

            // withdrawnFurnis
            writeInt(0)

            // isIncompleteData: o client mostra "Mais mobis não listados"
            writeBoolean(log.depositFurniCount > 0 || log.withdrawFurniCount > 0)
        }
    }
}
