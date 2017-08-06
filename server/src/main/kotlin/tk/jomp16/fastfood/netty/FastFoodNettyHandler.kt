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

package tk.jomp16.fastfood.netty

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.fastfood.game.FastFoodSession
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.game.user.HabboSessionManager
import tk.jomp16.habbo.kotlin.ip

@ChannelHandler.Sharable
class FastFoodNettyHandler : ChannelInboundHandlerAdapter() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        val ip = ctx.channel().ip()

        log.info("New connection of {}!", ip)

        if (!HabboServer.habboSessionManager.makeFastFoodSession(ctx.channel())) {
            log.error("Connection of {} unsuccessful!", ip)

            ctx.disconnect()
        }
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        val fastFoodSession: FastFoodSession = ctx.channel().attr(HabboSessionManager.fastFoodAttributeKey).get()
        val username = if (fastFoodSession.authenticated) fastFoodSession.habboSession!!.userInformation.username else fastFoodSession.channel.ip()

        log.info("Disconnecting user {}", username)

        if (HabboServer.habboSessionManager.removeFastFoodSession(ctx.channel())) log.info("User {} disconnected with success!", username)
        else log.warn("Disconnection of {} unsuccessful!", username)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HabboRequest) {
            val fastFoodSession: FastFoodSession = ctx.channel().attr(HabboSessionManager.fastFoodAttributeKey).get()

            HabboServer.fastFoodHandler.handle(fastFoodSession, msg)
        }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext?, evt: Any?) {
        super.userEventTriggered(ctx, evt)

        // todo: ping?
    }
}