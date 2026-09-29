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

package ovh.rwx.habbo.game

import kotlinx.coroutines.launch
import org.jasypt.util.password.PasswordEncryptor
import org.jasypt.util.password.StrongPasswordEncryptor
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.achievement.AchievementManager
import ovh.rwx.habbo.game.badge.BadgeManager
import ovh.rwx.habbo.game.camera.CameraManager
import ovh.rwx.habbo.game.catalog.CatalogManager
import ovh.rwx.habbo.game.chest.ChestManager
import ovh.rwx.habbo.game.figure.FigureManager
import ovh.rwx.habbo.game.group.GroupManager
import ovh.rwx.habbo.game.habbicon.HabbiconManager
import ovh.rwx.habbo.game.item.ItemManager
import ovh.rwx.habbo.game.landing.LandingManager
import ovh.rwx.habbo.game.moderation.ModerationManager
import ovh.rwx.habbo.game.navigator.NavigatorManager
import ovh.rwx.habbo.game.permission.PermissionManager
import ovh.rwx.habbo.game.pet.PetManager
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomManager
import ovh.rwx.habbo.game.snowwar.SnowWarManager
import java.util.concurrent.TimeUnit

class HabboGame {
    @Suppress("unused")
    private val log = LoggerFactory.getLogger(javaClass)

    val passwordEncryptor: PasswordEncryptor = StrongPasswordEncryptor()
    val landingManager: LandingManager = LandingManager()
    val roomManager: RoomManager = RoomManager()
    val itemManager: ItemManager = ItemManager()
    val catalogManager: CatalogManager = CatalogManager()
    val navigatorManager: NavigatorManager = NavigatorManager()
    val permissionManager: PermissionManager = PermissionManager()
    val moderationManager: ModerationManager = ModerationManager()
    val groupManager: GroupManager = GroupManager()
    val cameraManager: CameraManager = CameraManager()
    val achievementManager: AchievementManager = AchievementManager()
    val figureManager: FigureManager = FigureManager()
    val petManager: PetManager = PetManager()
    val badgeManager: BadgeManager = BadgeManager()
    val habbiconManager: HabbiconManager = HabbiconManager()
    val chestManager: ChestManager = ChestManager()
    val snowWarManager: SnowWarManager = SnowWarManager()

    init {
        HabboServer.applicationScope.launch { landingManager.load() }
        HabboServer.applicationScope.launch { itemManager.load() }
        HabboServer.applicationScope.launch { catalogManager.load() }
        HabboServer.applicationScope.launch { navigatorManager.load() }
        HabboServer.applicationScope.launch { permissionManager.load() }
        HabboServer.applicationScope.launch { moderationManager.load() }
        HabboServer.applicationScope.launch { groupManager.load() }
        HabboServer.applicationScope.launch { achievementManager.load() }
        HabboServer.applicationScope.launch { figureManager.load() }
        HabboServer.applicationScope.launch { petManager.load() }
        HabboServer.applicationScope.launch { badgeManager.load() }
        HabboServer.applicationScope.launch { habbiconManager.load() }
        HabboServer.applicationScope.launch { snowWarManager.load() }

        // 1. Guardamos a referência do Job do RoomManager
        val roomJob = HabboServer.applicationScope.launch { roomManager.load() }

        // 2. Lançamos o CameraManager, mas mandamos ele esperar o RoomManager terminar
        HabboServer.applicationScope.launch {
            roomJob.join() // Suspende essa coroutine até que roomManager.load() termine
            cameraManager.load()
        }

        HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
            HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && !it.handshaking && !it.habboSubscription.validUserSubscription }
                .forEach { HabboServer.applicationScope.launch { it.habboSubscription.clearHabboClub() } }
        }, 0, 1, TimeUnit.MINUTES)

        if (HabboServer.habboConfig.timerConfig.creditsSeconds > 0) {
            HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
                HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && !it.handshaking }
                    .forEach { HabboServer.applicationScope.launch { it.rewardUser() } }
            }, 0, HabboServer.habboConfig.timerConfig.creditsSeconds.toLong(), TimeUnit.SECONDS)
        }

        HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
            HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && !it.handshaking }
                .forEach { HabboServer.applicationScope.launch { it.processPeriodicAchievements() } }
        }, 0, 1, TimeUnit.MINUTES)

        HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
            roomManager.rooms.values.filter { it.running }.forEach(Room::saveRoom)

            HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && !it.handshaking }
                .forEach { HabboServer.applicationScope.launch { it.saveAllQueuedStuffs() } }

            achievementManager.saveQueuedAchievements()
        }, 0, HabboServer.habboConfig.roomTaskConfig.saveItemSeconds.toLong(), TimeUnit.SECONDS)
    }
}