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

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.unix.FileDescriptor
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class HandoffTransportTest {
    private fun tempSocketPath(): String = "/tmp/habbo-handoff-test-${UUID.randomUUID()}.sock"

    private class EchoHandler(private val reply: String) : SimpleChannelInboundHandler<String>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
            ctx.writeAndFlush("$reply\n")
        }
    }

    @Test
    fun `transfers live epoll server and client FDs and continues TCP traffic without dropping connection`() {
        Assume.assumeTrue("Requires Linux Epoll", Epoll.isAvailable())

        val socketPath = tempSocketPath()
        val group1 = MultiThreadIoEventLoopGroup(2, EpollIoHandler.newFactory())
        val group2 = MultiThreadIoEventLoopGroup(2, EpollIoHandler.newFactory())

        val acceptedChannelRef = AtomicReference<EpollSocketChannel>()
        val acceptedLatch = CountDownLatch(1)
        val ackLatch = CountDownLatch(1)
        val rollbackReason = AtomicReference<String?>()

        var oldServerChannel: EpollServerSocketChannel? = null
        var tcpClient: Socket? = null
        var handoffServer: HandoffServer? = null
        var received: ReceivedHandoff? = null
        var adoptedServer: EpollServerSocketChannel? = null
        var adoptedClient: EpollSocketChannel? = null

        try {
            oldServerChannel = ServerBootstrap()
                .group(group1)
                .channel(EpollServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<EpollSocketChannel>() {
                    override fun initChannel(ch: EpollSocketChannel) {
                        ch.pipeline().addLast(
                            LineBasedFrameDecoder(1024),
                            StringDecoder(Charsets.UTF_8),
                            StringEncoder(Charsets.UTF_8),
                            EchoHandler("PONG1"),
                        )
                        acceptedChannelRef.set(ch)
                        acceptedLatch.countDown()
                    }
                })
                .bind("127.0.0.1", 0)
                .sync()
                .channel() as EpollServerSocketChannel

            val port = oldServerChannel.localAddress().port
            tcpClient = Socket().apply {
                soTimeout = 5000
                connect(InetSocketAddress("127.0.0.1", port), 5000)
            }
            assertTrue("Server did not accept TCP client", acceptedLatch.await(5, TimeUnit.SECONDS))
            val oldClientChannel = acceptedChannelRef.get()

            val writer = PrintWriter(tcpClient.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(tcpClient.getInputStream()))
            writer.print("PING1\n")
            writer.flush()
            assertEquals("PONG1", reader.readLine())

            handoffServer = HandoffServer(
                socketPath = socketPath,
                onQuiesceAndSnapshot = {
                    oldServerChannel.eventLoop().submit {
                        oldServerChannel.config().isAutoRead = false
                    }.sync()
                    oldClientChannel.eventLoop().submit {
                        oldClientChannel.config().isAutoRead = false
                    }.sync()

                    val payload = HandoffPayload(
                        listenerTypes = listOf("TCP"),
                        sessions = listOf(
                            SessionSnapshot(
                                fdIndex = 0,
                                isWebSocket = false,
                                userId = 42,
                                release = "RELEASE63-201208142302",
                                protocolEra = "R63B",
                                habboVersionIso = "2012-08-14",
                                habboVersionBuild = 20120814,
                                uniqueId = "test-uid",
                                osInformation = "Linux",
                                isBot = false,
                                cryptoIn = null,
                                cryptoOut = null,
                                expectedBase64Length = 0,
                                pendingRc4BytesB64 = "",
                                pendingDecoderBytesB64 = "",
                                currentRoomId = null,
                                roomUser = null,
                            )
                        ),
                        rooms = emptyList(),
                    )
                    payload to listOf(
                        oldServerChannel.fd().intValue(),
                        oldClientChannel.fd().intValue(),
                    )
                },
                onAck = {
                    oldClientChannel.close()
                    oldServerChannel.close()
                    ackLatch.countDown()
                },
                onRollback = { reason -> rollbackReason.set(reason) },
            ).start()

            val handoff = HandoffClient.receive(socketPath)
            received = handoff

            assertEquals(1, handoff.listenerFds.size)
            assertEquals(1, handoff.sessionFds.size)
            assertEquals(listOf("TCP"), handoff.payload.listenerTypes)
            assertEquals(42, handoff.payload.sessions.single().userId)
            assertFalse(handoff.payload.sessions.single().isWebSocket)

            adoptedServer = EpollServerSocketChannel(handoff.listenerFds[0].intValue()).apply {
                config().isAutoRead = false
            }
            group2.register(adoptedServer).sync()

            adoptedClient = EpollSocketChannel(handoff.sessionFds[0].intValue()).apply {
                config().isAutoRead = false
                pipeline().addLast(
                    LineBasedFrameDecoder(1024),
                    StringDecoder(Charsets.UTF_8),
                    StringEncoder(Charsets.UTF_8),
                    EchoHandler("PONG2"),
                )
            }
            group2.register(adoptedClient).sync()

            adoptedServer.config().isAutoRead = true
            adoptedClient.config().isAutoRead = true

            handoff.sendAck()
            assertTrue("Server did not invoke onAck", ackLatch.await(5, TimeUnit.SECONDS))
            assertEquals(null, rollbackReason.get())

            writer.print("PING2\n")
            writer.flush()
            assertEquals("PONG2", reader.readLine())
        } finally {
            tcpClient?.close()
            adoptedClient?.close()?.awaitUninterruptibly(2, TimeUnit.SECONDS)
            adoptedServer?.close()?.awaitUninterruptibly(2, TimeUnit.SECONDS)
            received?.close?.invoke()
            handoffServer?.close()
            oldServerChannel?.close()?.awaitUninterruptibly(2, TimeUnit.SECONDS)
            group1.shutdownGracefully(0, 2, TimeUnit.SECONDS).syncUninterruptibly()
            group2.shutdownGracefully(0, 2, TimeUnit.SECONDS).syncUninterruptibly()
            Files.deleteIfExists(Path.of(socketPath))
            Files.deleteIfExists(Path.of("$socketPath.fd"))
        }
    }

    @Test
    fun `invokes onRollback and not onAck when client sends NACK`() {
        Assume.assumeTrue("Requires Linux Epoll", Epoll.isAvailable())

        val socketPath = tempSocketPath()
        val pipe = FileDescriptor.pipe()
        val acked = AtomicBoolean(false)
        val rollbackLatch = CountDownLatch(1)
        val rollbackReason = AtomicReference<String?>()

        var handoffServer: HandoffServer? = null
        var received: ReceivedHandoff? = null

        try {
            handoffServer = HandoffServer(
                socketPath = socketPath,
                onQuiesceAndSnapshot = {
                    val payload = HandoffPayload(
                        listenerTypes = listOf("TCP"),
                        sessions = emptyList(),
                        rooms = emptyList(),
                    )
                    payload to listOf(pipe[0].intValue())
                },
                onAck = { acked.set(true) },
                onRollback = { reason ->
                    rollbackReason.set(reason)
                    rollbackLatch.countDown()
                },
            ).start()

            val handoff = HandoffClient.receive(socketPath)
            received = handoff

            handoff.sendNack("boom")
            assertTrue("onRollback was not invoked", rollbackLatch.await(5, TimeUnit.SECONDS))
            assertEquals("boom", rollbackReason.get())
            assertFalse("onAck must not be invoked after NACK", acked.get())
        } finally {
            received?.listenerFds?.forEach { runCatching { it.close() } }
            received?.close?.invoke()
            handoffServer?.close()
            pipe.forEach { runCatching { it.close() } }
            Files.deleteIfExists(Path.of(socketPath))
            Files.deleteIfExists(Path.of("$socketPath.fd"))
        }
    }
}
