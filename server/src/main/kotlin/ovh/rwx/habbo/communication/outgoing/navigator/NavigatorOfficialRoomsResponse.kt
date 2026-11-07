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
            if (isVersionBefore(2009, 6, 30)) {
                // RELEASE34 (2009-06-16): nao e a arvore oficial, e o payload plano de busca de quartos:
                // int N x guestRoom, bool, int, bool, int M x (int,int), int P x (str,int). Vazio e valido.
                writeInt(0)
                writeBoolean(false)
                writeInt(0)
                writeBoolean(false)
                writeInt(0)
                writeInt(0)
                return@apply
            }

            if (isVersionBefore(2010, 1, 14)) {
                // R35..R40: entries sem index; o unico int de contagem e o userCount (tooltip "usercounttooltip.users")
                if (isVersionBefore(2009, 8, 21)) {
                    // R35-R36: bool lider (false = arvore oficial; true = fluxo plano de busca, nunca usado aqui)
                    writeBoolean(false)
                } else {
                    // R37-R40: lista de nos (int id, str nome) antes das entries; vazia
                    writeInt(0)
                }
                writeInt(data.entries.size)
                data.entries.forEach { writeLegacyEntry(it) }
                return@apply
            }

            writeInt(data.entries.size)
            data.entries.forEach { writeEntry(it) }

            if (isVersionAtLeast(2011, 6, 21)) {
                writeInt(min(data.promoAds.size, 1))
                data.promoAds.firstOrNull()?.let { writeEntry(it) }
            }

            if (isVersionAtLeast(2012, 3, 20)) {
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

    /**
     * Entry pre-2010-01-14 (R35..R42, verificado no AS3 de RELEASE35-18827 / RELEASE37-20416):
     * str name, str desc, int (R35-R36: largura da celula 1..4, 4 = linha inteira; R37+: showDetails==1),
     * str picText, str picLink, int userCount, int type e o payload do tipo.
     */
    private fun HabboResponse.writeLegacyEntry(entry: OfficialRoomEntry) {
        val isR37 = isVersionAtLeast(2009, 8, 21)
        writeUTF(entry.name)
        writeUTF(entry.description)
        writeInt(if (isR37) (if (entry.showDetails) 1 else 0) else 4)
        writeUTF(entry.picText)
        writeUTF(entry.picLink)
        writeInt(entry.userCount)
        writeInt(entry.type.id)
        when (entry.type) {
            OfficialRoomEntryType.TAG -> writeUTF(entry.tag ?: "")
            OfficialRoomEntryType.GUEST_ROOM -> {
                // sem quarto o parser leria um guestRoom inexistente: o chamador deve fornecer o Room
                entry.guestRoom?.let { serialize(it) }
            }
            OfficialRoomEntryType.PUBLIC_ROOM -> {
                val publicRoom = entry.publicRoom ?: OfficialRoomPublicData()
                writeUTF(publicRoom.name)
                writeInt(publicRoom.enterBytes)
                writeInt(publicRoom.owner)
                writeUTF(publicRoom.imageNames.joinToString(","))
                // 5o campo (usersNow): R36 de 2009-08-07 (RELEASE36-20012); 6o (maxUsers): R38 de 2009-09-21 (RELEASE38-21541)
                if (isVersionAtLeast(2009, 8, 7)) writeInt(publicRoom.usersNow)
                if (isVersionAtLeast(2009, 9, 21)) writeInt(publicRoom.maxUsers)
            }
            // type 4 (pasta) so existe a partir de R37 e le um int; types fora de 1..4 nao tem payload
            OfficialRoomEntryType.FOLDER -> if (isR37) writeInt(0)
            OfficialRoomEntryType.NONE -> Unit
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
}
