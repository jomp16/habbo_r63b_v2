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

package ovh.rwx.habbo.game.figure.xml

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import ovh.rwx.habbo.game.figure.FigureColor
import ovh.rwx.habbo.game.figure.FigureSet
import ovh.rwx.habbo.game.figure.FigureSetType

class FigureXMLHandler : DefaultHandler() {
    val figureSets: MutableMap<Int, FigureSet> = mutableMapOf()
    val figurePalette: MutableMap<Int, MutableList<FigureColor>> = mutableMapOf()
    val figureSetTypes: MutableMap<String, FigureSetType> = mutableMapOf()

    private var currentPaletteId = 0
    private var currentColorId = 0
    private var currentSetType: String = ""
    private var currentSet: FigureSet? = null
    private var currentSetColors = 0
    private var content: StringBuilder = StringBuilder()

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        when (qName) {
            "palette" -> {
                currentPaletteId = attributes.getValue("id").toInt()
                figurePalette[currentPaletteId] = mutableListOf()
            }

            "color" -> {
                content.setLength(0)
                currentColorId = attributes.getValue("id").toInt()
                val index = attributes.getValue("index")?.toIntOrNull() ?: 0
                val club = attributes.getValue("club")?.toIntOrNull() ?: 0
                val selectable = attributes.getValue("selectable")?.let { it == "1" } ?: true

                val figureColor = FigureColor(
                    id = currentColorId,
                    index = index,
                    club = club,
                    clubOnly = club > 0,
                    selectable = selectable
                )
                figurePalette[currentPaletteId]?.add(figureColor)
            }

            "settype" -> {
                currentSetType = attributes.getValue("type")
                val paletteId = attributes.getValue("paletteid").toInt()
                val mandM0 = attributes.getValue("mand_m_0") == "1"
                val mandF0 = attributes.getValue("mand_f_0") == "1"
                val mandM1 = attributes.getValue("mand_m_1") == "1"
                val mandF1 = attributes.getValue("mand_f_1") == "1"

                val setType = FigureSetType(
                    type = currentSetType,
                    paletteId = paletteId,
                    mandM0 = mandM0,
                    mandF0 = mandF0,
                    mandM1 = mandM1,
                    mandF1 = mandF1
                )
                figureSetTypes[currentSetType] = setType
                currentPaletteId = paletteId
            }

            "set" -> {
                val id = attributes.getValue("id").toInt()
                val gender = attributes.getValue("gender") ?: "U"
                val club = attributes.getValue("club")?.toIntOrNull() ?: 0
                val colorable = attributes.getValue("colorable")?.let { it == "1" } ?: true
                val selectable = attributes.getValue("selectable")?.let { it == "1" } ?: true
                val preselectable = attributes.getValue("preselectable")?.let { it == "1" } ?: false
                val sellable = attributes.getValue("sellable")?.let { it == "1" } ?: false

                currentSetColors = 0
                currentSet = FigureSet(
                    id = id,
                    type = currentSetType,
                    paletteId = currentPaletteId,
                    colors = 0,
                    gender = gender,
                    club = club,
                    colorable = colorable,
                    selectable = selectable,
                    preselectable = preselectable,
                    sellable = sellable
                )
            }

            "part" -> {
                val color = attributes.getValue("colorindex")?.toIntOrNull() ?: 0
                if (color > currentSetColors) {
                    currentSetColors = color
                }
            }
        }
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        when (qName) {
            "color" -> {
                val hex = content.toString().trim()
                figurePalette[currentPaletteId]?.find { it.id == currentColorId }?.color = hex
            }

            "set" -> {
                val s = currentSet
                if (s != null) {
                    s.colors = currentSetColors
                    figureSets[s.id] = s
                    figureSetTypes[currentSetType]?.sets?.add(s)
                    currentSet = null
                }
            }
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        content.appendRange(ch, start, start + length)
    }
}