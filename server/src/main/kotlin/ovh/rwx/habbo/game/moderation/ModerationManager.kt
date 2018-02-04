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

package ovh.rwx.habbo.game.moderation

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.moderation.ModerationDao

class ModerationManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val moderationCategories: MutableMap<Int, String> = mutableMapOf()
    val moderationTopics: MutableMap<Int, ModerationTopic> = mutableMapOf()

    fun load() {
        log.info("Loading moderation stuffs...")

        moderationCategories.clear()
        moderationTopics.clear()

        moderationCategories += ModerationDao.getCategories()
        moderationTopics += ModerationDao.getTopics().associateBy { it.topicId }

        log.info("Loaded ${moderationCategories.size} moderation categories!")
        log.info("Loaded ${moderationTopics.size} moderation topics!")
    }
}