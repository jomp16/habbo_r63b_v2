/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.communication.incoming.navigator

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.QueuedHabboResponse
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorInitializeHandler {
    @Handler(Incoming.NAVIGATOR_INITIALIZE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        val queuedHabboResponse = QueuedHabboResponse()

        queuedHabboResponse += Outgoing.NAVIGATOR_METADATA to arrayOf(
                HabboServer.habboGame.navigatorManager.navTabs
        ) // NavigatorMetaDataParserComposer
        queuedHabboResponse += Outgoing.NAVIGATOR_LIFTED_ROOMS to arrayOf(
                arrayOf<Room>()
        ) // NavigatorLiftedRoomsComposer
        queuedHabboResponse += Outgoing.NAVIGATOR_COLLAPSED_CATEGORIES to arrayOf()

        queuedHabboResponse += Outgoing.NAVIGATOR_PREFERENCES to arrayOf(
                habboSession.userPreferences.navigatorX,
                habboSession.userPreferences.navigatorY,
                habboSession.userPreferences.navigatorWidth,
                habboSession.userPreferences.navigatorHeight,
                false // open search bar
        ) // NavigatorPreferencesComposer

        habboSession.sendQueuedHabboResponse(queuedHabboResponse)
    }
}