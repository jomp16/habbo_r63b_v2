/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.misc

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class MiscSuperNotificationResponse {
    @Response(Outgoing.MISC_SUPER_NOTIFICATION)
    fun response(habboResponse: HabboResponse, type: MiscSuperNotificationKeys, strings: Array<String>) {
        habboResponse.apply {
            writeUTF(type.key)
            writeInt(strings.size / 2)

            strings.forEach { writeUTF(it) }
        }
    }

    enum class MiscSuperNotificationKeys(val key: String) {
        ADMIN_PERSISTENT("admin.persistent"),
        ADMIN_TRANSIENT("admin.transient"),
        BUILDERS_CLUB_MEMBERSHIP_EXPIRED("builders_club.membership_expired"),
        BUILDERS_CLUB_MEMBERSHIP_EXPIRES("builders_club.membership_expires"),
        BUILDERS_CLUB_MEMBERSHIP_EXTENDED("builders_club.membership_extended"),
        BUILDERS_CLUB_MEMBERSHIP_MADE("builders_club.membership_made"),
        BUILDERS_CLUB_MEMBERSHIP_RENEWED("builders_club.membership_renewed"),
        BUILDERS_CLUB_ROOM_LOCKED("builders_club.room_locked"),
        BUILDERS_CLUB_ROOM_UNLOCKED("builders_club.room_unlocked"),
        BUILDERS_CLUB_VISIT_DENIED_OWNER("builders_club.visit_denied_for_owner"),
        BUILDERS_CLUB_VISIT_DENIED_GUEST("builders_club.visit_denied_for_visitor"),
        CASINO_TOO_MANY_DICE_PLACEMENT("casino.too_many_dice.placement"),
        CASINO_TOO_MANY_DICE("casino.too_many_dice"),
        FLOOR_PLAN_EDITOR_ERROR("floorplan_editor.error"),
        FORUMS_DELIVERED("forums.delivered"),
        FORUMS_FORUM_SETTINGS_UPDATED("forums.forum_settings_updated"),
        FORUMS_MESSAGE_HIDDEN("forums.message.hidden"),
        FORUMS_MESSAGE_RESTORED("forums.message.restored"),
        FORUMS_THREAD_HIDDEN("forums.thread.hidden"),
        FORUMS_THREAD_LOCKED("forums.thread.locked"),
        FORUMS_THREAD_PINNED("forums.thread.pinned"),
        FORUMS_THREAD_RESTORED("forums.thread.restored"),
        FORUMS_THREAD_UNLOCKED("forums.thread.unlocked"),
        FORUMS_THREAD_UNPINNED("forums.thread.unpinned"),
        FURNITURE_PLACEMENT_ERROR("furni_placement_error"),
        GIFTING_VALENTINE("gifting.valentine"),
        NUX_POPUP("nux.popup"),
        PURCHASING_ROOM("purchasing.room"),
        RECEIVED_GIFT("received.gift"),
        RECEIVED_BADGE("received.badge"),
        FIGURESET_REDEEMED("figureset.redeemed.success"),
        FIGURESET_OWNED_ALREADY("figureset.already.redeemed")
    }
}