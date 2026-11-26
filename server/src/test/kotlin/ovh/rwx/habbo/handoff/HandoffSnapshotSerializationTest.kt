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

package ovh.rwx.habbo.handoff

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HandoffSnapshotSerializationTest {
    private val mapper = ObjectMapper().registerKotlinModule()

    @Test
    fun `round trip preserves RC4, Wedgie, AIR platform, room user statuses and pets`() {
        val rc4State = IntArray(256) { (it * 7 + 3) and 0xFF }
        val payload = HandoffPayload(
            schemaVersion = 1,
            listenerTypes = listOf("habbo-tcp", "habbo-ws", "fastfood-tcp", "fastfood-ws"),
            sessions = listOf(
                SessionSnapshot(
                    fdIndex = 0,
                    isWebSocket = true,
                    userId = 4,
                    release = "WIN63-202605181200-123456789",
                    protocolEra = "SHUFFLED",
                    habboVersionIso = "2026-05-18T12:00",
                    habboVersionBuild = 63,
                    habboVersionPlatform = "AIR",
                    habboVersionReleaseString = "WIN63-202605181200-123456789",
                    uniqueId = "uid-air-1",
                    osInformation = "Linux",
                    isBot = false,
                    cryptoIn = CryptoSnapshot(type = "RC4", state = rc4State, x = 12, y = 99),
                    cryptoOut = CryptoSnapshot(
                        type = "WEDGIE",
                        innerState = rc4State, innerI = 1, innerJ = 2,
                        outerState = rc4State, outerI = 3, outerJ = 4,
                        rngSeed = 42, rngModulus = 65536
                    ),
                    expectedBase64Length = 16,
                    pendingRc4BytesB64 = "AQID",
                    pendingDecoderBytesB64 = "BAUG",
                    currentRoomId = 14,
                    roomUser = RoomUserSnapshot(
                        virtualId = 7,
                        x = 5,
                        y = 8,
                        z = 1.5,
                        headRotation = 2,
                        bodyRotation = 4,
                        idle = true,
                        danceId = 3,
                        handItem = 9,
                        effectId = 25,
                        statuses = mapOf("sit" to "1.0", "mv" to "5,8,1.5"),
                        objectiveX = 9,
                        objectiveY = 10,
                        objectiveRotation = 4
                    )
                )
            ),
            rooms = listOf(
                RoomSnapshot(
                    roomId = 14,
                    tickCounter = 6,
                    rollerCounter = 2,
                    roomTimer = 120,
                    pets = listOf(
                        PetSnapshot(petId = 11, virtualId = 8, x = 3, y = 4, z = 0.0, headRotation = 0, bodyRotation = 2)
                    )
                )
            )
        )

        val json = mapper.writeValueAsString(payload)
        val restored: HandoffPayload = mapper.readValue(json)

        assertEquals(payload.listenerTypes, restored.listenerTypes)
        assertEquals(1, restored.sessions.size)
        val s = restored.sessions[0]
        assertEquals("AIR", s.habboVersionPlatform)
        assertEquals("WIN63-202605181200-123456789", s.habboVersionReleaseString)
        assertArrayEquals(rc4State, s.cryptoIn?.state)
        assertEquals(42, s.cryptoOut?.rngSeed)
        assertEquals(mapOf("sit" to "1.0", "mv" to "5,8,1.5"), s.roomUser?.statuses)
        assertEquals(9, s.roomUser?.objectiveX)
        assertEquals(10, s.roomUser?.objectiveY)
        assertEquals(4, s.roomUser?.objectiveRotation)
        assertEquals(1, restored.rooms[0].pets.size)
        assertEquals(8, restored.rooms[0].pets[0].virtualId)
    }
}
