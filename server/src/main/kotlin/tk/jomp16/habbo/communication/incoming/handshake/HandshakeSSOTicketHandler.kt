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
        queuedHabboResponse += Outgoing.ACTIVITY_POINTS_BALANCE to arrayOf(habboSession.userInformation.pixels, habboSession.userInformation.vipPoints)
        queuedHabboResponse += Outgoing.ACHIEVEMENT_SCORE to arrayOf(
                habboSession.userStats.achievementScore
        )
        queuedHabboResponse += Outgoing.AUTHENTICATION_UNKNOWN_ID2 to arrayOf(false)
        queuedHabboResponse += Outgoing.AUTHENTICATION_UNKNOWN_ID3 to arrayOf("", "")
        queuedHabboResponse += Outgoing.BUILDERS_CLUB_MEMBERSHIP to arrayOf()
        queuedHabboResponse += Outgoing.CAMPAIGN_CALENDAR to arrayOf("xmas16", "", LocalDate.now().dayOfMonth - 1, LocalDate.now().lengthOfMonth(), arrayOf<Int>(), arrayOf<Int>())

        /*
         * todo: CfhTopicsInitMessageParser
         *
         * Incoming(325, 885, _-0HH, CfhTopicsInitMessageParser) <- [0][0][3]u[1]E[0][0][0][6][0]sexual_content[0][0][0][4][0]explicit_sexual_talk[0][0][0][1][0][4]mods[0][8]cybersex[0][0][0][2][0][4]mods[0]sexual_webcam_images[0][0][0][3][0][4]mods[0][9]sex_links[0][0][0]$[0][4]mods[0]pii_meeting_irl[0][0][0][2][0][8]meet_irl[0][0][0][6][0][4]mods[0][10]asking_pii[0][0][0][8][0][4]mods[0][8]scamming[0][0][0][5][0]scamsites_promoting[0][0][0][9][0][4]mods[0] selling_buying_accounts_or_furni[0][0][0][10][0][4]mods[0]stealing_accounts_or_furni[0][0][0][11][0][4]mods[0]hacking_scamming_tricks[0][0][0] [0][4]mods[0][5]fraud[0][0][0]![0][4]mods[0]trolling_bad_behavior[0][0][0][8][0][8]bullying[0][0][0][12][0]mods_till_logout[0][10]habbo_name[0][0][0][13][0][4]mods[0]inappropiate_room_group_event[0][0][0]"[0][4]mods[0][8]swearing[0][0][0][0][10]auto_reply[0]drugs_promotion[0][0][0][0]mods_till_logout[0][8]gambling[0][0][0][0][4]mods[0]staff_impersonation[0][0][0][0][4]mods[0][13]minors_access[0][0][0][0][4]mods[0]violent_behavior[0][0][0][3][0][11]hate_speech[0][0][0][0]mods_till_logout[0]violent_roleplay[0][0][0][0]mods_till_logout[0]self_threatening[0][0][0][0][4]mods[0]game_interruption[0][0][0][4][0][8]flooding[0][0][0][0]mods_till_logout[0][13]door_blocking[0][0][0][0][10]auto_reply[0][5]raids[0][0][0][0][4]mods[0][9]scripting[0][0][0]#[0][4]mods
         */

        habboSession.sendQueuedHabboResponse(queuedHabboResponse)

        if (HabboServer.habboConfig.motdEnabled) {
            val motd = HabboServer.habboConfig.motdContents
                    .replace("\$server_name", BuildConfig.NAME)
                    .replace("\$server_version", BuildConfig.VERSION)
                    .replace("\$username", habboSession.userInformation.username)

            habboSession.sendNotification(NotificationType.MOTD_ALERT, motd)
        } // MOTDNotificationComposer
    }
}