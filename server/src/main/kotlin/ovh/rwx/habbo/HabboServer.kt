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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jdbi.v3.core.Jdbi
import org.reflections8.Reflections
import org.reflections8.util.ClasspathHelper
import org.reflections8.util.ConfigurationBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.fastfood.communication.FastFoodHandler
import ovh.rwx.habbo.communication.HabboHandler
import ovh.rwx.habbo.config.HabboConfig
import ovh.rwx.habbo.console.ServerConsole
import ovh.rwx.habbo.console.ServerControlSocket
import ovh.rwx.habbo.database.DatabaseManager
import ovh.rwx.habbo.database.migration.FlywayMigration
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.encryption.HabboEncryptionHandler
import ovh.rwx.habbo.game.HabboGame
import ovh.rwx.habbo.game.user.HabboSessionManager
import ovh.rwx.habbo.netty.*
import ovh.rwx.habbo.plugin.core.PluginManager
import java.io.File
import java.security.Security
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.system.exitProcess

object HabboServer : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val habboConfig: HabboConfig = ObjectMapper(YAMLFactory()).registerKotlinModule().readValue(File("config.yaml"))
    val pluginManager: PluginManager = PluginManager()
    // SQL
    val hikariDataSource: HikariDataSource
    val jdbi: Jdbi
    // Netty
    private val habboServerBootstrap: ServerBootstrap
    private val habboWebSocketServerBootstrap: ServerBootstrap
    //private val fastFoodServerBootstrap: ServerBootstrap
    private val workerGroup: EventLoopGroup
    private val bossGroup: EventLoopGroup
    // Habbo
    val habboEncryptionHandler: HabboEncryptionHandler
    val habboSessionManager: HabboSessionManager
    val habboHandler: HabboHandler
    val fastFoodHandler: FastFoodHandler
    val habboGame: HabboGame
    // Thread Executors
    val serverScheduledExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(3 + if (habboConfig.roomTaskConfig.threads == 0) 1 else habboConfig.roomTaskConfig.threads)
    val cachedExecutorDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    val applicationScope = CoroutineScope(cachedExecutorDispatcher + SupervisorJob())
    val serverConsole: ServerConsole = ServerConsole(this)
    val serverControlSocket: ServerControlSocket = ServerControlSocket(this, serverConsole)
    val DATE_TIME_FORMATTER_WITH_HOURS: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val DATE_TIME_FORMATTER_ONLY_DAYS: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val reflections: Reflections
        get() = Reflections(ConfigurationBuilder().addUrls(ClasspathHelper.forClassLoader()).addUrls(ClasspathHelper.forManifest()).addUrls(ClasspathHelper.forJavaClassPath()))
    val started: Boolean
        get() = !workerGroup.isShuttingDown && !bossGroup.isShuttingDown

    init {
        Security.addProvider(BouncyCastleProvider())

        javaClass.classLoader.getResourceAsStream("ascii_art.txt")?.bufferedReader()?.forEachLine { log.info(it) }

        log.info("")
        log.info("Version: ${BuildConfig.VERSION}.")
        log.info("Built in: ${DATE_TIME_FORMATTER_WITH_HOURS.format(BuildConfig.BUILD_INSTANT.atZone(ZoneId.systemDefault()).toLocalDateTime())}")
        log.info("Git commit hash: ${BuildConfig.GIT_COMMIT_FULL}.")
        log.info("By jomp16 and Lucas.")
        log.info("Credits for developers of IDK, Phoenix, Butterfly, Uber, Azure, Nova and probably other niggas for code and packets.")
        log.info("Licensed under GPLv3. See https://www.gnu.org/licenses/gpl-3.0.en.html")
        log.info("")
        log.info("Loading ${BuildConfig.NAME} server...")
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
        habboEncryptionHandler = HabboEncryptionHandler(habboConfig.encryptionConfig.rsaConfig.n, habboConfig.encryptionConfig.rsaConfig.d, habboConfig.encryptionConfig.rsaConfig.e)
        log.info("Done!")

        log.info("Loading Habbo game...")
        habboGame = HabboGame()
        log.info("Done!")
        log.info("Loading FastFood handler...")
        fastFoodHandler = FastFoodHandler()
        log.info("Done!")

        log.info("Loading Netty...")
        habboSessionManager = HabboSessionManager()
        habboServerBootstrap = ServerBootstrap()
        habboWebSocketServerBootstrap = ServerBootstrap()
        //fastFoodServerBootstrap = ServerBootstrap()
        val ioHandlerFactory = if (Epoll.isAvailable()) EpollIoHandler.newFactory() else NioIoHandler.newFactory()
        workerGroup = MultiThreadIoEventLoopGroup(ioHandlerFactory)
        bossGroup = MultiThreadIoEventLoopGroup(ioHandlerFactory)
        log.info("Done!")

        log.info("Loading plugins...")
        pluginManager.loadPlugins()
        pluginManager.loadPluginsFromDir(File("plugins"))
        log.info("Done!")

        log.info("Adding shutdown hook...")
        Runtime.getRuntime().addShutdownHook(Thread { close() })
        log.info("Done!")
    }

    fun start() {
        try {
            serverControlSocket.start()
            serverConsole.startConsoleReader()
            val stringEncoder = StringEncoder(Charsets.UTF_8)
            val habboNettyEncoder = HabboNettyEncoder()
            val habboNettyHandler = HabboNettyHandler()
            val habboNettyWebSocketHandler = HabboNettyWebSocketHandler()
            //val fastFoodNettyEncoder = FastFoodNettyEncoder()
            //val fastFoodNettyHandler = FastFoodNettyHandler()
            habboServerBootstrap.group(bossGroup, workerGroup)
                    .channel(if (Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java)
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(socketChannel: SocketChannel) {
                            socketChannel.pipeline().apply {
                                addLast(IdleStateHandler(30, 10, 0))

                                addLast(stringEncoder)
                                addLast(habboNettyEncoder)

                                if (habboConfig.encryptionConfig.rc4) addLast(HabboNettyRC4Decoder())

                                addLast(HabboNettyDecoder())
                                addLast(habboNettyHandler)
                            }
                        }
                    })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            habboWebSocketServerBootstrap.group(bossGroup, workerGroup)
                .channel(if (Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(socketChannel: SocketChannel) {
                        socketChannel.pipeline().apply {
                            addLast(IdleStateHandler(30, 10, 0))
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
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
            /*fastFoodServerBootstrap.group(bossGroup, workerGroup)
                    .channel(if (Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java)
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(socketChannel: SocketChannel) {
                            socketChannel.pipeline().apply {
                                addLast(IdleStateHandler(30, 10, 0))

                                addLast(stringEncoder)
                                addLast(fastFoodNettyEncoder)
                                addLast(FastFoodNettyDecoder())
                                addLast(fastFoodNettyHandler)
                            }
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)*/
            val habboChannelFuture = habboServerBootstrap.bind(habboConfig.port)
            val habboWebSocketChannelFuture = habboWebSocketServerBootstrap.bind(habboConfig.wsPort)
            //val fastFoodChannelFuture = fastFoodServerBootstrap.bind(habboConfig.port + 1)
            habboChannelFuture.sync()
            habboWebSocketChannelFuture.sync()
            //fastFoodChannelFuture.sync()
            if (habboChannelFuture.isSuccess && habboWebSocketChannelFuture.isSuccess/* && fastFoodChannelFuture.isSuccess*/) {
                log.info("${BuildConfig.NAME} server started on port {}!", habboConfig.port)
                log.info("${BuildConfig.NAME} WebSocket server started on port {}!", habboConfig.wsPort)
                //log.info("FastFood server started on port {}!", habboConfig.port + 1)
                habboChannelFuture.channel().closeFuture().await()
                habboWebSocketChannelFuture.channel().closeFuture().await()
                //fastFoodChannelFuture.channel().closeFuture().sync()
            } else {
                log.error(
                    "Error starting ${BuildConfig.NAME} server!",
                    habboChannelFuture.cause() ?: habboWebSocketChannelFuture.cause()
                )

                exitProcess(1)
            }
        } catch (e: Exception) {
            log.error("An exception happened!", e)
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

    override fun close() {
        if (started) {
            serverControlSocket.close()
            log.info("Shutting down ${BuildConfig.NAME} server...")
            habboGame.close()
            // Start room
            log.debug("Closing all loaded rooms...")
            habboGame.roomManager.roomTaskManager.rooms.toList().forEach {
                habboGame.roomManager.roomTaskManager.removeRoom(it)
            }
            log.debug("Done!")
            // End room
            // Start achievements
            log.debug("Saving queued achievements...")
            habboGame.achievementManager.saveQueuedAchievements()
            log.debug("Done!")
            // End achievements
            // Flush Write-Behind before shutdown
            log.debug("Flushing write-behind persistence...")
            WriteBehindManager.stop()
            log.debug("Done!")
            // Start Netty
            log.debug("Shutting down Netty server...")
            bossGroup.shutdownGracefully().awaitUninterruptibly()
            workerGroup.shutdownGracefully().awaitUninterruptibly()
            log.debug("Done!")
            // End Netty
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
}
