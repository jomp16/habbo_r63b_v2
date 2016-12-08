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

package tk.jomp16.habbo.game.moderation

import tk.jomp16.habbo.database.moderation.ModerationDao

class ModerationManager {
    val moderationCategories: MutableMap<Int, String> = mutableMapOf()
    val moderationTopics: MutableMap<Int, ModerationTopic> = mutableMapOf()

    init {
        load()
    }

    fun load() {
        moderationCategories.clear()
        moderationTopics.clear()

        moderationCategories += ModerationDao.getCategories()
        moderationTopics += ModerationDao.getTopics().associateBy { it.topicId }
    }
}