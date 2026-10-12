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
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.isVersionBetween
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.information.UserPreferences
import ovh.rwx.habbo.game.user.information.UserStats

/**
 * Payload estruturado do USER_OBJECT (UserObjectMessageEvent), válido de 2010 a 2026.
 *
 * @param userInformation dados base do usuário.
 * @param userStats respectos e lastOnline.
 * @param userPreferences usado no tail preshuffle (friendStreamEnabled).
 * @param canChangeName flag de troca de nome (lida por clientes >= 2012-05-01).
 */
data class UserObjectData(
    val userInformation: UserInformation,
    val userStats: UserStats,
    val userPreferences: UserPreferences,
    val canChangeName: Boolean = false,
)

@Suppress("unused", "UNUSED_PARAMETER")
class UserObjectResponse {
    @Response(Outgoing.USER_OBJECT)
    @ResponseR63A(OutgoingR63A.USER_OBJECT)
    fun response(habboResponse: HabboResponse, data: UserObjectData) {
        habboResponse.apply {
            val userInformation = data.userInformation
            val userStats = data.userStats

            // 1. O Identificador (String -> Int em 2011-08-17, RELEASE63-201108172310)
            if (isVersionAtLeast(2011, 8, 17)) {
                writeInt(userInformation.id)
            } else {
                writeUTF(userInformation.id.toString())
            }

            // 2. Bloco de Dados Base (Sempre presente; sex nasce 2009-11-13)
            writeUTF(userInformation.username)
            writeUTF(userInformation.figure)
            writeUTF(userInformation.gender)
            writeUTF(userInformation.motto)
            writeUTF(userInformation.realname)

            // 3. directMail + slots extras por era (estruturas verificadas pelo habbo-timeline em 1329 builds)
            if (isVersionBefore(2011, 7, 21)) {
                // Estrutura "Poluída" de 2010 até Junho de 2011 (RELEASE63-201103091336)
                writeInt(0)  // directMail
                writeUTF("") // O slot extra que o script detectou como readString()
                writeInt(0)  // Slot extra 1
                writeInt(0)  // Slot extra 2
            } else if (isVersionBefore(2011, 8, 17)) {
                // Janela Julho/2011: directMail ainda é Int, sem o slot String/Ints extras
                writeInt(0)
            } else {
                // Era moderna (2011-08-17 em diante): directMail vira Boolean
                writeBoolean(false)
            }

            // 4. Respectos (idênticos em todas as eras)
            writeInt(userStats.respect)
            writeInt(userStats.dailyRespectPoints)
            writeInt(userStats.dailyPetRespectPoints)

            // 5. streamPublishingAllowed nasce 2011-07-21 e sobrevive até hoje
            if (isVersionAtLeast(2011, 7, 21)) {
                // Experimento PetRespect v2 (janela 2017-06-21..07-04): 4º int = pet respects restantes
                // do usuário; o infostand (btn_petrespect) o gasta via composer _-1Gl@3836 (só existe nessa
                // release) e o evento de 2 ints (md5 1a4f5d22, vivo 06-13..2018-03) o incrementa.
                // Revertido em PRODUCTION-201707041014. 0 = botão desativado no cliente, seguro pois o
                // emulador não implementa o composer 3836.
                if (isVersionBetween(2017, 6, 21, 2017, 7, 4)) {
                    writeInt(0)
                }
                writeBoolean(data.userPreferences.friendStreamEnabled)
                // lastAccessDate nasce 2012-03-27 (RELEASE63-201203271238); preshuffle não lê
                if (isVersionAtLeast(2012, 3, 27)) {
                    writeUTF(userStats.lastOnline.format(HabboServer.DATE_TIME_FORMATTER_WITH_HOURS))
                }
                if (isVersionAtLeast(2012, 4, 11)) {
                    writeBoolean(data.canChangeName)
                }
                if (isVersionAtLeast(2012, 6, 5)) {
                    writeBoolean(false) // accountSafetyLocked
                }
                if (isVersionAtLeast(2025, 10, 3)) {
                    // Campos opcionais do AIR (guard bytesAvailable no parser 2026):
                    // defaults do cliente: false, "", 0, 3
                    writeBoolean(false) // accountTradeLocked
                    writeUTF("")        // nameColor
                    writeInt(userStats.dailyRespectPoints) // respectReplenishesLeft
                    writeInt(3)         // maxRespectPerDay
                }
            }
        }
    }
}
