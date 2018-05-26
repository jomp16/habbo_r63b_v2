/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.netty

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import kotlinx.coroutines.experimental.async
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.HabboSessionManager
import ovh.rwx.habbo.kotlin.ip

@ChannelHandler.Sharable
class HabboNettyHandler : ChannelInboundHandlerAdapter() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        async {
            val ip = ctx.channel().ip()

            log.info("New connection of {}!", ip)

            if (!HabboServer.habboSessionManager.makeHabboSession(ctx.channel())) {
                log.error("Connection of {} unsuccessful!", ip)

                ctx.disconnect()
            }
        }
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        async {
            val habboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()
            val username = if (habboSession.authenticated) habboSession.userInformation.username else habboSession.channel.ip()

            log.info("Disconnecting user {}", username)

            if (HabboServer.habboSessionManager.removeHabboSession(ctx.channel())) log.info("User {} disconnected with success!", username)
            else log.warn("Disconnection of {} unsuccessful!", username)
        }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        async {
            if (msg is HabboRequest) {
                val habboSession: HabboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()

                HabboServer.habboHandler.handle(habboSession, msg)
            }
        }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        async {
            val habboSession: HabboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()
                    ?: return@async
            val username = if (habboSession.authenticated) habboSession.userInformation.username else habboSession.channel.ip()

            if (evt is IdleStateEvent) {
                if (!habboSession.authenticated && !habboSession.handshaking) {
                    log.error("Found an connected user $username without doing the handshake! Disconnecting it!")

                    habboSession.channel.disconnect()

                    return@async
                }

                if (evt.state() == IdleState.READER_IDLE && !habboSession.handshaking) {
                    log.error("User $username didn't reply ping! Disconnecting it.")

                    ctx.close()
                } else if (evt.state() == IdleState.WRITER_IDLE && (habboSession.authenticated || !habboSession.handshaking)) {
                    log.info("Didn't send any message to user $username, pinging it.")

                    habboSession.ping = System.nanoTime()
                    habboSession.sendHabboResponse(Outgoing.MISC_PING)
                }
            }
        }
    }

    @Suppress("OverridingDeprecatedMember")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        async {
            val habboSession: HabboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()
                    ?: return@async
            val username = if (habboSession.authenticated) habboSession.userInformation.username else habboSession.channel.ip()

            log.error("An error happened while handling packet for user $username!", cause)
        }
    }
}