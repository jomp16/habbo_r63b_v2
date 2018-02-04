/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

import org.jasypt.util.password.PasswordEncryptor
import org.jasypt.util.password.StrongPasswordEncryptor
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.camera.CameraManager
import ovh.rwx.habbo.game.catalog.CatalogManager
import ovh.rwx.habbo.game.group.GroupManager
import ovh.rwx.habbo.game.item.ItemManager
import ovh.rwx.habbo.game.landing.LandingManager
import ovh.rwx.habbo.game.moderation.ModerationManager
import ovh.rwx.habbo.game.navigator.NavigatorManager
import ovh.rwx.habbo.game.permission.PermissionManager
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomManager
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.TimeUnit

class HabboGame {
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

    init {
        landingManager.load()
        roomManager.load()
        itemManager.load()
        catalogManager.load()
        navigatorManager.load()
        permissionManager.load()
        moderationManager.load()
        groupManager.load()
        cameraManager.load()

        HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
            HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && !it.handshaking && !it.habboSubscription.validUserSubscription }.forEach { it.habboSubscription.clearSubscription() }
        }, 0, 1, TimeUnit.MINUTES)

        if (HabboServer.habboConfig.timerConfig.creditsSeconds > 0) {
            HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
                HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && !it.handshaking }.forEach(HabboSession::rewardUser)
            }, 0, HabboServer.habboConfig.timerConfig.creditsSeconds.toLong(), TimeUnit.SECONDS)
        }

        HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
            roomManager.rooms.values.filter { it.roomTask != null }.forEach(Room::saveRoom)

            HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && !it.handshaking }.forEach { it.saveAllQueuedStuffs() }
        }, 0, HabboServer.habboConfig.roomTaskConfig.saveItemSeconds.toLong(), TimeUnit.SECONDS)
    }
}