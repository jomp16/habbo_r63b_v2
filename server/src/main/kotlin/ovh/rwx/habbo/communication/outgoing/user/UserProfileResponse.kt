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
import ovh.rwx.habbo.communication.isAir
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.information.UserStats
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil

@Suppress("unused", "UNUSED_PARAMETER")
class UserProfileResponse {
    @Response(Outgoing.USER_PROFILE)
    fun response(
        habboResponse: HabboResponse,
        userInformation: UserInformation,
        userStats: UserStats,
        showProfile: Boolean,
        friends: Int,
        isFriend: Boolean,
        isRequest: Boolean,
        isOnline: Boolean
    ) {
        habboResponse.apply {
            val groups = userInformation.groups

            writeInt(userInformation.id)
            writeUTF(userInformation.username)
            writeUTF(userInformation.figure)
            writeUTF(userInformation.motto)
            writeUTF(userInformation.accountCreated.format(HabboServer.DATE_TIME_FORMATTER_ONLY_DAYS))
            writeInt(userStats.achievementScore)
            writeInt(friends)
            writeBoolean(isFriend)
            writeBoolean(isRequest)

            // onlineStatus — Boolean no Flash/AIR antigo, Byte no AIR >= 2026-05-18
            if (isAir && isVersionAtLeast(2026, 5, 18)) {
                writeByte(
                    when {
                        // isHidden  -> 2  // online, oculto
                        isOnline -> 1
                        else -> 0
                    }
                ) // onlineStatus
            } else {
                writeBoolean(isOnline)
            }

            writeInt(groups.size)

            groups.forEach { group ->
                writeInt(group.groupData.id)              // _groupId
                writeUTF(group.groupData.name)            // _groupName
                writeUTF(group.groupData.badge)           // _SafeStr_7119 (badgeCode)
                writeUTF(group.groupData.symbolColor.toString())    // _primaryColor (String Hex)
                writeUTF(group.groupData.backgroundColor.toString())  // _secondaryColor (String Hex)
                writeBoolean(userStats.favoriteGroupId == group.groupData.id) // _SafeStr_9334 (favourite)
                writeInt(group.groupData.ownerId)         // _SafeStr_6449 (ownerId)
                writeBoolean(true)    // todo: _SafeStr_8641 (hasForum)
            }

            writeInt(
                ceil(
                    Instant.now().epochSecond.toDouble() - userStats.lastOnline.atZone(ZoneId.systemDefault())
                        .toEpochSecond().toDouble()
                ).toInt()
            )
            writeBoolean(showProfile)

            if (isAir) {
                // AIR-only desde 2021-03-17 (Flash nunca leu esses campos)
                if (isVersionAtLeast(2021, 3, 17)) {
                    writeBoolean(false) // isHidden
                    writeInt(0) // accountLevel
                    writeInt(0) // identityLevel
                    writeInt(0) // starGemCount
                    writeBoolean(false) // friendRequestsEnabled
                    writeBoolean(false) // banned
                }

                // AIR-only desde 2026-05-18
                if (isVersionAtLeast(2026, 5, 18)) {
                    val totalBadges = HabboServer.habboGame.badgeManager.getBadgeCount(userInformation.id)
                    val achievementLevel = HabboServer.habboGame.achievementManager.userAchievements[userInformation.id]
                        ?.sumOf { it.level } ?: 0

                    writeInt(totalBadges) // totalBadges
                    writeInt(achievementLevel) // achievementLevel
                    writeInt(0) // TODO: badgeRarityCounts.size (lista vazia)
                    // badgeRarityCounts.forEach { writeByte(rarityId); writeInt(count) }
                    writeInt(0) // TODO: totalBadgesRank
                }
            }
        }
    }
}