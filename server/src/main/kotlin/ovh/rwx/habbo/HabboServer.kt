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

package ovh.rwx.habbo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.zaxxer.hikari.HikariDataSource
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import kotlinx.coroutines.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jdbi.v3.core.Jdbi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.fastfood.communication.FastFoodHandler
import ovh.rwx.fastfood.netty.FastFoodNettyDecoder
import ovh.rwx.fastfood.netty.FastFoodNettyEncoder
import ovh.rwx.fastfood.netty.FastFoodNettyHandler
import ovh.rwx.habbo.communication.HabboHandler
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.config.HabboConfig
import ovh.rwx.habbo.console.ServerConsole
import ovh.rwx.habbo.database.DatabaseManager
import ovh.rwx.habbo.database.migration.FlywayMigration
import ovh.rwx.habbo.game.BootPrefetch
import ovh.rwx.habbo.game.gamedata.ExternalVariables
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.encryption.HabboEncryptionHandler
import ovh.rwx.habbo.game.HabboGame
import ovh.rwx.habbo.game.user.HabboSessionManager
import ovh.rwx.habbo.kotlin.ip
import ovh.rwx.habbo.netty.*
import ovh.rwx.habbo.plugin.api.PluginListener
import ovh.rwx.habbo.plugin.core.PluginManager
import ovh.rwx.habbo.util.ClassIndex
import ovh.rwx.habbo.util.Utils
import java.io.File
import java.security.Security
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.system.exitProcess

object HabboServer : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val habboConfig: HabboConfig by lazy {
        val file = listOf(File("config.yaml"), File("../config.yaml")).firstOrNull { it.exists() } ?: File("config.yaml")
        ObjectMapper(YAMLFactory()).registerKotlinModule().readValue(file)
    }
    val pluginManager: PluginManager = PluginManager()

    // SQL
    val hikariDataSource: HikariDataSource
    val jdbi: Jdbi

    // Netty
    private val habboServerBootstrap: ServerBootstrap
    private val habboWebSocketServerBootstrap: ServerBootstrap
    private val fastFoodServerBootstrap: ServerBootstrap
    private val fastFoodWebSocketServerBootstrap: ServerBootstrap
    private val workerGroup: EventLoopGroup
    private val bossGroup: EventLoopGroup

    // Habbo
    val habboEncryptionHandler: HabboEncryptionHandler
    val habboSessionManager: HabboSessionManager
    val habboHandler: HabboHandler
    val fastFoodHandler: FastFoodHandler
    val habboGame: HabboGame
        get() = HabboGame.instance ?: error("habboGame não inicializado")

    // Thread Executors
    val serverScheduledExecutor: ScheduledExecutorService =
        Executors.newScheduledThreadPool(3 + if (habboConfig.roomTaskConfig.threads == 0) 1 else habboConfig.roomTaskConfig.threads)
    val cachedExecutorDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()

    /**
     * Handshake criptográfico (RSA/DH: BigInteger.modPow, só CPU) em poucas threads dedicadas. No pool cacheado
     * uma enxurrada de logins (ex.: todos reconectando após uma queda) vira centenas de threads disputando os
     * núcleos com os ticks das salas, que travavam por segundos; aqui o excesso espera na fila e os ticks mantêm
     * CPU. Tamanho: 1/3 dos núcleos (mín. 1: numa VPS de 2 núcleos, 2 threads de handshake ocupariam a máquina toda).
     */
    val handshakeDispatcher = Executors.newFixedThreadPool(
        maxOf(1, Runtime.getRuntime().availableProcessors() / 3)
    ) { runnable -> Thread(runnable, "handshake-crypto").apply { isDaemon = true } }.asCoroutineDispatcher()
    val applicationScope = CoroutineScope(cachedExecutorDispatcher + SupervisorJob())
    val serverConsole: ServerConsole = ServerConsole(this)
    val DATE_TIME_FORMATTER_WITH_HOURS: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val DATE_TIME_FORMATTER_ONLY_DAYS: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    @Volatile
    private var isRunning: Boolean = false

    /**
     * Ligado por [close]: a partir daí o desligamento é dele. O `finally` de [start] (acordado quando [close] fecha os
     * listeners) não derruba os event loops sozinho, senão os canais caíam em paralelo com o flush do write-behind.
     */
    @Volatile
    private var closing: Boolean = false

    /** Canais de escuta (TCP, WebSocket, FastFood TCP e WebSocket), fechados primeiro no [close] para parar de aceitar conexões. */
    private val serverChannels = CopyOnWriteArrayList<Channel>()
    private var handoffServer: ovh.rwx.habbo.handoff.HandoffServer? = null
    @Volatile
    private var handoffSocketPath: String? = null
    val started: Boolean
        get() = isRunning && !workerGroup.isShuttingDown && !bossGroup.isShuttingDown

    init {
        check(!Utils.isTestEnvironment()) {
            "HabboServer não pode ser inicializado dentro de um ambiente de testes! " +
                    "Testes unitários devem usar seams (HabboGame.instance, mocks) e testes com banco devem usar Testcontainers."
        }
        Security.addProvider(BouncyCastleProvider())

        javaClass.classLoader.getResourceAsStream("ascii_art.txt")?.bufferedReader()?.forEachLine { log.info(it) }

        log.info("")
        log.info("Version: ${BuildConfig.VERSION}.")
        log.info(
            "Built in: ${
                DATE_TIME_FORMATTER_WITH_HOURS.format(
                    BuildConfig.BUILD_INSTANT.atZone(ZoneId.systemDefault()).toLocalDateTime()
                )
            }"
        )
        log.info("Git commit hash: ${BuildConfig.GIT_COMMIT_FULL}.")
        log.info("By jomp16 and Lucas.")
        log.info("Credits for developers of IDK, Phoenix, Butterfly, Uber, Azure, Nova and probably other niggas for code and packets.")
        log.info("Licensed under GPLv3. See https://www.gnu.org/licenses/gpl-3.0.en.html")
        log.info("")
        log.info("Loading ${BuildConfig.NAME} server... (JVM up for {}ms)", java.lang.management.ManagementFactory.getRuntimeMXBean().uptime)

        // furnidata, figuredata, Habbicons e reciclador saem daqui: sem as variables (ou sem uma chave lida), não
        // sobe. Antes do banco, para falhar sem nenhum pool/thread aberto segurando a JVM
        log.info("Loading external variables...")
        ExternalVariables.loadOrFail()
        log.info("Done!")

        // asset mapping (disco) e furnidata (HTTP) não dependem do banco: carregam enquanto o pool/Flyway esperam
        BootPrefetch.start(habboConfig.cameraConfig.assetsPath)

        // Initialize database
        log.info("Initializing database...")

        hikariDataSource = HikariDataSource(habboConfig.databaseConfig.hikariConfig)
        FlywayMigration.migrate(hikariDataSource)
        jdbi = DatabaseManager.init(hikariDataSource)
        WriteBehindManager.start(applicationScope)

        log.info("Database and Flyway initialized!")

        log.info("Loading Habbo handler...")
        habboHandler = HabboHandler()
        log.info("Done!")

        log.info("Loading Habbo encryption...")
        habboEncryptionHandler = HabboEncryptionHandler(
            habboConfig.encryptionConfig.rsaConfig.n,
            habboConfig.encryptionConfig.rsaConfig.d,
            habboConfig.encryptionConfig.rsaConfig.e
        )
        log.info("Done!")

        log.info("Loading Habbo game...")
        HabboGame.instance = HabboGame().apply {
            habboHandler = this@HabboServer.habboHandler
            config = this@HabboServer.habboConfig
        }
        log.info("Done!")
        log.info("Loading FastFood handler...")
        fastFoodHandler = FastFoodHandler()
        log.info("Done!")

        log.info("Loading Netty...")
        habboSessionManager = HabboSessionManager()
        HabboGame.instance?.sessionManager = habboSessionManager
        habboServerBootstrap = ServerBootstrap()
        habboWebSocketServerBootstrap = ServerBootstrap()
        fastFoodServerBootstrap = ServerBootstrap()
        fastFoodWebSocketServerBootstrap = ServerBootstrap()
        val ioHandlerFactory = if (Epoll.isAvailable()) EpollIoHandler.newFactory() else NioIoHandler.newFactory()
        workerGroup = MultiThreadIoEventLoopGroup(ioHandlerFactory)
        bossGroup = MultiThreadIoEventLoopGroup(ioHandlerFactory)
        log.info("Done!")

        log.info("Loading plugins...")
        pluginManager.loadPlugins(ClassIndex.classesOf(ClassIndex.Kind.PLUGIN_LISTENER).map { it.asSubclass(PluginListener::class.java) })
        log.info("Done!")

        log.info("Adding shutdown hook...")
        Runtime.getRuntime().addShutdownHook(Thread { close() })
        log.info("Done!")
    }

    @JvmOverloads
    fun start(
        handoffSocketPath: String? = null,
        handoffMode: ovh.rwx.habbo.handoff.HandoffMode = ovh.rwx.habbo.handoff.HandoffMode.AUTO
    ) {
        val resolvedSocketPath = handoffSocketPath ?: System.getProperty("habbo.handoff.socket")
        this.handoffSocketPath = resolvedSocketPath
        try {
            HabboResponse.debugEnabled = habboConfig.debug
            serverConsole.startConsoleReader()
            val stringEncoder = StringEncoder(Charsets.UTF_8)
            val habboNettyEncoder = HabboNettyEncoder()
            val habboNettyHandler = HabboNettyHandler()
            val habboNettyWebSocketHandler = HabboNettyWebSocketHandler()
            val fastFoodNettyEncoder = FastFoodNettyEncoder()
            val fastFoodNettyHandler = FastFoodNettyHandler()
            val habboTcpInit = object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline().apply {
                        addLast(IdleStateHandler(60, 0, 0))

                        addLast(stringEncoder)
                        addLast(habboNettyEncoder)

                        if (habboConfig.encryptionConfig.rc4) addLast(HabboNettyRC4Decoder())

                        addLast(HabboNettyDecoder())
                        addLast(habboNettyHandler)
                    }
                }
            }
            val habboWsInit = object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline().apply {
                        addLast(IdleStateHandler(60, 0, 0))
                        addLast(HttpServerCodec())
                        addLast(HttpObjectAggregator(65536))
                        addLast(WebSocketServerProtocolHandler("/"))
                        addLast(habboNettyWebSocketHandler)

                        addLast(stringEncoder)
                        addLast(habboNettyEncoder)

                        if (habboConfig.encryptionConfig.rc4) addLast(HabboNettyRC4Decoder())

                        addLast(HabboNettyDecoder())
                        addLast(habboNettyHandler)
                    }
                }
            }
            val fastFoodTcpInit = object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline().apply {
                        addLast(IdleStateHandler(60, 0, 0))

                        addLast(stringEncoder)
                        addLast(fastFoodNettyEncoder)
                        addLast(FastFoodNettyDecoder())
                        addLast(fastFoodNettyHandler)
                    }
                }
            }
            val fastFoodWsInit = object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline().apply {
                        addLast(IdleStateHandler(60, 0, 0))
                        addLast(HttpServerCodec())
                        addLast(HttpObjectAggregator(65536))
                        addLast(WebSocketServerProtocolHandler("/"))
                        addLast(habboNettyWebSocketHandler)

                        addLast(stringEncoder)
                        addLast(fastFoodNettyEncoder)
                        addLast(FastFoodNettyDecoder())
                        addLast(fastFoodNettyHandler)
                    }
                }
            }
            habboServerBootstrap.group(bossGroup, workerGroup)
                .channel(if (Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java)
                .childHandler(habboTcpInit)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            habboWebSocketServerBootstrap.group(bossGroup, workerGroup)
                .channel(if (Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java)
                .childHandler(habboWsInit)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
            fastFoodServerBootstrap.group(bossGroup, workerGroup)
                .channel(if (Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java)
                .childHandler(fastFoodTcpInit)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
            // Mesmo protocolo do TCP acima, em frames binários (clientes de navegador só abrem WebSocket); o
            // HabboNettyWebSocketHandler entrega o conteúdo dos frames como stream ao FastFoodNettyDecoder
            fastFoodWebSocketServerBootstrap.group(bossGroup, workerGroup)
                .channel(if (Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java)
                .childHandler(fastFoodWsInit)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
            log.info("Waiting for Habbo game to finish loading...")
            habboGame.awaitLoaded()
            log.info("Habbo game loaded!")

            // Pré-reserva os blocos Hi-Lo para que a 1ª interação (conquistas, itens, etc.) não pague um round-trip ao banco.
            // Cada reserva é uma transação (~5 idas ao banco, ~600 ms com banco remoto): 11 em série seguravam o bind do
            // Netty por ~7 s. Rodam em segundo plano e em paralelo; se um usuário pedir um id antes, o próprio nextId()
            // reserva o bloco (o lock do HiLoSequence impede reservar duas vezes).
            habboSessionManager.startHeartbeat(serverScheduledExecutor, habboConfig.heartbeatIntervalSeconds)
            ovh.rwx.habbo.metrics.ServerMetrics.start(habboConfig.metricsSampleIntervalMs)
            if (habboConfig.stressTest) ovh.rwx.habbo.database.writebehind.WriteBehindStressLog.start()

            habboConfig.dynamicDnsConfig?.let { ovh.rwx.habbo.net.DynamicDnsUpdater.start(it) }

            val socketPath = this.handoffSocketPath
            val canAdopt = Epoll.isAvailable() && socketPath != null &&
                handoffMode != ovh.rwx.habbo.handoff.HandoffMode.FRESH &&
                File(socketPath).exists()

            if (canAdopt) {
                adoptFromHandoff(
                    socketPath,
                    stringEncoder,
                    habboNettyEncoder,
                    habboNettyHandler,
                    habboNettyWebSocketHandler,
                    habboTcpInit,
                    habboWsInit,
                    fastFoodTcpInit,
                    fastFoodWsInit
                )
            } else {
                val habboChannelFuture = habboServerBootstrap.bind(habboConfig.port)
                val habboWebSocketChannelFuture = habboWebSocketServerBootstrap.bind(habboConfig.wsPort)
                val fastFoodChannelFuture = fastFoodServerBootstrap.bind(habboConfig.port + 10)
                val fastFoodWebSocketChannelFuture = fastFoodWebSocketServerBootstrap.bind(habboConfig.fastFoodWsPort)
                habboChannelFuture.sync()
                habboWebSocketChannelFuture.sync()
                fastFoodChannelFuture.sync()
                fastFoodWebSocketChannelFuture.sync()
                if (habboChannelFuture.isSuccess && habboWebSocketChannelFuture.isSuccess && fastFoodChannelFuture.isSuccess && fastFoodWebSocketChannelFuture.isSuccess) {
                    serverChannels += listOf(
                        habboChannelFuture.channel(),
                        habboWebSocketChannelFuture.channel(),
                        fastFoodChannelFuture.channel(),
                        fastFoodWebSocketChannelFuture.channel()
                    )
                } else {
                    log.error(
                        "Error starting ${BuildConfig.NAME} server!",
                        habboChannelFuture.cause() ?: habboWebSocketChannelFuture.cause() ?: fastFoodChannelFuture.cause()
                            ?: fastFoodWebSocketChannelFuture.cause()
                    )
                    exitProcess(1)
                }
            }

            pluginManager.loadPluginsFromDir(File("plugins"))

            val hiLoSequences = listOf(
                ovh.rwx.habbo.database.achievement.AchievementDao.achievementSequence,
                ovh.rwx.habbo.database.badge.BadgeDao.badgeSequence,
                ovh.rwx.habbo.database.camera.CameraDao.cameraSequence,
                ovh.rwx.habbo.database.group.GroupDao.groupSequence,
                ovh.rwx.habbo.database.group.GroupDao.groupMemberSequence,
                ovh.rwx.habbo.database.messenger.MessengerDao.friendshipSequence,
                ovh.rwx.habbo.database.messenger.MessengerDao.requestSequence,
                ovh.rwx.habbo.database.pet.PetDao.petSequence,
                ovh.rwx.habbo.database.room.RoomDao.roomSequence,
                ovh.rwx.habbo.database.wardrobe.WardrobeDao.wardrobeSequence,
                habboGame.itemManager.itemSequence,
            )
            val hiLoWarmUpDispatcher = Dispatchers.IO.limitedParallelism(4)
            hiLoSequences.forEach { sequence ->
                applicationScope.launch(hiLoWarmUpDispatcher) {
                    try {
                        sequence.warmUp()
                    } catch (e: Exception) {
                        log.warn("Falha ao pré-reservar o bloco Hi-Lo de '{}'", sequence.sequenceName, e)
                    }
                }
            }

            log.info("Handoff config: epoll={}, socketPath={}, mode={}", Epoll.isAvailable(), this.handoffSocketPath, handoffMode)
            if (Epoll.isAvailable() && this.handoffSocketPath != null) {
                handoffServer = ovh.rwx.habbo.handoff.HandoffServer(
                    socketPath = this.handoffSocketPath!!,
                    onQuiesceAndSnapshot = { quiesceForHandoff() },
                    onAck = { finalizeHandoffExit() },
                    onRollback = { reason ->
                        log.warn("Handoff rollback triggered: {}", reason)
                        resumeFromFailedHandoff()
                    }
                ).also { it.start() }
            }

            isRunning = true
            if (!canAdopt) ovh.rwx.habbo.handoff.SystemdNotify.notifyReady()
            log.info("${BuildConfig.NAME} server started on port {}!", habboConfig.port)
            log.info("${BuildConfig.NAME} WebSocket server started on port {}!", habboConfig.wsPort)
            log.info("FastFood server started on port {}!", habboConfig.port + 10)
            log.info("FastFood WebSocket server started on port {}!", habboConfig.fastFoodWsPort)
            log.info("Cold start finished: JVM up for {}ms", java.lang.management.ManagementFactory.getRuntimeMXBean().uptime)
            serverChannels.forEach { it.closeFuture().await() }
            serverChannels.lastOrNull()?.closeFuture()?.sync()
        } catch (e: Exception) {
            log.error("An exception happened!", e)
        } finally {
            if (!closing) {
                workerGroup.shutdownGracefully()
                bossGroup.shutdownGracefully()
            }
        }
    }

    /**
     * Ordem importa: tudo que enfileira escrita (salas, sessões, conquistas) tem que rodar ANTES do flush final do
     * write-behind. Antes os canais só caíam no shutdown do Netty, depois de [WriteBehindManager.stop]; o
     * `HabboSession.close()` enfileirava badges/último acesso num worker já parado e tudo se perdia.
     */
    override fun close() {
        if (started) {
            closing = true
            isRunning = false
            handoffServer?.close()
            handoffServer = null
            habboSessionManager.stopHeartbeat()
            ovh.rwx.habbo.metrics.ServerMetrics.stop()
            log.info("Shutting down ${BuildConfig.NAME} server...")
            // 1. Para de aceitar conexões (o finally de start() acorda, mas não derruba os event loops: closing)
            log.debug("Closing listeners...")
            serverChannels.forEach { it.close().awaitUninterruptibly() }
            log.debug("Done!")
            habboGame.close()
            // 2. Salas: stopLoop() tira as entidades (avisando o cliente) e salva sala, itens e pets
            log.debug("Closing all loaded rooms...")
            habboGame.roomManager.roomTaskManager.rooms.toList().forEach {
                habboGame.roomManager.roomTaskManager.removeRoom(it)
            }
            log.debug("Done!")
            // 3. Sessões: removeHabboSession() chama HabboSession.close(), que enfileira badges e último acesso. Os canais ficam abertos até o fim (o channelUnregistered depois não acha mais a sessão)
            log.debug("Closing all sessions...")
            habboSessionManager.habboSessions.values.toList().forEach { session ->
                try {
                    habboSessionManager.removeHabboSession(session.channel)
                } catch (e: Exception) {
                    log.error("Error closing session of {}", session.channel.ip(), e)
                }
            }
            habboSessionManager.fastFoodSessions.values.toList().forEach { session ->
                try {
                    habboSessionManager.removeFastFoodSession(session.channel)
                } catch (e: Exception) {
                    log.error("Error closing FastFood session", e)
                }
            }
            log.debug("Done!")
            // 4. Conquistas
            log.debug("Saving queued achievements...")
            habboGame.achievementManager.saveQueuedAchievements()
            log.debug("Done!")
            // 5. Flush final do write-behind: nada mais enfileira depois daqui
            log.debug("Flushing write-behind persistence...")
            WriteBehindManager.stop()
            log.debug("Done!")
            // 5b. Hi-Lo: devolve os blocos reservados no boot e não usados (senão cada restart queima um por sequência)
            ovh.rwx.habbo.database.sequence.HiLoSequence.releaseAllUnusedBlocks()
            // 6. Netty (derruba os canais dos clientes)
            log.debug("Shutting down Netty server...")
            bossGroup.shutdownGracefully().awaitUninterruptibly()
            workerGroup.shutdownGracefully().awaitUninterruptibly()
            log.debug("Done!")
            // Start database
            log.debug("Closing database connection pool...")
            hikariDataSource.close()
            log.debug("Done!")
            // End database
            // Start plugins
            pluginManager.close()
            // End plugins
            log.info("Done!")
        }
    }

    private fun adoptFromHandoff(
        socketPath: String,
        stringEncoder: StringEncoder,
        habboNettyEncoder: HabboNettyEncoder,
        habboNettyHandler: HabboNettyHandler,
        habboNettyWebSocketHandler: HabboNettyWebSocketHandler,
        habboTcpInit: ChannelInitializer<SocketChannel>,
        habboWsInit: ChannelInitializer<SocketChannel>,
        fastFoodTcpInit: ChannelInitializer<SocketChannel>,
        fastFoodWsInit: ChannelInitializer<SocketChannel>
    ) {
        log.info("Adopting connections from previous instance via {}...", socketPath)
        val received = ovh.rwx.habbo.handoff.HandoffClient.receive(socketPath)
        try {
            val payload = received.payload
            val decoder = java.util.Base64.getDecoder()
            val adoptedChannels = mutableListOf<Channel>()

            received.listenerFds.forEachIndexed { idx, fd ->
                val type = payload.listenerTypes.getOrNull(idx) ?: return@forEachIndexed
                val init = when (type) {
                    "habbo-tcp" -> habboTcpInit
                    "habbo-ws" -> habboWsInit
                    "fastfood-tcp" -> fastFoodTcpInit
                    else -> fastFoodWsInit
                }
                val ch = EpollServerSocketChannel(fd.intValue())
                ch.config().isAutoRead = true
                ch.pipeline().addLast(object : io.netty.channel.ChannelInboundHandlerAdapter() {
                    override fun channelRead(ctx: io.netty.channel.ChannelHandlerContext, msg: Any) {
                        val child = msg as SocketChannel
                        child.config().setOption(ChannelOption.SO_KEEPALIVE, true)
                        child.pipeline().addLast(init)
                        workerGroup.register(child)
                    }
                })
                bossGroup.register(ch).syncUninterruptibly()
                serverChannels += ch
            }

            // If any listener wasn't passed, bind fresh
            if (serverChannels.isEmpty()) {
                serverChannels += habboServerBootstrap.bind(habboConfig.port).sync().channel()
                serverChannels += habboWebSocketServerBootstrap.bind(habboConfig.wsPort).sync().channel()
                serverChannels += fastFoodServerBootstrap.bind(habboConfig.port + 10).sync().channel()
                serverChannels += fastFoodWebSocketServerBootstrap.bind(habboConfig.fastFoodWsPort).sync().channel()
            }

            val restoredRooms = mutableMapOf<Int, ovh.rwx.habbo.game.room.Room>()
            payload.rooms.forEach { roomSnap ->
                val room = habboGame.roomManager.rooms[roomSnap.roomId] ?: return@forEach
                habboGame.roomManager.roomTaskManager.registerRestoredRoom(
                    room,
                    roomSnap.tickCounter,
                    roomSnap.rollerCounter,
                    roomSnap.roomTimer
                )
                room.userManager.reconcilePetPositions(
                    roomSnap.pets.map {
                        ovh.rwx.habbo.game.room.managers.RoomUserManager.PetRestoreInfo(
                            it.petId, it.virtualId, it.x, it.y, it.z, it.headRotation, it.bodyRotation
                        )
                    }
                )
                restoredRooms[roomSnap.roomId] = room
            }

            payload.sessions.forEach { snap ->
                val fd = received.sessionFds[snap.fdIndex]
                val ch = io.netty.channel.epoll.EpollSocketChannel(fd.intValue())
                ch.config().isAutoRead = false
                ch.config().setOption(ChannelOption.SO_KEEPALIVE, true)

                val session = ovh.rwx.habbo.game.user.HabboSession(ch)
                if (!session.restoreFromSnapshot(snap)) {
                    log.warn("Failed to restore session for userId={}, closing adopted channel", snap.userId)
                    ch.close()
                    return@forEach
                }

                snap.cryptoIn?.let { session.rc4Encryption = restoreCrypto(it) }
                snap.cryptoOut?.let { session.rc4EncryptionOut = restoreCrypto(it) }

                val rc4Decoder = if (habboConfig.encryptionConfig.rc4) {
                    HabboNettyRC4Decoder().apply { expectedBase64Length = snap.expectedBase64Length }
                } else null
                val habboDecoder = HabboNettyDecoder()

                ch.pipeline().apply {
                    addLast(IdleStateHandler(60, 0, 0))
                    if (snap.isWebSocket) {
                        addLast(io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder(true, false, 65536))
                        addLast(io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder(false))
                        addLast(habboNettyWebSocketHandler)
                    }
                    addLast(stringEncoder)
                    addLast(habboNettyEncoder)
                    if (rc4Decoder != null) addLast(rc4Decoder)
                    addLast(habboDecoder)
                    addLast(habboNettyHandler)
                }

                habboSessionManager.registerAdoptedSession(ch, session)
                workerGroup.register(ch).syncUninterruptibly()

                if (snap.currentRoomId != null && snap.roomUser != null) {
                    val room = restoredRooms[snap.currentRoomId]
                    val ru = snap.roomUser
                    val restoredUser = room?.userManager?.restoreUser(
                        session = session,
                        virtualId = ru.virtualId,
                        pos = ovh.rwx.habbo.util.Vector3(ru.x, ru.y, ru.z),
                        headRot = ru.headRotation,
                        bodyRot = ru.bodyRotation,
                        idle = ru.idle,
                        danceId = ru.danceId,
                        handItem = ru.handItem,
                        effectId = ru.effectId,
                        statuses = ru.statuses
                    )
                    if (restoredUser != null && ru.objectiveX != null && ru.objectiveY != null) {
                        restoredUser.moveTo(
                            ovh.rwx.habbo.util.Vector2(ru.objectiveX, ru.objectiveY),
                            ru.objectiveRotation
                        )
                    }
                }

                val pendingRc4 = if (snap.pendingRc4BytesB64.isNotEmpty()) decoder.decode(snap.pendingRc4BytesB64) else ByteArray(0)
                val pendingDec = if (snap.pendingDecoderBytesB64.isNotEmpty()) decoder.decode(snap.pendingDecoderBytesB64) else ByteArray(0)
                if (pendingRc4.isNotEmpty() || pendingDec.isNotEmpty()) {
                    ch.eventLoop().execute {
                        if (pendingRc4.isNotEmpty()) {
                            ch.pipeline().fireChannelRead(io.netty.buffer.Unpooled.wrappedBuffer(pendingRc4))
                        }
                        if (pendingDec.isNotEmpty() && rc4Decoder != null) {
                            ch.pipeline().context(rc4Decoder)?.fireChannelRead(io.netty.buffer.Unpooled.wrappedBuffer(pendingDec))
                        } else if (pendingDec.isNotEmpty()) {
                            ch.pipeline().fireChannelRead(io.netty.buffer.Unpooled.wrappedBuffer(pendingDec))
                        }
                    }
                }

                adoptedChannels += ch
            }

            adoptedChannels.forEach { it.config().isAutoRead = true }
            ovh.rwx.habbo.handoff.SystemdNotify.notifyReady()
            received.sendAck()
            log.info("Adopted {} sessions and {} rooms via handoff!", adoptedChannels.size, restoredRooms.size)
        } catch (t: Throwable) {
            log.error("Handoff adoption failed, sending NACK", t)
            runCatching { received.sendNack(t.message ?: t.javaClass.simpleName) }
            throw t
        } finally {
            received.close()
        }
    }

    private fun quiesceForHandoff(): Pair<ovh.rwx.habbo.handoff.HandoffPayload, List<Int>> {
        log.info("Quiescing server for seamless handoff...")
        habboGame.pausedForHandoff = true
        habboSessionManager.stopHeartbeat()
        ovh.rwx.habbo.metrics.ServerMetrics.stop()

        // Close unauthenticated and FastFood sessions cleanly
        habboSessionManager.habboSessions.values.toList().forEach { session ->
            if (!session.authenticated) {
                runCatching { session.channel.close().awaitUninterruptibly() }
            } else {
                session.channel.config().isAutoRead = false
            }
        }
        habboSessionManager.fastFoodSessions.values.toList().forEach { session ->
            runCatching {
                habboSessionManager.removeFastFoodSession(session.channel)
                session.channel.close().awaitUninterruptibly()
            }
        }

        // Drain in-flight packet handlers before pausing rooms and flushing DB
        habboSessionManager.habboSessions.values
            .filter { it.authenticated && it.channel.isActive }
            .forEach { it.drainPacketDispatcherBlocking() }

        // Pause all active rooms without kicking entities
        val activeRooms = habboGame.roomManager.roomTaskManager.rooms.toList()
        activeRooms.forEach { it.pauseForHandoff() }

        // Drain outboxes on event loops
        val authSessions = habboSessionManager.habboSessions.values.filter { it.authenticated && it.channel.isActive }
        authSessions.forEach { it.flushOutboxBlocking() }

        // Flush achievements + write-behind + release unused Hi-Lo blocks before new JVM warms up
        habboGame.achievementManager.saveQueuedAchievements()
        WriteBehindManager.flushAllAndPause()
        ovh.rwx.habbo.database.sequence.HiLoSequence.releaseAllUnusedBlocks()

        // Stop external plugins (releases web_port)
        pluginManager.close()

        val encoder = java.util.Base64.getEncoder()
        val fds = mutableListOf<Int>()
        val listenerTypes = mutableListOf<String>()
        serverChannels.forEachIndexed { idx, ch ->
            if (ch is io.netty.channel.unix.UnixChannel) {
                fds += ch.fd().intValue()
                listenerTypes += when (idx) {
                    0 -> "habbo-tcp"
                    1 -> "habbo-ws"
                    2 -> "fastfood-tcp"
                    else -> "fastfood-ws"
                }
            }
        }

        val sessionSnapshots = authSessions.mapIndexed { idx, session ->
            val ch = session.channel as io.netty.channel.unix.UnixChannel
            fds += ch.fd().intValue()
            val rc4Dec = session.channel.pipeline().get(HabboNettyRC4Decoder::class.java)
            val habboDec = session.channel.pipeline().get(HabboNettyDecoder::class.java)
            val pendingRc4 = rc4Dec?.drainPendingBytes() ?: ByteArray(0)
            val pendingDec = habboDec?.drainPendingBytes() ?: ByteArray(0)

            val ru = session.roomUser
            val roomUserSnap = if (ru != null && session.currentRoom != null) {
                ovh.rwx.habbo.handoff.RoomUserSnapshot(
                    virtualId = ru.virtualID,
                    x = ru.currentVector3.x,
                    y = ru.currentVector3.y,
                    z = ru.currentVector3.z,
                    headRotation = ru.headRotation,
                    bodyRotation = ru.bodyRotation,
                    idle = ru.idle,
                    danceId = ru.danceId,
                    handItem = ru.handItem,
                    effectId = ru.effect?.effectId ?: 0,
                    statuses = ru.statusMap.mapValues { it.value.second },
                    objectiveX = ru.objectiveVector2?.x,
                    objectiveY = ru.objectiveVector2?.y,
                    objectiveRotation = ru.objectiveRotation
                )
            } else null

            ovh.rwx.habbo.handoff.SessionSnapshot(
                fdIndex = idx,
                isWebSocket = session.isWebSocket,
                userId = session.userInformation.id,
                release = session.releaseForHandoff(),
                protocolEra = session.protocolEra.name,
                habboVersionIso = session.habboVersion.buildDate.toString(),
                habboVersionBuild = session.habboVersion.majorVersion,
                habboVersionPlatform = session.habboVersion.platform.name,
                habboVersionReleaseString = session.habboVersion.releaseString,
                uniqueId = session.uniqueID,
                osInformation = session.osInformation,
                isBot = session.isBot,
                cryptoIn = session.rc4Encryption?.let { snapshotCrypto(it) },
                cryptoOut = session.rc4EncryptionOut?.let { snapshotCrypto(it) },
                expectedBase64Length = rc4Dec?.expectedBase64Length ?: 0,
                pendingRc4BytesB64 = encoder.encodeToString(pendingRc4),
                pendingDecoderBytesB64 = encoder.encodeToString(pendingDec),
                currentRoomId = session.currentRoom?.roomData?.id,
                roomUser = roomUserSnap
            )
        }

        val roomSnapshots = activeRooms.map { room ->
            val pets = room.userManager.entities.values
                .filterIsInstance<ovh.rwx.habbo.game.room.user.RoomPet>()
                .map { pet ->
                    ovh.rwx.habbo.handoff.PetSnapshot(
                        petId = pet.petData.id,
                        virtualId = pet.virtualID,
                        x = pet.currentVector3.x,
                        y = pet.currentVector3.y,
                        z = pet.currentVector3.z,
                        headRotation = pet.headRotation,
                        bodyRotation = pet.bodyRotation
                    )
                }
            ovh.rwx.habbo.handoff.RoomSnapshot(
                roomId = room.roomData.id,
                tickCounter = room.tickCounter,
                rollerCounter = room.rollerCounter.get(),
                roomTimer = room.roomTimer.get(),
                pets = pets
            )
        }

        return ovh.rwx.habbo.handoff.HandoffPayload(
            schemaVersion = 1,
            listenerTypes = listenerTypes,
            sessions = sessionSnapshots,
            rooms = roomSnapshots
        ) to fds
    }

    private fun resumeFromFailedHandoff() {
        log.warn("Resuming server after failed handoff...")
        habboGame.pausedForHandoff = false
        WriteBehindManager.resume(applicationScope)
        habboGame.roomManager.roomTaskManager.rooms.forEach { it.resumeAfterFailedHandoff() }
        pluginManager.loadPlugins(ClassIndex.classesOf(ClassIndex.Kind.PLUGIN_LISTENER).map { it.asSubclass(PluginListener::class.java) })
        pluginManager.loadPluginsFromDir(File("plugins"))
        habboSessionManager.habboSessions.values.forEach { it.channel.config().isAutoRead = true }
        habboSessionManager.startHeartbeat(serverScheduledExecutor, habboConfig.heartbeatIntervalSeconds)
        ovh.rwx.habbo.metrics.ServerMetrics.start(habboConfig.metricsSampleIntervalMs)
    }

    private fun finalizeHandoffExit() {
        log.info("Handoff ACK received! Exiting old instance cleanly without closing client sockets...")
        closing = true
        isRunning = false
        habboSessionManager.habboSessions.values.forEach {
            it.channel.attr(HabboSessionManager.handedOffAttributeKey).set(true)
        }
        hikariDataSource.close()
        exitProcess(0)
    }

    private fun snapshotCrypto(crypto: ovh.rwx.habbo.encryption.IHabboEncryption): ovh.rwx.habbo.handoff.CryptoSnapshot? =
        when (crypto) {
            is ovh.rwx.habbo.encryption.RC4Encryption -> {
                val s = crypto.snapshot()
                ovh.rwx.habbo.handoff.CryptoSnapshot(type = "RC4", state = s.state, x = s.x, y = s.y)
            }
            is ovh.rwx.habbo.encryption.wedgie.WedgieWrapper -> {
                val s = crypto.snapshot()
                ovh.rwx.habbo.handoff.CryptoSnapshot(
                    type = "WEDGIE",
                    innerState = s.inner.sBox, innerI = s.inner.i, innerJ = s.inner.j,
                    outerState = s.outer.sBox, outerI = s.outer.i, outerJ = s.outer.j,
                    rngSeed = s.rng.seed, rngModulus = s.rng.modulus
                )
            }
            else -> null
        }

    private fun restoreCrypto(snap: ovh.rwx.habbo.handoff.CryptoSnapshot): ovh.rwx.habbo.encryption.IHabboEncryption? =
        when (snap.type) {
            "RC4" -> ovh.rwx.habbo.encryption.RC4Encryption.fromSnapshot(
                ovh.rwx.habbo.encryption.RC4Encryption.Snapshot(snap.state!!, snap.x, snap.y)
            )
            "WEDGIE" -> ovh.rwx.habbo.encryption.wedgie.WedgieWrapper.fromSnapshot(
                ovh.rwx.habbo.encryption.wedgie.WedgieWrapper.Snapshot(
                    ovh.rwx.habbo.encryption.WedgieRC4.Snapshot(snap.innerState!!, snap.innerI, snap.innerJ),
                    ovh.rwx.habbo.encryption.WedgieRC4.Snapshot(snap.outerState!!, snap.outerI, snap.outerJ),
                    ovh.rwx.habbo.encryption.wedgie.HabboRandom.Snapshot(snap.rngSeed, snap.rngModulus)
                )
            )
            else -> null
        }
}
