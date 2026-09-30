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

package ovh.rwx.habbo.database.messenger

import ovh.rwx.habbo.game.user.messenger.MessengerFriend
import ovh.rwx.habbo.game.user.messenger.MessengerRelationship
import ovh.rwx.habbo.game.user.messenger.MessengerRequest
import ovh.rwx.habbo.database.sequence.HiLoSequence
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import ovh.rwx.habbo.database.*
import org.slf4j.LoggerFactory

object MessengerDao {
    private val log = LoggerFactory.getLogger(javaClass)

    val friendshipSequence = HiLoSequence("messenger_friendships", blockSize = 500)
    val requestSequence = HiLoSequence("messenger_requests", blockSize = 500)
    fun getFriends(userId: Int): List<MessengerFriend> = db {
        query<MessengerFriendDto>(
            "sql/messenger/select_friends.sql",
            mapOf("user_one_id" to userId)
        ).map { it.toDomain() }
    }

    fun getRequests(toUserId: Int): List<MessengerRequest> = db {
        query<MessengerRequest>(
            "sql/messenger/select_requests.sql",
            mapOf("to_id" to toUserId)
        )
    }

    fun searchFriends(userId: Int, username: String): List<MessengerFriend> = db {
        query<MessengerFriendDto>(
            "sql/messenger/select_search_friends.sql",
            mapOf(
                "username" to "$username%",
                "user_id" to userId
            )
        ).map { it.toDomain() }
    }

    fun removeFriendships(userId: Int, friendIds: List<Int>) {
        db {
            batchUpdate(javaClass.classLoader.getResource("sql/messenger/delete_friends.sql")!!.readText(),
                    friendIds.map {
                        listOf(
                                mapOf(
                                        "user_one_id" to userId,
                                        "user_two_id" to it
                                ),
                                mapOf(
                                        "user_one_id" to it,
                                        "user_two_id" to userId
                                )
                        )
                    }.flatten()
            )
        }
    }

    fun removeAllRequests(toUserId: Int) {
        db {
            update(javaClass.classLoader.getResource("sql/messenger/delete_all_requests.sql")!!.readText(),
                    mapOf(
                            "to_id" to toUserId
                    )
            )
        }
    }

    fun removeRequests(requestIds: List<Int>) {
        db {
            batchUpdate(javaClass.classLoader.getResource("sql/messenger/delete_request.sql")!!.readText(),
                    requestIds.map {
                        mapOf(
                                "id" to it
                        )
                    }
            )
        }
    }

    fun addFriends(userId: Int, friendIds: Collection<Int>): Set<MessengerFriend> {
        val friends: MutableSet<MessengerFriend> = HashSet()
        val inserts = mutableListOf<Map<String, Any?>>()

        friendIds.forEach { friendId ->
            val id1 = friendshipSequence.nextId()
            val id2 = friendshipSequence.nextId()
            friends += MessengerFriend(id1, friendId, MessengerRelationship.NONE)
            inserts += mapOf("id" to id1, "user_one_id" to userId, "user_two_id" to friendId)
            inserts += mapOf("id" to id2, "user_one_id" to friendId, "user_two_id" to userId)
        }

        WriteBehindManager.queue {
            db {
                batchUpdate(
                    "INSERT INTO `messenger_friendships` (`id`, `user_one_id`, `user_two_id`) VALUES (:id, :user_one_id, :user_two_id)",
                    inserts
                )
            }
        }
        return friends
    }

    fun addRequest(fromUserId: Int, toUserId: Int): MessengerRequest {
        val id = requestSequence.nextId()
        val request = MessengerRequest(id, fromUserId)
        WriteBehindManager.queue {
            db {
                update(
                    "INSERT INTO `messenger_requests` (`id`, `to_id`, `from_id`) VALUES (:id, :to_id, :from_id)",
                    mapOf("id" to id, "to_id" to toUserId, "from_id" to fromUserId)
                )
            }
        }
        return request
    }

    fun getOfflineMessages(toUserId: Int): Set<Triple<Int, String, Int>> = db {
        val messages = query<OfflineMessageDto>(
            javaClass.classLoader.getResource("sql/messenger/select_offline_messages.sql")!!.readText(),
            mapOf("to_id" to toUserId)
        ).map { it.toDomain() }.toSet()

        if (messages.isNotEmpty()) {
            update(
                javaClass.classLoader.getResource("sql/messenger/delete_offline_messages.sql")!!.readText(),
                mapOf("to_id" to toUserId)
            )
        }

        messages
    }

    fun addOfflineMessage(fromUserId: Int, toUserId: Int, message: String) {
        WriteBehindManager.queue {
            db {
                insertAndGetGeneratedKey(
                    javaClass.classLoader.getResource("sql/messenger/insert_offline_message.sql")!!.readText(),
                    mapOf(
                        "to_id" to toUserId,
                        "from_id" to fromUserId,
                        "message" to message
                    )
                )
            }
        }
    }

    fun updateRelationship(messengerFriend: MessengerFriend) {
        db {
            update(javaClass.classLoader.getResource("sql/messenger/update_relationship.sql")!!.readText(),
                    mapOf(
                            "relationship" to messengerFriend.relationship.type,
                            "id" to messengerFriend.id
                    )
            )
        }
    }
}


data class MessengerFriendDto(
    val id: Int,
    val userId: Int,
    val relationship: Int
) {
    fun toDomain() = MessengerFriend(id, userId, MessengerRelationship.findByType(relationship))
}

data class OfflineMessageDto(
    val fromId: Int,
    val message: String,
    val timestamp: LocalDateTime
) {
    fun toDomain(): Triple<Int, String, Int> = Triple(
        fromId,
        message,
        (Instant.now().epochSecond - timestamp.atZone(ZoneId.systemDefault()).toEpochSecond()).toInt()
    )
}

