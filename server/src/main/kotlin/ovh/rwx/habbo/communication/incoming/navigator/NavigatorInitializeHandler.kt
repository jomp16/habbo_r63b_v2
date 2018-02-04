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

package ovh.rwx.habbo.communication.incoming.navigator

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorInitializeHandler {
    @Handler(Incoming.NAVIGATOR_INITIALIZE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_METADATA, HabboServer.habboGame.navigatorManager.navTabs) // NavigatorMetaDataParserComposer
        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_LIFTED_ROOMS, arrayOf<Room>()) // NavigatorLiftedRoomsComposer
        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_COLLAPSED_CATEGORIES,
                HabboServer.habboGame.navigatorManager.navigatorRoomCategories.values.map { "category__" + it.caption }.plus(
                        listOf(
                                "new_ads",
                                "friend_finding",
                                "staffpicks",
                                "with_friends",
                                "with_rights",
                                "recommended",
                                "my_groups",
                                "favorites",
                                "history",
                                "top_promotions",
                                "friends_rooms",
                                "groups",
                                "metadata",
                                "history_freq",
                                "highest_score",
                                "competition"
                        )
                )
        )

        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_PREFERENCES,
                habboSession.userPreferences.navigatorX,
                habboSession.userPreferences.navigatorY,
                habboSession.userPreferences.navigatorWidth,
                habboSession.userPreferences.navigatorHeight,
                false // open search bar
        ) // NavigatorPreferencesComposer
    }
}