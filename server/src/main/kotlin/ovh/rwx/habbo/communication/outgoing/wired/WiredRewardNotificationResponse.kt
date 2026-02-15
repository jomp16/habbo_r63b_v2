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

package ovh.rwx.habbo.communication.outgoing.wired

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class WiredRewardNotificationResponse {
    @Response(Outgoing.WIRED_REWARD_NOTIFICATION)
    fun response(habboResponse: HabboResponse, wiredRewardNotification: WiredRewardNotification) {
        habboResponse.writeInt(wiredRewardNotification.code)
    }

    enum class WiredRewardNotification(val code: Int) {
        // wiredfurni.rewardfailed.reason.0=Desculpe! Existe um limite de prêmios. Todos os prêmios já foram entregues.
        ERROR_NO_MORE_REWARDS(0),

        // wiredfurni.rewardfailed.reason.1=Você já ganhou esse prêmio. Cada Habbo só ganha uma vez.
        ERROR_ITEM_ALREADY_REWARDED_IN_ACCOUNT(1),

        // wiredfurni.rewardfailed.reason.2=Você já ganhou prêmio hoje. Tente de novo amanhã!
        ERROR_ITEM_ALREADY_REWARDED_TODAY(2),

        // wiredfurni.rewardfailed.reason.3=Você já ganhou prêmio essa hora. Tente de novo daqui uma hora!
        ERROR_ITEM_ALREADY_REWARDED_HOUR(3),

        // wiredfurni.rewardfailed.reason.4=Que pena! Você não teve sorte dessa vez. Tente novamente!
        ERROR_YOU_DIDN_T_WON(4),

        // wiredfurni.rewardfailed.reason.5=Você já ganhou os prêmios possíveis de hoje.
        ERROR_YOU_WON_ALL_REWARDS(5),

        // wiredfurni.rewardsuccess.title / wiredfurni.rewardsuccess.body
        ITEM_REWARDED(6),

        // wiredfurni.badgereceived.title / wiredfurni.badgereceived.body
        BADGE_REWARDED(7),

        // wiredfurni.rewardfailed.reason.8=Você já recebeu um prêmio neste minuto. Tente de novo um pouquinho mais tarde.
        ERROR_ITEM_ALREADY_REWARDED_MINUTE(8)
    }
}