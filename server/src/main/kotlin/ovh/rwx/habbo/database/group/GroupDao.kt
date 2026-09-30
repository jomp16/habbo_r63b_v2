/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.game.group.GroupData
import ovh.rwx.habbo.game.group.GroupMember
import ovh.rwx.habbo.game.group.GroupMembershipState
import ovh.rwx.habbo.game.group.GroupRequest
import ovh.rwx.habbo.database.sequence.HiLoSequence
import ovh.rwx.habbo.database.*
import java.time.LocalDateTime

object GroupDao {
    val groupSequence = HiLoSequence("groups", blockSize = 100)

    fun getGroupsBadgesBases(): List<Triple<Int, String, String>> = db {
        query<GroupBadgePartDto>("SELECT * FROM `groups_badges_base`")
            .map { Triple(it.id, it.value1, it.value2) }
    }

    fun getGroupsBadgesSymbols(): List<Triple<Int, String, String>> = db {
        query<GroupBadgePartDto>("SELECT * FROM `groups_badges_symbol`")
            .map { Triple(it.id, it.value1, it.value2) }
    }

    fun getGroupsBadgesBaseColors(): List<Pair<Int, String>> = db {
        query<GroupBadgeColorDto>("SELECT * FROM `groups_badges_base_color`")
            .map { it.id to it.color }
    }

    fun getGroupsBadgesSymbolColors(): List<Pair<Int, String>> = db {
        query<GroupBadgeColorDto>("SELECT * FROM `groups_badges_symbol_color`")
            .map { it.id to it.color }
    }

    fun getGroupsBadgesBackgroundColors(): List<Pair<Int, String>> = db {
        query<GroupBadgeColorDto>("SELECT * FROM `groups_badges_background_color`")
            .map { it.id to it.color }
    }

    fun getGroupsData(): List<GroupData> = db {
        query<GroupDataDto>("sql/groups/select_groups.sql").map { it.toDomain() }
    }

    fun createGroup(name: String, description: String, badge: String, ownerId: Int, roomId: Int, groupMembershipState: GroupMembershipState, symbolColor: Int, backgroundColor: Int, onlyAdminCanDecorateRoom: Boolean): Int {
        val groupId = groupSequence.nextId()
        db {
            update(
                """INSERT INTO `groups` (`id`, `name`, `description`, `badge`, `owner_id`, `room_id`, `state`, `symbol_color`, `background_color`, `only_admin_can_decorate`)
                   VALUES (:id, :name, :description, :badge, :owner_id, :room_id, :state, :symbol_color, :background_color, :only_admin_can_decorate)""",
                mapOf(
                    "id" to groupId,
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
        return groupId
    }

    fun addMember(groupId: Int, userId: Int, rank: Int): Int {
        return db {
            insertAndGetGeneratedKey(javaClass.getResource("/sql/groups/member/insert_group_member.sql").readText(),
                    mapOf(
                            "group_id" to groupId,
                            "user_id" to userId,
                            "rank" to rank.toString()
                    )
            )
        }
    }

    fun getGroupData(groupId: Int): GroupData = db {
        queryOne<GroupDataDto>(
            "sql/groups/select_group_from_id.sql",
            mapOf("id" to groupId)
        )!!.toDomain()
    }

    fun getGroupMembers(groupId: Int): List<GroupMember> = db {
        query<GroupMember>(
            "sql/groups/member/select_group_members_from_group_id.sql",
            mapOf("group_id" to groupId)
        )
    }

    fun getGroupRequests(groupId: Int): List<GroupRequest> = db {
        query<GroupRequest>(
            "sql/groups/request/select_group_requests_from_group_id.sql",
            mapOf("group_id" to groupId)
        )
    }

    @Suppress("unused")
    fun addRequest(groupId: Int, userId: Int): Int {
        return db {
            insertAndGetGeneratedKey(javaClass.getResource("/sql/groups/request/insert_group_request.sql").readText(),
                    mapOf(
                            "group_id" to groupId,
                            "user_id" to userId
                    )
            )
        }
    }

    fun updateGroupData(groupData: GroupData) {
        db {
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
}

data class GroupDataDto(
    val id: Int,
    val name: String,
    val description: String,
    val badge: String,
    val ownerId: Int,
    val roomId: Int,
    val state: Int,
    val symbolColor: Int,
    val backgroundColor: Int,
    val onlyAdminCanDecorate: Boolean,
    val createdAt: LocalDateTime
) {
    fun toDomain() = GroupData(
        id, name, description, badge, ownerId, roomId,
        GroupMembershipState.valueOf(state), symbolColor, backgroundColor,
        onlyAdminCanDecorate, createdAt
    )
}

data class GroupBadgePartDto(val id: Int, val value1: String, val value2: String)
data class GroupBadgeColorDto(val id: Int, val color: String)

