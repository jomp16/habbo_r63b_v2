/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.user

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

/**
 * AvailabilityStatusMessageEvent — estado do hotel enviado no login (antes do SSO).
 *
 * @param isOpen Hotel aberto para jogar (`false` = manutenção, cliente mostra tela de manutenção).
 * @param onShutDown Hotel em processo de desligamento/reboot — o cliente exibe aviso de desconexão iminente.
 * @param isAuthenticHabbo Gate de "sessão autêntica": quando `true` e o furniture data terminou de carregar,
 *        o SessionDataManager dispara os callbacks de sessão pronta (_SafeStr_16167). Nasce em 2016-07-26;
 *        antes disso o parser lê apenas `isOpen`/`onShutDown` e o byte extra desincronizaria o buffer.
 */
data class AvailabilityStatusData(
    val isOpen: Boolean = true,
    val onShutDown: Boolean = false,
    val isAuthenticHabbo: Boolean = true,
)

@Suppress("unused", "UNUSED_PARAMETER")
class UserAvailabilityStatusResponse {
    @Response(Outgoing.AVAILABILITY_STATUS)
    fun response(habboResponse: HabboResponse, data: AvailabilityStatusData) {
        habboResponse.apply {
            writeBoolean(data.isOpen)
            writeBoolean(data.onShutDown)
            // isAuthenticHabbo nasce em 2016-07-26; antes o parser lê apenas isOpen/onShutDown
            if (isVersionAtLeast(2016, 7, 26)) {
                writeBoolean(data.isAuthenticHabbo)
            }
        }
    }

    @ResponseR63A(OutgoingR63A.AVAILABILITY_STATUS)
    fun responseR63A(habboResponse: HabboResponse) {
        habboResponse.apply {
            writeBoolean(true)
            writeBoolean(false)
        }
    }
}