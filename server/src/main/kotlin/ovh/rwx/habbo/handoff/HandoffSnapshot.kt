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

data class HandoffPayload(
    val schemaVersion: Int = 1,
    val listenerTypes: List<String>,
    val sessions: List<SessionSnapshot>,
    val rooms: List<RoomSnapshot>,
)

data class CryptoSnapshot(
    val type: String,
    val state: IntArray? = null,
    val x: Int = 0,
    val y: Int = 0,
    val innerState: IntArray? = null,
    val innerI: Int = 0,
    val innerJ: Int = 0,
    val outerState: IntArray? = null,
    val outerI: Int = 0,
    val outerJ: Int = 0,
    val rngSeed: Int = 0,
    val rngModulus: Int = 65536,
)

data class SessionSnapshot(
    val fdIndex: Int,
    val isWebSocket: Boolean,
    val userId: Int,
    val release: String,
    val protocolEra: String,
    val habboVersionIso: String,
    val habboVersionBuild: Int,
    val habboVersionPlatform: String = "UNKNOWN",
    val habboVersionReleaseString: String = "",
    val uniqueId: String,
    val osInformation: String,
    val isBot: Boolean,
    val cryptoIn: CryptoSnapshot?,
    val cryptoOut: CryptoSnapshot?,
    val expectedBase64Length: Int,
    val pendingRc4BytesB64: String,
    val pendingDecoderBytesB64: String,
    val currentRoomId: Int?,
    val roomUser: RoomUserSnapshot?,
)

data class RoomUserSnapshot(
    val virtualId: Int,
    val x: Int,
    val y: Int,
    val z: Double,
    val headRotation: Int,
    val bodyRotation: Int,
    val idle: Boolean,
    val danceId: Int,
    val handItem: Int,
    val effectId: Int,
    val statuses: Map<String, String>,
    val objectiveX: Int? = null,
    val objectiveY: Int? = null,
    val objectiveRotation: Int = -1,
)

data class RoomSnapshot(
    val roomId: Int,
    val tickCounter: Int,
    val rollerCounter: Int,
    val roomTimer: Int,
    val pets: List<PetSnapshot>,
)

data class PetSnapshot(
    val petId: Int,
    val virtualId: Int,
    val x: Int,
    val y: Int,
    val z: Double,
    val headRotation: Int,
    val bodyRotation: Int,
)
