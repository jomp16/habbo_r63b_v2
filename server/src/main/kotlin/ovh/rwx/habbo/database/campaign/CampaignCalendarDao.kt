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

package ovh.rwx.habbo.database.campaign

import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.game.campaign.CampaignCalendar
import ovh.rwx.habbo.game.campaign.CampaignCalendarReward

object CampaignCalendarDao {
    fun getCampaignCalendars(): List<CampaignCalendar> = db {
        query<CampaignCalendar>(
            "SELECT `name`, `start_date`, `days`, IFNULL(`campaign_image`, '') AS `campaign_image` FROM `campaign_calendars`"
        )
    }

    fun getRewards(campaignName: String): List<CampaignCalendarReward> = db {
        query<CampaignCalendarReward>(
            """
            SELECT `campaign_name`, `door_index`, `product_name`, IFNULL(`custom_image`, '') AS `custom_image`,
                   IFNULL(`furniture_class_name`, '') AS `furniture_class_name`
            FROM `campaign_calendar_rewards`
            WHERE `campaign_name` = :campaign_name
            """.trimIndent(),
            mapOf("campaign_name" to campaignName)
        )
    }

    fun getOpenedDoors(userId: Int, campaignName: String): List<Int> = db {
        query<Int>(
            "SELECT `door_index` FROM `user_campaign_calendar_opens` WHERE `user_id` = :user_id AND `campaign_name` = :campaign_name ORDER BY `door_index`",
            mapOf("user_id" to userId, "campaign_name" to campaignName)
        )
    }

    fun isOpen(userId: Int, campaignName: String, doorIndex: Int): Boolean = db {
        queryOne<Int>(
            "SELECT 1 FROM `user_campaign_calendar_opens` WHERE `user_id` = :user_id AND `campaign_name` = :campaign_name AND `door_index` = :door_index LIMIT 1",
            mapOf("user_id" to userId, "campaign_name" to campaignName, "door_index" to doorIndex)
        ) != null
    }

    fun insertOpen(userId: Int, campaignName: String, doorIndex: Int) {
        WriteBehindManager.queue {
            db {
                update(
                    """
                    INSERT INTO `user_campaign_calendar_opens` (`user_id`, `campaign_name`, `door_index`)
                    VALUES (:user_id, :campaign_name, :door_index)
                    ON DUPLICATE KEY UPDATE `opened_at` = CURRENT_TIMESTAMP
                    """.trimIndent(),
                    mapOf("user_id" to userId, "campaign_name" to campaignName, "door_index" to doorIndex)
                )
            }
        }
    }
}
