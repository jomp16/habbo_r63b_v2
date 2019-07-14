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

import com.github.andrewoma.kwery.core.Row
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.user.messenger.MessengerFriend
import ovh.rwx.habbo.game.user.messenger.MessengerRelationship
import ovh.rwx.habbo.game.user.messenger.MessengerRequest
import ovh.rwx.habbo.kotlin.batchInsertAndGetGeneratedKeys
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey
import ovh.rwx.habbo.kotlin.localDateTime
import java.time.Instant
import java.time.ZoneId
import java.util.*

object MessengerDao {
    fun getFriends(userId: Int): List<MessengerFriend> = HabboServer.database {
        select(javaClass.classLoader.getResource("sql/messenger/select_friends.sql")!!.readText(),
                mapOf(
                        "user_one_id" to userId
                )
        ) { createMessengerFriend(it) }
    }

    fun getRequests(toUserId: Int): List<MessengerRequest> = HabboServer.database {
        select(javaClass.classLoader.getResource("sql/messenger/select_requests.sql")!!.readText(),
                mapOf(
                        "to_id" to toUserId
                )
        ) {
            MessengerRequest(
                    it.int("id"),
                    it.int("from_id")
            )
        }
    }

    fun searchFriends(userId: Int, username: String): List<MessengerFriend> = HabboServer.database {
        select(javaClass.classLoader.getResource("sql/messenger/select_search_friends.sql")!!.readText(),
                mapOf(
                        "username" to "$username%",
                        "user_id" to userId
                )
        ) { createMessengerFriend(it) }
    }

    private fun createMessengerFriend(row: Row): MessengerFriend = MessengerFriend(row.int("id"), row.int("user_id"), MessengerRelationship.findByType(row.int("relationship")))

    fun removeFriendships(userId: Int, friendIds: List<Int>) {
        HabboServer.database {
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
        HabboServer.database {
            update(javaClass.classLoader.getResource("sql/messenger/delete_all_requests.sql")!!.readText(),
                    mapOf(
                            "to_id" to toUserId
                    )
            )
        }
    }

    fun removeRequests(requestIds: List<Int>) {
        HabboServer.database {
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

        HabboServer.database {
            friendIds.forEach {
                val ids = batchInsertAndGetGeneratedKeys(javaClass.classLoader.getResource("sql/messenger/insert_friends.sql")!!.readText(),
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
                )

                friends += ids.map { id -> MessengerFriend(id, it, MessengerRelationship.NONE) }
            }
        }

        return friends
    }

    fun addRequest(fromUserId: Int, toUserId: Int): MessengerRequest {
        val id = HabboServer.database {
            insertAndGetGeneratedKey(javaClass.classLoader.getResource("sql/messenger/insert_request.sql")!!.readText(),
                    mapOf(
                            "to_id" to toUserId,
                            "from_id" to fromUserId
                    )
            )
        }

        return MessengerRequest(id, fromUserId)
    }

    fun getOfflineMessages(toUserId: Int): Set<Triple<Int, String, Int>> {
        val offlineMessages: MutableSet<Triple<Int, String, Int>> = HashSet()

        HabboServer.database {
            select(javaClass.classLoader.getResource("sql/messenger/select_offline_messages.sql")!!.readText(),
                    mapOf(
                            "to_id" to toUserId
                    )
            ) {
                offlineMessages += Triple(
                        it.int("from_id"),
                        it.string("message"),
                        (Instant.now().epochSecond - it.localDateTime("timestamp").atZone(ZoneId.systemDefault()).toEpochSecond()).toInt()
                )
            }

            if (offlineMessages.isNotEmpty()) {
                update(javaClass.classLoader.getResource("sql/messenger/delete_offline_messages.sql")!!.readText(),
                        mapOf(
                                "to_id" to toUserId
                        )
                )
            }
        }

        return offlineMessages
    }

    fun addOfflineMessage(fromUserId: Int, toUserId: Int, message: String) {
        HabboServer.database {
            insertAndGetGeneratedKey(javaClass.classLoader.getResource("sql/messenger/insert_offline_message.sql")!!.readText(),
                    mapOf(
                            "to_id" to toUserId,
                            "from_id" to fromUserId,
                            "message" to message
                    )
            )
        }
    }

    fun updateRelationship(messengerFriend: MessengerFriend) {
        HabboServer.database {
            update(javaClass.classLoader.getResource("sql/messenger/update_relationship.sql")!!.readText(),
                    mapOf(
                            "relationship" to messengerFriend.relationship.type,
                            "id" to messengerFriend.id
                    )
            )
        }
    }
}