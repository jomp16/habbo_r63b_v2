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

package ovh.rwx.habbo.game.user

import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.item.ItemDao
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private val log = LoggerFactory.getLogger("ovh.rwx.habbo.game.user.SessionAchievements")

fun HabboSession.processPeriodicAchievementsImpl() {
    if (isBot) return

    val totalMinutes = (userStats.totalOnlineSeconds / 60).toInt()

    log.debug(
        "Processing periodic achievements for user {} - totalOnlineSeconds: {}, totalMinutes: {}",
        userInformation.username, userStats.totalOnlineSeconds, totalMinutes
    )

    HabboServer.habboGame.achievementManager.progress(
        this,
        "ACH_AllTimeHotelPresence",
        totalMinutes,
        accumulate = false
    )

    if (habboSubscription.validUserSubscription) {
        val totalMonths = ChronoUnit.MONTHS.between(
            habboSubscription.habboClubSubscription?.activated,
            LocalDateTime.now()
        ).toInt()
        HabboServer.habboGame.achievementManager.progress(this, "ACH_BasicClub", totalMonths, accumulate = false)

        val totalDays = ChronoUnit.DAYS.between(
            habboSubscription.habboClubSubscription?.activated,
            LocalDateTime.now()
        ).toInt()
        HabboServer.habboGame.achievementManager.progress(this, "ACH_HC", totalDays, accumulate = false)
    }

    if (habboSubscription.hasBuildersClub && !habboSubscription.buildersClubSubscription.trial) {
        val totalDays = ChronoUnit.DAYS.between(
            habboSubscription.buildersClubSubscription.activated,
            LocalDateTime.now()
        ).toInt()
        HabboServer.habboGame.achievementManager.progress(this, "ACH_BuildersClub", totalDays, accumulate = false)
    }
}

fun HabboSession.processLoginAchievements() {
    sessionScope.launch {
        // LTD Purchaser & Early Bird & Credit Value & Photos
        launch {
            val allUserItemsWithRoom = ItemDao.getAllUserItemsWithRoom(userInformation.id)
            val totalLTDs = allUserItemsWithRoom.count { it.userItem.limited }
            if (totalLTDs > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    this@processLoginAchievements,
                    "ACH_LTDPurchaser",
                    totalLTDs,
                    accumulate = false
                )
            }

            val totalEarlyBirdLTDs = allUserItemsWithRoom.count { itemWithRoom ->
                val userItem = itemWithRoom.userItem
                if (!userItem.limited) return@count false
                val limitedData = userItem.limitedItemData ?: return@count false
                val earlyBirdThreshold = (limitedData.limitedTotal * 0.1).toInt()
                limitedData.limitedNumber <= earlyBirdThreshold
            }
            if (totalEarlyBirdLTDs > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    this@processLoginAchievements,
                    "ACH_LTDEarlyBird",
                    totalEarlyBirdLTDs,
                    accumulate = false
                )
            }

            val totalCreditValue = allUserItemsWithRoom
                .filter { it.roomId != null }
                .filter { it.userItem.itemName.startsWith("CF_") || it.userItem.itemName.startsWith("CFC_") }
                .sumOf { itemWithRoom ->
                    val split = itemWithRoom.userItem.itemName.split('_')
                    if (split.size > 2 && split[1] == "diamond") {
                        split[2].toIntOrNull() ?: 0
                    } else if (split.size > 1) {
                        split[1].toIntOrNull() ?: 0
                    } else {
                        0
                    }
                }
            if (totalCreditValue > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    this@processLoginAchievements,
                    "ACH_PlaceCreditValue",
                    totalCreditValue,
                    accumulate = false
                )
            }

            val totalPhotos = allUserItemsWithRoom.count {
                it.userItem.itemName == "external_image_wallitem_poster_small"
            }
            if (totalPhotos > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    this@processLoginAchievements,
                    "ACH_CameraPhotoCount",
                    totalPhotos,
                    accumulate = false
                )
            }
        }

        // Room decoration achievements
        launch {
            val userRooms =
                HabboServer.habboGame.roomManager.rooms.values.filter { it.roomData.ownerId == userInformation.id }

            val totalFloorChanges = userRooms.count { it.roomData.floor.isNotEmpty() && it.roomData.floor != "0" }
            if (totalFloorChanges > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    this@processLoginAchievements,
                    "ACH_RoomDecoFloor",
                    totalFloorChanges,
                    accumulate = false
                )
            }

            val totalWallpaperChanges =
                userRooms.count { it.roomData.wallpaper.isNotEmpty() && it.roomData.wallpaper != "0" }
            if (totalWallpaperChanges > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    this@processLoginAchievements,
                    "ACH_RoomDecoWallpaper",
                    totalWallpaperChanges,
                    accumulate = false
                )
            }

            val totalLandscapeChanges =
                userRooms.count { it.roomData.landscape.isNotEmpty() && it.roomData.landscape != "0" }
            if (totalLandscapeChanges > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    this@processLoginAchievements,
                    "ACH_RoomDecoLandscape",
                    totalLandscapeChanges,
                    accumulate = false
                )
            }
        }

        // Registration, Daily Login, Friends, Respect
        launch {
            val daysRegistered =
                java.time.Duration.between(userInformation.accountCreated, LocalDateTime.now()).toDays().toInt()
            HabboServer.habboGame.achievementManager.progress(
                this@processLoginAchievements,
                "ACH_RegistrationDuration",
                daysRegistered,
                accumulate = false
            )

            if (userStats.firstLoginOfDay) {
                HabboServer.habboGame.achievementManager.progress(
                    this@processLoginAchievements,
                    "ACH_Login",
                    1,
                    accumulate = true
                )
            }

            val totalFriends = habboMessenger.friends.size
            HabboServer.habboGame.achievementManager.progress(
                this@processLoginAchievements,
                "ACH_FriendListSize",
                totalFriends,
                accumulate = false
            )

            HabboServer.habboGame.achievementManager.progress(
                this@processLoginAchievements,
                "ACH_RespectEarned",
                userStats.respect,
                accumulate = false
            )
        }
    }
}
