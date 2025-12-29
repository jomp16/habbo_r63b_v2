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

package ovh.rwx.habbo.game.item.wired.trigger.triggers

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import java.util.Locale.getDefault

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_SAYS_SOMETHING)
class WiredTriggerSaysSomething(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    var message = ""
    private var onlyOwner = false
    private var triggerType = ChatTriggerType.CONTAINS
    private var hideMessage = false

    init {
        setData()
    }

    override fun code() = WiredTriggerType.AVATAR_SAYS_SOMETHING.code

    override fun setData() {
        roomItem.wiredData?.let {
            message = it.message
            onlyOwner = it.options.getOrElse(0) { 0 } == 1
            triggerType = ChatTriggerType.fromValue(it.options.getOrElse(1) { 0 })
            hideMessage = it.options.getOrElse(2) { 0 } == 1
        }
    }

    override fun onTrigger(roomUser: RoomUser?, data: Any?): Boolean {
        if (data == null || data !is String || roomUser == null) return false
        if (onlyOwner && !room.hasRights(roomUser.habboSession, true)) return false

        return when (triggerType) {
            ChatTriggerType.CONTAINS -> message.isNotEmpty() && data.lowercase(getDefault())
                .contains(message.lowercase(getDefault()))

            ChatTriggerType.EXACT_MATCH -> data.equals(message, ignoreCase = true)
            ChatTriggerType.ALL_MESSAGES -> true
        }
    }

    fun shouldHideMessage(): Boolean = hideMessage

    enum class ChatTriggerType(val value: Int) {
        CONTAINS(0),
        EXACT_MATCH(1),
        ALL_MESSAGES(2);

        companion object {
            fun fromValue(value: Int) = values().find { it.value == value } ?: CONTAINS
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, ChatTriggerType.CONTAINS.value, 0), "")
        }
    }
}