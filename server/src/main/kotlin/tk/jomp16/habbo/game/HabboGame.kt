/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.game

import org.jasypt.util.password.PasswordEncryptor
import org.jasypt.util.password.StrongPasswordEncryptor
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.camera.CameraManager
import tk.jomp16.habbo.game.catalog.CatalogManager
import tk.jomp16.habbo.game.group.GroupManager
import tk.jomp16.habbo.game.item.ItemManager
import tk.jomp16.habbo.game.landing.LandingManager
import tk.jomp16.habbo.game.moderation.ModerationManager
import tk.jomp16.habbo.game.navigator.NavigatorManager
import tk.jomp16.habbo.game.permission.PermissionManager
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.RoomManager
import tk.jomp16.habbo.game.user.HabboSession
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
        HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
            HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && !it.handshaking && !it.habboSubscription.validUserSubscription }.forEach { it.habboSubscription.clearSubscription() }
        }, 0, 1, TimeUnit.MINUTES)

        if (HabboServer.habboConfig.timerConfig.creditsSeconds > 0) {
            HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
                HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && !it.handshaking }.forEach(HabboSession::rewardUser)
            }, 0, HabboServer.habboConfig.timerConfig.creditsSeconds.toLong(), TimeUnit.SECONDS)
        }

        HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
            roomManager.rooms.values.filter { it.roomTask != null }.forEach(Room::saveQueuedItems)

            HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && !it.handshaking }.forEach { it.saveAllQueuedStuffs() }
        }, 0, HabboServer.habboConfig.roomTaskConfig.saveItemSeconds.toLong(), TimeUnit.SECONDS)
    }
}