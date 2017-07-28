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

package tk.jomp16.habbo.game.user

import io.netty.channel.Channel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.communication.outgoing.misc.MiscGenericErrorResponse
import tk.jomp16.habbo.database.badge.BadgeDao
import tk.jomp16.habbo.database.information.UserInformationDao
import tk.jomp16.habbo.database.information.UserPreferencesDao
import tk.jomp16.habbo.database.information.UserStatsDao
import tk.jomp16.habbo.database.item.ItemDao
import tk.jomp16.habbo.database.room.RoomDao
import tk.jomp16.habbo.encryption.RC4Encryption
import tk.jomp16.habbo.game.misc.NotificationType
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.RoomState
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.game.user.badge.HabboBadge
import tk.jomp16.habbo.game.user.information.UserInformation
import tk.jomp16.habbo.game.user.information.UserPreferences
import tk.jomp16.habbo.game.user.information.UserStats
import tk.jomp16.habbo.game.user.inventory.HabboInventory
import tk.jomp16.habbo.game.user.messenger.HabboMessenger
import tk.jomp16.habbo.game.user.subscription.HabboSubscription
import tk.jomp16.habbo.kotlin.ip
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.crypto.spec.DHParameterSpec
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class HabboSession(val channel: Channel) : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val incomingExecutor: Executor = Executors.newSingleThreadExecutor()
    val outgoingExecutor: Executor = Executors.newSingleThreadExecutor()
    lateinit var release: String
    lateinit var diffieHellmanParams: DHParameterSpec
    lateinit var userInformation: UserInformation
        private set
    lateinit var userStats: UserStats
        private set
    lateinit var userPreferences: UserPreferences
        private set
    lateinit var habboSubscription: HabboSubscription
        private set
    lateinit var habboBadge: HabboBadge
        private set
    lateinit var habboMessenger: HabboMessenger
        private set
    lateinit var habboInventory: HabboInventory
        private set
    val rooms: List<Room>
        get() = HabboServer.habboGame.roomManager.rooms.values.filter { it.hasRights(this, true) }
    lateinit var favoritesRooms: MutableList<Pair<Int, Int>>
        private set
    val scriptEngine: ScriptEngine by lazy { ScriptEngineManager().getEngineByName("JavaScript") }
    var handshaking: Boolean = false
    var currentRoom: Room? = null
    var roomUser: RoomUser? = null
    var targetTeleporterId: Int = -1
    var teleportRoom: Room? = null
    val teleporting: Boolean
        get() = targetTeleporterId != -1
    val authenticated: Boolean
        get() {
            try {
                return userInformation.id > 0 && userStats.id > 0 && userPreferences.id > 0
            } catch (exception: UninitializedPropertyAccessException) {
                return false
            }
        }
    var rc4Encryption: RC4Encryption? = null
    var uniqueID: String = ""
    var ping: Long = 0

    fun sendHabboResponse(outgoing: Outgoing, vararg args: Any?) = outgoingExecutor.execute {
        HabboServer.habboHandler.invokeResponse(this, outgoing, *args)?.let {
            sendHabboResponse(it)
        }
    }

    fun sendHabboResponse(habboResponse: HabboResponse?) {
        habboResponse?.let { channel.writeAndFlush(it) }
    }

    fun sendNotification(message: String) = sendNotification(NotificationType.BROADCAST_ALERT, message)

    fun sendNotification(notificationType: NotificationType, message: String) {
        when (notificationType) {
            NotificationType.MOTD_ALERT -> sendHabboResponse(Outgoing.MISC_MOTD_NOTIFICATION, message)
            NotificationType.BROADCAST_ALERT -> sendHabboResponse(Outgoing.MISC_BROADCAST_NOTIFICATION, message)
        }
    }

    fun sendSuperNotification(type: String, vararg strings: String) {
        if (strings.size % 2 != 0) {
            log.warn("Tried to send a super notification with an odd length of array!")

            return
        }

        sendHabboResponse(Outgoing.MISC_SUPER_NOTIFICATION, type, strings)
    }

    fun sendSettings() {
        val tmp = userPreferences.volume.split(',').map(String::toInt)

        sendHabboResponse(Outgoing.USER_SETTINGS,
                tmp[0],
                tmp[1],
                tmp[2],
                userPreferences.preferOldChat,
                userPreferences.ignoreRoomInvite,
                userPreferences.disableCameraFollow,
                userPreferences.friendBarOpen,
                userPreferences.chatColor
        )
    }

    fun hasPermission(permission: String) =
            if (HabboServer.habboGame.permissionManager.userHasCustomPermission(userInformation.id)) HabboServer.habboGame.permissionManager.userHasPermission(userInformation.id, permission)
            else HabboServer.habboGame.permissionManager.rankHasPermission(userInformation.rank, permission)

    internal fun authenticate(ssoTicket: String): Boolean {
        val ip = channel.ip()
        val userInformation1 = UserInformationDao.getUserInformationByAuthTicket(ssoTicket) ?: return false

        if (HabboServer.habboSessionManager.containsHabboSessionById(userInformation1.id)) {
            val habboSession = HabboServer.habboSessionManager.getHabboSessionById(userInformation1.id)

            habboSession?.sendNotification("An user tried to login as you!\n\nIP: $ip")

            return false
        }

        userInformation = userInformation1
        userStats = UserStatsDao.getUserStats(userInformation.id)
        userPreferences = UserPreferencesDao.getUserPreferences(userInformation.id)

        userStats.lastOnline = LocalDateTime.now(Clock.systemUTC())

        favoritesRooms = RoomDao.getFavoritesRooms(userInformation.id).toMutableList()

        HabboServer.serverExecutor.execute {
            habboMessenger = HabboMessenger(this)
            habboSubscription = HabboSubscription(this)
            habboBadge = HabboBadge(this)
            habboInventory = HabboInventory(this)

            habboSubscription.load()
            habboBadge.load()
            habboInventory.load()

            sendHabboResponse(Outgoing.INVENTORY_UPDATE) // notify the user that the inventory was loaded
        }

        UserInformationDao.saveInformation(userInformation, true, ip, "")

        return true
    }

    internal fun rewardUser() {
        val localDateTime = userStats.creditsLastUpdate.plusSeconds(HabboServer.habboConfig.timerConfig.creditsSeconds.toLong())
        var update = false

        if (LocalDateTime.now(Clock.systemUTC()).isAfter(localDateTime)) {
            if (HabboServer.habboConfig.rewardConfig.creditsMax < 0 && HabboServer.habboConfig.rewardConfig.credits > 0
                    && userInformation.credits < Int.MAX_VALUE) {
                userInformation.credits += HabboServer.habboConfig.rewardConfig.credits

                update = true
            }

            if (HabboServer.habboConfig.rewardConfig.pixelsMax < 0 && HabboServer.habboConfig.rewardConfig.pixels > 0
                    && userInformation.pixels < Int.MAX_VALUE) {
                userInformation.pixels += HabboServer.habboConfig.rewardConfig.pixels

                update = true
            }

            if (userInformation.vip && HabboServer.habboConfig.rewardConfig.vipPointsMax < 0
                    && HabboServer.habboConfig.rewardConfig.vipPoints > 0
                    && userInformation.vipPoints < Int.MAX_VALUE) {
                userInformation.vipPoints += HabboServer.habboConfig.rewardConfig.vipPoints

                update = true
            }
        }

        if (update) {
            userStats.creditsLastUpdate = LocalDateTime.now(Clock.systemUTC())

            updateAllCurrencies()
        }
    }

    fun updateAllCurrencies() {
        if (userInformation.credits < 0) userInformation.credits = Int.MAX_VALUE
        if (userInformation.pixels < 0) userInformation.pixels = Int.MAX_VALUE
        if (userInformation.vip && userInformation.vipPoints < 0) userInformation.vipPoints = Int.MAX_VALUE

        if (HabboServer.habboConfig.rewardConfig.creditsMax >= 0 && userInformation.credits > HabboServer.habboConfig.rewardConfig.creditsMax) userInformation.credits = HabboServer.habboConfig.rewardConfig.creditsMax
        if (HabboServer.habboConfig.rewardConfig.pixelsMax >= 0 && userInformation.pixels > HabboServer.habboConfig.rewardConfig.pixelsMax) userInformation.pixels = HabboServer.habboConfig.rewardConfig.pixelsMax
        if (userInformation.vip && HabboServer.habboConfig.rewardConfig.vipPointsMax >= 0 && userInformation.vipPoints > HabboServer.habboConfig.rewardConfig.vipPointsMax) userInformation.vipPoints = HabboServer.habboConfig.rewardConfig.vipPointsMax

        sendHabboResponse(Outgoing.CREDITS_BALANCE, userInformation.credits)
        sendHabboResponse(Outgoing.ACTIVITY_POINTS_BALANCE, userInformation.pixels, userInformation.vipPoints)
    }

    fun enterRoom(room: Room, password: String = "", bypassAuth: Boolean = false) {
        currentRoom?.removeUser(roomUser, false, false)

        if (room.roomTask == null) HabboServer.habboGame.roomManager.roomTaskManager.addRoomToTask(room)

        if (room.roomUsers.size >= room.roomData.usersMax && !room.hasRights(this, true) && !hasPermission("acc_enter_full_room")) {
            sendHabboResponse(Outgoing.ROOM_ERROR, 1, "")
            sendHabboResponse(Outgoing.ROOM_EXIT)

            return
        }
        val loading = !bypassAuth && !room.hasRights(this, true)

        if (loading) {
            if (room.roomData.state == RoomState.PASSWORD && !HabboServer.habboGame.passwordEncryptor.checkPassword(password, room.roomData.password)) {
                sendHabboResponse(Outgoing.MISC_GENERIC_ERROR, MiscGenericErrorResponse.MiscGenericError.WRONG_PASSWORD)
                sendHabboResponse(Outgoing.ROOM_EXIT)

                return
            } else if (room.roomData.state == RoomState.LOCKED) {
                val roomUsersWithRights = room.roomUsersWithRights

                if (roomUsersWithRights.isEmpty()) {
                    sendHabboResponse(Outgoing.ROOM_DOORBELL_DENIED, "")
                    sendHabboResponse(Outgoing.ROOM_EXIT)
                } else {
                    currentRoom = room

                    roomUsersWithRights.forEach {
                        it.habboSession?.sendHabboResponse(Outgoing.ROOM_DOORBELL, userInformation.username)
                    }

                    sendHabboResponse(Outgoing.ROOM_DOORBELL, "")
                }

                return
            }
        }

        currentRoom = room

        // todo: group
        sendHabboResponse(Outgoing.ROOM_OPEN)
        sendHabboResponse(Outgoing.ROOM_INITIAL_INFO, room.roomModel.id, room.roomData.id)

        if (room.roomData.wallpaper != "0.0") sendHabboResponse(Outgoing.ROOM_DECORATION, "wallpaper", room.roomData.wallpaper)
        if (room.roomData.floor != "0.0") sendHabboResponse(Outgoing.ROOM_DECORATION, "floor", room.roomData.floor)
        if (room.roomData.landscape != "0.0") sendHabboResponse(Outgoing.ROOM_DECORATION, "landscape", room.roomData.landscape)
    }

    override fun close() {
        if (authenticated) {
            currentRoom?.removeUser(roomUser, false, false)

            BadgeDao.saveBadges(habboBadge.badges.values)
            UserInformationDao.saveInformation(userInformation, false, channel.ip(), "")
            UserPreferencesDao.savePreferences(userPreferences)
            UserStatsDao.saveStats(userStats)

            saveAllQueuedStuffs()

            habboMessenger.notifyFriends()
        }
    }

    internal fun saveAllQueuedStuffs() {
        ItemDao.removeRoomItems(habboInventory.roomItemsToRemove)

        habboInventory.roomItemsToRemove.clear()
    }
}