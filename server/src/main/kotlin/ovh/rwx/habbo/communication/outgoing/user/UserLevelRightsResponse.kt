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

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

data class UserLevelRightsData(
    val club: Int,
    val rank: Int,
    val ambassador: Boolean
)

@Suppress("unused", "UNUSED_PARAMETER")
class UserLevelRightsResponse {
    @Response(Outgoing.USER_RIGHTS)
    @ResponseR63A(OutgoingR63A.USER_RIGHTS)
    fun response(habboResponse: HabboResponse, data: UserLevelRightsData) {
        habboResponse.apply {
            // =========================================================================
            // 1. ERA PRÉ-HISTÓRICA (Fuserights - RELEASE34 até meados de RELEASE63 em 2011)
            // =========================================================================
            if (isVersionBefore(2011, 3, 7)) {
                val fuses = resolveFuserights(data.club, data.rank)

                if (isVersionBefore(2010, 4, 7)) {
                    // Releases antigas (R34 a R48): lê strings até encontrar uma vazia ("")
                    fuses.forEach { writeUTF(it) }
                    writeUTF("") // Terminador que quebra o while do client
                } else {
                    // A partir da R49/R50 até 2011-03-07: mudou para contagem explícita [count: Int + strings]
                    writeInt(fuses.size)
                    fuses.forEach { writeUTF(it) }
                }

                return@apply
            }

            // =========================================================================
            // 2. ERA MODERNA (RELEASE63 20110307+ até o AIR de 2026)
            // Pacote enxuto: clubLevel (Int) + securityLevel/rank (Int) + [ambassador: Boolean]
            // =========================================================================
            writeInt(data.club)
            writeInt(data.rank)

            // Flag de Embaixador adicionada a partir do PRODUCTION-201512012203
            if (isVersionAtLeast(2015, 12, 1)) {
                writeBoolean(data.ambassador)
            }
        }
    }

    private fun resolveFuserights(club: Int, rank: Int): List<String> {
        val fuses = mutableListOf(
            "fuse_trade",
            "fuse_room_queue_default"
        )

        // Habbo Club (Nível 1+)
        if (club >= 1) {
            fuses.addAll(
                listOf(
                    "fuse_use_club_dance",
                    "fuse_use_club_outfits",
                    "fuse_use_wardrobe",
                    "fuse_extended_buddylist",
                    "fuse_room_queue_club",
                    "fuse_use_special_room_layouts",
                    "fuse_can_create_more_rooms",
                    "fuse_habbo_chooser",
                    "fuse_furni_chooser"
                )
            )
        }

        // Habbo VIP (Nível 2)
        if (club >= 2) {
            fuses.addAll(
                listOf(
                    "fuse_use_vip_outfits",
                    "fuse_use_vip_room_layouts",
                    "fuse_larger_wardrobe",
                    "fuse_super_extended_buddylist",
                    "fuse_hide_room_walls"
                )
            )
        }

        // Moderação / Staff
        if (rank >= 4) {
            fuses.add("fuse_tour")
        }

        if (rank >= 5) {
            fuses.add("fuse_any_room_controller")
            fuses.add("fuse_cancel_roomevent")
            fuses.add("fuse_time")
        }

        return fuses
    }
}