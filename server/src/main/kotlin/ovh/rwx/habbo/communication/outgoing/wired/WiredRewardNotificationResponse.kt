/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.wired

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class WiredRewardNotificationResponse {
    @Response(Outgoing.WIRED_REWARD_NOTIFICATION)
    fun handle(habboResponse: HabboResponse, wiredRewardNotification: WiredRewardNotification) {
        habboResponse.apply {
            writeInt(wiredRewardNotification.code)
        }
    }

    enum class WiredRewardNotification(val code: Int) {
        // success
        ITEM_REWARDED(6),
        BADGE_REWARDED(7),

        // errors
        ERROR_NO_MORE_REWARDS(0),
        ERROR_ITEM_ALREADY_REWARDED_IN_ACCOUNT(1),
        ERROR_ITEM_ALREADY_REWARDED_TODAY(2),
        ERROR_ITEM_ALREADY_REWARDED_HOUR(3),
        ERROR_YOU_DIDN_T_WON(4),
        ERROR_YOU_WON_ALL_REWARDS(5),
        ERROR_ITEM_ALREADY_REWARDED_MINUTE(8)
    }
}