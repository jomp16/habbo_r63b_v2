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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.HabboVersion
import ovh.rwx.habbo.communication.ProtocolEra
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.database.badge.BadgeDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.database.user.UserPreferencesDao
import ovh.rwx.habbo.database.user.UserStatsDao
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.writebehind.WriteKey
import ovh.rwx.habbo.encryption.IHabboEncryption
import ovh.rwx.habbo.game.HabboGame
import ovh.rwx.habbo.game.habbicon.HabboHabbicon
import ovh.rwx.habbo.game.misc.NotificationType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.UserDisconnectRoomTask
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.badge.HabboBadge
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.information.UserPreferences
import ovh.rwx.habbo.game.user.information.UserStats
import ovh.rwx.habbo.game.user.inventory.HabboInventory
import ovh.rwx.habbo.game.user.messenger.HabboMessenger
import ovh.rwx.habbo.game.user.subscription.HabboSubscription
import ovh.rwx.habbo.kotlin.ip
import ovh.rwx.habbo.plugin.event.events.user.UserDisconnectEvent
import ovh.rwx.habbo.plugin.event.events.user.UserLoginEvent
import ovh.rwx.habbo.util.Utils
import java.lang.Runnable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
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
    var releaseCheckReceived: Boolean = false
    /** Era do protocolo, detectada empiricamente no decoder pelo framing; ver [ProtocolEra]. */
    var protocolEra: ProtocolEra = ProtocolEra.SHUFFLED
    val isPreShuffle: Boolean get() = protocolEra.isPreShuffle
    lateinit var diffieHellmanParams: DHParameterSpec
    lateinit var userInformation: UserInformation
        internal set
    lateinit var userStats: UserStats
        internal set
    lateinit var userPreferences: UserPreferences
        internal set
    /** Últimos pedidos de página do catálogo atendidos (chave -> nanoTime), para ignorar duplicatas em rajada do cliente. */
    val recentCatalogPageRequests: MutableMap<String, Long> = ConcurrentHashMap()
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
        get() {
            val roomManager = HabboGame.instance?.roomManager
                ?: if (Utils.isTestEnvironment()) null else HabboServer.habboGame.roomManager
            return roomManager?.ownedBy(userInformation.id) ?: emptyList()
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

    /** Cipher RC4 server→client (cipher 2 do cliente, `_SafeStr_8088`). Instância independente
     *  do [rc4Encryption] (mesma chave, estado separado) — só existe quando a resposta
     *  SecretKey levou o boolean `true` (era pacote, ≥ 2014-08-14). */
    var rc4EncryptionOut: IHabboEncryption? = null
    var uniqueID: String = ""
    var osInformation: String = ""
    var ping: Long = 0
    val lastCatalogOfferRequest: MutableMap<Int, Long> = ConcurrentHashMap()
    var gameSSOToken: String = ""
    var fastFoodSession: FastFoodSession? = null
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
        if (!isPreShuffle) {
            val handler = HabboGame.instance?.habboHandler
                ?: if (Utils.isTestEnvironment()) null else HabboServer.habboHandler
            handler?.invokeResponse(this@HabboSession, outgoing, *args)?.let {
                sendHabboResponse(it)
            }
        } else {
            log.error("Tried to send $outgoing to a pre-shuffle client!")
        }
    }

    fun sendHabboResponse(outgoing: OutgoingR63A, vararg args: Any?) {
        if (isPreShuffle) {
            val handler = HabboGame.instance?.habboHandler
                ?: if (Utils.isTestEnvironment()) null else HabboServer.habboHandler
            handler?.invokeResponse(this@HabboSession, outgoing, *args)?.let {
                sendHabboResponse(it)
            }
        } else {
            log.error("Tried to send $outgoing to a post-shuffle client!")
        }
    }

    /**
     * Envia a resposta correta de acordo com a release do cliente.
     * Evita if/else nos handlers quando há dois enum variants para a mesma mensagem lógica ou pacotes exclusivos de uma release.
     */
    fun sendResponse(outgoing: Outgoing?, outgoingR63A: OutgoingR63A?, vararg args: Any?) {
        when {
            isPreShuffle && outgoingR63A != null -> {
                sendHabboResponse(outgoingR63A, *args)
            }
            !isPreShuffle && outgoing != null -> {
                sendHabboResponse(outgoing, *args)
            }
        }
    }

    // Fila de saída da sessão. Cada `channel.writeAndFlush` vindo de fora do event loop cria uma tarefa de escrita +
    // promise e pode acordar o loop (syscall): num quarto com 500 usuários isso é por mensagem POR ouvinte
    // (~75 mil/s por sala). Aqui o produtor só enfileira; UMA tarefa por rajada drena no event loop e dá UM flush.
    // A ordem é preservada (fila única por sessão, drenada pela thread do canal).
    private val outbox = ConcurrentLinkedQueue<HabboResponse>()
    private val outboxDrainScheduled = AtomicBoolean(false)
    private val outboxDrain = Runnable { drainOutbox() }

    fun sendHabboResponse(habboResponse: HabboResponse?) {
        habboResponse ?: return

        if (!channel.isActive) {
            // Channel already closed/disconnecting: release the pooled
            // ByteBuf ourselves, otherwise it leaks (the encoder never runs).
            habboResponse.close()
            return
        }

        outbox.offer(habboResponse)

        // Dentro de um OutboxBatch (tick de sala) o agendamento do drain é adiado para o fim do bloco: UM por sessão
        // por tick, em vez de um por rajada (cada um pode custar uma syscall de wakeup do event loop).
        if (outboxDrainScheduled.compareAndSet(false, true) && !OutboxBatch.defer(this)) scheduleOutboxDrain()
    }

    internal fun scheduleOutboxDrain() {
        try {
            channel.eventLoop().execute(outboxDrain)
        } catch (_: RejectedExecutionException) {
            // event loop já encerrado: o drain cai no ramo "canal inativo" e só libera os buffers
            drainOutbox()
        }
    }

    private fun drainOutbox() {
        // O flag é limpo ANTES de esvaziar: um offer concorrente posterior agenda outro drain; um anterior é
        // enxergado pelo poll abaixo (o offer acontece antes do compareAndSet que falhou)
        outboxDrainScheduled.set(false)

        var wrote = false
        while (true) {
            val response = outbox.poll() ?: break

            if (channel.isActive) {
                channel.write(response)
                wrote = true
            } else {
                response.close()
            }
        }

        if (wrote) channel.flush()
    }

    fun sendNotification(message: String) = sendNotification(NotificationType.BROADCAST_ALERT, message)

    fun sendNotification(notificationType: NotificationType, message: String) {
        when (notificationType) {
            NotificationType.MOTD_ALERT -> {
                sendResponse(Outgoing.MISC_MOTD_NOTIFICATION, OutgoingR63A.MISC_MOTD_NOTIFICATION, message)
            }

            NotificationType.BROADCAST_ALERT -> {
                sendResponse(Outgoing.MISC_BROADCAST_NOTIFICATION, OutgoingR63A.MISC_BROADCAST_NOTIFICATION, message)
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

    fun hasPermission(permission: String): Boolean {
        val permManager = HabboGame.instance?.permissionManager
            ?: if (Utils.isTestEnvironment()) null else HabboServer.habboGame.permissionManager
        return when {
            permManager == null -> false
            permManager.userHasCustomPermission(userInformation.id) -> permManager.userHasPermission(
                userInformation.id,
                permission
            )

            else -> permManager.rankHasPermission(userInformation.rank, permission)
        }
    }

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

        // Estas leituras são independentes entre si: em paralelo custam 1 ida ao banco em vez de 4 em série
        // (a thread do handler espera o lote; clothings é lazy e o handler do SSO a lê logo em seguida)
        val userId = userInformation1.id
        habboSubscription = HabboSubscription(this@HabboSession)
        val (loadedStats, loadedPreferences, loadedFavorites) = runBlocking(Dispatchers.IO) {
            val stats = async { UserStatsDao.getUserStats(userId) }
            val preferences = async { UserPreferencesDao.getUserPreferences(userId) }
            val favorites = async { RoomDao.getFavoritesRooms(userId).toMutableList() }
            val clothings = async { userInformation1.clothings }
            // o handler do SSO lê a assinatura (BUILDERS_SUBSCRIPTION_STATUS) logo em seguida: aquece aqui, em paralelo
            // a assinatura HC precisa estar carregada antes de responder: clientes 2011 pedem o status do clube
            // (SUBSCRIPTION_STATUS) e leem USER_RIGHTS logo no login; um load() assíncrono respondia "0 dias" ("Seja HC")
            val subscription = async { habboSubscription.load() }
            clothings.await()
            subscription.await()
            Triple(stats.await(), preferences.await(), favorites.await())
        }
        userStats = loadedStats
        userPreferences = loadedPreferences
        favoritesRooms = loadedFavorites

        if (userStats.lastOnline.toLocalDate().isBefore(LocalDate.now())) userStats.firstLoginOfDay = true

        userStats.lastOnline = LocalDateTime.now()
        userStats.lastOnlineDatabase = userStats.lastOnline

        habboMessenger = HabboMessenger(this@HabboSession)
        habboBadge = HabboBadge(this@HabboSession)
        habboInventory = HabboInventory(this@HabboSession)
        habboHabbicon = HabboHabbicon(this@HabboSession)

        sessionScope.launch {
            launch { habboBadge.load() }
            launch { habboMessenger.load() }
            launch { habboHabbicon.load() }
            // aquece as estatísticas do SnowWar (cacheadas) para o GAME_2_GET_ACCOUNT_GAME_STATUS não pagar a ida ao banco
            launch { HabboServer.habboGame.snowWarManager.getPlayerStats(userId) }
            launch {
                habboInventory.load()

                // notify the user that the inventory was loaded
                sendResponse(Outgoing.INVENTORY_UPDATE, OutgoingR63A.INVENTORY_UPDATE)
            }
        }
        processLoginAchievements()

        // UserInformationDao.updateAuthTicket(userInformation, null)
        // write-behind por chave do usuário: login e logout do MESMO usuário rodam em ordem (o logout vem depois),
        // enquanto usuários diferentes são persistidos em paralelo
        WriteBehindManager.queue(WriteKey.user(userInformation1.id)) {
            UserInformationDao.saveInformation(userInformation1, true, ip)
            UserStatsDao.saveStats(userStats)
        }

        HabboServer.pluginManager.executeEventAsync(UserLoginEvent(this))

        return true
    }

    fun enterRoom(room: Room, password: String = "", bypassAuth: Boolean = false) =
        enterRoomImpl(room, password, bypassAuth)

    fun rewardUser() = rewardUserImpl()

    fun updateAllCurrencies() = updateAllCurrenciesImpl()

    fun processPeriodicAchievements() = processPeriodicAchievementsImpl()

    override fun close() {
        if (authenticated) {
            HabboServer.pluginManager.executeEventAsync(UserDisconnectEvent(this))

            // Cleanup serializado na thread do quarto: o roomUser ainda é null antes do
            // UserJoinRoomTask rodar, e removeEntity inline deixava room user fantasma
            currentRoom?.addTask(UserDisconnectRoomTask(this))
            HabboServer.habboGame.snowWarManager.leaveGame(this, true)

            if (!isBot) {
                userStats.lastOnlineDatabase = LocalDateTime.now()

                // close() roda na thread de I/O do Netty: nada de JDBC síncrono aqui (travava o event loop ~500ms).
                // Enfileirado em ordem após o save do login; o snapshot dos badges é tirado agora.
                val badgesSnapshot = habboBadge.badges.values.toList()
                val closeIp = channel.ip()
                WriteBehindManager.queue(WriteKey.user(userInformation.id)) {
                    BadgeDao.saveBadges(badgesSnapshot)
                    WriteBehindManager.flushSession(this, online = false, ip = closeIp)
                }

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
