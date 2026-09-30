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

package ovh.rwx.habbo.game.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.habbicon.HabboHabbicon
import ovh.rwx.habbo.game.user.badge.HabboBadge
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.information.UserPreferences
import ovh.rwx.habbo.game.user.information.UserStats
import ovh.rwx.habbo.game.user.inventory.HabboInventory
import ovh.rwx.habbo.game.user.messenger.HabboMessenger
import ovh.rwx.habbo.game.user.subscription.HabboSubscription
import java.time.LocalDateTime

fun HabboSession.authenticateBot(ssoTicket: String): Boolean {
    val botPrefix = HabboServer.habboConfig.botTicketPrefix
    val suffix = ssoTicket.removePrefix(botPrefix).toIntOrNull() ?: return false
    val botId = 2_000_000_000 + suffix
    val botUsername = "Bot_$suffix"
    val now = LocalDateTime.now()

    if (HabboServer.habboSessionManager.containsHabboSessionById(botId)) {
        return false
    }

    userInformation = UserInformation(
        id = botId,
        username = botUsername,
        email = "",
        accountCreated = now,
        realname = "",
        rank = 1,
        credits = 0,
        figure = HabboServer.habboConfig.serverConsoleFigure,
        gender = "M",
        motto = "",
        homeRoom = 0,
        vip = false,
        password = "",
        activityPointsCurrencies = mutableMapOf()
    )

    userStats = UserStats(
        id = botId,
        lastOnline = now,
        lastOnlineDatabase = now,
        onlineSeconds = 0L,
        roomVisits = 0,
        respect = 0,
        giftsGiven = 0,
        giftsReceived = 0,
        dailyRespectPoints = 0,
        dailyPetRespectPoints = 0,
        dailyCompetitionVotes = 0,
        achievementScore = 0,
        questId = 0,
        questProgress = 0,
        favoriteGroupId = 0,
        ticketsAnswered = 0,
        marketplaceTickets = 0,
        creditsLastUpdate = now,
        respectLastUpdate = now
    )

    userPreferences = UserPreferences(
        id = botId,
        volume = "100;100;100",
        preferOldChat = false,
        ignoreRoomInvite = false,
        disableCameraFollow = false,
        navigatorX = 0,
        navigatorY = 0,
        navigatorWidth = 0,
        navigatorHeight = 0,
        hideInRoom = false,
        blockNewFriends = false,
        chatColor = 0,
        friendBarOpen = false,
        friendStreamEnabled = false
    )

    favoritesRooms = mutableListOf()

    habboMessenger = HabboMessenger(this)
    habboSubscription = HabboSubscription(this)
    habboBadge = HabboBadge(this)
    habboInventory = HabboInventory(this)
    habboHabbicon = HabboHabbicon(this)

    isBot = true
    handshaking = false

    return true
}
