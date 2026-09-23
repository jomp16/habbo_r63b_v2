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

package ovh.rwx.habbo.game.item.wired.effect.effects

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.WiredUserSource
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomChatMessageBubbles
import ovh.rwx.habbo.game.room.RoomChatType
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_SHOW_MESSAGE)
class WiredEffectShowMessage(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var message: String = ""
    private var visibility: MessageVisibility = MessageVisibility.USER_ONLY
    private var style: Int = RoomChatMessageBubbles.WIRED.type // notification style

    override val requiresUsers = true
    override val allowedUserSources = listOf(
        WiredUserSource.TRIGGERING_USER,
        WiredUserSource.SELECTOR_USERS,
        WiredUserSource.SIGNAL_USERS,
        WiredUserSource.ALL_ROOM_USERS
    )
    override val defaultUserSource = WiredUserSource.TRIGGERING_USER

    init {
        setData()
    }

    override fun code() = WiredEffectType.SHOW_MESSAGE.code

    override fun setData() {
        roomItem.wiredData?.let {
            message = it.message
            if (it.options.isNotEmpty()) {
                visibility = MessageVisibility.fromCode(it.options.getOrElse(0) { 0 })
                style = it.options.getOrElse(1) { 0 }
            }
        }
    }

    override fun onEffect(wiredContext: WiredContext) {
        val formatted = wiredContext.formatPlaceholders(message)
        if (formatted.isNotBlank()) {
            val bubble = RoomChatMessageBubbles.fromType(style)
            val targetUsers = when (visibility) {
                MessageVisibility.USER_ONLY -> wiredContext.getEffectiveUsers(this).filterIsInstance<RoomUser>()
                MessageVisibility.ALL_USERS -> room.userManager.entities.values.filterIsInstance<RoomUser>().toList()
            }

            targetUsers.forEach { roomUser ->
                roomUser.chat(
                    roomUser.virtualID,
                    formatted,
                    bubble,
                    RoomChatType.WHISPER,
                    true
                )
            }
        }
    }

    enum class MessageVisibility(val code: Int) {
        USER_ONLY(0),
        ALL_USERS(1);

        companion object {
            fun fromCode(code: Int) = entries.find { it.code == code } ?: USER_ONLY
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(
                0,
                0,
                emptyList(),
                "",
                listOf(MessageVisibility.USER_ONLY.code, RoomChatMessageBubbles.WIRED.type),
                ""
            )
        }
    }
}