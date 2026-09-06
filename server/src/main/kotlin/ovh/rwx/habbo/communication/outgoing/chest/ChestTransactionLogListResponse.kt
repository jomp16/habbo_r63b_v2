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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Payload estruturado para a listagem paginada de logs de transação do baú (CHEST_TRANSACTION_LOG_LIST).
 *
 * @param logListType Tipo de visualização (0 = sala/todos, 1 = baú específico).
 * @param logListId ID associado ao filtro (ex: chestItemId ou roomId).
 * @param totalLogs Contagem total de logs existentes no banco.
 * @param currentPage Número da página atual (0-based).
 * @param pageSize Quantidade pedida por página (ex: 25).
 * @param logs Lista de registros de log a enviar nesta página.
 */
data class ChestTransactionLogListData(
    val logListType: Int,
    val logListId: Long,
    val totalLogs: Int,
    val currentPage: Int,
    val pageSize: Int,
    val logs: List<ChestDao.ChestLog>,
)

@Suppress("unused", "UNUSED_PARAMETER")
class ChestTransactionLogListResponse {
    @Response(Outgoing.CHEST_TRANSACTION_LOG_LIST)
    fun response(habboResponse: HabboResponse, data: ChestTransactionLogListData) {
        habboResponse.apply {
            writeInt(data.logListType)
            writeLong(data.logListId)
            writeInt(data.totalLogs)
            writeInt(data.currentPage)
            // amount DEVE ecoar o pageSize pedido pelo client (TransactionConfig.PAGE_SIZE = 25):
            // onLogList só exibe se amount == PAGE_SIZE (e != TRANSACTIONS_PREVIEW_AMOUNT = 10)
            writeInt(data.pageSize)
            writeInt(data.logs.size)

            data.logs.forEach { log -> serializeTransactionInfo(this, log) }
        }
    }

    companion object {
        private val READABLE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        fun serializeTransactionInfo(habboResponse: HabboResponse, log: ChestDao.ChestLog) {
            habboResponse.apply {
                writeLong(log.id.toLong()) // transactionId
                writeInt(log.roomId) // flatId
                writeInt(0) // transactionType (manual)
                writeUTF("") // transactionDefinitionInfo
                writeInt(log.userId)
                writeUTF(log.username)
                writeLong(log.createdAt.atZone(ZoneId.systemDefault()).toEpochSecond())
                writeUTF(READABLE_FORMATTER.format(log.createdAt))
                writeInt(1) // chestCount
                writeInt(log.withdrawFurniCount)
                writeInt(log.depositFurniCount)
                writeInt(log.withdrawCoinsCount)
                writeInt(log.depositCoinsCount)
            }
        }
    }
}
