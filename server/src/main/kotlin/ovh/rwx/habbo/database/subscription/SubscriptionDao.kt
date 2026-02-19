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

package ovh.rwx.habbo.database.subscription

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.user.subscription.ClubType
import ovh.rwx.habbo.game.user.subscription.Subscription
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey
import ovh.rwx.habbo.kotlin.localDateTime
import ovh.rwx.habbo.kotlin.localDateTimeNowWithoutSecondsAndNanos

object SubscriptionDao {
    fun getSubscription(userId: Int, clubType: ClubType): Subscription? = HabboServer.database {
        select(
            javaClass.classLoader.getResource("sql/subscription/select_subscription_by_type.sql").readText(),
                mapOf(
                    "user_id" to userId,
                    "club_type" to clubType.name.lowercase()
                )
        ) {
            Subscription(
                    it.int("id"),
                it.int("user_id"),
                ClubType.valueOf(it.string("club_type").uppercase()),
                    it.localDateTime("activated"),
                it.localDateTime("expire"),
                it.int("items_limit"),
                it.int("items_used")
            )
        }.firstOrNull()
    }

    fun hasActiveBuildersClub(userId: Int): Boolean {
        val subscription = getSubscription(userId, ClubType.BUILDERS_CLUB) ?: return false
        return localDateTimeNowWithoutSecondsAndNanos().isBefore(subscription.expire)
    }

    fun countBuildersItems(userId: Int): Int = HabboServer.database {
        select(
            javaClass.classLoader.getResource("sql/subscription/count_builders_items.sql").readText(),
            mapOf("user_id" to userId)
        ) {
            it.int("count")
        }.firstOrNull() ?: 0
    }

    fun syncBuildersItemsUsed(subscription: Subscription?, userId: Int) {
        if (subscription == null) return

        val actualCount = countBuildersItems(userId)

        if (subscription.itemsUsed != actualCount) {
            updateBuildersItemsUsed(subscription, actualCount)
        }
    }

    fun createSubscription(userId: Int, months: Long, clubType: ClubType, itemsLimit: Int = 0): Subscription =
        HabboServer.database {
        val activated = localDateTimeNowWithoutSecondsAndNanos()
        val expire = localDateTimeNowWithoutSecondsAndNanos().plusMonths(months)
            val id = insertAndGetGeneratedKey(
                javaClass.classLoader.getResource("sql/subscription/insert_subscription_with_type.sql").readText(),
                mapOf(
                        "user_id" to userId,
                    "club_type" to clubType.name.lowercase(),
                        "activated" to activated,
                    "expire" to expire,
                    "items_limit" to itemsLimit,
                    "items_used" to 0
                )
        )

            Subscription(id, userId, clubType, activated, expire, itemsLimit, 0)
    }

    fun extendSubscription(subscription: Subscription?, months: Long) {
        if (subscription == null) return

        HabboServer.database {
            subscription.expire = subscription.expire.plusMonths(months)

            update(
                javaClass.classLoader.getResource("sql/subscription/update_subscription_expire.sql").readText(),
                mapOf(
                    "expire" to subscription.expire,
                    "id" to subscription.id
                )
            )
        }
    }

    fun extendBuildersClubSubscription(subscription: Subscription?, months: Long, itemsLimit: Int) {
        if (subscription == null) return

        HabboServer.database {
            subscription.expire = subscription.expire.plusMonths(months)

            update(
                javaClass.classLoader.getResource("sql/subscription/update_subscription_expire_with_limit.sql")
                    .readText(),
                    mapOf(
                            "expire" to subscription.expire,
                        "items_limit" to itemsLimit,
                        "id" to subscription.id
                    )
            )
        }
    }

    fun updateBuildersItemsUsed(subscription: Subscription?, itemsUsed: Int) {
        if (subscription == null) return

        HabboServer.database {
            subscription.itemsUsed = itemsUsed

            update(
                javaClass.classLoader.getResource("sql/subscription/update_builders_items_used.sql").readText(),
                mapOf(
                    "items_used" to itemsUsed,
                            "id" to subscription.id
                    )
            )
        }
    }

    fun clearSubscription(subscription: Subscription?) {
        if (subscription == null) return

        HabboServer.database {
            update(
                javaClass.classLoader.getResource("sql/subscription/delete_subscription.sql").readText(),
                    mapOf(
                            "id" to subscription.id
                    )
            )
        }
    }
}
