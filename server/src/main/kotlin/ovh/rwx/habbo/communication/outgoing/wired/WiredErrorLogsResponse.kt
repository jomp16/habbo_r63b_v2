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

package ovh.rwx.habbo.communication.outgoing.wired

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class WiredErrorLogsResponse {
    @Response(Outgoing.WIRED_ERROR_LOGS)
    fun response(habboResponse: HabboResponse) {
        // TODO: Implementar quando refazer wired 2.0
        // Array de erros, cada erro contém:
        // - errorId (Int): ID do erro
        // - errorName (String): Nome do erro
        // - category (String): Categoria do erro
        // - throwCount (Int): Quantidade de vezes que o erro ocorreu
        // - msSinceLastOccurrence (Long): Milissegundos desde a última ocorrência
        habboResponse.writeInt(0) // empty errors array
    }
}
