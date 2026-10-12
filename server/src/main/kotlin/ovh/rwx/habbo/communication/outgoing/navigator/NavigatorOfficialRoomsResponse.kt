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

package ovh.rwx.habbo.communication.outgoing.navigator

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.Room
import kotlin.math.min

/** type 4 do _SafeStr_2709 = pasta (folder_cont com exibir/ocultar). Ids fora de 1..4 caem no else do parser (sem payload). */
enum class OfficialRoomEntryType(val id: Int) { NONE(0), TAG(1), GUEST_ROOM(2), PUBLIC_ROOM(3), FOLDER(4);
    companion object {
        fun from(id: Int) = entries.firstOrNull { it.id == id } ?: NONE
    }
}

/** _SafeStr_3890 (PublicRoomData): campos do quarto público dentro da entry type 3. */
data class OfficialRoomPublicData(
    val name: String = "",
    val enterBytes: Int = 0,
    val owner: Int = 0,
    // 4º campo do parser: lista comma-separated de nomes dos pngs em
    // c_images/officialrooms_defaults/ (o AS3 faz split(","))
    val imageNames: List<String> = emptyList(),
    val usersNow: Int = 0,
    val maxUsers: Int = 0,
)

data class OfficialRoomEntry(
    val index: Int,
    val name: String = "",
    val description: String = "",
    val showDetails: Boolean = false,
    val picText: String = "",
    val picLink: String = "",
    val parentIndex: Int = 0,
    val userCount: Int = 0,
    val type: OfficialRoomEntryType = OfficialRoomEntryType.NONE,
    val tag: String? = null,           // type TAG
    val guestRoom: Room? = null,       // type GUEST_ROOM
    val publicRoom: OfficialRoomPublicData? = null, // type PUBLIC_ROOM
)

data class PromotedRoom(
    val code: String = "",        // promotedroomcategory.<code> no external texts
    val figure: String = "",      // avatar exibido no header da categoria
    val room: Room? = null,        // quarto principal (lido incondicional pelo parser)
    val extraRooms: List<Room> = emptyList(), // top ten expandido ao abrir
)

/** OfficialRoomsEvent (1263 pós-shuffle / 450 pré-shuffle) — dados completos por era. */
data class NavigatorOfficialRoomsData(
    val entries: List<OfficialRoomEntry> = emptyList(),
    val promoAds: List<OfficialRoomEntry> = emptyList(),
    val promotedRooms: List<PromotedRoom> = emptyList(),
)

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorOfficialRoomsResponse {
    // pós-shuffle: OfficialRoomsEvent 1263 (parser _SafeStr_2757); pré-shuffle: 450 (_SafeStr_2008)
    @Response(Outgoing.NAVIGATOR_OFFICIAL_ROOMS_RESULT)
    @ResponseR63A(OutgoingR63A.NAVIGATOR_OFFICIAL_ROOMS_RESULT)
    fun response(habboResponse: HabboResponse, data: NavigatorOfficialRoomsData) {
        habboResponse.apply {
            writeInt(data.entries.size)
            data.entries.forEach { writeEntry(it) }

            if (isVersionBefore(2009, 6, 30)) {
                // RELEASE34: tail bool/int/bool + promo loop {int,int} + tag loop {String,int}
                writeBoolean(false)
                writeInt(0)
                writeBoolean(false)
                writeInt(0) // 0 promo entries
                writeInt(0) // 0 promoted tags
                return@apply
            }

            if (isVersionBefore(2010, 1, 14)) {
                if (isVersionBefore(2009, 8, 21)) {
                    // 2009-06-30: bool isGuestRoom; true → listas, false → 1 int
                    writeBoolean(true)
                    writeBoolean(false)
                    writeInt(0)
                    writeBoolean(false)
                    writeInt(0)
                    writeInt(0)
                } else {
                    // 2009-08-21: 1 int extra
                    writeInt(0)
                }
                return@apply
            }

            if (isVersionAtLeast(2011, 6, 21)) {
                writeInt(min(data.promoAds.size, 1))
                data.promoAds.firstOrNull()?.let { writeEntry(it) }
            }

            if (isVersionAtLeast(2013, 5, 22)) {
                writeInt(data.promotedRooms.size)
                data.promotedRooms.forEach {
                    writeUTF(it.code)
                    writeUTF(it.figure)
                    writeInt(it.extraRooms.size + 1) // principal + extras
                    if (it.room != null) {
                        serialize(it.room)
                    }
                    it.extraRooms.forEach { room -> serialize(room) }
                }
            }
        }
    }

    private fun HabboResponse.writeEntry(entry: OfficialRoomEntry) {
        writeInt(entry.index)
        writeUTF(entry.name)
        writeUTF(entry.description)
        writeInt(if (entry.showDetails) 1 else 0)
        writeUTF(entry.picText)
        writeUTF(entry.picLink)
        writeInt(entry.parentIndex)
        writeInt(entry.userCount)
        writeInt(entry.type.id)
        when (entry.type) {
            OfficialRoomEntryType.TAG -> writeUTF(entry.tag ?: "")
            OfficialRoomEntryType.GUEST_ROOM -> {
                if (entry.guestRoom != null) {
                    serialize(entry.guestRoom)
                }
            }
            OfficialRoomEntryType.PUBLIC_ROOM -> {
                val publicRoom = entry.publicRoom ?: OfficialRoomPublicData()
                writeUTF(publicRoom.name)
                writeInt(publicRoom.enterBytes)
                writeInt(publicRoom.owner)
                writeUTF(publicRoom.imageNames.joinToString(","))
                writeInt(publicRoom.usersNow)
                writeInt(publicRoom.maxUsers)
            }
            OfficialRoomEntryType.NONE, OfficialRoomEntryType.FOLDER -> writeBoolean(false)
        }
    }

    /**
     * OfficialRoomsEvent pré-shuffle (450) — era 2011-08 (_SafeStr_1987): cada quarto é
     * uma entry GUEST_ROOM (mesmo wire do 1263), tail = 1 int promoAdCount, sem promotedRooms.
     */
    @ResponseR63A(OutgoingR63A.NAVIGATOR_OFFICIAL_ROOMS_RESULT)
    fun responseR63A(habboResponse: HabboResponse, data: NavigatorOfficialRoomsData) =
        response(habboResponse, data)
}
