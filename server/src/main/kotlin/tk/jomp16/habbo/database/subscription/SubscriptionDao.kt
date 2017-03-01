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

package tk.jomp16.habbo.database.subscription

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.subscription.Subscription
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey
import tk.jomp16.habbo.kotlin.localDateTime
import tk.jomp16.habbo.kotlin.localDateTimeNowWithoutSecondsAndNanos

object SubscriptionDao {
    fun getSubscription(userId: Int): Subscription? = HabboServer.database {
        select("SELECT * FROM users_subscriptions WHERE user_id = :user_id LIMIT 1",
                mapOf(
                        "user_id" to userId
                )
        ) {
            Subscription(
                    it.int("id"),
                    it.localDateTime("activated"),
                    it.localDateTime("expire")
            )
        }.firstOrNull()
    }

    fun createSubscription(userId: Int, months: Long): Subscription = HabboServer.database {
        val activated = localDateTimeNowWithoutSecondsAndNanos()
        val expire = localDateTimeNowWithoutSecondsAndNanos().plusMonths(months)

        val id = insertAndGetGeneratedKey(
                "INSERT INTO users_subscriptions (user_id, activated, expire) VALUES (:user_id, :activated, :expire)",
                mapOf(
                        "user_id" to userId,
                        "activated" to activated,
                        "expire" to expire
                )
        )

        Subscription(id, activated, expire)
    }

    fun extendSubscription(subscription: Subscription?, months: Long) {
        if (subscription == null) return

        HabboServer.database {
            subscription.expire = subscription.expire.plusMonths(months)

            update("UPDATE users_subscriptions SET expire = :expire WHERE id = :id",
                    mapOf(
                            "expire" to subscription.expire,
                            "id" to subscription.id
                    )
            )
        }
    }

    fun clearSubscription(subscription: Subscription?) {
        if (subscription == null) return

        HabboServer.database {
            update("DELETE FROM users_subscriptions WHERE id = :id",
                    mapOf(
                            "id" to subscription.id
                    )
            )
        }
    }
}