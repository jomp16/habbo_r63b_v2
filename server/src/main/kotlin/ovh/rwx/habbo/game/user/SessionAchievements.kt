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
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.pet.PetDao
import ovh.rwx.habbo.game.achievement.CreditFurniValue
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
    HabboServer.habboGame.achievementManager.progress(this, "ACH_TradingPass2", totalMinutes, accumulate = false)

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

/**
 * Backfill: grupos derivados de estado já persistido (minutos online, idade da conta, amigos, salas visitadas).
 * Roda a cada login com `accumulate = false` (idempotente), então contas antigas e eventos ocorridos com o gatilho
 * ao vivo ainda inexistente são creditados retroativamente. `ACH_Name` vale quando `realname` está preenchido.
 */
private fun HabboSession.backfillDerivedAchievements() {
    val manager = HabboServer.habboGame.achievementManager
    val daysRegistered = ChronoUnit.DAYS.between(userInformation.accountCreated, LocalDateTime.now()).toInt()

    if (userInformation.realname.isNotBlank()) manager.progress(this, "ACH_Name", 1, accumulate = false)
    manager.progress(this, "ACH_TradingPass2", (userStats.totalOnlineSeconds / 60).toInt(), accumulate = false)
    manager.progress(this, "ACH_TradingPass3", daysRegistered, accumulate = false)
    manager.progress(this, "ACH_TradingPass5", manager.currentProgress(this, "ACH_RoomEntry"), accumulate = false)
    manager.progress(this, "ACH_TradingPass7", habboMessenger.friends.size, accumulate = false)
    manager.progress(this, "ACH_PetLover", PetDao.countPetsByUserId(userInformation.id), accumulate = false)
    manager.completeTutorialIfDone(this)
    HabboServer.habboGame.habbiconManager.backfillAchievements(this)
}

/** Teto da espera pelo pedido de limites: clientes que nunca o fazem só atrasam o backfill por este tempo. */
private const val BADGE_LIMITS_WAIT_MS = 10_000L

fun HabboSession.processLoginAchievements() {
    sessionScope.launch {
        // Os pacotes de conquista/emblema do backfill só podem chegar depois da tabela de limites (ver
        // badgePointLimitsRequested); sem isso o cliente mostra %limit% = 0 na aba de emblemas
        if (!isBot) withTimeoutOrNull(BADGE_LIMITS_WAIT_MS) { badgePointLimitsRequested.await() }

        launch { backfillDerivedAchievements() }

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
                .sumOf { CreditFurniValue.of(it.userItem.itemName) }
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

        // Presentes: o contador persistido também cobre os presentes recebidos com o usuário offline
        launch {
            if (userStats.giftsGiven > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    this@processLoginAchievements, "ACH_GiftGiver", userStats.giftsGiven, accumulate = false
                )
            }
            if (userStats.giftsReceived > 0) {
                HabboServer.habboGame.achievementManager.progress(
                    this@processLoginAchievements, "ACH_GiftReceiver", userStats.giftsReceived, accumulate = false
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

            // Dias seguidos: perder um dia zera a sequência
            if (userStats.firstLoginOfDay) {
                if (!userStats.loginStreakContinues) {
                    HabboServer.habboGame.achievementManager.resetProgress(this@processLoginAchievements, "ACH_Login")
                }
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
