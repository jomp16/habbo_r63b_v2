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

package ovh.rwx.habbo.communication.incoming.landing

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("unused", "UNUSED_PARAMETER")
class LandingLoadWidgetHandler {
    @Handler(Incoming.REFRESH_CAMPAIGN)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val campaignString = habboRequest.readUTF() // A string .conf completa
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        var currentActiveCampaign = ""

        // 1. Separar os blocos (Ex: "2026-02-12 12:00,feb26globe")
        val schedules = campaignString.split(';')

        for (entry in schedules) {
            if (!entry.contains(',')) continue

            val parts = entry.split(',')
            val scheduledTime = LocalDateTime.parse(parts[0], formatter)
            val campaignName = parts[1]

            // 2. Se o horário atual é DEPOIS ou IGUAL ao agendado,
            // ele se torna o candidato a "ativo".
            if (now.isAfter(scheduledTime) || now.isEqual(scheduledTime)) {
                currentActiveCampaign = campaignName
            } else {
                // Como a lista costuma ser cronológica, se chegamos em uma data
                // no futuro, paramos o loop e ficamos com a última válida encontrada.
                break
            }
        }

        // 3. Envia a string original E o nome da campanha que passou no teste de tempo
        habboSession.sendHabboResponse(Outgoing.CAMPAIGN, campaignString, currentActiveCampaign)
    }
}