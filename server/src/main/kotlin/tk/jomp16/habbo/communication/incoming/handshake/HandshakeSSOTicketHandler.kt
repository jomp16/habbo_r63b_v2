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

package tk.jomp16.habbo.communication.incoming.handshake

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.BuildConfig
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.user.UserUniqueIdDao
import tk.jomp16.habbo.game.misc.NotificationType
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeSSOTicketHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Handler(Incoming.SSO_TICKET)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticate(habboRequest.readUTF())) {
            log.info("Unauthenticated user!")

            habboSession.channel.disconnect()

            return
        }

        log.info("{} logged in!", habboSession.userInformation.username)

        habboSession.sendHabboResponse(Outgoing.AUTHENTICATION_OK)
        habboSession.sendHabboResponse(Outgoing.AVATAR_EFFECTS)
        habboSession.sendHabboResponse(Outgoing.INVENTORY_NEW_OBJECTS, false, 0, listOf<Int>())
        habboSession.sendHabboResponse(Outgoing.HOME_ROOM,
                habboSession.userInformation.homeRoom,
                HabboServer.habboConfig.autoJoinRoom)
        habboSession.sendHabboResponse(Outgoing.USER_CLOTHINGS, habboSession.userInformation.clothings)
        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_FAVORITES, habboSession.favoritesRooms.map { it.second })
        habboSession.sendHabboResponse(Outgoing.AUTHENTICATION_UNKNOWN_ID1, 0)
        habboSession.sendHabboResponse(Outgoing.USER_RIGHTS,
                if (habboSession.userInformation.vip || habboSession.habboSubscription.validUserSubscription) 2 else 0,
                habboSession.userInformation.rank,
                habboSession.userInformation.ambassador)
        habboSession.sendHabboResponse(Outgoing.AVAILABILITY_STATUS)
        habboSession.sendHabboResponse(Outgoing.ENABLE_TRADING, true)
        habboSession.sendHabboResponse(Outgoing.ACHIEVEMENT_SCORE, habboSession.userStats.achievementScore)
        habboSession.sendHabboResponse(Outgoing.AUTHENTICATION_UNKNOWN_ID2, true)
        habboSession.sendHabboResponse(Outgoing.AUTHENTICATION_UNKNOWN_ID3, "", "")
        habboSession.sendHabboResponse(Outgoing.BUILDERS_CLUB_MEMBERSHIP)
        //        habboSession.sendHabboResponse(Outgoing.CAMPAIGN_CALENDAR, "xmas16", "", LocalDate.now(Clock.systemUTC()).dayOfMonth - 1, LocalDate.now(Clock.systemUTC()).lengthOfMonth(), intArrayOf(), intArrayOf())
        habboSession.sendHabboResponse(Outgoing.MODERATION_TOPICS_INIT,
                HabboServer.habboGame.moderationManager.moderationCategories,
                HabboServer.habboGame.moderationManager.moderationTopics.values)

        if (habboSession.hasPermission("acc_mod_tools")) habboSession.sendHabboResponse(Outgoing.MODERATION_INIT)

        if (HabboServer.habboConfig.motdEnabled) {
            val motd = HabboServer.habboConfig.motdContents
                    .replace("\$server_name", BuildConfig.NAME)
                    .replace("\$server_version", BuildConfig.VERSION)
                    .replace("\$username", habboSession.userInformation.username)

            habboSession.sendNotification(NotificationType.MOTD_ALERT, motd)
        }

        habboSession.handshaking = false

        if (!UserUniqueIdDao.containsUniqueIdForUser(habboSession.userInformation.id, habboSession.uniqueID)) {
            // save unique id to database
            UserUniqueIdDao.addUniqueIdForUser(habboSession.userInformation.id, habboSession.uniqueID)
        }
    }
}