/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.database.moderation

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.moderation.ModerationTopic

object ModerationDao {
    fun getCategories(): List<Pair<Int, String>> = HabboServer.database {
        select("SELECT * FROM moderation_categories") {
            it.int("id") to it.string("name")
        }
    }

    fun getTopics(): List<ModerationTopic> = HabboServer.database {
        select("SELECT * FROM moderation_topics") {
            ModerationTopic(
                    it.int("id"),
                    it.int("category_id"),
                    it.string("name"),
                    it.int("topic_id"),
                    it.string("consequence")
            )
        }
    }
}