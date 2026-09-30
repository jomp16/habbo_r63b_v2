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

import io.netty.channel.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.HabboVersion
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.database.badge.BadgeDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.database.user.UserPreferencesDao
import ovh.rwx.habbo.database.user.UserStatsDao
import ovh.rwx.habbo.encryption.IHabboEncryption
import ovh.rwx.habbo.game.habbicon.HabboHabbicon
import ovh.rwx.habbo.game.misc.NotificationType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.badge.HabboBadge
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.information.UserPreferences
import ovh.rwx.habbo.game.user.information.UserStats
import ovh.rwx.habbo.game.user.inventory.HabboInventory
import ovh.rwx.habbo.game.user.messenger.HabboMessenger
import ovh.rwx.habbo.game.user.subscription.HabboSubscription
import ovh.rwx.habbo.kotlin.ip
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.spec.DHParameterSpec
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class HabboSession(val channel: Channel) : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val sessionJob = SupervisorJob()
    val sessionScope = CoroutineScope(Dispatchers.Default + sessionJob)
    lateinit var release: String
    val releaseInitialized get() = this::release.isInitialized
    var habboVersion: HabboVersion = HabboVersion(LocalDateTime.now(), 63)
    var r63ANewEncoding: Boolean = false
    lateinit var diffieHellmanParams: DHParameterSpec
    lateinit var userInformation: UserInformation
        internal set
    lateinit var userStats: UserStats
        internal set
    lateinit var userPreferences: UserPreferences
        internal set
    lateinit var habboSubscription: HabboSubscription
        internal set
    lateinit var habboBadge: HabboBadge
        internal set
    lateinit var habboMessenger: HabboMessenger
        internal set
    lateinit var habboInventory: HabboInventory
        internal set
    lateinit var habboHabbicon: HabboHabbicon
        internal set
    val rooms: List<Room>
        get() = HabboServer.habboGame.roomManager.rooms.values.filter {
            it.userManager.hasRights(
                this,
                ownerRight = true,
                ignorePermissionAnyRoomOwner = true
            )
        }
    lateinit var favoritesRooms: MutableList<Pair<Int, Int>>
        internal set
    val scriptEngine: ScriptEngine by lazy { ScriptEngineManager().getEngineByName("JavaScript") }
    var handshaking: Boolean = false
    var currentRoom: Room? = null
    var roomUser: RoomUser? = null
    var targetTeleportId: Int = -1
    var teleportRoom: Room? = null
    val teleporting: Boolean
        get() = targetTeleportId != -1
    val authenticated: Boolean
        get() = ::userInformation.isInitialized && userInformation.id > 0 && ::userStats.isInitialized && userStats.id > 0 && ::userPreferences.isInitialized && userPreferences.id > 0
    var rc4Encryption: IHabboEncryption? = null
    var uniqueID: String = ""
    var osInformation: String = ""
    var ping: Long = 0
    val lastCatalogOfferRequest: MutableMap<Int, Long> = ConcurrentHashMap()
    var gameSSOToken: String = ""
    var cryptoToken: String = ""
    var isBot: Boolean = false

    fun sendAnyResponse(outgoing: Any, vararg args: Any?) {
        when (outgoing) {
            is Outgoing -> sendHabboResponse(outgoing, *args)
            is OutgoingR63A -> sendHabboResponse(outgoing, *args)
            else -> log.error("Tipo de mensagem desconhecido: ${outgoing::class.simpleName}")
        }
    }

    fun sendHabboResponse(outgoing: Outgoing, vararg args: Any?) {
        if (release != "R63A") {
            HabboServer.habboHandler.invokeResponse(this@HabboSession, outgoing, *args)?.let {
                sendHabboResponse(it)
            }
        } else {
            log.error("Tried to send $outgoing to a R63A client!")
        }
    }

    fun sendHabboResponse(outgoing: OutgoingR63A, vararg args: Any?) {
        if (release == "R63A") {
            HabboServer.habboHandler.invokeResponse(this@HabboSession, outgoing, *args)?.let {
                sendHabboResponse(it)
            }
        } else {
            log.error("Tried to send $outgoing to a non R63A client!")
        }
    }

    /**
     * Envia a resposta correta de acordo com a release do cliente.
     * Evita if/else nos handlers quando há dois enum variants para a mesma mensagem lógica ou pacotes exclusivos de uma release.
     */
    fun sendResponse(outgoing: Outgoing?, outgoingR63A: OutgoingR63A?, vararg args: Any?) {
        if (release == "R63A") {
            if (outgoingR63A != null) {
                sendHabboResponse(outgoingR63A, *args)
            }
        } else {
            if (outgoing != null) {
                sendHabboResponse(outgoing, *args)
            }
        }
    }

    fun sendHabboResponse(habboResponse: HabboResponse?) {
        habboResponse?.let {
            if (channel.isActive) {
                channel.writeAndFlush(it)
            } else {
                // Channel already closed/disconnecting: release the pooled
                // ByteBuf ourselves, otherwise it leaks (the encoder never runs).
                it.close()
            }
        }
    }

    fun sendNotification(message: String) = sendNotification(NotificationType.BROADCAST_ALERT, message)

    fun sendNotification(notificationType: NotificationType, message: String) {
        when (notificationType) {
            NotificationType.MOTD_ALERT -> {
                if (release == "R63A") {
                    sendHabboResponse(OutgoingR63A.MISC_MOTD_NOTIFICATION, message)
                } else {
                    sendHabboResponse(Outgoing.MISC_MOTD_NOTIFICATION, message)
                }
            }

            NotificationType.BROADCAST_ALERT -> {
                if (release == "R63A") {
                    sendHabboResponse(OutgoingR63A.MISC_BROADCAST_NOTIFICATION, message)
                } else {
                    sendHabboResponse(Outgoing.MISC_BROADCAST_NOTIFICATION, message)
                }
            }
        }
    }

    fun sendSuperNotification(
        type: MiscSuperNotificationResponse.MiscSuperNotificationKeys,
        parameters: Map<String, String> = emptyMap()
    ) {
        sendHabboResponse(Outgoing.MISC_SUPER_NOTIFICATION, type, parameters)
    }

    fun sendUserNotification(title: String, message: String, parameters: Map<String, String> = emptyMap()) {
        sendHabboResponse(OutgoingR63A.MISC_USER_NOTIFICATION, title, message, parameters)
    }

    fun hasPermission(permission: String) =
        if (HabboServer.habboGame.permissionManager.userHasCustomPermission(userInformation.id)) HabboServer.habboGame.permissionManager.userHasPermission(
            userInformation.id,
            permission
        )
        else HabboServer.habboGame.permissionManager.rankHasPermission(userInformation.rank, permission)

    internal fun authenticate(ssoTicket: String): Boolean {
        val botPrefix = HabboServer.habboConfig.botTicketPrefix
        if (botPrefix.isNotBlank() && ssoTicket.startsWith(botPrefix)) {
            return authenticateBot(ssoTicket)
        }

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

        if (userStats.lastOnline.toLocalDate().isBefore(LocalDate.now())) userStats.firstLoginOfDay = true

        userStats.lastOnline = LocalDateTime.now()
        userStats.lastOnlineDatabase = userStats.lastOnline

        favoritesRooms = RoomDao.getFavoritesRooms(userInformation.id).toMutableList()

        habboMessenger = HabboMessenger(this@HabboSession)
        habboSubscription = HabboSubscription(this@HabboSession)
        habboBadge = HabboBadge(this@HabboSession)
        habboInventory = HabboInventory(this@HabboSession)
        habboHabbicon = HabboHabbicon(this@HabboSession)

        sessionScope.launch {
            launch { habboSubscription.load() }
            launch { habboBadge.load() }
            launch { habboMessenger.load() }
            launch { habboHabbicon.load() }
            launch {
                habboInventory.load()

                if (release == "R63A") {
                    sendHabboResponse(OutgoingR63A.INVENTORY_UPDATE) // notify the user that the inventory was loaded
                } else {
                    sendHabboResponse(Outgoing.INVENTORY_UPDATE) // notify the user that the inventory was loaded
                }
            }
        }
        processLoginAchievements()

        UserInformationDao.updateAuthTicket(userInformation, null)
        UserInformationDao.saveInformation(userInformation, true, ip)
        UserStatsDao.saveStats(userStats)

        return true
    }

    fun enterRoom(room: Room, password: String = "", bypassAuth: Boolean = false) =
        enterRoomImpl(room, password, bypassAuth)

    fun rewardUser() = rewardUserImpl()

    fun updateAllCurrencies() = updateAllCurrenciesImpl()

    fun processPeriodicAchievements() = processPeriodicAchievementsImpl()

    override fun close() {
        if (authenticated) {
            currentRoom?.userManager?.removeEntity(roomUser, notifyClient = false, kickNotification = false)
            HabboServer.habboGame.snowWarManager.leaveGame(this, true)

            if (!isBot) {
                userStats.lastOnlineDatabase = LocalDateTime.now()

                BadgeDao.saveBadges(habboBadge.badges.values)
                WriteBehindManager.flushSession(this, online = false, ip = channel.ip())

                habboMessenger.notifyFriends()
            }
        }
        sessionJob.cancel()
    }

    internal fun saveAllQueuedStuffs() {
        if (authenticated && !isBot) {
            WriteBehindManager.flushSession(this, online = true, ip = channel.ip())
        }
    }
}
