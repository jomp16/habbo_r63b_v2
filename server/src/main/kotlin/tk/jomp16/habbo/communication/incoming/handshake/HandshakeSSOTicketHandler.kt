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
        queuedHabboResponse += Outgoing.AVATAR_EFFECTS to arrayOf() // AvatarEffectsComposer
        queuedHabboResponse += Outgoing.HOME_ROOM to arrayOf(
                habboSession.userInformation.homeRoom,
                HabboServer.habboConfig.autoJoinRoom
        ) // NavigatorSettingsComposer
        queuedHabboResponse += Outgoing.NAVIGATOR_FAVORITES to arrayOf() // FavouritesComposer
        queuedHabboResponse += Outgoing.FIGURE_SETS to arrayOf() // FigureSetIdsComposer
        queuedHabboResponse += Outgoing.USER_RIGHTS to arrayOf(
                if (habboSession.userInformation.vip || habboSession.habboSubscription.validUserSubscription) 2 else 0,
                habboSession.userInformation.rank,
                habboSession.userInformation.ambassador
        ) // UserRightsComposer
        queuedHabboResponse += Outgoing.AVAILABILITY_STATUS to arrayOf() // AvailabilityStatusComposer
        queuedHabboResponse += Outgoing.ACHIEVEMENT_SCORE to arrayOf(
                habboSession.userStats.achievementScore
        ) // AchievementScoreComposer
        queuedHabboResponse += Outgoing.BUILDERS_CLUB_MEMBERSHIP to arrayOf() // BuildersClubMembershipComposer

        habboSession.sendQueuedHabboResponse(queuedHabboResponse)

        // todo: add PlusEMU packets

        // CfhTopicsInitComposer
        // BadgeDefinitionsComposer

        if (HabboServer.habboConfig.motdEnabled) {
            val motd = HabboServer.habboConfig.motdContents
                    .replace("\$server_name", BuildConfig.NAME)
                    .replace("\$server_version", BuildConfig.VERSION)
                    .replace("\$username", habboSession.userInformation.username)

            habboSession.sendNotification(NotificationType.MOTD_ALERT, motd)
        } // MOTDNotificationComposer
    }
}