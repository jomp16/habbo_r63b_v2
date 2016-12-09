/*
 * Copyright (C) 2016 jomp16
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
import tk.jomp16.habbo.communication.QueuedHabboResponse
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.badge.BadgeDao
import tk.jomp16.habbo.database.information.UserInformationDao
import tk.jomp16.habbo.database.information.UserPreferencesDao
import tk.jomp16.habbo.database.information.UserStatsDao
import tk.jomp16.habbo.database.item.ItemDao
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
import java.io.Closeable
import java.time.Clock
import java.time.LocalDateTime
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class HabboSession(val channel: Channel) : Closeable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

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

    val scriptEngine: ScriptEngine by lazy { ScriptEngineManager().getEngineByName("JavaScript") }

    var currentRoom: Room? = null
    var roomUser: RoomUser? = null

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
    private var alreadySentPurse: Boolean = false

    var ping: Long = 0

    fun sendHabboResponse(headerId: Int, vararg args: Any?) = HabboServer.habboHandler.invokeResponse(headerId, *args)?.let {
        sendHabboResponse(it)
    }

    fun sendHabboResponse(habboResponse: HabboResponse?, flush: Boolean = true) {
        habboResponse?.let { channel.write(it) }

        if (flush) channel.flush()
    }

    fun sendQueuedHabboResponse(queuedHabboResponse: QueuedHabboResponse) {
        queuedHabboResponse.headerIds.forEach {
            HabboServer.habboHandler.invokeResponse(it.first, *it.second)?.let {
                sendHabboResponse(it, false)
            }
        }

        // flush channel
        channel.flush()
    }

    fun sendNotification(message: String) = sendNotification(NotificationType.BROADCAST_ALERT, message)

    fun sendNotification(notificationType: NotificationType, message: String) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (notificationType) {
            NotificationType.MOTD_ALERT -> sendHabboResponse(Outgoing.MOTD_NOTIFICATION, message)
            NotificationType.BROADCAST_ALERT -> sendHabboResponse(Outgoing.BROADCAST_NOTIFICATION, message)
        }
    }

    fun sendSuperNotification(type: String, vararg strings: String) {
        if (strings.size % 2 != 0) {
            log.warn("Tried to send a super notification with an odd length of array!")

            return
        }

        sendHabboResponse(Outgoing.SUPER_NOTIFICATION, type, strings)
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

    fun hasPermission(permission: String) = HabboServer.habboGame.permissionManager.userHasPermission(userInformation.id, permission)
            || HabboServer.habboGame.permissionManager.rankHasPermission(userInformation.rank, permission)

    fun authenticate(ssoTicket: String): Boolean {
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

        habboSubscription = HabboSubscription(this)
        habboBadge = HabboBadge(this)
        habboMessenger = HabboMessenger(this)
        habboInventory = HabboInventory(this)

        UserInformationDao.saveInformation(userInformation, true, ip, "")

        return true
    }

    fun rewardUser() {
        val localDateTime = userStats.creditsLastUpdate.plusSeconds(HabboServer.habboConfig.timerConfig.creditsSeconds.toLong())

        var updateCurrency = false

        if (LocalDateTime.now(Clock.systemUTC()).isAfter(localDateTime)) {
            if (HabboServer.habboConfig.rewardConfig.creditsMax < 0 && userInformation.credits < HabboServer.habboConfig.rewardConfig.creditsMax || userInformation.credits < Int.MAX_VALUE) {
                userInformation.credits += HabboServer.habboConfig.rewardConfig.credits

                updateCurrency = true

            }

            if (HabboServer.habboConfig.rewardConfig.pixelsMax < 0 && userInformation.credits < HabboServer.habboConfig.rewardConfig.creditsMax || userInformation.pixels < Int.MAX_VALUE) {
                userInformation.pixels += HabboServer.habboConfig.rewardConfig.pixels

                updateCurrency = true
            }

            if (userInformation.vip && (HabboServer.habboConfig.rewardConfig.vipPointsMax < 0 && userInformation.vipPoints < HabboServer.habboConfig.rewardConfig.vipPoints || userInformation.vipPoints < Int.MAX_VALUE)) {
                userInformation.vipPoints += HabboServer.habboConfig.rewardConfig.vipPoints

                updateCurrency = true
            }

            userStats.creditsLastUpdate = LocalDateTime.now(Clock.systemUTC())
        }

        if (userInformation.credits < 0) userInformation.credits = Int.MAX_VALUE
        if (userInformation.pixels < 0) userInformation.pixels = Int.MAX_VALUE
        if (userInformation.vip && userInformation.vipPoints < 0) userInformation.vipPoints = Int.MAX_VALUE

        if (HabboServer.habboConfig.rewardConfig.creditsMax >= 0 && userInformation.credits > HabboServer.habboConfig.rewardConfig.creditsMax) userInformation.credits = HabboServer.habboConfig.rewardConfig.creditsMax
        if (HabboServer.habboConfig.rewardConfig.pixelsMax >= 0 && userInformation.pixels > HabboServer.habboConfig.rewardConfig.pixelsMax) userInformation.pixels = HabboServer.habboConfig.rewardConfig.pixelsMax
        if (userInformation.vip && HabboServer.habboConfig.rewardConfig.vipPointsMax >= 0 && userInformation.vipPoints > HabboServer.habboConfig.rewardConfig.vipPointsMax) userInformation.vipPoints = HabboServer.habboConfig.rewardConfig.vipPointsMax

        if (!alreadySentPurse || updateCurrency) updateAllCurrencies()
    }

    fun updateAllCurrencies() {
        if (!alreadySentPurse) alreadySentPurse = true

        val queuedHabboResponseEvent = QueuedHabboResponse()

        queuedHabboResponseEvent += Outgoing.CREDITS_BALANCE to arrayOf(userInformation.credits)
        queuedHabboResponseEvent += Outgoing.ACTIVITY_POINTS_BALANCE to arrayOf(userInformation.pixels, userInformation.vipPoints)

        sendQueuedHabboResponse(queuedHabboResponseEvent)
    }

    fun enterRoom(room: Room, password: String, bypassAuth: Boolean = false) {
        currentRoom?.removeUser(roomUser, false, false)

        if (room.roomTask == null) HabboServer.habboGame.roomManager.roomTaskManager.addRoomToTask(room)

        val queuedHabboResponse = QueuedHabboResponse()

        if (room.roomUsers.size >= room.roomData.usersMax && !room.hasRights(this, true) && !hasPermission("acc_enter_full_room")) {
            queuedHabboResponse += Outgoing.ROOM_ERROR to arrayOf(1, "")
            queuedHabboResponse += Outgoing.ROOM_EXIT to arrayOf()

            sendQueuedHabboResponse(queuedHabboResponse)

            return
        }

        val loading = !bypassAuth && !room.hasRights(this, true)

        if (loading) {
            if (room.roomData.state == RoomState.PASSWORD && !HabboServer.habboGame.passwordEncryptor.checkPassword(password, room.roomData.password)) {
                queuedHabboResponse += Outgoing.GENERIC_ERROR to arrayOf(-100002)
                queuedHabboResponse += Outgoing.ROOM_EXIT to arrayOf()

                sendQueuedHabboResponse(queuedHabboResponse)

                return
            } else if (room.roomData.state == RoomState.LOCKED) {
                val roomUsersWithRights = room.roomUsersWithRights

                if (roomUsersWithRights.isEmpty()) {
                    queuedHabboResponse += Outgoing.ROOM_DOORBELL_DENIED to arrayOf("")
                    queuedHabboResponse += Outgoing.ROOM_EXIT to arrayOf()
                } else {
                    currentRoom = room

                    roomUsersWithRights.forEach {
                        it.habboSession?.sendHabboResponse(Outgoing.ROOM_DOORBELL, userInformation.username)
                    }

                    queuedHabboResponse += Outgoing.ROOM_DOORBELL to arrayOf("")
                }

                sendQueuedHabboResponse(queuedHabboResponse)

                return
            }
        }

        currentRoom = room

        // todo: group

        queuedHabboResponse += Outgoing.ROOM_OPEN to arrayOf()
        queuedHabboResponse += Outgoing.ROOM_INITIAL_INFO to arrayOf(room.roomModel.id, room.roomData.id)

        if (room.roomData.wallpaper != "0.0") queuedHabboResponse += Outgoing.ROOM_DECORATION to arrayOf("wallpaper", room.roomData.wallpaper)
        if (room.roomData.floor != "0.0") queuedHabboResponse += Outgoing.ROOM_DECORATION to arrayOf("floor", room.roomData.floor)
        if (room.roomData.landscape != "0.0") queuedHabboResponse += Outgoing.ROOM_DECORATION to arrayOf("landscape", room.roomData.landscape)

        sendQueuedHabboResponse(queuedHabboResponse)
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

    fun saveAllQueuedStuffs() {
        ItemDao.removeRoomItems(habboInventory.roomItemsToRemove)

        habboInventory.roomItemsToRemove.clear()
    }
}