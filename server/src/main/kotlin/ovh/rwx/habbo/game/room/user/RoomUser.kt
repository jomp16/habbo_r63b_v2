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

package ovh.rwx.habbo.game.room.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.isAir
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomChatMessageBubbles
import ovh.rwx.habbo.game.room.RoomChatType
import ovh.rwx.habbo.game.room.tasks.UserChatTask
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Vector3
import java.util.*

class RoomUser(
    val habboSession: HabboSession,
    room: Room,
    virtualID: Int,
    currentVector3: Vector3,
    headRotation: Int,
    bodyRotation: Int
) : RoomHumanoid(room, virtualID, currentVector3, headRotation, bodyRotation) {

    override fun onMovementStepCommitted() {
        HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_LegDay", 1, accumulate = true)
        // Por dar uma volta pelo Hotel (primeiro passo em um quarto)
        HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_Tutorial1", 1)
    }

    fun chat(
        virtualID: Int,
        message: String,
        bubble: RoomChatMessageBubbles,
        type: RoomChatType,
        skipCommands: Boolean
    ) {
        room.addTask(UserChatTask(this, virtualID, message, bubble, type, skipCommands))
    }

    /**
     * Serialização unificada (UsersMessageEvent / header pré-shuffle 28) — evolução completa
     * do wire por bissecção AS3 + habbo-timeline:
     * - campos comuns: id, name, motto, figure, roomIndex, x, y, z, dir, entityType.
     * - seção user pré-shuffle (≤2011-10-04): sex, xp, groupId, groupStatus, groupName,
     *   [achievementScore ≥ 2010-12-03].
     * - 2011-10-05 (1º pós-shuffle): +String entre groupName e achievementScore.
     * - 2011-10-17: xp morre (extinto entre os builds 10-05 e 10-17).
     * - 2014-01-30: +isModerator (Boolean) — o AS3 chama de isModerator; escrever
     *   hasBuildersClub ali era valor trocado.
     * - 2026-05-18 (AIR): +badgesRank (Int).
     */
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(habboSession.userInformation.id)
            writeUTF(habboSession.userInformation.username)
            writeUTF(habboSession.userInformation.motto)
            writeUTF(habboSession.userInformation.figure)
            writeInt(virtualID)
            writeInt(currentVector3.x)
            writeInt(currentVector3.y)
            writeUTF(currentVector3.z.toString())
            writeInt(bodyRotation) // dir no entry — campo 9 do objeto comum em todas as eras
            writeInt(1) // 1 for user, 2 for pet, 3 for bot.
            writeUTF(habboSession.userInformation.gender.lowercase(Locale.getDefault()))

            if (isVersionBefore(2011, 10, 17)) {
                writeInt(-1) // xp — campo extinto entre os builds 2011-10-05 e 2011-10-17
            }

            val group = habboSession.userStats.favoriteGroup

            if (group == null) {
                writeInt(-1)
                writeInt(0)
                writeUTF("")
            } else {
                writeInt(group.groupData.id)
                writeInt(0)
                writeUTF(group.groupData.name)
            }

            if (isVersionAtLeast(2011, 10, 5)) {
                writeUTF("") // String nascida no primeiro build pós-shuffle
            }

            if (isVersionAtLeast(2010, 12, 3)) {
                writeInt(habboSession.userStats.achievementScore)
            }

            if (isVersionAtLeast(2014, 1, 30)) {
                writeBoolean(habboSession.hasPermission("acc_mod_tools")) // isModerator
            }

            if (isAir && isVersionAtLeast(2026, 5, 18)) {
                writeInt(0) // badgesRank
            }
        }
    }
}
