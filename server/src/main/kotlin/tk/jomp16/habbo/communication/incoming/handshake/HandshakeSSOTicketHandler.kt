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

package tk.jomp16.habbo.communication.incoming.handshake

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.BuildConfig
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.QueuedHabboResponse
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.misc.NotificationType
import tk.jomp16.habbo.game.user.HabboSession
import java.time.LocalDate

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

        val queuedHabboResponse = QueuedHabboResponse()

        queuedHabboResponse += Outgoing.AUTHENTICATION_OK to arrayOf()
        queuedHabboResponse += Outgoing.AVATAR_EFFECTS to arrayOf()
        queuedHabboResponse += Outgoing.INVENTORY_NEW_OBJECTS to arrayOf(false, 0, listOf<Int>())
        queuedHabboResponse += Outgoing.HOME_ROOM to arrayOf(
                habboSession.userInformation.homeRoom,
                HabboServer.habboConfig.autoJoinRoom
        )
        queuedHabboResponse += Outgoing.FIGURE_SETS to arrayOf()
        queuedHabboResponse += Outgoing.NAVIGATOR_FAVORITES to arrayOf()
        queuedHabboResponse += Outgoing.AUTHENTICATION_UNKNOWN_ID1 to arrayOf(0)
        queuedHabboResponse += Outgoing.USER_RIGHTS to arrayOf(
                if (habboSession.userInformation.vip || habboSession.habboSubscription.validUserSubscription) 2 else 0,
                habboSession.userInformation.rank,
                habboSession.userInformation.ambassador
        )
        queuedHabboResponse += Outgoing.AVAILABILITY_STATUS to arrayOf()
        queuedHabboResponse += Outgoing.ENABLE_TRADING to arrayOf(true)
        queuedHabboResponse += Outgoing.ACHIEVEMENT_SCORE to arrayOf(
                habboSession.userStats.achievementScore
        )
        queuedHabboResponse += Outgoing.AUTHENTICATION_UNKNOWN_ID2 to arrayOf(false)
        queuedHabboResponse += Outgoing.AUTHENTICATION_UNKNOWN_ID3 to arrayOf("", "")
        queuedHabboResponse += Outgoing.BUILDERS_CLUB_MEMBERSHIP to arrayOf()
        queuedHabboResponse += Outgoing.CAMPAIGN_CALENDAR to arrayOf("xmas16", "", LocalDate.now().dayOfMonth - 1, LocalDate.now().lengthOfMonth(), intArrayOf(), intArrayOf())
        queuedHabboResponse += Outgoing.MODERATION_TOPICS_INIT to arrayOf(HabboServer.habboGame.moderationManager.moderationCategories, HabboServer.habboGame.moderationManager.moderationTopics.values)

        if (habboSession.hasPermission("acc_mod_tools")) queuedHabboResponse += Outgoing.MODERATION_INIT to arrayOf()

        habboSession.sendQueuedHabboResponse(queuedHabboResponse)

        if (HabboServer.habboConfig.motdEnabled) {
            val motd = HabboServer.habboConfig.motdContents
                    .replace("\$server_name", BuildConfig.NAME)
                    .replace("\$server_version", BuildConfig.VERSION)
                    .replace("\$username", habboSession.userInformation.username)

            habboSession.sendNotification(NotificationType.MOTD_ALERT, motd)
        }
    }
}