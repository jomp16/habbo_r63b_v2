/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.netty

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.game.user.HabboSessionManager
import tk.jomp16.habbo.kotlin.ip

@ChannelHandler.Sharable
class HabboNettyHandler : ChannelInboundHandlerAdapter() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        val ip = ctx.channel().ip()

        log.info("New connection of {}!", ip)

        if (!HabboServer.habboSessionManager.makeHabboSession(ctx.channel())) {
            log.error("Connection of {} unsuccessful!", ip)

            ctx.disconnect()
        }
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        val habboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()
        val username = try {
            habboSession.userInformation.username
        } catch (exception: UninitializedPropertyAccessException) {
            ctx.channel().ip()
        }

        log.info("Disconnecting user {}", username)

        if (!HabboServer.habboSessionManager.removeHabboSession(ctx.channel())) log.warn("Disconnection of {} unsuccessful!", username)
        else log.info("Disconnection of user {} successful!", username)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HabboRequest) {
            val habboSession: HabboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()

            HabboServer.habboHandler.handle(habboSession, msg)
        }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        val habboSession: HabboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()

        if (evt is IdleStateEvent) {
            if (evt.state() == IdleState.READER_IDLE) {
                log.info("User ${habboSession.userInformation.username} didn't reply ping! Disconnecting it.")

                ctx.close()
            } else if (evt.state() == IdleState.WRITER_IDLE) {
                log.error("Didn't send any message to user ${habboSession.userInformation.username}, pinging it.")

                habboSession.sendHabboResponse(Outgoing.PING)
            }
        }
    }

    @Suppress("OverridingDeprecatedMember")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        log.error("An error happened while handling packet!", cause)
    }
}