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

package ovh.rwx.habbo.handoff

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.epoll.EpollChannelOption
import io.netty.channel.epoll.EpollDomainSocketChannel
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerDomainSocketChannel
import io.netty.channel.unix.DomainSocketAddress
import io.netty.channel.unix.DomainSocketReadMode
import io.netty.channel.unix.FileDescriptor
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.MessageToMessageEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val MAX_CONTROL_FRAME_BYTES = 64 * 1024 * 1024
private const val DEFAULT_ACK_TIMEOUT_MS = 15_000L

private val handoffJson: ObjectMapper = ObjectMapper().registerKotlinModule()

data class HandoffControlMessage(
    val type: String,
    val schemaVersion: Int = 1,
    val pid: Long = 0,
    val payload: HandoffPayload? = null,
    val reason: String? = null,
)

data class ReceivedHandoff(
    val payload: HandoffPayload,
    val listenerFds: List<FileDescriptor>,
    val sessionFds: List<FileDescriptor>,
    val sendAck: () -> Unit,
    val sendNack: (String) -> Unit,
    val close: () -> Unit,
)

private class ControlMessageEncoder : MessageToMessageEncoder<HandoffControlMessage>() {
    override fun encode(ctx: ChannelHandlerContext, msg: HandoffControlMessage, out: MutableList<Any>) {
        val bytes = handoffJson.writeValueAsBytes(msg)
        out.add(ctx.alloc().buffer(bytes.size).writeBytes(bytes))
    }
}

private fun addControlCodec(channel: Channel) {
    channel.pipeline().addLast(
        LengthFieldBasedFrameDecoder(MAX_CONTROL_FRAME_BYTES, 0, 4, 0, 4),
        LengthFieldPrepender(4),
        ControlMessageEncoder(),
    )
}

class HandoffServer(
    private val socketPath: String,
    private val onQuiesceAndSnapshot: () -> Pair<HandoffPayload, List<Int>>,
    private val onAck: () -> Unit,
    private val onRollback: (String) -> Unit,
) : AutoCloseable {
    private val fdSocketPath = "$socketPath.fd"
    private val closing = AtomicBoolean(false)

    private var group: EventLoopGroup? = null
    private var snapshotExecutor: ExecutorService? = null
    private var controlServerChannel: Channel? = null
    private var fdServerChannel: Channel? = null

    @Volatile
    private var activeControlChannel: Channel? = null

    @Volatile
    private var activeFdChannelFuture: CompletableFuture<Channel> = CompletableFuture()

    fun start(): HandoffServer {
        deleteSocketFiles()

        val loopGroup = MultiThreadIoEventLoopGroup(1, EpollIoHandler.newFactory())
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "handoff-snapshot").apply { isDaemon = true }
        }
        group = loopGroup
        snapshotExecutor = executor

        try {
            fdServerChannel = ServerBootstrap()
                .group(loopGroup)
                .channel(EpollServerDomainSocketChannel::class.java)
                .childOption(EpollChannelOption.DOMAIN_SOCKET_READ_MODE, DomainSocketReadMode.BYTES)
                .childHandler(object : ChannelInitializer<EpollDomainSocketChannel>() {
                    override fun initChannel(ch: EpollDomainSocketChannel) {
                        activeFdChannelFuture.complete(ch)
                    }
                })
                .bind(DomainSocketAddress(fdSocketPath))
                .sync()
                .channel()

            controlServerChannel = ServerBootstrap()
                .group(loopGroup)
                .channel(EpollServerDomainSocketChannel::class.java)
                .childOption(EpollChannelOption.DOMAIN_SOCKET_READ_MODE, DomainSocketReadMode.BYTES)
                .childHandler(object : ChannelInitializer<EpollDomainSocketChannel>() {
                    override fun initChannel(ch: EpollDomainSocketChannel) {
                        activeControlChannel = ch
                        addControlCodec(ch)
                        ch.pipeline().addLast(ServerControlHandler(executor))
                    }
                })
                .bind(DomainSocketAddress(socketPath))
                .sync()
                .channel()
        } catch (t: Throwable) {
            close()
            throw t
        }

        return this
    }

    private fun deleteSocketFiles() {
        Files.deleteIfExists(Path.of(socketPath))
        Files.deleteIfExists(Path.of(fdSocketPath))
    }

    override fun close() {
        closing.set(true)
        activeControlChannel?.close()?.awaitUninterruptibly(2, TimeUnit.SECONDS)
        activeFdChannelFuture.getNow(null)?.close()?.awaitUninterruptibly(2, TimeUnit.SECONDS)
        controlServerChannel?.close()?.awaitUninterruptibly(2, TimeUnit.SECONDS)
        fdServerChannel?.close()?.awaitUninterruptibly(2, TimeUnit.SECONDS)
        snapshotExecutor?.shutdownNow()
        group?.shutdownGracefully(0, 2, TimeUnit.SECONDS)?.syncUninterruptibly()
        deleteSocketFiles()
    }

    private inner class ServerControlHandler(
        private val executor: ExecutorService,
    ) : SimpleChannelInboundHandler<ByteBuf>() {
        private val helloReceived = AtomicBoolean(false)
        private val decided = AtomicBoolean(false)

        @Volatile
        private var ackTimeoutFuture: ScheduledFuture<*>? = null

        private fun finishAck() {
            if (decided.compareAndSet(false, true)) {
                ackTimeoutFuture?.cancel(false)
                onAck()
            }
        }

        private fun finishRollback(reason: String, ctx: ChannelHandlerContext? = null) {
            if (decided.compareAndSet(false, true)) {
                ackTimeoutFuture?.cancel(false)
                onRollback(reason)
                ctx?.close()
                activeFdChannelFuture.getNow(null)?.close()
            }
        }

        override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
            val message = handoffJson.readValue<HandoffControlMessage>(ByteBufUtil.getBytes(msg))
            when (message.type) {
                "HELLO" -> {
                    if (!helloReceived.compareAndSet(false, true)) return
                    activeFdChannelFuture.orTimeout(DEFAULT_ACK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .thenAcceptAsync({ fdChannel ->
                            try {
                                val (payload, fds) = onQuiesceAndSnapshot()
                                val expectedCount = payload.listenerTypes.size + payload.sessions.size
                                require(fds.size == expectedCount) {
                                    "Expected $expectedCount FDs for snapshot, got ${fds.size}"
                                }

                                ackTimeoutFuture = ctx.executor().schedule({
                                    finishRollback("Timed out waiting for ACK", ctx)
                                }, DEFAULT_ACK_TIMEOUT_MS, TimeUnit.MILLISECONDS)

                                ctx.writeAndFlush(
                                    HandoffControlMessage(
                                        type = "SNAPSHOT",
                                        schemaVersion = payload.schemaVersion,
                                        pid = ProcessHandle.current().pid(),
                                        payload = payload,
                                    )
                                ).sync()

                                for (fd in fds) {
                                    fdChannel.writeAndFlush(FileDescriptor(fd)).sync()
                                }
                            } catch (t: Throwable) {
                                finishRollback(t.message ?: "Snapshot transfer failed", ctx)
                            }
                        }, executor)
                        .exceptionally { err ->
                            finishRollback(err.cause?.message ?: err.message ?: "FD channel unavailable", ctx)
                            null
                        }
                }

                "ACK" -> finishAck()
                "NACK" -> finishRollback(message.reason ?: "NACK received", ctx)
            }
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            if (helloReceived.get() && !closing.get()) {
                finishRollback("Control channel closed before ACK", ctx)
            }
        }

        @Deprecated("Netty SimpleChannelInboundHandler override")
        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            if (helloReceived.get() && !closing.get()) {
                finishRollback(cause.message ?: "Control channel error", ctx)
            } else {
                ctx.close()
            }
        }
    }
}

object HandoffClient {
    fun receive(socketPath: String, timeoutMs: Long = DEFAULT_ACK_TIMEOUT_MS): ReceivedHandoff {
        val group = MultiThreadIoEventLoopGroup(1, EpollIoHandler.newFactory())
        val fdQueue = LinkedBlockingQueue<FileDescriptor>()
        val snapshotFuture = CompletableFuture<HandoffPayload>()
        var fdChannel: Channel? = null
        var controlChannel: Channel? = null

        fun cleanup() {
            controlChannel?.close()?.awaitUninterruptibly(2, TimeUnit.SECONDS)
            fdChannel?.close()?.awaitUninterruptibly(2, TimeUnit.SECONDS)
            group.shutdownGracefully(0, 2, TimeUnit.SECONDS).syncUninterruptibly()
        }

        try {
            fdChannel = Bootstrap()
                .group(group)
                .channel(EpollDomainSocketChannel::class.java)
                .option(EpollChannelOption.DOMAIN_SOCKET_READ_MODE, DomainSocketReadMode.FILE_DESCRIPTORS)
                .handler(object : ChannelInitializer<EpollDomainSocketChannel>() {
                    override fun initChannel(ch: EpollDomainSocketChannel) {
                        ch.pipeline().addLast(object : SimpleChannelInboundHandler<FileDescriptor>() {
                            override fun channelRead0(ctx: ChannelHandlerContext, msg: FileDescriptor) {
                                fdQueue.offer(msg)
                            }

                            @Deprecated("Netty SimpleChannelInboundHandler override")
                            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                                snapshotFuture.completeExceptionally(cause)
                                ctx.close()
                            }
                        })
                    }
                })
                .connect(DomainSocketAddress("$socketPath.fd"))
                .sync()
                .channel()

            controlChannel = Bootstrap()
                .group(group)
                .channel(EpollDomainSocketChannel::class.java)
                .option(EpollChannelOption.DOMAIN_SOCKET_READ_MODE, DomainSocketReadMode.BYTES)
                .handler(object : ChannelInitializer<EpollDomainSocketChannel>() {
                    override fun initChannel(ch: EpollDomainSocketChannel) {
                        addControlCodec(ch)
                        ch.pipeline().addLast(object : SimpleChannelInboundHandler<ByteBuf>() {
                            override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
                                val message = handoffJson.readValue<HandoffControlMessage>(ByteBufUtil.getBytes(msg))
                                when (message.type) {
                                    "SNAPSHOT" -> {
                                        val payload = message.payload
                                        if (payload != null) {
                                            snapshotFuture.complete(payload)
                                        } else {
                                            snapshotFuture.completeExceptionally(
                                                IllegalStateException("SNAPSHOT message missing payload")
                                            )
                                        }
                                    }

                                    "NACK" -> snapshotFuture.completeExceptionally(
                                        IllegalStateException(message.reason ?: "Server sent NACK")
                                    )
                                }
                            }

                            override fun channelInactive(ctx: ChannelHandlerContext) {
                                if (!snapshotFuture.isDone) {
                                    snapshotFuture.completeExceptionally(
                                        IllegalStateException("Control channel closed before SNAPSHOT")
                                    )
                                }
                            }

                            @Deprecated("Netty SimpleChannelInboundHandler override")
                            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                                snapshotFuture.completeExceptionally(cause)
                                ctx.close()
                            }
                        })
                    }
                })
                .connect(DomainSocketAddress(socketPath))
                .sync()
                .channel()

            val deadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)

            controlChannel.writeAndFlush(
                HandoffControlMessage(
                    type = "HELLO",
                    pid = ProcessHandle.current().pid(),
                )
            ).sync()

            val payload = snapshotFuture.get(timeoutMs, TimeUnit.MILLISECONDS)
            val totalFds = payload.listenerTypes.size + payload.sessions.size
            val fds = ArrayList<FileDescriptor>(totalFds)
            while (fds.size < totalFds) {
                val remainingNs = deadlineNs - System.nanoTime()
                if (remainingNs <= 0) {
                    throw IllegalStateException("Timed out waiting for file descriptors (${fds.size}/$totalFds)")
                }
                val fd = fdQueue.poll(remainingNs, TimeUnit.NANOSECONDS)
                    ?: throw IllegalStateException("Timed out waiting for file descriptors (${fds.size}/$totalFds)")
                fds.add(fd)
            }

            val listenerCount = payload.listenerTypes.size
            val activeControl = controlChannel
            return ReceivedHandoff(
                payload = payload,
                listenerFds = fds.subList(0, listenerCount).toList(),
                sessionFds = fds.subList(listenerCount, totalFds).toList(),
                sendAck = {
                    activeControl.writeAndFlush(
                        HandoffControlMessage(
                            type = "ACK",
                            pid = ProcessHandle.current().pid(),
                        )
                    ).sync()
                },
                sendNack = { reason ->
                    activeControl.writeAndFlush(
                        HandoffControlMessage(
                            type = "NACK",
                            pid = ProcessHandle.current().pid(),
                            reason = reason,
                        )
                    ).sync()
                },
                close = ::cleanup,
            )
        } catch (t: Throwable) {
            cleanup()
            throw t
        }
    }
}
