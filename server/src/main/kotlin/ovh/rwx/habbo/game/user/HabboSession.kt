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
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.HabboVersion
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.misc.MiscGenericErrorResponse
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.database.badge.BadgeDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.database.user.UserPreferencesDao
import ovh.rwx.habbo.database.user.UserStatsDao
import ovh.rwx.habbo.encryption.IHabboEncryption
import ovh.rwx.habbo.game.habbicon.HabboHabbicon
import ovh.rwx.habbo.game.misc.NotificationType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomState
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.badge.HabboBadge
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.information.UserPreferences
import ovh.rwx.habbo.game.user.information.UserStats
import ovh.rwx.habbo.game.user.inventory.HabboInventory
import ovh.rwx.habbo.game.user.messenger.HabboMessenger
import ovh.rwx.habbo.game.user.subscription.HabboSubscription
import ovh.rwx.habbo.kotlin.ip
import ovh.rwx.habbo.util.ActivityPointType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.spec.DHParameterSpec
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class HabboSession(val channel: Channel) : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    lateinit var release: String
    val releaseInitialized get() = this::release.isInitialized
    var habboVersion: HabboVersion = HabboVersion(LocalDateTime.now(), 63)
    var r63ANewEncoding: Boolean = false
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
    lateinit var habboHabbicon: HabboHabbicon
        private set
    val rooms: List<Room>
        get() = HabboServer.habboGame.roomManager.rooms.values.filter {
            it.userManager.hasRights(
                this,
                ownerRight = true,
                ignorePermissionAnyRoomOwner = true
            )
        }
    lateinit var favoritesRooms: MutableList<Pair<Int, Int>>
        private set
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

        CoroutineScope(HabboServer.cachedExecutorDispatcher).launch {
            launch { habboSubscription.load() }
            launch { habboBadge.load() }
            launch { habboMessenger.load() }
            launch {
                habboHabbicon.load()
            }
            launch {
                habboInventory.load()

                if (release == "R63A") {
                    sendHabboResponse(OutgoingR63A.INVENTORY_UPDATE) // notify the user that the inventory was loaded
                } else {
                    sendHabboResponse(Outgoing.INVENTORY_UPDATE) // notify the user that the inventory was loaded
                }

                // LTD Purchaser Achievement (para usuários herdados)
                // Busca TODOS os itens do usuário (inventário + quartos)
                val allUserItemsWithRoom = ItemDao.getAllUserItemsWithRoom(userInformation.id)
                val totalLTDs = allUserItemsWithRoom.count { it.userItem.limited }
                if (totalLTDs > 0) {
                    HabboServer.habboGame.achievementManager.progress(
                        this@HabboSession,
                        "ACH_LTDPurchaser",
                        totalLTDs,
                        accumulate = false
                    )
                }

                // LTD Early Bird Achievement (para usuários herdados)
                val totalEarlyBirdLTDs = allUserItemsWithRoom.count { itemWithRoom ->
                    val userItem = itemWithRoom.userItem
                    if (!userItem.limited) return@count false
                    val limitedData = userItem.limitedItemData ?: return@count false
                    val earlyBirdThreshold = (limitedData.limitedTotal * 0.1).toInt()
                    limitedData.limitedNumber <= earlyBirdThreshold
                }
                if (totalEarlyBirdLTDs > 0) {
                    HabboServer.habboGame.achievementManager.progress(
                        this@HabboSession,
                        "ACH_LTDEarlyBird",
                        totalEarlyBirdLTDs,
                        accumulate = false
                    )
                }

                // ACH_PlaceCreditValue: moedas de câmbio colocadas nos quartos (herdado)
                val totalCreditValue = allUserItemsWithRoom
                    .filter { it.roomId != null } // Apenas itens colocados em quartos
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
                        this@HabboSession,
                        "ACH_PlaceCreditValue",
                        totalCreditValue,
                        accumulate = false
                    )
                }

                // ACH_CameraPhotoCount: fotos de câmera compradas (herdado)
                val totalPhotos = allUserItemsWithRoom.count {
                    it.userItem.itemName == "external_image_wallitem_poster_small"
                }
                if (totalPhotos > 0) {
                    HabboServer.habboGame.achievementManager.progress(
                        this@HabboSession,
                        "ACH_CameraPhotoCount",
                        totalPhotos,
                        accumulate = false
                    )
                }
            }
            launch {
                // Busca todos os quartos do usuário para achievements de decoração
                val userRooms =
                    HabboServer.habboGame.roomManager.rooms.values.filter { it.roomData.ownerId == userInformation.id }

                // ACH_RoomDecoFloor: contar quantas vezes mudou o piso (herdado)
                val totalFloorChanges = userRooms.count { it.roomData.floor.isNotEmpty() && it.roomData.floor != "0" }
                if (totalFloorChanges > 0) {
                    HabboServer.habboGame.achievementManager.progress(
                        this@HabboSession,
                        "ACH_RoomDecoFloor",
                        totalFloorChanges,
                        accumulate = false
                    )
                }

                // ACH_RoomDecoWallpaper: contar quantas vezes mudou o papel de parede (herdado)
                val totalWallpaperChanges =
                    userRooms.count { it.roomData.wallpaper.isNotEmpty() && it.roomData.wallpaper != "0" }
                if (totalWallpaperChanges > 0) {
                    HabboServer.habboGame.achievementManager.progress(
                        this@HabboSession,
                        "ACH_RoomDecoWallpaper",
                        totalWallpaperChanges,
                        accumulate = false
                    )
                }

                // ACH_RoomDecoLandscape: contar quantas vezes mudou o fundo (herdado)
                val totalLandscapeChanges =
                    userRooms.count { it.roomData.landscape.isNotEmpty() && it.roomData.landscape != "0" }
                if (totalLandscapeChanges > 0) {
                    HabboServer.habboGame.achievementManager.progress(
                        this@HabboSession,
                        "ACH_RoomDecoLandscape",
                        totalLandscapeChanges,
                        accumulate = false
                    )
                }
            }
            launch {
                // Registration Duration Achievement
                val daysRegistered =
                    java.time.Duration.between(userInformation.accountCreated, LocalDateTime.now()).toDays().toInt()
                HabboServer.habboGame.achievementManager.progress(
                    this@HabboSession,
                    "ACH_RegistrationDuration",
                    daysRegistered,
                    accumulate = false
                )

                // Daily Login Achievement
                if (userStats.firstLoginOfDay) {
                    // todo: test this, if it resets after some gap.
                    HabboServer.habboGame.achievementManager.progress(
                        this@HabboSession,
                        "ACH_Login",
                        1,
                        accumulate = true
                    )
                }

                // Friend List Size Achievement (para usuários herdados)
                val totalFriends = habboMessenger.friends.size
                HabboServer.habboGame.achievementManager.progress(
                    this@HabboSession,
                    "ACH_FriendListSize",
                    totalFriends,
                    accumulate = false
                )

                // Respect Earned Achievement (para usuários herdados)
                HabboServer.habboGame.achievementManager.progress(
                    this@HabboSession,
                    "ACH_RespectEarned",
                    userStats.respect,
                    accumulate = false
                )
            }
        }

        UserInformationDao.updateAuthTicket(userInformation, null)
        UserInformationDao.saveInformation(userInformation, true, ip)
        UserStatsDao.saveStats(userStats)

        return true
    }

    private fun authenticateBot(ssoTicket: String): Boolean {
        val botPrefix = HabboServer.habboConfig.botTicketPrefix
        val suffix = ssoTicket.removePrefix(botPrefix).toIntOrNull() ?: return false
        val botId = 2_000_000_000 + suffix
        val botUsername = "Bot_$suffix"
        val now = LocalDateTime.now()

        if (HabboServer.habboSessionManager.containsHabboSessionById(botId)) {
            return false
        }

        userInformation = UserInformation(
            id = botId,
            username = botUsername,
            email = "",
            accountCreated = now,
            realname = "",
            rank = 1,
            credits = 0,
            figure = HabboServer.habboConfig.serverConsoleFigure,
            gender = "M",
            motto = "",
            homeRoom = 0,
            vip = false,
            password = "",
            activityPointsCurrencies = mutableMapOf()
        )

        userStats = UserStats(
            id = botId,
            lastOnline = now,
            lastOnlineDatabase = now,
            onlineSeconds = 0L,
            roomVisits = 0,
            respect = 0,
            giftsGiven = 0,
            giftsReceived = 0,
            dailyRespectPoints = 0,
            dailyPetRespectPoints = 0,
            dailyCompetitionVotes = 0,
            achievementScore = 0,
            questId = 0,
            questProgress = 0,
            favoriteGroupId = 0,
            ticketsAnswered = 0,
            marketplaceTickets = 0,
            creditsLastUpdate = now,
            respectLastUpdate = now
        )

        userPreferences = UserPreferences(
            id = botId,
            volume = "100;100;100",
            preferOldChat = false,
            ignoreRoomInvite = false,
            disableCameraFollow = false,
            navigatorX = 0,
            navigatorY = 0,
            navigatorWidth = 0,
            navigatorHeight = 0,
            hideInRoom = false,
            blockNewFriends = false,
            chatColor = 0,
            friendBarOpen = false,
            friendStreamEnabled = false
        )

        favoritesRooms = mutableListOf()

        habboMessenger = HabboMessenger(this@HabboSession)
        habboSubscription = HabboSubscription(this@HabboSession)
        habboBadge = HabboBadge(this@HabboSession)
        habboInventory = HabboInventory(this@HabboSession)
        habboHabbicon = HabboHabbicon(this@HabboSession)

        isBot = true
        handshaking = false

        return true
    }

    internal fun rewardUser() {
        val localDateTime =
            userStats.creditsLastUpdate.plusSeconds(HabboServer.habboConfig.timerConfig.creditsSeconds.toLong())
        var update = false

        if (LocalDateTime.now().isAfter(localDateTime)) {
            if (HabboServer.habboConfig.rewardConfig.creditsMax < 0 && HabboServer.habboConfig.rewardConfig.credits > 0
                && userInformation.credits < Int.MAX_VALUE
            ) {
                userInformation.credits += HabboServer.habboConfig.rewardConfig.credits

                update = true
            }

            val currentPixels = userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.PIXELS, 0)
            if (HabboServer.habboConfig.rewardConfig.pixelsMax < 0 && HabboServer.habboConfig.rewardConfig.pixels > 0
                && currentPixels < Int.MAX_VALUE
            ) {
                userInformation.activityPointsCurrencies.merge(
                    ActivityPointType.PIXELS,
                    HabboServer.habboConfig.rewardConfig.pixels,
                    Int::plus
                )

                update = true
            }

            val currentVipPoints = userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.DIAMONDS, 0)
            if (userInformation.vip && HabboServer.habboConfig.rewardConfig.vipPointsMax < 0
                && HabboServer.habboConfig.rewardConfig.vipPoints > 0
                && currentVipPoints < Int.MAX_VALUE
            ) {
                userInformation.activityPointsCurrencies.merge(
                    ActivityPointType.DIAMONDS,
                    HabboServer.habboConfig.rewardConfig.vipPoints,
                    Int::plus
                )

                update = true
            }
        }

        if (update) {
            userStats.creditsLastUpdate = LocalDateTime.now()

            updateAllCurrencies()
        }
    }

    internal fun processPeriodicAchievements() {
        if (isBot) return

        // ACH_AllTimeHotelPresence: tempo total online em minutos
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

        // ACH_BasicClub: meses de club ativo (para usuários que já têm club)
        if (habboSubscription.validUserSubscription) {
            val totalMonths = ChronoUnit.MONTHS.between(
                habboSubscription.habboClubSubscription?.activated,
                LocalDateTime.now()
            ).toInt()
            HabboServer.habboGame.achievementManager.progress(this, "ACH_BasicClub", totalMonths, accumulate = false)

            // ACH_HC: dias de club ativo
            val totalDays = ChronoUnit.DAYS.between(
                habboSubscription.habboClubSubscription?.activated,
                LocalDateTime.now()
            ).toInt()
            HabboServer.habboGame.achievementManager.progress(this, "ACH_HC", totalDays, accumulate = false)
        }

        // ACH_BuildersClub: dias de Builders Club ativo
        if (habboSubscription.hasBuildersClub && !habboSubscription.buildersClubSubscription.trial) {
            val totalDays = ChronoUnit.DAYS.between(
                habboSubscription.buildersClubSubscription.activated,
                LocalDateTime.now()
            ).toInt()
            HabboServer.habboGame.achievementManager.progress(this, "ACH_BuildersClub", totalDays, accumulate = false)
        }
    }

    fun updateAllCurrencies() {
        // === CREDITS ===
        if (userInformation.credits < 0) userInformation.credits = Int.MAX_VALUE
        if (HabboServer.habboConfig.rewardConfig.creditsMax >= 0 && userInformation.credits > HabboServer.habboConfig.rewardConfig.creditsMax) {
            userInformation.credits = HabboServer.habboConfig.rewardConfig.creditsMax
        }

        // === PIXELS / DUCKETS ===
        var currentPixels = userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.PIXELS, 0)

        if (currentPixels < 0) currentPixels = Int.MAX_VALUE
        if (HabboServer.habboConfig.rewardConfig.pixelsMax >= 0 && currentPixels > HabboServer.habboConfig.rewardConfig.pixelsMax) {
            currentPixels = HabboServer.habboConfig.rewardConfig.pixelsMax
        }

        // Atualiza o Map com o valor validado
        userInformation.activityPointsCurrencies[ActivityPointType.PIXELS] = currentPixels

        // === VIP POINTS / DIAMANTES ===
        var currentVipPoints = userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.DIAMONDS, 0)

        if (userInformation.vip) {
            if (currentVipPoints < 0) currentVipPoints = Int.MAX_VALUE
            if (HabboServer.habboConfig.rewardConfig.vipPointsMax >= 0 && currentVipPoints > HabboServer.habboConfig.rewardConfig.vipPointsMax) {
                currentVipPoints = HabboServer.habboConfig.rewardConfig.vipPointsMax
            }
            // Atualiza o Map com o valor validado
            userInformation.activityPointsCurrencies[ActivityPointType.DIAMONDS] = currentVipPoints
        }

        // === ENVIAR PACOTES ===
        if (release != "R63A") {
            sendHabboResponse(Outgoing.CREDITS_BALANCE, userInformation.credits)
            sendHabboResponse(Outgoing.ACTIVITY_POINTS_BALANCE, userInformation.activityPointsCurrencies)
        } else {
            sendHabboResponse(OutgoingR63A.CREDITS_BALANCE, userInformation.credits)
            sendHabboResponse(OutgoingR63A.ACTIVITY_POINTS_BALANCE, userInformation.activityPointsCurrencies)
        }
    }

    fun enterRoom(room: Room, password: String = "", bypassAuth: Boolean = false) {
        if (!bypassAuth && room == currentRoom) return
        val methodName = HabboServer.habboHandler.getOverrideMethodForHeader(Outgoing.ROOM_OWNER, release)

        currentRoom?.userManager?.removeEntity(roomUser, notifyClient = false, kickNotification = false)

        if (room.itemManager.hiddenBuildersClub && userInformation.id != room.roomData.ownerId) {
            if (release != "R63A") {
                sendHabboResponse(Outgoing.ROOM_EXIT)
                sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.BUILDERS_CLUB_VISIT_DENIED_GUEST)
            } else {
                sendHabboResponse(OutgoingR63A.ROOM_EXIT)
                sendNotification($$"${notification.builders_club.visit_denied_for_visitor.message}")
            }
            return
        }

        if (!room.running) HabboServer.habboGame.roomManager.roomTaskManager.addRoom(room)

        if (room.userManager.entities.values.filterIsInstance<RoomUser>().size >= room.roomData.usersMax && !room.userManager.hasRights(
                this,
                true
            ) && !hasPermission("acc_enter_full_room")
        ) {
            if (release != "R63A") {
                sendHabboResponse(Outgoing.ROOM_ERROR, 1, "")
                sendHabboResponse(Outgoing.ROOM_EXIT)
            } else {
                sendHabboResponse(OutgoingR63A.ROOM_ERROR, 1)
                sendHabboResponse(OutgoingR63A.ROOM_EXIT)
            }

            return
        }
        val loading = !bypassAuth && !room.userManager.hasRights(this, true)

        if (loading) {
            if (room.roomData.state == RoomState.PASSWORD && !HabboServer.habboGame.passwordEncryptor.checkPassword(
                    password,
                    room.roomData.password
                )
            ) {
                if (release != "R63A") {
                    sendHabboResponse(
                        Outgoing.MISC_GENERIC_ERROR,
                        MiscGenericErrorResponse.MiscGenericError.WRONG_PASSWORD
                    )
                    sendHabboResponse(Outgoing.ROOM_EXIT)
                } else {
                    sendHabboResponse(
                        OutgoingR63A.MISC_GENERIC_ERROR,
                        MiscGenericErrorResponse.MiscGenericError.WRONG_PASSWORD
                    )
                    sendHabboResponse(Outgoing.ROOM_EXIT)
                }

                return
            } else if (room.roomData.state == RoomState.LOCKED) {
                val roomUsersWithRights = room.userManager.usersWithRights

                if (roomUsersWithRights.isEmpty()) {
                    if (methodName == "response") sendHabboResponse(Outgoing.ROOM_DOORBELL_DENIED, "")
                    else if (methodName == "responseWithRoomId") sendHabboResponse(
                        Outgoing.ROOM_DOORBELL_DENIED,
                        room.roomData.id,
                        ""
                    )

                    if (release != "R63A") {
                        sendHabboResponse(Outgoing.ROOM_EXIT)
                    } else {
                        sendHabboResponse(OutgoingR63A.ROOM_EXIT)
                    }
                } else {
                    currentRoom = room

                    roomUsersWithRights.forEach {
                        (it as? RoomUser)?.habboSession?.let { habboSession ->
                            if (habboSession.release != "R63A") {
                                habboSession.sendHabboResponse(Outgoing.ROOM_DOORBELL, userInformation.username)
                            } else {
                                habboSession.sendHabboResponse(OutgoingR63A.ROOM_DOORBELL, userInformation.username)
                            }
                        }
                    }

                    if (release != "R63A") {
                        sendHabboResponse(Outgoing.ROOM_DOORBELL, "")
                    } else {
                        sendHabboResponse(OutgoingR63A.ROOM_DOORBELL, "")
                    }
                }

                return
            }
        }

        userStats.roomVisits++

        currentRoom = room

        userStats.favoriteGroup?.let {
            room.loadedGroups.add(it)

            room.sendHabboResponse(Outgoing.ROOM_GROUP_BADGES, room.loadedGroups)
            room.sendHabboResponse(OutgoingR63A.ROOM_GROUPS_BADGES, room.loadedGroups)
        }

        room.userManager.addUser(this)
    }

    override fun close() {
        if (authenticated) {
            currentRoom?.userManager?.removeEntity(roomUser, notifyClient = false, kickNotification = false)

            if (!isBot) {
                userStats.lastOnlineDatabase = LocalDateTime.now()

                BadgeDao.saveBadges(habboBadge.badges.values)
                UserInformationDao.saveInformation(userInformation, false, channel.ip())
                UserPreferencesDao.savePreferences(userPreferences)
                UserStatsDao.saveStats(userStats)

                saveAllQueuedStuffs()

                habboMessenger.notifyFriends()
            }
        }
    }

    internal fun saveAllQueuedStuffs() {

    }
}