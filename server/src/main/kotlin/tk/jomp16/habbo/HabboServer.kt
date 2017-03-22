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

package tk.jomp16.habbo

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.core.SessionFactory
import com.github.andrewoma.kwery.core.dialect.MysqlDialect
import com.zaxxer.hikari.HikariDataSource
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import net.sf.ehcache.CacheManager
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.communication.HabboHandler
import tk.jomp16.habbo.config.HabboConfig
import tk.jomp16.habbo.encryption.HabboEncryptionHandler
import tk.jomp16.habbo.game.HabboGame
import tk.jomp16.habbo.game.user.HabboSessionManager
import tk.jomp16.habbo.kotlin.cleanUpUsers
import tk.jomp16.habbo.netty.HabboNettyDecoder
import tk.jomp16.habbo.netty.HabboNettyEncoder
import tk.jomp16.habbo.netty.HabboNettyHandler
import tk.jomp16.habbo.netty.HabboNettyRC4Decoder
import tk.jomp16.habbo.plugin.listeners.catalog.CatalogCommandsListener
import tk.jomp16.habbo.plugin.listeners.room.RoomCommandsListener
import tk.jomp16.habbo.plugin.listeners.room.RoomCommandsManagerListener
import tk.jomp16.habbo.plugin.listeners.room.badge.RoomBadgeCommandsListener
import tk.jomp16.utils.plugin.core.PluginManager
import java.io.File
import java.security.Security
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

object HabboServer : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    lateinit var habboConfig: HabboConfig

    val pluginManager: PluginManager = PluginManager()
    val cacheManager: CacheManager = CacheManager.newInstance()

    // SQL
    lateinit private var hikariDataSource: HikariDataSource
    lateinit var databaseFactory: SessionFactory
        private set

    // Netty
    lateinit private var serverBootstrap: ServerBootstrap
    lateinit private var workerGroup: EventLoopGroup
    lateinit private var bossGroup: EventLoopGroup

    // Habbo
    lateinit var habboEncryptionHandler: HabboEncryptionHandler
        private set
    lateinit var habboGame: HabboGame
        private set
    lateinit var habboSessionManager: HabboSessionManager
        private set
    lateinit var habboHandler: HabboHandler
        private set

    // Thread Executors
    lateinit var serverScheduledExecutor: ScheduledExecutorService
        private set
    lateinit var serverExecutor: ExecutorService
        private set

    val DATE_TIME_FORMATTER_WITH_HOURS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val DATE_TIME_FORMATTER_ONLY_DAYS = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val started: Boolean
        get() {
            try {
                return !workerGroup.isShuttingDown && !bossGroup.isShuttingDown
            } catch (exception: UninitializedPropertyAccessException) {
                return false
            }
        }

    init {
        Security.addProvider(BouncyCastleProvider())

        Runtime.getRuntime().addShutdownHook(Thread { close() })
    }

    fun init() {
        // Instantiate thread executors
        serverScheduledExecutor = Executors.newScheduledThreadPool(3 + if (habboConfig.roomTaskConfig.threads == 0) 1 else habboConfig.roomTaskConfig.threads)
        serverExecutor = Executors.newCachedThreadPool()

        javaClass.classLoader.getResourceAsStream("ascii_art.txt").bufferedReader().forEachLine { log.info(it) }

        log.info("")
        log.info("Version: ${BuildConfig.VERSION}.")
        log.info("By jomp16 and Lucas.")
        log.info("Credits for developers of IDK, Phoenix, Butterfly, Uber, Azure, Nova and probably other niggas for code and packets.")
        log.info("Licensed under GPLv3. See http://www.gnu.org/licenses/")
        log.info("")
        log.info("Loading ${BuildConfig.NAME} emulator...")

        try {
            // Initialize database
            log.info("Initializing database...")

            hikariDataSource = HikariDataSource(habboConfig.databaseConfig.hikariConfig)
            databaseFactory = SessionFactory(hikariDataSource, MysqlDialect())

            log.info("Database initialized!")
            // Clean up things in database
            log.info("Cleaning up some things in database...")
            // START USERS
            log.debug("Fixing some data in users table...")
            cleanUpUsers()
            log.debug("Done!")

            // END USERS
            log.info("Done!")

            // Load HabboGame...
            log.info("Loading Habbo game...")
            habboEncryptionHandler = HabboEncryptionHandler(habboConfig.rsaConfig.n, habboConfig.rsaConfig.d, habboConfig.rsaConfig.e)
            habboHandler = HabboHandler()
            habboSessionManager = HabboSessionManager()
            habboGame = HabboGame()
            log.info("Done!")

            // load plugins
            log.info("Loading plugins...")
            pluginManager.addPlugin(RoomCommandsManagerListener())
            pluginManager.addPlugin(RoomCommandsListener())
            pluginManager.addPlugin(RoomBadgeCommandsListener())
            pluginManager.addPlugin(CatalogCommandsListener())
            pluginManager.loadPluginsFromDir(File("plugins"))
            log.info("Done!")
        } catch (e: Exception) {
            log.error("An error happened!", e)

            System.exit(1)
        }
    }

    fun start() {
        serverExecutor.execute {
            try {
                serverBootstrap = ServerBootstrap()
                workerGroup = if (Epoll.isAvailable()) EpollEventLoopGroup() else NioEventLoopGroup()
                bossGroup = if (Epoll.isAvailable()) EpollEventLoopGroup() else NioEventLoopGroup()

                val stringEncoder = StringEncoder(Charsets.UTF_8)
                val habboNettyEncoder = HabboNettyEncoder()
                val habboNettyHandler = HabboNettyHandler()

                serverBootstrap.group(bossGroup, workerGroup)
                        .channel(if (Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java)
                        .childHandler(object : ChannelInitializer<SocketChannel>() {
                            override fun initChannel(socketChannel: SocketChannel) {
                                socketChannel.pipeline().apply {
                                    addLast(IdleStateHandler(30, 10, 0))

                                    addLast(stringEncoder)
                                    addLast(habboNettyEncoder)

                                    if (habboConfig.rc4) addLast(HabboNettyRC4Decoder())

                                    addLast(HabboNettyDecoder())
                                    addLast(habboNettyHandler)
                                }
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)

                val channelFuture = serverBootstrap.bind(habboConfig.port)

                channelFuture.awaitUninterruptibly()

                if (channelFuture.isDone) {
                    if (channelFuture.isSuccess) {
                        log.info("${BuildConfig.NAME} server started on port {}!", habboConfig.port)

                        channelFuture.channel().closeFuture().awaitUninterruptibly()
                    } else {
                        log.error("Error starting ${BuildConfig.NAME} server!", channelFuture.cause())

                        System.exit(1)
                    }
                }
            } catch (e: Exception) {
                log.error("An exception happened!", e)
            } finally {
                workerGroup.shutdownGracefully()
                bossGroup.shutdownGracefully()
            }
        }
    }

    inline fun <R> database(rollbackTransaction: Boolean = false, crossinline task: Session.() -> R): R = serverExecutor.submit(
            Callable {
                databaseFactory.use { session ->
                    session.transaction { transaction ->
                        transaction.rollbackOnly = rollbackTransaction

                        session.task()
                    }
                }
            }
    ).get()

    override fun close() {
        if (started) {
            log.info("Shutting down ${BuildConfig.NAME} server...")

            // Start Netty
            log.debug("Shutting down Netty server...")
            bossGroup.shutdownGracefully().awaitUninterruptibly()
            workerGroup.shutdownGracefully().awaitUninterruptibly()
            log.debug("Done!")
            // End Netty

            // Start room
            log.debug("Closing all loaded rooms...")
            habboGame.roomManager.roomTaskManager.rooms.toList().forEach {
                habboGame.roomManager.roomTaskManager.removeRoomFromTask(it)
            }
            log.debug("Done!")
            // End room

            // Start database
            log.debug("Fixing some data in users table...")
            cleanUpUsers()
            log.debug("Done!")
            // End database

            // Start plugins
            pluginManager.close()
            // End plugins

            log.info("Done!")
        }
    }
}