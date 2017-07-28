/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.database.messenger

import com.github.andrewoma.kwery.core.Row
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.messenger.MessengerFriend
import tk.jomp16.habbo.game.user.messenger.MessengerRequest
import tk.jomp16.habbo.kotlin.batchInsertAndGetGeneratedKeys
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey
import tk.jomp16.habbo.kotlin.localDateTime
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

object MessengerDao {
    fun getFriends(userId: Int): List<MessengerFriend> = HabboServer.database {
        select("SELECT `id`, `user_two_id` AS `user_id` FROM `messenger_friendships` WHERE `user_one_id` = :user_one_id",
                mapOf(
                        "user_one_id" to userId
                )
        ) { createMessengerFriend(it) }
    }

    fun getRequests(toUserId: Int): List<MessengerRequest> = HabboServer.database {
        select("SELECT `id`, `from_id` FROM `messenger_requests` WHERE `to_id` = :to_id", mapOf("to_id" to toUserId)) {
            MessengerRequest(
                    it.int("id"),
                    it.int("from_id")
            )
        }
    }

    fun searchFriends(userId: Int, username: String): List<MessengerFriend> = HabboServer.database {
        select("SELECT `id` AS `user_id` FROM `users` WHERE `username` LIKE :username AND NOT `id` = :user_id LIMIT 50",
                mapOf(
                        "username" to "$username%",
                        "user_id" to userId
                )
        ) { createMessengerFriend(it) }
    }

    private fun createMessengerFriend(row: Row): MessengerFriend = MessengerFriend(row.int("user_id"))

    fun removeFriendships(userId: Int, friendIds: List<Int>) {
        HabboServer.database {
            batchUpdate("DELETE FROM `messenger_friendships` WHERE `user_one_id` = :user_one_id AND `user_two_id` = :user_two_id",
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
                    }.flatMap { it }
            )
        }
    }

    fun removeAllRequests(toUserId: Int) {
        HabboServer.database {
            update("DELETE FROM `messenger_requests` WHERE `to_id` = :to_id",
                    mapOf(
                            "to_id" to toUserId
                    )
            )
        }
    }

    fun removeRequests(requestIds: List<Int>) {
        HabboServer.database {
            batchUpdate("DELETE FROM `messenger_requests` WHERE `id` = :id",
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
                batchInsertAndGetGeneratedKeys("INSERT INTO `messenger_friendships` (`user_one_id`, `user_two_id`) VALUES (:user_one_id, :user_two_id)",
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

                friends += MessengerFriend(it)
            }
        }

        return friends
    }

    fun addRequest(fromUserId: Int, toUserId: Int): MessengerRequest {
        val id = HabboServer.database {
            insertAndGetGeneratedKey("INSERT INTO `messenger_requests` (`to_id`, `from_id`) VALUES (:to_id, :from_id)",
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
            select("SELECT * FROM `messenger_offline_messages` WHERE `to_id` = :to_id",
                    mapOf(
                            "to_id" to toUserId
                    )
            ) {
                offlineMessages += Triple(
                        it.int("from_id"),
                        it.string("message"),
                        (Instant.now(Clock.systemUTC()).epochSecond - it.localDateTime("timestamp").toEpochSecond(ZoneOffset.UTC)).toInt()
                )
            }

            if (offlineMessages.isNotEmpty()) {
                update("DELETE FROM `messenger_offline_messages` WHERE `to_id` = :to_id",
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
            insertAndGetGeneratedKey("INSERT INTO `messenger_offline_messages` (`to_id`, `from_id`, `message`) VALUES (:to_id, :from_id, :message)",
                    mapOf(
                            "to_id" to toUserId,
                            "from_id" to fromUserId,
                            "message" to message
                    )
            )
        }
    }
}