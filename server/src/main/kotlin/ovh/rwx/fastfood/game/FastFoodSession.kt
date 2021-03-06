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

package ovh.rwx.fastfood.game

import io.netty.channel.Channel
import ovh.rwx.fastfood.communication.outgoing.FFOutgoing
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class FastFoodSession(val channel: Channel) : AutoCloseable {
    var habboSession: HabboSession? = null
    val authenticated: Boolean
        get() = habboSession != null
    val incomingExecutor: Executor = Executors.newSingleThreadExecutor()
    private val outgoingExecutor: Executor = Executors.newSingleThreadExecutor()

    fun sendHabboResponse(ffOutgoing: FFOutgoing, vararg args: Any?) = outgoingExecutor.execute {
        HabboServer.fastFoodHandler.invokeResponse(ffOutgoing, *args)?.let {
            sendHabboResponse(it)
        }
    }

    fun sendHabboResponse(habboResponse: HabboResponse?) {
        habboResponse?.let { channel.writeAndFlush(it) }
    }

    override fun close() {
    }
}