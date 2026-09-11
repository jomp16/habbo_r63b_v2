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

package ovh.rwx.habbo.communication.outgoing.handshake

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.information.UserInformation

enum class HabboAirLoginAction(val id: Int) {
    NAME_CHANGE(0),
    ROOM_PICKING(1);
}

data class AuthenticationOkData(
    val userInformation: UserInformation,
    val requireNameChange: Boolean = false,
    val requireRoomPicking: Boolean = false,
    val additionalActions: List<Int> = emptyList()
) {
    /**
     * Monta as ações de login/onboarding sugeridas para o cliente Habbo AIR:
     * - Valor 0: Força abertura do diálogo de troca de nome inicial (startNameChange)
     * - Valor 1: Força abertura da seleção de quartos pré-fabricados (startRoomPicking)
     * - Lista vazia: Pula qualquer etapa de onboarding e abre o Hotel View normalmente
     */
    val suggestedLoginActions: List<Int>
        get() = buildList {
            if (requireNameChange) add(HabboAirLoginAction.NAME_CHANGE.id)
            if (requireRoomPicking) add(HabboAirLoginAction.ROOM_PICKING.id)
            addAll(additionalActions)
        }
}

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeAuthenticationOkResponse {
    @Response(Outgoing.AUTHENTICATION_OK)
    @ResponseR63A(OutgoingR63A.HANDSHAKE_AUTHENTICATION_OK)
    fun response(habboResponse: HabboResponse, data: AuthenticationOkData) {
        habboResponse.apply {
            // Flash Clássico (Web / R63A / R63B original):
            // O AuthenticationOKMessageParser.as é completamente vazio (não lê nada do buffer).
            //
            // Habbo AIR (Desktop WIN63 / MAC63 - 2021-04-09+):
            // AuthenticationOKMessageParser.as lê:
            //   - accountId: Int
            //   - suggestedLoginActions: Array de Int (tamanho como Int, seguido dos elementos como Int)
            //   - identityId: Int
            //
            // No AS3 (Habbo AIR Onboarding Flow):
            //   isOnboardingRequired(suggestedLoginActions) -> return actions.indexOf(0) >= 0 || actions.indexOf(1) >= 0
            //   Ações suportadas:
            //     - 0: Dispara troca/escolha de nome inicial (startNameChange())
            //     - 1: Dispara seleção de quarto inicial pré-fabricado (startRoomPicking() / isRoomPickingNeeded)
            //   Se suggestedLoginActions for vazio (size = 0), o fluxo de onboarding é pulado e entra direto no hotel view.
            if (isAir && isVersionAtLeast(2021, 4, 9)) {
                writeInt(data.userInformation.id) // accountId
                writeInt(data.suggestedLoginActions.size) // size of suggestedLoginActions
                data.suggestedLoginActions.forEach { action ->
                    writeInt(action)
                }
                writeInt(data.userInformation.id) // identityId
            }
        }
    }
}