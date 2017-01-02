/*
 * Copyright (C) 2017 jomp16
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

package tk.jomp16.habbo.game.item.xml

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.*

class FurniXMLHandler : DefaultHandler() {
    val furniXMLInfos: MutableList<FurniXMLInfo> = ArrayList()

    private var wallFurni: Boolean = false
    private var furniXMLInfo: FurniXMLInfo? = null
    private var content: String = ""

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        when (qName) {
            "roomitemtypes" -> wallFurni = false
            "wallitemtypes" -> wallFurni = true
            "furnitype" -> {
                furniXMLInfo = FurniXMLInfo()

                furniXMLInfo?.spriteId = attributes.getValue("id").toInt()
                furniXMLInfo?.itemName = attributes.getValue("classname")
            }
        }
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        when (qName) {
            "furnitype" -> furniXMLInfo?.let { furniXMLInfos += it }
            "defaultdir" -> furniXMLInfo?.defaultDir = content.trim().toInt()
            "xdim" -> furniXMLInfo?.xDim = content.trim().toInt()
            "ydim" -> furniXMLInfo?.yDim = content.trim().toInt()
            "canstandon" -> furniXMLInfo?.canStandOn = content.trim() == "1"
            "cansiton" -> furniXMLInfo?.canSitOn = content.trim() == "1"
            "canlayon" -> furniXMLInfo?.canLayOn = content.trim() == "1"
            "customparams" -> furniXMLInfo?.customParams = content.trim()
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        content = String(ch, start, length)
    }
}