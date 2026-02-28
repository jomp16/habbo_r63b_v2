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

package ovh.rwx.habbo.game.item.wired.trigger.triggers

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.UserAction

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_USER_PERFORMS_ACTION)
class WiredTriggerUserPerformsAction(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    private var userAction: WiredUserAction = WiredUserAction.WAVE
    private var filter: String = ""

    override fun code() = WiredTriggerType.USER_PERFORMS_ACTION.code

    override fun setData() {
        roomItem.wiredData?.let {
            userAction = WiredUserAction.fromValue(it.options.getOrElse(0) { 1 })
            filter = it.message.removePrefix("dance").trim()
        }
    }

    override fun onTrigger(wiredContext: WiredContext, data: Any?): Boolean {
        if (data is WiredUserAction) {
            return data == userAction
        } else if (data is List<*>) {
            val action = data.first() as WiredUserAction

            if (action != userAction) {
                return false
            }

            return when (action) {
                WiredUserAction.SIGN -> {
                    val sign = data[1] as Int

                    if (filter.isEmpty()) {
                        true
                    } else {
                        sign == (filter.toIntOrNull() ?: 0)
                    }
                }

                WiredUserAction.DANCE -> {
                    val dance = data[1] as Int

                    if (filter.isEmpty()) {
                        true
                    } else {
                        dance == (filter.toIntOrNull() ?: 0)
                    }
                }

                else -> false
            }
        }

        return false
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0), "")
        }
    }

    enum class WiredUserAction(val wiredIndex: Int, val userAction: UserAction? = null) {
        WAVE(0, UserAction.WAVE),
        BLOW_KISS(1, UserAction.BLOW_KISS),
        LAUGH(2, UserAction.LAUGH),
        THUMB_UP(3, UserAction.THUMB_UP),
        AWAKE(4),
        IDLE(5, UserAction.IDLE),
        SIT(6),
        STAND(7),
        LAY(8),
        SWIM(9),
        SIGN(10),
        DANCE(11);

        companion object {
            fun fromValue(index: Int) = entries.find { it.wiredIndex == index } ?: WAVE

            fun fromUserAction(userAction: UserAction) = entries.find { it.userAction == userAction } ?: WAVE
        }
    }
}
