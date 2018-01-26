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

package ovh.rwx.habbo.database.group

import com.github.andrewoma.kwery.core.Row
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.group.GroupData
import ovh.rwx.habbo.game.group.GroupMember
import ovh.rwx.habbo.game.group.GroupMembershipState
import ovh.rwx.habbo.game.group.GroupRequest
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey
import ovh.rwx.habbo.kotlin.localDateTime

object GroupDao {
    fun getGroupsBadgesBases(): List<Triple<Int, String, String>> = HabboServer.database {
        select("SELECT * FROM `groups_badges_base`") {
            Triple(it.int("id"), it.string("value1"), it.string("value2"))
        }
    }

    fun getGroupsBadgesSymbols(): List<Triple<Int, String, String>> = HabboServer.database {
        select("SELECT * FROM `groups_badges_symbol`") {
            Triple(it.int("id"), it.string("value1"), it.string("value2"))
        }
    }

    fun getGroupsBadgesBaseColors(): List<Pair<Int, String>> = HabboServer.database {
        select("SELECT * FROM `groups_badges_base_color`") {
            it.int("id") to it.string("color")
        }
    }

    fun getGroupsBadgesSymbolColors(): List<Pair<Int, String>> = HabboServer.database {
        select("SELECT * FROM `groups_badges_symbol_color`") {
            it.int("id") to it.string("color")
        }
    }

    fun getGroupsBadgesBackgroundColors(): List<Pair<Int, String>> = HabboServer.database {
        select("SELECT * FROM `groups_badges_background_color`") {
            it.int("id") to it.string("color")
        }
    }

    fun getGroupsData(): List<GroupData> = HabboServer.database {
        select(javaClass.getResource("/sql/groups/select_groups.sql").readText()) {
            getGroupData(it)
        }
    }

    fun createGroup(name: String, description: String, badge: String, ownerId: Int, roomId: Int, groupMembershipState: GroupMembershipState, symbolColor: Int, backgroundColor: Int, onlyAdminCanDecorateRoom: Boolean): Int {
        return HabboServer.database {
            insertAndGetGeneratedKey(javaClass.getResource("/sql/groups/insert_group.sql").readText(),
                    mapOf(
                            "name" to name,
                            "description" to description,
                            "badge" to badge,
                            "owner_id" to ownerId,
                            "room_id" to roomId,
                            "state" to groupMembershipState.state.toString(),
                            "symbol_color" to symbolColor,
                            "background_color" to backgroundColor,
                            "only_admin_can_decorate" to onlyAdminCanDecorateRoom
                    )
            )
        }
    }

    fun addMember(groupId: Int, userId: Int, rank: Int): Int {
        return HabboServer.database {
            insertAndGetGeneratedKey(javaClass.getResource("/sql/groups/member/insert_group_member.sql").readText(),
                    mapOf(
                            "group_id" to groupId,
                            "user_id" to userId,
                            "rank" to rank.toString()
                    )
            )
        }
    }

    fun getGroupData(groupId: Int): GroupData {
        return HabboServer.database {
            select(javaClass.getResource("/sql/groups/select_group_from_id.sql").readText(),
                    mapOf(
                            "id" to groupId
                    )) {
                getGroupData(it)
            }.first()
        }
    }

    fun getGroupMembers(groupId: Int): List<GroupMember> {
        return HabboServer.database {
            select(javaClass.getResource("/sql/groups/member/select_group_members_from_group_id.sql").readText(),
                    mapOf(
                            "group_id" to groupId
                    )
            ) {
                getGroupMember(it)
            }
        }
    }

    fun getGroupRequests(groupId: Int): List<GroupRequest> {
        return HabboServer.database {
            select(javaClass.getResource("/sql/groups/request/select_group_requests_from_group_id.sql").readText(),
                    mapOf(
                            "group_id" to groupId
                    )
            ) {
                getGroupRequests(it)
            }
        }
    }

    fun addRequest(groupId: Int, userId: Int): Int {
        return HabboServer.database {
            insertAndGetGeneratedKey(javaClass.getResource("/sql/groups/request/insert_group_request.sql").readText(),
                    mapOf(
                            "group_id" to groupId,
                            "user_id" to userId
                    )
            )
        }
    }

    fun updateGroupData(groupData: GroupData) {
        HabboServer.database {
            update(javaClass.getResource("/sql/groups/update_group.sql").readText(),
                    mapOf(
                            "name" to groupData.name,
                            "description" to groupData.description,
                            "badge" to groupData.badge,
                            "state" to groupData.membershipState.state.toString(),
                            "symbol_color" to groupData.symbolColor,
                            "background_color" to groupData.backgroundColor,
                            "only_admin_can_decorate" to groupData.onlyAdminCanDecorateRoom,
                            "group_id" to groupData.id
                    )
            )
        }
    }

    private fun getGroupData(row: Row) = GroupData(
            row.int("id"),
            row.string("name"),
            row.string("description"),
            row.string("badge"),
            row.int("owner_id"),
            row.int("room_id"),
            GroupMembershipState.valueOf(row.int("state")),
            row.int("symbol_color"),
            row.int("background_color"),
            row.boolean("only_admin_can_decorate"),
            row.localDateTime("created_at")
    )

    private fun getGroupMember(row: Row) = GroupMember(
            row.int("id"),
            row.int("user_id"),
            row.int("rank"),
            row.localDateTime("created_at")
    )

    private fun getGroupRequests(row: Row) = GroupRequest(
            row.int("id"),
            row.int("user_id"),
            row.localDateTime("created_at")
    )
}