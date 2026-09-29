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

package ovh.rwx.habbo.game.figure

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.figure.xml.FigureXMLHandler
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.urlUserAgent
import java.net.URI
import java.util.concurrent.ThreadLocalRandom
import javax.xml.parsers.SAXParserFactory

class FigureManager {
    private val log = LoggerFactory.getLogger(javaClass)

    val figureSets: MutableMap<Int, FigureSet> = mutableMapOf()
    val figurePalette: MutableMap<Int, MutableList<FigureColor>> = mutableMapOf()
    val figureSetTypes: MutableMap<String, FigureSetType> = mutableMapOf()

    fun load() {
        log.info("Loading figure data...")

        figureSets.clear()
        figurePalette.clear()
        figureSetTypes.clear()

        val figureUrl = resolveFigureDataUrl()
        try {
            log.info("Loading figuredata from: {}", figureUrl)
            urlUserAgent(figureUrl).inputStream.buffered().use {
                val saxParser = SAXParserFactory.newInstance().newSAXParser()
                val handler = FigureXMLHandler()

                saxParser.parse(it, handler)

                figureSets += handler.figureSets
                figurePalette += handler.figurePalette
                figureSetTypes += handler.figureSetTypes
            }

            log.info("Loaded {} figure palettes!", figurePalette.size)
            log.info("Loaded {} figure set types!", figureSetTypes.size)
            log.info("Loaded {} figure sets!", figureSets.size)
        } catch (e: Exception) {
            log.error("Failed to load figure data from $figureUrl: ${e.message}", e)
        }
    }

    fun resolveFigureDataUrl(): String {
        val configured = HabboServer.habboConfig.figuredataXml
        if (configured.isNotBlank()) return configured

        val extVarsUrl = HabboServer.habboConfig.externalVariablesTxt
        if (extVarsUrl.isNotBlank()) {
            runCatching {
                URI.create(extVarsUrl)
                    .toURL()
                    .openStream()
                    .bufferedReader()
                    .useLines { lines ->
                        lines.mapNotNull { line ->
                            val idx = line.indexOf('=')
                            if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim() else null
                        }.toMap()
                    }["external.figurepartlist.txt"]
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        }

        return "https://swf.habbo.rwx.ovh/gamedata/figuredata/0"
    }

    fun generateRandomFigure(
        gender: String = if (ThreadLocalRandom.current().nextBoolean()) "M" else "F",
        allowClub: Boolean = false
    ): String {
        val g = if (gender.equals("F", ignoreCase = true)) "F" else "M"
        if (figureSetTypes.isEmpty() || figureSets.isEmpty()) {
            return if (g == "M") {
                "hr-115-42.hd-190-1.ch-215-62.lg-270-62.sh-290-62"
            } else {
                "hr-515-33.hd-600-1.ch-635-70.lg-700-64.sh-725-62"
            }
        }

        val parts = mutableListOf<String>()
        val rnd = ThreadLocalRandom.current()

        // Core mandatory parts: hd, hr, ch, lg, sh
        val typesToGenerate = mutableListOf("hd", "hr", "ch", "lg", "sh")

        // Optional accessories
        if (rnd.nextDouble() < 0.25) typesToGenerate.add("ea") // glasses / eyewear
        if (g == "M" && rnd.nextDouble() < 0.20) typesToGenerate.add("fa") // facial accessory / beard
        if (rnd.nextDouble() < 0.15) typesToGenerate.add("ha") // hat
        if (rnd.nextDouble() < 0.15) typesToGenerate.add("wa") // belt / waist
        if (rnd.nextDouble() < 0.10) typesToGenerate.add("ca") // chest accessory
        if (rnd.nextDouble() < 0.10) typesToGenerate.add("cc") // jacket / coat

        for (type in typesToGenerate) {
            val setType = figureSetTypes[type] ?: continue
            val validSets = setType.sets.filter {
                it.selectable &&
                        (it.gender == "U" || it.gender.equals(g, ignoreCase = true)) &&
                        (allowClub || it.club == 0)
            }
            if (validSets.isEmpty()) continue

            val chosenSet = validSets[rnd.nextInt(validSets.size)]
            val palette = figurePalette[chosenSet.paletteId] ?: figurePalette[setType.paletteId] ?: emptyList()
            val validColors = palette.filter {
                it.selectable && (allowClub || (!it.clubOnly && it.club == 0))
            }

            val colorCount = if (chosenSet.colorable && chosenSet.colors > 0) chosenSet.colors else 1
            val chosenColorIds = mutableListOf<String>()

            if (validColors.isNotEmpty()) {
                repeat(colorCount) {
                    chosenColorIds.add(validColors[rnd.nextInt(validColors.size)].id.toString())
                }
            } else {
                chosenColorIds.add("0")
            }

            parts.add("$type-${chosenSet.id}-${chosenColorIds.joinToString("-")}")
        }

        return if (parts.isNotEmpty()) parts.joinToString(".") else "hr-115-42.hd-190-1.ch-215-62.lg-270-62.sh-290-62"
    }

    fun isValidFigure(figure: String, gender: String, session: HabboSession): Boolean {
        val clubLevel = when {
            session.userInformation.vip || session.habboSubscription.validUserSubscription -> 2
            session.userInformation.rank >= 7 || session.hasPermission("acc_any_clothing") -> 2
            else -> 0
        }
        val allowAnyClothing = session.userInformation.rank >= 7 || session.hasPermission("acc_any_clothing")
        val unlockedSetIds = if (allowAnyClothing) {
            emptySet()
        } else {
            val clothings = session.userInformation.clothings
            if (clothings.isEmpty()) {
                emptySet()
            } else {
                HabboServer.habboGame.itemManager.furniXMLInfos
                    .filterKeys { clothings.contains(it) }
                    .values
                    .flatMap { info ->
                        info.customParams.split(',').mapNotNull { it.trim().toIntOrNull() }
                    }
                    .toSet()
            }
        }
        return isValidFigureSet(figure, gender, clubLevel, unlockedSetIds, allowAnyClothing)
    }

    fun isValidFigureSet(figure: String, gender: String, club: Boolean): Boolean {
        return isValidFigureSet(figure, gender, clubLevel = if (club) 2 else 0, allowAnyClothing = false)
    }

    fun isValidFigureSet(
        figure: String,
        gender: String,
        clubLevel: Int = 0,
        unlockedSetIds: Set<Int> = emptySet(),
        allowAnyClothing: Boolean = false
    ): Boolean {
        val g = gender.uppercase()
        if (g != "M" && g != "F") return false
        if (figure.isBlank()) return false

        // Fallback when figure metadata is not loaded (e.g. uninitialized / offline)
        if (figureSetTypes.isEmpty() || figureSets.isEmpty()) {
            val parts = figure.split('.')
            if (parts.isEmpty()) return false
            val types = mutableSetOf<String>()
            for (part in parts) {
                val tokens = part.split('-')
                if (tokens.size < 2) return false
                types.add(tokens[0].lowercase())
            }
            return types.contains("hd") && types.contains("lg") && (g == "M" || types.contains("ch"))
        }

        val partTokens = figure.split('.')
        if (partTokens.isEmpty()) return false

        val presentTypes = mutableSetOf<String>()

        for (part in partTokens) {
            val tokens = part.split('-')
            if (tokens.size < 2) return false

            val type = tokens[0].lowercase()
            val setId = tokens[1].toIntOrNull() ?: return false

            // Disallow duplicate part types (e.g. multiple "ch" or "hd")
            if (!presentTypes.add(type)) return false

            val setType = figureSetTypes[type] ?: return false
            val set = figureSets[setId] ?: return false

            // Ensure the set belongs to this setType
            if (set.type != type) return false

            // Check gender: "U" or matching user gender
            if (set.gender != "U" && !set.gender.equals(g, ignoreCase = true)) return false

            // Selectable check
            if (!allowAnyClothing && !set.selectable) return false

            // Sellable (purchased clothing furni) check
            if (!allowAnyClothing && set.sellable && setId !in unlockedSetIds) return false

            // Club level check on set
            if (!allowAnyClothing && set.club > clubLevel) return false

            // Palette resolution
            val palette = figurePalette[set.paletteId].takeIf { !it.isNullOrEmpty() }
                ?: figurePalette[setType.paletteId]
                ?: return false

            // Parse colors
            val colorTokens = if (tokens.size > 2) tokens.subList(2, tokens.size) else emptyList()
            val colorIds = colorTokens.mapNotNull { it.toIntOrNull() }

            if (colorTokens.size != colorIds.size) return false

            if (set.colorable && set.colors > 0 && colorIds.size > set.colors) {
                return false
            }

            for (cid in colorIds) {
                if (cid == 0 && palette.none { it.id == 0 }) continue

                val colorObj = palette.find { it.id == cid } ?: return false

                if (!allowAnyClothing && !colorObj.selectable) return false
                if (!allowAnyClothing && colorObj.club > clubLevel) return false
            }
        }

        // Validate mandatory parts according to AS3 rules
        for ((type, setType) in figureSetTypes) {
            if (type !in presentTypes) {
                val optLevel = setType.optionalFromClubLevel(g)
                if (optLevel == -1) {
                    // Mandatory for all club levels (e.g. hd, lg, or female ch)
                    return false
                }
                if (!allowAnyClothing && clubLevel < optLevel) {
                    // Missing part requires higher club level than user has (e.g. shirtless male without club)
                    return false
                }
            }
        }

        return true
    }

    fun resolveClubLevel(figure: String, gender: String): Int {
        val g = gender.uppercase()
        if (g != "M" && g != "F") return 0
        if (figureSetTypes.isEmpty() || figureSets.isEmpty()) return 0

        var requiredClub = 0
        val presentTypes = mutableSetOf<String>()

        for (part in figure.split('.')) {
            val tokens = part.split('-')
            if (tokens.size < 2) continue
            val type = tokens[0].lowercase()
            val setId = tokens[1].toIntOrNull() ?: continue
            presentTypes.add(type)

            val setType = figureSetTypes[type] ?: continue
            val set = figureSets[setId] ?: continue
            requiredClub = maxOf(requiredClub, set.club)

            val palette = figurePalette[set.paletteId].takeIf { !it.isNullOrEmpty() }
                ?: figurePalette[setType.paletteId]
                ?: continue

            val colorTokens = if (tokens.size > 2) tokens.subList(2, tokens.size) else emptyList()
            for (cidStr in colorTokens) {
                val cid = cidStr.toIntOrNull() ?: continue
                val colorObj = palette.find { it.id == cid } ?: continue
                requiredClub = maxOf(requiredClub, colorObj.club)
            }
        }

        for ((type, setType) in figureSetTypes) {
            if (type !in presentTypes) {
                val optLevel = setType.optionalFromClubLevel(g)
                if (optLevel > 0) {
                    requiredClub = maxOf(requiredClub, optLevel)
                }
            }
        }

        return requiredClub
    }
}