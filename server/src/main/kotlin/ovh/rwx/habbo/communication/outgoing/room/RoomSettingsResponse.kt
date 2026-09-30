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

package ovh.rwx.habbo.communication.outgoing.room

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.Room

@Suppress("unused", "UNUSED_PARAMETER")
class RoomSettingsResponse {
    @Response(Outgoing.ROOM_SETTINGS)
    @ResponseR63A(OutgoingR63A.ROOM_SETTINGS)
    fun response(habboResponse: HabboResponse, room: Room) {
        habboResponse.apply {
            // =========================================================================
            // 1. CABEÇALHO BASE (Presente desde a RELEASE34 até o AIR de 2026)
            // =========================================================================
            writeInt(room.roomData.id)
            writeUTF(room.roomData.name)
            writeUTF(room.roomData.description)
            writeInt(room.roomData.state.state)
            writeInt(room.roomData.category)
            writeInt(room.roomData.usersMax)
            writeInt(if (room.roomModel.mapSizeX * room.roomModel.mapSizeY > 100) 50 else 25) // maximumVisitorsLimit

            // =========================================================================
            // 2. ERA LEGACY (RELEASE34 até RELEASE41 - Antes de 2010-01-22)
            // Ordem: Flags -> Tags -> Flatmates -> [hideWalls] -> [wallThick] -> [floorThick]
            // =========================================================================
            if (isVersionBefore(2010, 1, 22)) {
                // 3 Flags enviadas antes das tags no Beta inicial de 2009 (showOwnerName, allowDoorbell, allowTrading)
                writeInt(1) // showOwnerName
                writeInt(0) // allowDoorbell
                writeInt(if (room.roomData.tradeState > 0) 1 else 0) // allowTrading

                // Tags da sala
                writeInt(room.roomData.tags.size)
                room.roomData.tags.forEach { writeUTF(it) }

                // Flatmates embutidos
                writeFlatControllers(room)

                // RELEASE40+ (2009-11-13) introduziu allowPets e allowPetsEat pós-flatmates
                if (isVersionAtLeast(2009, 11, 27)) {
                    writeInt(if (room.roomData.allowPets) 1 else 0)
                    writeInt(if (room.roomData.allowPetsEat) 1 else 0)
                } else if (isVersionAtLeast(2009, 11, 13)) {
                    writeInt(if (room.roomData.allowPets) 1 else 0)
                }

                return@apply
            }

            // =========================================================================
            // 3. ERA MODERNA (RELEASE44 em 2010-01-22 até 2026)
            // Tags foram movidas para logo após o maximumVisitorsLimit
            // =========================================================================
            writeInt(room.roomData.tags.size)
            room.roomData.tags.forEach { writeUTF(it) }

            // 3.1 Flatmates vs TradeState (Mudança ocorrida na RELEASE63 em 2011-09-21)
            if (isVersionBefore(2011, 9, 21)) {
                // R44 até R63 transitória ainda utilizavam o loop de flatmates
                writeFlatControllers(room)
            } else {
                // R63A em diante substituiu o bloco de flatmates por tradeMode
                writeInt(room.roomData.tradeState)
            }

            // 3.2 Flags de permissão
            writeInt(if (room.roomData.allowPets) 1 else 0)
            writeInt(if (room.roomData.allowPetsEat) 1 else 0)
            writeInt(if (room.roomData.allowWalkThrough) 1 else 0)

            // 3.3 hideWalls (Adicionado a partir da RELEASE52 em 2010-05-27)
            if (isVersionAtLeast(2010, 5, 27)) {
                writeInt(if (room.roomData.hideWall) 1 else 0)
            }

            // 3.4 Espessuras de Parede e Piso (Introduzidas juntas na RELEASE63-33647 em 2011-05-13)
            if (isVersionAtLeast(2011, 5, 13)) {
                writeInt(room.roomData.wallThick)
                writeInt(room.roomData.floorThick)
            }

            // =========================================================================
            // 4. CHAT & COMPORTAMENTO DE SALA (R63B até 2026)
            // =========================================================================
            when {
                // AIR Moderno (WIN63 2026-05-18+): novo formato com flags de AFK/Sleep
                isAir && isVersionAtLeast(2026, 5, 18) -> {
                    writeInt(room.roomData.chatFloodProtection)
                    writeBoolean(room.roomData.leaveOnDoorTileEnabled)
                    writeBoolean(room.roomData.idleSleepEnabled)
                    writeInt(room.roomData.idleSleepTimeoutSeconds)
                    writeBoolean(room.roomData.idleAutokickEnabled)
                    writeInt(room.roomData.idleAutokickTimeoutSeconds)
                    writeBoolean(room.roomData.muteAllPets)
                }

                // Flash Moderno e AIR pré-2026 (PRODUCTION-20151201+ até 2026-05-18)
                isVersionAtLeast(2015, 12, 1) -> {
                    writeInt(room.roomData.chatType)
                    writeInt(room.roomData.chatBalloon)
                    writeInt(room.roomData.chatSpeed)
                    writeInt(room.roomData.chatMaxDistance)
                    writeInt(room.roomData.chatFloodProtection)
                    writeBoolean(room.roomData.allowNavigatorDynamicCats)
                }

                // R63B Inicial/Intermediário (2012-11-19 até 2015-12-01): Chat de 3 Ints
                isVersionAtLeast(2012, 11, 19) -> {
                    writeInt(room.roomData.chatType)
                    writeInt(room.roomData.chatBalloon)
                    writeInt(room.roomData.chatSpeed)
                }
            }

            // =========================================================================
            // 5. MODERAÇÃO (Mute, Kick, Ban - Introduzido em 2013-06-10)
            // =========================================================================
            if (isVersionAtLeast(2013, 6, 10)) {
                writeInt(room.roomData.muteSettings)
                writeInt(room.roomData.kickSettings)
                writeInt(room.roomData.banSettings)
            }

            // =========================================================================
            // 6. BUILDERS CLUB (Introduzido no AIR Desktop em 2023-09-22)
            // =========================================================================
            if (isAir && isVersionAtLeast(2023, 9, 22)) {
                writeBoolean(room.itemManager.hiddenBuildersClub)
            }
        }
    }

    private fun HabboResponse.writeFlatControllers(room: Room) {
        val controllers = room.userManager.rights

        // 1. Quantidade de usuários enviados no loop
        writeInt(controllers.size)

        // 2. Loop de flatmates: userId (Int) e userName (String)
        controllers.forEach { controller ->
            writeInt(controller.id)
            writeUTF(controller.username)
        }

        // 3. Total geral de usuários com direitos cadastrados
        writeInt(controllers.size)
    }
}