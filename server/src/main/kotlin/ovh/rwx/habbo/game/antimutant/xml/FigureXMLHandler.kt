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

package ovh.rwx.habbo.game.antimutant.xml

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import ovh.rwx.habbo.game.antimutant.FigureColor
import ovh.rwx.habbo.game.antimutant.FigureSet

class FigureXMLHandler : DefaultHandler() {
    val figureSets: MutableMap<Int, FigureSet> = mutableMapOf()
    val figurePalette: MutableMap<Int, MutableList<FigureColor>> = mutableMapOf()

    private var paletteId = 0
    private var colorId = 0
    private var setId = 0
    private var colors = 0
    private var content: String = ""

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        when (qName) {
            "palette" -> {
                paletteId = attributes.getValue("id").toInt()

                figurePalette[paletteId] = mutableListOf()
            }
            "color" -> {
                colorId = attributes.getValue("id").toInt()

                val figureColor = FigureColor()

                figureColor.id = colorId
                figureColor.clubOnly = attributes.getValue("club").toInt() == 2

                figurePalette[paletteId]!!.add(figureColor)
            }
            "settype" -> paletteId = attributes.getValue("paletteid").toInt()
            "set" -> {
                if (setId > 0) {
                    figureSets[setId]!!.colors = colors

                    colors = 0
                }

                setId = attributes.getValue("id").toInt()

                val figureSet = FigureSet()

                figureSet.paletteId = (paletteId)
                figureSet.gender = attributes.getValue("gender")

                figureSets[setId] = figureSet
            }
            "part" -> {
                val color = attributes.getValue("colorindex").toInt()

                if (color > colors) colors = color
            }
        }
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        when (qName) {
            "color" -> figurePalette[paletteId]!!.find { it.id == colorId }!!.color = content
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        content = String(ch, start, length)
    }
}