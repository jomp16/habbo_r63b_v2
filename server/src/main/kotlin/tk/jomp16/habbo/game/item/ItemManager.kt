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

package tk.jomp16.habbo.game.item

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.database.item.ItemDao
import tk.jomp16.habbo.game.item.interactors.DefaultItemInteractor
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.user.UserItem
import tk.jomp16.habbo.game.item.xml.FurniXMLHandler
import tk.jomp16.habbo.game.item.xml.FurniXMLInfo
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.kotlin.urlUserAgent
import tk.jomp16.habbo.util.Vector2
import tk.jomp16.habbo.util.Vector3
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.parsers.SAXParserFactory

class ItemManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val furniXMLInfos: MutableMap<String, FurniXMLInfo> = HashMap()

    val furnishings: MutableMap<String, Furnishing> = HashMap()
    val oldGiftWrapper: MutableList<Furnishing> = mutableListOf()
    val newGiftWrapper: MutableList<Furnishing> = mutableListOf()
    val furniInteractor: MutableMap<InteractionType, ItemInteractor> = HashMap()

    init {
        log.info("Loading furnishings...")

        urlUserAgent(HabboServer.habboConfig.furnidataXml).inputStream.buffered().use {
            val saxParser = SAXParserFactory.newInstance().newSAXParser()
            val handler = FurniXMLHandler()

            saxParser.parse(it, handler)

            furniXMLInfos += handler.furniXMLInfos.associateBy { it.itemName }
        }

        furnishings += ItemDao.getFurnishings(furniXMLInfos).associateBy { it.itemName }

        oldGiftWrapper += furnishings.filterKeys { it.startsWith("present_gen") }.values
        newGiftWrapper += furnishings.filterKeys { it.startsWith("present_wrap*") }.values

        furniInteractor.put(InteractionType.DEFAULT, DefaultItemInteractor())

        log.info("Loaded {} furnishings from XML!", furniXMLInfos.size)
        log.info("Loaded {} furnishings!", furnishings.size)
        log.info("Loaded {} item interactors!", furniInteractor.size)
    }

    fun getAffectedTiles(x: Int, y: Int, rotation: Int, width: Int, height: Int): List<Vector2> {
        val list: MutableList<Vector2> = mutableListOf()

        for (i in 0..width - 1) {
            val x1 = if (rotation == 0 || rotation == 4) x + i else x
            val y1 = if (rotation == 2 || rotation == 6) y + i else y

            for (j in 0..height - 1) {
                val xb = if (rotation == 2 || rotation == 6) x1 + j else x1
                val xn = if (rotation == 0 || rotation == 4) y1 + j else y1

                list += Vector2(xb, xn)
            }
        }

        return list
    }

    fun getRoomItemFromUserItem(roomId: Int, userItem: UserItem): RoomItem = RoomItem(
            userItem.id,
            userItem.userId,
            roomId,
            userItem.baseItem,
            userItem.extraData,
            userItem.limitedId,
            Vector3(0, 0, 0.toDouble()),
            0,
            "",
            userItem.limitedItemData
    )

    // todo: see if I can improve it
    fun writeExtradata(habboResponse: HabboResponse, extraData: String, furnishing: Furnishing, limitedItemData: LimitedItemData?) {
        habboResponse.apply {
            if (limitedItemData != null) {
                writeInt(1)
                writeInt(256)
                writeUTF(extraData)
                writeInt(limitedItemData.limitedNumber)
                writeInt(limitedItemData.limitedTotal)

                return
            }

            if (furnishing.itemName == "wallpaper" || furnishing.itemName == "floor" || furnishing.itemName == "landscape") {
                when (furnishing.itemName) {
                    "wallpaper" -> writeInt(2)
                    "floor" -> writeInt(3)
                    "landscape" -> writeInt(4)
                }

                writeInt(0)
                writeUTF(extraData)

                return
            }

            when (furnishing.interactionType) {
                InteractionType.BADGE_DISPLAY -> {
                    val splitData = extraData.split(7.toChar())

                    writeInt(0)
                    writeInt(2)
                    writeInt(4)
                    writeUTF("0")
                    writeUTF(splitData[0]) // badge name
                    writeUTF(splitData[1]) // owner
                    writeUTF(splitData[2]) // date
                }
                else -> {
                    writeInt(1)
                    writeInt(0)
                    writeUTF(extraData)
                }
            }
        }
    }

    fun correctExtradataCatalog(habboSession: HabboSession, extraData: String, furnishing: Furnishing): String? {
        return when (furnishing.interactionType) {
            InteractionType.POST_IT -> "FFFF33"
            InteractionType.ROOM_EFFECT -> if (extraData.isEmpty()) "0" else extraData.trim()
            InteractionType.DIMMER -> "1,1,1,#000000,255"
            InteractionType.MANNEQUIN -> "m${7.toChar()}ch-215-92.lg-3202-1322-73${7.toChar()}Mannequin"
            InteractionType.BADGE_DISPLAY -> {
                if (!habboSession.habboBadge.badges.containsKey(extraData)) return null

                "${extraData.trim()}${7.toChar()}${habboSession.userInformation.username}${7.toChar()}${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
            }
            InteractionType.TROPHY -> "${habboSession.userInformation.username}${9.toChar()}${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}${9.toChar()}${extraData.trim()}"
            else -> ""
        }
    }
}