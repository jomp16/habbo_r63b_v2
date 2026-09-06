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

package ovh.rwx.habbo.communication.incoming.handshake

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.user.UserIPDao
import ovh.rwx.habbo.database.user.UserUniqueIdDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.ip
import ovh.rwx.habbo.util.IpInfo
import ovh.rwx.habbo.util.Utils
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.time.LocalDate

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeSSOTicketHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Handler(Incoming.SSO_TICKET, requiredAuth = false)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticate(habboRequest.readUTF())) {
            log.info("Unauthenticated user!")

            habboSession.channel.disconnect()

            return
        }

        log.info("{} logged in!", habboSession.userInformation.username)

        habboSession.sendHabboResponse(Outgoing.AUTHENTICATION_OK)
        habboSession.sendHabboResponse(Outgoing.AVATAR_EFFECTS)
        habboSession.sendHabboResponse(Outgoing.INVENTORY_UNSEEN_ITEMS, false, 0, listOf<Int>())
        habboSession.sendHabboResponse(
            Outgoing.HOME_ROOM,
            habboSession.userInformation.homeRoom,
            HabboServer.habboConfig.autoJoinRoom
        )
        if (!habboSession.isBot) habboSession.sendHabboResponse(
            Outgoing.USER_CLOTHINGS,
            habboSession.userInformation.clothings
        )
        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_FAVORITES, habboSession.favoritesRooms.map { it.second })
        habboSession.sendHabboResponse(Outgoing.USER_NOOBNESS_LEVEL, 0)
        habboSession.sendHabboResponse(
            Outgoing.USER_RIGHTS,
            if (habboSession.userInformation.vip || habboSession.habboSubscription.validUserSubscription) 2 else 0,
            habboSession.userInformation.rank,
            habboSession.userInformation.ambassador
        )
        habboSession.sendHabboResponse(Outgoing.AVAILABILITY_STATUS)
        habboSession.sendHabboResponse(Outgoing.ENABLE_TRADING, true)
        habboSession.sendHabboResponse(Outgoing.ACHIEVEMENT_SCORE, habboSession.userStats.achievementScore)
        habboSession.sendHabboResponse(
            Outgoing.AUTHENTICATION_FIRST_LOGIN_OF_DAY,
            habboSession.userStats.firstLoginOfDay
        )
        habboSession.sendHabboResponse(Outgoing.MYSTERY_BOX_CHALLENGE, "", "")
        if (!habboSession.isBot) habboSession.sendHabboResponse(
            Outgoing.BUILDERS_SUBSCRIPTION_STATUS,
            habboSession.habboSubscription
        )
        habboSession.sendHabboResponse(
            Outgoing.CAMPAIGN_CALENDAR,
            "easter21",
            "",
            LocalDate.now().dayOfMonth - 1,
            LocalDate.now().lengthOfMonth(),
            intArrayOf(),
            intArrayOf()
        )
        habboSession.sendHabboResponse(
            Outgoing.MODERATION_TOPICS_INIT,
            HabboServer.habboGame.moderationManager.moderationCategories,
            HabboServer.habboGame.moderationManager.moderationTopics.values
        )

        if (habboSession.hasPermission("acc_mod_tools")) habboSession.sendHabboResponse(Outgoing.MODERATION_INIT)

        commonStuff(habboSession)
    }

    @HandlerR63A(IncomingR63A.HANDSHAKE_SSO_TICKET, requiredAuth = false)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticate(habboRequest.readUTF())) {
            log.info("Unauthenticated user!")

            habboSession.channel.disconnect()

            return
        }

        log.info("{} logged in!", habboSession.userInformation.username)

        habboSession.sendHabboResponse(OutgoingR63A.HANDSHAKE_AUTHENTICATION_OK)
        habboSession.sendHabboResponse(
            OutgoingR63A.USER_RIGHTS,
            if (habboSession.userInformation.vip || habboSession.habboSubscription.validUserSubscription) 2 else 0,
            habboSession.userInformation.rank
        )
        habboSession.sendHabboResponse(OutgoingR63A.USER_AVATAR_EFFECTS)
        habboSession.sendHabboResponse(OutgoingR63A.NAVIGATOR_FAVORITES, habboSession.favoritesRooms.map { it.second })
        habboSession.sendHabboResponse(OutgoingR63A.AVAILABILITY_STATUS)
        habboSession.sendHabboResponse(OutgoingR63A.ENABLE_TRADING, true)
        habboSession.sendHabboResponse(
            OutgoingR63A.HOME_ROOM,
            habboSession.userInformation.homeRoom,
            HabboServer.habboConfig.autoJoinRoom
        )

        if (habboSession.hasPermission("acc_mod_tools")) habboSession.sendHabboResponse(OutgoingR63A.MODERATION_INIT)

        commonStuff(habboSession)
    }

    @Handler(Incoming.SSO_TICKET, requiredAuth = false)
    fun handleHabboAir(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticate(habboRequest.readUTF())) {
            log.info("Unauthenticated user!")

            habboSession.channel.disconnect()

            return
        }

        log.info("{} logged in!", habboSession.userInformation.username)

        habboSession.sendHabboResponse(Outgoing.AUTHENTICATION_OK, habboSession)
        habboSession.sendHabboResponse(Outgoing.AVATAR_EFFECTS)
        habboSession.sendHabboResponse(Outgoing.INVENTORY_UNSEEN_ITEMS, false, 0, listOf<Int>())
        habboSession.sendHabboResponse(
            Outgoing.HOME_ROOM,
            habboSession.userInformation.homeRoom,
            HabboServer.habboConfig.autoJoinRoom
        )
        if (!habboSession.isBot) habboSession.sendHabboResponse(
            Outgoing.USER_CLOTHINGS,
            habboSession.userInformation.clothings
        )
        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_FAVORITES, habboSession.favoritesRooms.map { it.second })
        habboSession.sendHabboResponse(Outgoing.USER_NOOBNESS_LEVEL, 0)
        habboSession.sendHabboResponse(
            Outgoing.USER_RIGHTS,
            if (habboSession.userInformation.vip || habboSession.habboSubscription.validUserSubscription) 2 else 0,
            habboSession.userInformation.rank,
            habboSession.userInformation.ambassador
        )
        habboSession.sendHabboResponse(Outgoing.AVAILABILITY_STATUS)
        habboSession.sendHabboResponse(Outgoing.ENABLE_TRADING, true)
        habboSession.sendHabboResponse(Outgoing.ACHIEVEMENT_SCORE, habboSession.userStats.achievementScore)
        habboSession.sendHabboResponse(
            Outgoing.AUTHENTICATION_FIRST_LOGIN_OF_DAY,
            habboSession.userStats.firstLoginOfDay
        )
        habboSession.sendHabboResponse(Outgoing.MYSTERY_BOX_CHALLENGE, "", "")
        if (!habboSession.isBot) habboSession.sendHabboResponse(
            Outgoing.BUILDERS_SUBSCRIPTION_STATUS,
            habboSession.habboSubscription
        )
        habboSession.sendHabboResponse(
            Outgoing.CAMPAIGN_CALENDAR,
            "easter21",
            "",
            LocalDate.now().dayOfMonth - 1,
            LocalDate.now().lengthOfMonth(),
            intArrayOf(),
            intArrayOf()
        )
        habboSession.sendHabboResponse(
            Outgoing.MODERATION_TOPICS_INIT,
            HabboServer.habboGame.moderationManager.moderationCategories,
            HabboServer.habboGame.moderationManager.moderationTopics.values
        )
        habboSession.sendHabboResponse(
            Outgoing.USER_HABBICONS,
            habboSession.habboHabbicon.getUserHabbiconList(),
            habboSession.habboHabbicon.recentHabbiconIds
        )
        habboSession.sendHabboResponse(
            Outgoing.HABBICON_SHOP_DATA,
            HabboServer.habboGame.habbiconManager.collections.values.toList(),
            habboSession.habboHabbicon.userHabbicons
        )

        if (habboSession.hasPermission("acc_mod_tools")) habboSession.sendHabboResponse(Outgoing.MODERATION_INIT)

        commonStuff(habboSession)
    }

    private fun commonStuff(habboSession: HabboSession) {
        habboSession.handshaking = false

        if (habboSession.isBot) return

        if (HabboServer.habboConfig.analyticsConfig.uniqueId && !UserUniqueIdDao.containsUniqueIdForUser(
                habboSession.userInformation.id,
                habboSession.uniqueID
            )
        ) {
            // save unique id to database
            UserUniqueIdDao.addUniqueIdForUser(
                habboSession.userInformation.id,
                habboSession.uniqueID,
                habboSession.osInformation
            )
        }

        if (HabboServer.habboConfig.analyticsConfig.ipConfig.enabled && !UserIPDao.containsIPForUser(
                habboSession.userInformation.id,
                habboSession.channel.ip()
            )
        ) {
            // save IP to database
            val isInternalIP =
                (habboSession.channel.remoteAddress() as InetSocketAddress).address.let { it.isLoopbackAddress || it.isSiteLocalAddress }
            UserIPDao.addIPForUser(
                habboSession.userInformation.id,
                isInternalIP,
                if (!isInternalIP) Utils.getIpInfo(habboSession.channel.ip()) else IpInfo(
                    ip = habboSession.channel.ip(),
                    type = when ((habboSession.channel.remoteAddress() as InetSocketAddress).address) {
                        is Inet4Address -> "ipv4"
                        is Inet6Address -> "ipv6"
                        else -> "unknown"
                    }
                )
            )
        }
    }
}