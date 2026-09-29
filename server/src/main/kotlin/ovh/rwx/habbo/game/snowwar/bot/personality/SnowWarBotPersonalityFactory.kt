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

package ovh.rwx.habbo.game.snowwar.bot.personality

import ovh.rwx.habbo.game.snowwar.bot.BotRole
import kotlin.math.abs

object SnowWarBotPersonalityFactory {
    private val personalities: Map<BotRole, SnowWarBotPersonality> = mapOf(
        BotRole.NORMAL to NormalBotPersonality(),
        BotRole.SNIPER to SniperBotPersonality(),
        BotRole.RUSHER to RusherBotPersonality(),
        BotRole.SKIRMISHER to SkirmisherBotPersonality()
    )

    fun forBotId(botId: Int): SnowWarBotPersonality {
        val role = when (abs(botId) % 4) {
            0 -> BotRole.SNIPER
            1 -> BotRole.SKIRMISHER
            2 -> BotRole.RUSHER
            else -> BotRole.NORMAL
        }
        return personalities.getValue(role)
    }

    fun forRole(role: BotRole): SnowWarBotPersonality {
        return personalities.getValue(role)
    }
}
