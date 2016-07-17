/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.game.user

import io.netty.channel.Channel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.QueuedHabboResponse
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.information.UserInformationDao
import tk.jomp16.habbo.database.information.UserPreferencesDao
import tk.jomp16.habbo.database.information.UserStatsDao
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
import java.time.LocalDateTime

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

    lateinit var rooms: MutableList<Room>
        private set

    var currentRoom: Room? = null
    var roomUser: RoomUser? = null

    val authenticated: Boolean
        get() {
            try {
                return userInformation.id > 0
            } catch (exception: UninitializedPropertyAccessException) {
                return false
            }
        }

    var rc4Encryption: RC4Encryption? = null

    var uniqueID: String = ""
    private var alreadySentPurse: Boolean = false

    fun sendHabboResponse(headerId: Int, vararg args: Any) = HabboServer.habboHandler.invokeResponse(headerId,
                                                                                                     *args)?.let {
        sendHabboResponse(it)
    }

    fun sendHabboResponse(habboResponse: HabboResponse?, flush: Boolean = true) {
        habboResponse?.let { channel.write(it) }

        if (flush) channel.flush()
    }

    fun sendQueuedHabboResponse(queuedHabboResponse: QueuedHabboResponse) {
        queuedHabboResponse.headerIds.forEach {
            HabboServer.habboHandler.invokeResponse(it.key, *it.value)?.let {
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
            NotificationType.MOTD_ALERT      -> sendHabboResponse(Outgoing.MOTD_NOTIFICATION, message)
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

    fun authenticate(ssoTicket: String): Int {
        val ip = channel.ip()

        val userInformation1 = UserInformationDao.getUserInformationByAuthTicket(ssoTicket) ?: return 2

        if (HabboServer.habboSessionManager.containsHabboSessionById(userInformation1.id)) {
            val habboSession = HabboServer.habboSessionManager.getHabboSessionById(userInformation1.id)

            habboSession?.sendNotification("An user tried to login as you!\n\nIP: $ip")

            return 1
        }

        userInformation = userInformation1
        userStats = UserStatsDao.getUserStats(userInformation.id)
        userPreferences = UserPreferencesDao.getUserPreferences(userInformation.id)

        userStats.lastOnline = LocalDateTime.now()

        habboSubscription = HabboSubscription(this)
        habboBadge = HabboBadge(this)
        habboMessenger = HabboMessenger(this)
        habboInventory = HabboInventory(this)

        rooms = HabboServer.habboGame.roomManager.rooms.values.filter { it.hasRights(this, true) }.toMutableList()

        // TODO: move dis to database DAO
        HabboServer.database {
            update("UPDATE users SET auth_ticket = :ticket, online = :online, ip_last = :ip_last WHERE id = :id",
                   mapOf(
                           "ticket" to "",
                           "online" to true,
                           "ip_last" to ip,
                           "id" to userInformation.id
                   )
            )
        }

        return 0
    }

    fun rewardUser() {
        val localDateTime = userStats.creditsLastUpdate.plusSeconds(
                HabboServer.habboConfig.timerConfig.creditsSeconds.toLong())

        var updatePixels = false
        var updateCredits = false

        if (LocalDateTime.now().isAfter(localDateTime)) {
            if (userInformation.credits < HabboServer.habboConfig.rewardConfig.creditsMax) {
                userInformation.credits += HabboServer.habboConfig.rewardConfig.credits

                if (userInformation.credits > HabboServer.habboConfig.rewardConfig.creditsMax) userInformation.credits = HabboServer.habboConfig.rewardConfig.creditsMax

                updateCredits = true
            }

            if (userInformation.pixels < HabboServer.habboConfig.rewardConfig.pixelsMax) {
                userInformation.pixels += HabboServer.habboConfig.rewardConfig.pixels

                if (userInformation.pixels > HabboServer.habboConfig.rewardConfig.pixelsMax) userInformation.pixels = HabboServer.habboConfig.rewardConfig.pixelsMax

                updatePixels = true
            }

            if (userInformation.vip && userInformation.vipPoints < HabboServer.habboConfig.rewardConfig.vipPointsMax) {
                userInformation.vipPoints += HabboServer.habboConfig.rewardConfig.vipPoints

                if (userInformation.vipPoints > HabboServer.habboConfig.rewardConfig.vipPointsMax) userInformation.vipPoints = HabboServer.habboConfig.rewardConfig.vipPointsMax

                updatePixels = true
            }

            if (updateCredits || updatePixels) userStats.creditsLastUpdate = LocalDateTime.now()
            if (!alreadySentPurse || updateCredits && updatePixels) updateAllCurrencies()
            else if (updateCredits) sendHabboResponse(Outgoing.CREDITS_BALANCE, userInformation.credits)
            else if (updatePixels) sendHabboResponse(Outgoing.ACTIVITY_POINTS_BALANCE, userInformation.pixels,
                                                     userInformation.vipPoints)
        } else if (!alreadySentPurse) updateAllCurrencies()
    }

    fun updateAllCurrencies() {
        if (!alreadySentPurse) alreadySentPurse = true

        val queuedHabboResponseEvent = QueuedHabboResponse()

        queuedHabboResponseEvent += Outgoing.CREDITS_BALANCE to arrayOf(userInformation.credits)
        queuedHabboResponseEvent += Outgoing.ACTIVITY_POINTS_BALANCE to arrayOf(userInformation.pixels,
                                                                                userInformation.vipPoints)

        sendQueuedHabboResponse(queuedHabboResponseEvent)
    }

    fun enterRoom(room: Room, password: String, bypassAuth: Boolean = false) {
        currentRoom?.removeUser(roomUser, true, true)

        if (room.roomTask == null) HabboServer.habboGame.roomManager.roomTaskManager.addRoomToTask(room)

        val queuedHabboResponse = QueuedHabboResponse()

        if (room.roomUsers.size >= room.roomData.usersMax && !room.hasRights(this,
                                                                             true) /*&& !hasPermission("acc_enter_full_room")*/) {
            queuedHabboResponse += Outgoing.ROOM_ERROR to arrayOf(1, "")
            queuedHabboResponse += Outgoing.ROOM_EXIT to arrayOf()

            sendQueuedHabboResponse(queuedHabboResponse)

            return
        }

        val loading = !bypassAuth && !room.hasRights(this, true)

        if (loading) {
            if (room.roomData.state == RoomState.PASSWORD
                    && !HabboServer.habboGame.passwordEncryptor.checkPassword(password, room.roomData.password)) {
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

        if (room.roomData.wallpaper != "0.0") queuedHabboResponse += Outgoing.ROOM_DECORATION to arrayOf("wallpaper",
                                                                                                         room.roomData.wallpaper)
        if (room.roomData.floor != "0.0") queuedHabboResponse += Outgoing.ROOM_DECORATION to arrayOf("floor",
                                                                                                     room.roomData.floor)
        if (room.roomData.landscape != "0.0") queuedHabboResponse += Outgoing.ROOM_DECORATION to arrayOf("landscape",
                                                                                                         room.roomData.landscape)

        sendQueuedHabboResponse(queuedHabboResponse)
    }

    override fun close() {
        if (authenticated) {
            currentRoom?.removeUser(roomUser, false, false)

            // TODO: move everything to database DAO
            HabboServer.database {
                update("UPDATE users SET online = :online, credits = :credits, pixels = :pixels, vip_points = :vip_points, " +
                               "figure = :figure, gender = :gender, motto = :motto, home_room = :home_room WHERE id = :id",
                       mapOf(
                               "online" to false,
                               "credits" to userInformation.credits,
                               "pixels" to userInformation.pixels,
                               "vip_points" to userInformation.vipPoints,
                               "figure" to userInformation.figure,
                               "gender" to userInformation.gender,
                               "motto" to userInformation.motto,
                               "home_room" to userInformation.homeRoom,
                               "id" to userInformation.id
                       )
                )

                update("UPDATE users_preferences SET volume = :volume, prefer_old_chat = :prefer_old_chat, " +
                               "ignore_room_invite = :ignore_room_invite, disable_camera_follow = :disable_camera_follow, " +
                               "navigator_x = :navigator_x, navigator_y = :navigator_y, navigator_width = :navigator_width, " +
                               "navigator_height = :navigator_height, hide_in_room = :hide_in_room, block_new_friends = :block_new_friends, " +
                               "chat_color = :chat_color, friend_bar_open = :friend_bar_open WHERE id = :id",
                       mapOf(
                               "volume" to userPreferences.volume,
                               "prefer_old_chat" to userPreferences.preferOldChat,
                               "ignore_room_invite" to userPreferences.ignoreRoomInvite,
                               "disable_camera_follow" to userPreferences.disableCameraFollow,
                               "navigator_x" to userPreferences.navigatorX,
                               "navigator_y" to userPreferences.navigatorY,
                               "navigator_width" to userPreferences.navigatorWidth,
                               "navigator_height" to userPreferences.navigatorHeight,
                               "hide_in_room" to userPreferences.hideInRoom,
                               "block_new_friends" to userPreferences.blockNewFriends,
                               "chat_color" to userPreferences.chatColor,
                               "friend_bar_open" to userPreferences.friendBarOpen,
                               "id" to userPreferences.id
                       )
                )

                update("UPDATE users_stats SET last_online = :last_online, credits_last_update = :credits_last_update, " +
                               "favorite_group = :favorite_group, online_seconds = :online_seconds, respect = :respect, " +
                               "daily_respect_points = :daily_respect_points, daily_pet_respect_points = :daily_pet_respect_points, " +
                               "respect_last_update = :respect_last_update, marketplace_tickets = :marketplace_tickets WHERE id = :id",
                       mapOf(
                               "last_online" to LocalDateTime.now(),
                               "credits_last_update" to userStats.creditsLastUpdate,
                               "favorite_group" to userStats.favoriteGroup,
                               "online_seconds" to userStats.totalOnlineSeconds,
                               "respect" to userStats.respect,
                               "daily_respect_points" to userStats.dailyRespectPoints,
                               "daily_pet_respect_points" to userStats.dailyPetRespectPoints,
                               "respect_last_update" to userStats.respectLastUpdate,
                               "marketplace_tickets" to userStats.marketplaceTickets,
                               "id" to userStats.id
                       )
                )
            }

            habboMessenger.notifyFriends()
        }
    }
}