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

import java.util.concurrent.ThreadLocalRandom

object BotNameGenerator {
    private val ADJECTIVES = listOf(
        "Frosty", "Snowy", "Chilly", "Icy", "Arctic", "Polar", "Winter", "Cool",
        "Glacial", "Speedy", "Lucky", "Brave", "Mighty", "Slick", "Shadow", "Pixel",
        "Stormy", "Wild", "Quick", "Cold", "Hyper", "Mega", "Super", "Alpha"
    )

    private val NOUNS = listOf(
        "Habbo", "Bot", "Player", "Penguin", "Yeti", "Wolf", "Bear", "Hunter",
        "Striker", "Ranger", "Ace", "Hero", "Knight", "Ninja", "Ghost", "Beast",
        "Champ", "Frank", "Duck", "Fox", "Viper", "Hawk", "Storm", "Blaze"
    )

    private val STANDALONE_NAMES = listOf(
        "Frank", "Santini", "Olimpio", "Blizzard", "Frostbite", "SubZero",
        "IceBreaker", "SnowFlake", "Avalanche", "PolarBear", "WinterKing",
        "SnowBrawler", "FrostyBobba", "ColdSnap", "IceShield", "Snowman"
    )

    private val MOTTOS = listOf(
        "SnowWar Champion!",
        "Bring on the snow!",
        "Target locked.",
        "Ready to rumble!",
        "Winter is here!",
        "Ice in my veins.",
        "Catch this snowball!",
        "Snowball fight!",
        "Never back down!",
        "Brrr! So cold!",
        "Born to play SnowWar.",
        "Eat my snow!",
        "Victory is ours!"
    )

    fun generateNickname(existingNames: Set<String> = emptySet()): String {
        val rnd = ThreadLocalRandom.current()

        repeat(50) {
            val candidate = when (rnd.nextInt(4)) {
                0 -> {
                    // Adjective + Noun (e.g. FrostyWolf)
                    val adj = ADJECTIVES[rnd.nextInt(ADJECTIVES.size)]
                    val noun = NOUNS[rnd.nextInt(NOUNS.size)]
                    "$adj$noun"
                }

                1 -> {
                    // Standalone name + 2-digit number (e.g. Frank07)
                    val name = STANDALONE_NAMES[rnd.nextInt(STANDALONE_NAMES.size)]
                    val num = rnd.nextInt(10, 99)
                    "$name$num"
                }

                2 -> {
                    // Noun + Number (e.g. Striker_42)
                    val noun = NOUNS[rnd.nextInt(NOUNS.size)]
                    val num = rnd.nextInt(1, 999)
                    "${noun}_$num"
                }

                else -> {
                    // Adjective + Number (e.g. Polar_99)
                    val adj = ADJECTIVES[rnd.nextInt(ADJECTIVES.size)]
                    val num = rnd.nextInt(1, 99)
                    "${adj}_$num"
                }
            }.take(15) // Enforce Habbo 15-char maximum username constraint

            if (candidate !in existingNames && candidate.length in 3..15) {
                return candidate
            }
        }

        // Fallback guaranteed unique
        return "SnowBot_${rnd.nextInt(100, 999)}".take(15)
    }

    fun generateMotto(): String {
        val rnd = ThreadLocalRandom.current()
        return MOTTOS[rnd.nextInt(MOTTOS.size)]
    }
}
