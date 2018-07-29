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

package ovh.rwx.habbo.game.antimutant

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.antimutant.xml.FigureXMLHandler
import ovh.rwx.habbo.kotlin.urlUserAgent
import javax.xml.parsers.SAXParserFactory

class AntiMutantManager {
    private val log = LoggerFactory.getLogger(javaClass)

    private val figureSets: MutableMap<Int, FigureSet> = mutableMapOf()
    private val figurePalette: MutableMap<Int, MutableList<FigureColor>> = mutableMapOf()

    fun load() {
        log.info("Loading AntiMutant...")

        figureSets.clear()
        figurePalette.clear()

        urlUserAgent(HabboServer.habboConfig.figuredataXml).inputStream.buffered().use {
            val saxParser = SAXParserFactory.newInstance().newSAXParser()
            val handler = FigureXMLHandler()

            saxParser.parse(it, handler)

            figureSets += handler.figureSets
            figurePalette += handler.figurePalette
        }

        log.info("Loaded ${figurePalette.size} figure palettes!")
        log.info("Loaded ${figureSets.size} figure sets!")
    }

    fun isValidFigureSet(figure: String, gender: String, club: Boolean): Boolean {
        if ("M" != gender && "F" != gender) return false

        val parts = figure.split('.')
        val types = mutableListOf<String>()

        parts.forEach { part ->
            val data = part.split("-".toRegex(), 3)

            if (data.size < 3) return false

            types.add(data[0])

            val id = data[1].toInt()

            if (!figureSets.containsKey(id)) return false

            val set = figureSets[id] ?: return false

            if ("U" != set.gender && gender != set.gender) return false

            val palette = figurePalette[set.paletteId] ?: return false

            when {
                set.colors > 0 -> {
                    val colors = data[2].split('-')

                    if (colors.size != set.colors) return false

                    if (palette.any { figureColor -> !colors.contains(figureColor.color) && figureColor.clubOnly && !club }) return false
                }
                else -> if (data[2].contains('-') || palette.any { figureColor -> figureColor.color != data[2] && figureColor.clubOnly && !club }) return false
            }
        }

        return types.contains("lg") && types.contains("hd") && ("M" == gender || types.contains("ch"))
    }
}