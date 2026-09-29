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

package ovh.rwx.habbo.game.snowwar.bot

import org.junit.Assert.*
import org.junit.Test
import ovh.rwx.habbo.game.snowwar.SnowWarUser
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTeam
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTrajectory

class SnowWarBotTest {

    @Test
    fun testBotNameGenerator() {
        val generatedNames = mutableSetOf<String>()
        for (i in 0 until 50) {
            val name = BotNameGenerator.generateNickname(generatedNames)
            assertTrue("Name should be between 3 and 15 chars: $name", name.length in 3..15)
            assertTrue("Name should match Habbo username characters: $name", name.matches(Regex("^[a-zA-Z0-9_.-]+$")))
            assertFalse("Name should be unique: $name", generatedNames.contains(name))
            generatedNames.add(name)
        }

        val motto = BotNameGenerator.generateMotto()
        assertTrue("Motto should not be blank", motto.isNotBlank())
    }

    @Test
    fun testBotUserCreation() {
        val bot = SnowWarUser(
            session = null,
            team = SnowWarTeam.BLUE,
            botData = SnowWarBotData(
                id = -1001,
                name = "FrostyBot",
                figure = "hr-115-42.hd-190-1.ch-215-62.lg-270-62.sh-290-62",
                gender = "M",
                mission = "SnowWar Bot"
            )
        )

        assertFalse(bot.isRealPlayer)
        assertEquals(-1001, bot.userId)
        assertEquals("FrostyBot", bot.name)
        assertEquals("M", bot.gender)
        assertEquals("SnowWar Bot", bot.mission)
        assertTrue(bot.isBot)
        assertTrue(bot.isLoaded)

        bot.setupSpawn(0, 10 to 10)
        assertTrue(bot.isBot)
        assertEquals(5, bot.hitPoints)
        assertEquals(5, bot.snowBallCount)
        assertEquals(0, bot.getChecksumVariables()[9]) // Variable 9 is always 0 in AS3 HumanGameObject
    }

    @Test
    fun testBotPersonalities() {
        val sniper = ovh.rwx.habbo.game.snowwar.bot.personality.SnowWarBotPersonalityFactory.forBotId(-1000)
        assertEquals(BotRole.SNIPER, sniper.role)

        val skirmisher = ovh.rwx.habbo.game.snowwar.bot.personality.SnowWarBotPersonalityFactory.forBotId(-1001)
        assertEquals(BotRole.SKIRMISHER, skirmisher.role)

        val rusher = ovh.rwx.habbo.game.snowwar.bot.personality.SnowWarBotPersonalityFactory.forBotId(-1002)
        assertEquals(BotRole.RUSHER, rusher.role)

        val normal = ovh.rwx.habbo.game.snowwar.bot.personality.SnowWarBotPersonalityFactory.forBotId(-1003)
        assertEquals(BotRole.NORMAL, normal.role)

        val rnd = java.util.concurrent.ThreadLocalRandom.current()

        // Verify cooldown ranges
        assertTrue(rusher.getDecisionCooldown(rnd) in 4..8)
        assertTrue(normal.getDecisionCooldown(rnd) in 5..10)
        assertTrue(skirmisher.getDecisionCooldown(rnd) in 5..10)
        assertTrue(sniper.getDecisionCooldown(rnd) in 7..13)

        // Verify trajectory selection for close range (< 18k) is QUICK for all
        assertEquals(SnowWarTrajectory.QUICK, normal.selectTrajectory(10_000, rnd))
        assertEquals(SnowWarTrajectory.QUICK, rusher.selectTrajectory(10_000, rnd))
        assertEquals(SnowWarTrajectory.QUICK, sniper.selectTrajectory(10_000, rnd))

        // Verify long range (> 48k) is LONG_LOB
        assertEquals(SnowWarTrajectory.LONG_LOB, normal.selectTrajectory(60_000, rnd))
        assertEquals(SnowWarTrajectory.LONG_LOB, sniper.selectTrajectory(60_000, rnd))
    }
}
