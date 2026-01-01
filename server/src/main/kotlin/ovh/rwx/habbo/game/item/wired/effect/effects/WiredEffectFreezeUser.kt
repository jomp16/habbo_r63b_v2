/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.item.wired.effect.effects

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUserEffect

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_FREEZE, InteractionType.WIRED_EFFECT_UNFREEZE)
class WiredEffectFreezeUser(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var freezeEffect = FreezeEffectType.WIRED
    private var cancelOnTeleport = false

    enum class FreezeEffectType(val id: Int, val effectId: Int) {
        WIRED(0, 218),
        FROZEN(1, 12),
        XRAY(2, 11),
        BIRDS(3, 53),
        TRAP(4, 163);

        companion object {
            fun fromId(id: Int) = values().find { it.id == id } ?: WIRED
        }
    }

    init {
        setData()
    }

    override fun code() =
        if (roomItem.furnishing.interactionType == InteractionType.WIRED_EFFECT_FREEZE) WiredEffectType.FREEZE_USER.code else WiredEffectType.UNFREEZE_USER.code

    override fun setData() {
        if (roomItem.furnishing.interactionType == InteractionType.WIRED_EFFECT_FREEZE) {
            roomItem.wiredData?.let {
                freezeEffect = FreezeEffectType.fromId(it.options.getOrElse(0) { 0 })
                cancelOnTeleport = it.options.getOrElse(1) { 0 } == 1
            }
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        if (wiredContext.triggererUser == null) return

        val freeze = roomItem.furnishing.interactionType == InteractionType.WIRED_EFFECT_FREEZE

        // Apply freeze effect
        wiredContext.triggererUser.frozen = freeze

        if (freeze) {
            wiredContext.triggererUser.effect = RoomUserEffect(freezeEffect.effectId, 86400)
        } else {
            wiredContext.triggererUser.effect = null
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, 0), "")
        }
    }
}