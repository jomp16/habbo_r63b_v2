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

package ovh.rwx.habbo.game.item.wired.addon.addons

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.addon.WiredAddon
import ovh.rwx.habbo.game.item.wired.addon.WiredAddonType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_CUSTOM_CONTRACT)
class WiredAddonCustomContract(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.CUSTOM_CONTRACT

    override fun onAddon(wiredContext: WiredContext) {
        val options = roomItem.wiredData?.options ?: emptyList()
        val varIds = roomItem.wiredData?.variableIds ?: emptyList()

        val hasPayment = options.getOrElse(0) { 0 } == 1
        val paymentCost = options.getOrElse(3) { 0 }
        val paymentVarId = varIds.getOrNull(0)

        val user = (wiredContext.triggererUser as? RoomUser)

        if (hasPayment && paymentCost > 0) {
            if (!paymentVarId.isNullOrBlank()) {
                val currentVal = (wiredContext.getVariable(paymentVarId) as? Number)?.toLong() ?: 0L
                if (currentVal < paymentCost) {
                    wiredContext.cancelled = true
                    return
                }
                wiredContext.setVariable(paymentVarId, currentVal - paymentCost)
            } else if (user != null) {
                val userCredits = user.habboSession.userInformation.credits
                if (userCredits < paymentCost) {
                    wiredContext.cancelled = true
                    return
                }
                user.habboSession.userInformation.credits -= paymentCost
                user.habboSession.updateAllCurrencies()
            } else {
                wiredContext.cancelled = true
                return
            }
        }

        val hasReward = options.getOrElse(5) { 0 } == 1
        val rewardAmount = options.getOrElse(8) { 0 }
        val rewardVarId = varIds.getOrNull(1)

        if (hasReward && rewardAmount > 0) {
            if (!rewardVarId.isNullOrBlank()) {
                val currentVal = (wiredContext.getVariable(rewardVarId) as? Number)?.toLong() ?: 0L
                wiredContext.setVariable(rewardVarId, currentVal + rewardAmount)
            } else if (user != null) {
                user.habboSession.userInformation.credits += rewardAmount
                user.habboSession.updateAllCurrencies()
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
