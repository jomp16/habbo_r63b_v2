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

package ovh.rwx.habbo.communication.outgoing.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.information.UserPreferences
import ovh.rwx.habbo.game.user.information.UserStats

@Suppress("unused", "UNUSED_PARAMETER")
class UserObjectResponse {
    @Response(Outgoing.USER_OBJECT)
    fun response(
        habboResponse: HabboResponse,
        userInformation: UserInformation,
        userStats: UserStats,
        userPreferences: UserPreferences,
        canChangeName: Boolean
    ) {
        habboResponse.apply {
            writeInt(userInformation.id)
            writeUTF(userInformation.username)
            writeUTF(userInformation.figure)
            writeUTF(userInformation.gender)
            writeUTF(userInformation.motto)
            writeUTF(userInformation.realname)
            writeBoolean(false)
            writeInt(userStats.respect)
            writeInt(userStats.dailyRespectPoints)
            writeInt(userStats.dailyPetRespectPoints)
            writeBoolean(true) // Friends stream active
            writeUTF(userStats.lastOnline.format(HabboServer.DATE_TIME_FORMATTER_WITH_HOURS))
            writeBoolean(canChangeName)
            writeBoolean(false)
        }
    }

    @ResponseR63A(OutgoingR63A.USER_OBJECT)
    fun responseR63A(
        habboResponse: HabboResponse,
        userInformation: UserInformation,
        userStats: UserStats,
        userPreferences: UserPreferences,
    ) {
        habboResponse.apply {
            // 1. O Identificador (O divisor de águas é 30/08/2011)
            if (isVersionAtLeast(2011, 8, 30)) {
                writeInt(userInformation.id)
            } else {
                writeUTF(userInformation.id.toString())
            }

            // 2. Bloco de Dados Base (Sempre presente)
            writeUTF(userInformation.username)
            writeUTF(userInformation.figure)
            writeUTF(userInformation.gender)
            writeUTF(userInformation.motto)
            writeUTF(userInformation.realname)

            // 3. Diferenciação de Cauda (Estrutura Antiga vs Nova)
            if (isVersionAtLeast(2011, 7, 21)) {
                // Estrutura enxuta das builds de 2011 final
                writeBoolean(true) // flag de stream/permissão
                writeInt(userStats.respect)
                writeInt(userStats.dailyRespectPoints)
                writeInt(userStats.dailyPetRespectPoints)
                writeBoolean(userPreferences.friendStreamEnabled)
            } else {
                // Estrutura "Poluída" de 2010 até Junho de 2011
                writeInt(0)  // directMail
                writeUTF("") // O slot extra que o script detectou como readString()
                writeInt(0)  // Slot extra 1
                writeInt(0)  // Slot extra 2
                writeInt(userStats.respect)
                writeInt(userStats.dailyRespectPoints)
                writeInt(userStats.dailyPetRespectPoints)
            }
        }
    }
}