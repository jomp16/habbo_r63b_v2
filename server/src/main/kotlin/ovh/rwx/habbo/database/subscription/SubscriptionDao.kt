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

import ovh.rwx.habbo.game.user.subscription.ClubType
import ovh.rwx.habbo.game.user.subscription.Subscription
import ovh.rwx.habbo.kotlin.localDateTimeNowWithoutSecondsAndNanos
import ovh.rwx.habbo.database.*
import java.time.LocalDateTime

object SubscriptionDao {
    fun getSubscription(userId: Int, clubType: ClubType): Subscription? = db {
        val subscription = queryOne<SubscriptionDto>(
            "sql/subscription/select_subscription_by_type.sql",
            mapOf(
                "user_id" to userId,
                "club_type" to clubType.name.lowercase()
            )
        )?.toDomain()

        if (subscription == null && clubType == ClubType.BUILDERS_CLUB) {
            return@db createSubscription(userId, 0, clubType, 100, activated = null, expire = null)
        }

        return@db subscription
    }

    fun hasActiveBuildersClub(userId: Int): Boolean {
        val subscription = getSubscription(userId, ClubType.BUILDERS_CLUB) ?: return false
        if (subscription.trial) return false
        return localDateTimeNowWithoutSecondsAndNanos().isBefore(subscription.expire)
    }

    fun countBuildersItems(userId: Int): Int = db {
        queryOne<Int>(
            "sql/subscription/count_builders_items.sql",
            mapOf("user_id" to userId)
        ) ?: 0
    }

    fun syncBuildersItemsUsed(subscription: Subscription, userId: Int) {
        val actualCount = countBuildersItems(userId)

        if (subscription.itemsUsed != actualCount) {
            updateBuildersItemsUsed(subscription, actualCount)
        }
    }

    fun createSubscription(
        userId: Int,
        months: Long,
        clubType: ClubType,
        itemsLimit: Int = 0,
        activated: LocalDateTime? = localDateTimeNowWithoutSecondsAndNanos(),
        expire: LocalDateTime? = activated?.plusMonths(months)
    ): Subscription =
        db {
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

        db {
            subscription.expire?.let { expire ->
                subscription.expire = expire.plusMonths(months)

                update(
                    javaClass.classLoader.getResource("sql/subscription/update_subscription_expire.sql").readText(),
                    mapOf(
                        "expire" to subscription.expire,
                        "id" to subscription.id
                    )
                )
            }
        }
    }

    fun updateBuildersClubSubscription(subscription: Subscription?) {
        if (subscription == null) return

        db {
            update(
                javaClass.classLoader.getResource("sql/subscription/update_subscription_expire_with_limit.sql")
                    .readText(),
                mapOf(
                    "activated" to subscription.activated,
                    "expire" to subscription.expire,
                    "items_limit" to subscription.itemsLimit,
                    "id" to subscription.id
                )
            )
        }
    }

    fun updateBuildersItemsUsed(subscription: Subscription?, itemsUsed: Int) {
        if (subscription == null) return

        db {
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

        if (subscription.clubType == ClubType.BUILDERS_CLUB) {

        } else {
            db {
                update(
                    javaClass.classLoader.getResource("sql/subscription/delete_subscription.sql").readText(),
                    mapOf(
                        "id" to subscription.id
                    )
                )
            }
        }
    }
}

data class SubscriptionDto(
    val id: Int,
    val userId: Int,
    val clubType: String,
    val activated: LocalDateTime?,
    val expire: LocalDateTime?,
    val itemsLimit: Int,
    val itemsUsed: Int
) {
    fun toDomain() = Subscription(
        id, userId, ClubType.valueOf(clubType.uppercase()), activated, expire, itemsLimit, itemsUsed
    )
}

