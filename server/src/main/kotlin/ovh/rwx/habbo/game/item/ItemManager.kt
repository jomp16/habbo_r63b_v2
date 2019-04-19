/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.item

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.item.wired.WiredItem
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.xml.FurniXMLHandler
import ovh.rwx.habbo.game.item.xml.FurniXMLInfo
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.batchInsertAndGetGeneratedKeys
import ovh.rwx.habbo.kotlin.urlUserAgent
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.io.FileOutputStream
import java.lang.reflect.Constructor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.SAXParserFactory

class ItemManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val furniXMLInfos: MutableMap<String, FurniXMLInfo> = mutableMapOf()
    val furnishings: MutableMap<String, Furnishing> = mutableMapOf()
    val oldGiftWrapper: MutableList<Furnishing> = mutableListOf()
    val newGiftWrapper: MutableList<Furnishing> = mutableListOf()
    val teleportLinks: MutableMap<Int, Int> = mutableMapOf()
    val roomTeleportLinks: MutableMap<Int, Int> = mutableMapOf()
    val furniInteractor: MutableMap<InteractionType, ItemInteractor> = mutableMapOf()
    private val wiredItems: MutableMap<InteractionType, Constructor<out WiredItem>> = mutableMapOf()

    fun load() {
        log.info("Loading furnishings...")

        oldGiftWrapper.clear()
        newGiftWrapper.clear()
        furniInteractor.clear()
        teleportLinks.clear()
        roomTeleportLinks.clear()
        wiredItems.clear()

        if (furniXMLInfos.isEmpty()) {
            urlUserAgent(HabboServer.habboConfig.furnidataXml).inputStream.buffered().use {
                val saxParser = SAXParserFactory.newInstance().newSAXParser()
                val handler = FurniXMLHandler()

                saxParser.parse(it, handler)

                furniXMLInfos += handler.furniXMLInfos.associateBy { furniXMLInfo -> furniXMLInfo.itemName }
            }
        }

        furnishings += ItemDao.getFurnishings(furniXMLInfos).associateBy { it.itemName }
        oldGiftWrapper += furnishings.filterKeys { it.startsWith("present_gen") }.values
        newGiftWrapper += furnishings.filterKeys { it.startsWith("present_wrap*") }.values
        teleportLinks += ItemDao.getTeleportLinks()

        roomTeleportLinks.putAll(ItemDao.getLinkedTeleport(teleportLinks.keys))

        val interactors = HabboServer.reflections.getSubTypesOf(ItemInteractor::class.java)

        interactors.map { it.getConstructor().newInstance() }.forEach { interactor ->
            interactor.interactionType.forEach { furniInteractor[it] = interactor }
        }

        val wiredItemsInteractor = HabboServer.reflections.getTypesAnnotatedWith(WiredItemInteractor::class.java)

        wiredItemsInteractor.forEach { wiredItemClasses ->
            val wiredItemInteractor = wiredItemClasses.getAnnotation(WiredItemInteractor::class.java)

            wiredItemInteractor.interactionType.forEach {
                @Suppress("UNCHECKED_CAST")
                wiredItems[it] = (wiredItemClasses as Class<WiredItem>).getConstructor(Room::class.java, RoomItem::class.java)
            }
        }

        val missingItems = furniXMLInfos.keys.minus(furnishings.keys).sorted()

        if (missingItems.isNotEmpty()) {
            FileOutputStream("MISSING_ITEMS.txt", true).bufferedWriter().use {
                it.apply {
                    appendln()
                    appendln("================")
                    appendln(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    appendln()

                    missingItems.forEach { s ->
                        appendln(s)
                    }

                    appendln()
                    appendln("================")
                }

                it.flush()
            }

            HabboServer.database {
                batchInsertAndGetGeneratedKeys(javaClass.classLoader.getResource("sql/furnishings/insert_furnishings.sql").readText().trim(),
                        missingItems.map {
                            mapOf(
                                    "item_name" to it,
                                    "type" to if (!furniXMLInfos[it]!!.wallFurni) "s" else "i",
                                    "stack_height" to "1",
                                    "can_stack" to true,
                                    "allow_recycle" to true,
                                    "allow_trade" to true,
                                    "allow_marketplace_sell" to true,
                                    "allow_gift" to true,
                                    "allow_inventory_stack" to true,
                                    "interaction_type" to "default",
                                    "interaction_modes_count" to 1,
                                    "vending_ids" to "0"
                            )
                        }
                )
            }

            log.info("Added more {} items to database!", furniXMLInfos.size - furnishings.size)

            furnishings.clear()
            furnishings += ItemDao.getFurnishings(furniXMLInfos).associateBy { it.itemName }
        }

        log.info("Loaded {} furnishings from XML!", furniXMLInfos.size)
        log.info("Loaded {} furnishings!", furnishings.size)
        log.info("Loaded {} teleport links!", teleportLinks.size / 2)
        log.info("Loaded {} item interactors!", furniInteractor.size)
        log.info("Loaded {} wired interactors!", wiredItemsInteractor.size)
    }

    fun getAffectedTiles(x: Int, y: Int, rotation: Int, width: Int, height: Int): List<Vector2> {
        val list: MutableList<Vector2> = mutableListOf()

        for (i in 0 until width) {
            val x1 = if (rotation == 0 || rotation == 4) x + i else x
            val y1 = if (rotation == 2 || rotation == 6) y + i else y

            for (j in 0 until height) {
                val xb = if (rotation == 2 || rotation == 6) x1 + j else x1
                val xn = if (rotation == 0 || rotation == 4) y1 + j else y1

                list += Vector2(xb, xn)
            }
        }

        return list
    }

    fun getRoomItemFromUserItem(roomId: Int, userItem: UserItem): RoomItem = RoomItem(userItem.id, userItem.userId, roomId, userItem.itemName, userItem.extraData, Vector3(0, 0, 0.toDouble()), 0, "", userItem.limited)

    fun getWiredInstance(room: Room, roomItem: RoomItem): WiredItem? = wiredItems[roomItem.furnishing.interactionType]?.newInstance(room, roomItem)

    // todo: see if I can improve it
    fun writeExtradata(habboResponse: HabboResponse, extraData: String, furnishing: Furnishing, limitedItemData: LimitedItemData?, magicRemove: Boolean = false) {
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
                InteractionType.MANNEQUIN -> {
                    val splitData = extraData.split(7.toChar())

                    writeInt(0)
                    writeInt(1)
                    writeInt(3)
                    writeUTF("GENDER")
                    writeUTF(splitData[0])
                    writeUTF("FIGURE")
                    writeUTF(splitData[1])
                    writeUTF("OUTFIT_NAME")
                    writeUTF(splitData[2])
                }
                InteractionType.GIFT -> {
                    val split = extraData.split(7.toChar())

                    writeInt(split[2].toInt() * 1000 + split[3].toInt())
                    writeInt(1)
                    writeInt(if (split[4].toBoolean()) 6 else 4)
                    writeUTF("EXTRA_PARAM")
                    writeUTF("")
                    writeUTF("MESSAGE")
                    writeUTF(split[1])

                    if (split[4].toBoolean()) {
                        writeUTF("PURCHASER_NAME")
                        writeUTF(split[5])
                        writeUTF("PURCHASER_FIGURE")
                        writeUTF(split[6])
                    }

                    writeUTF("PRODUCT_CODE")
                    writeUTF(split[7])
                    writeUTF("state")
                    writeUTF(if (magicRemove) "1" else "0")
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

                "${extraData.trim()}${7.toChar()}${habboSession.userInformation.username}${7.toChar()}${LocalDateTime.now().format(HabboServer.DATE_TIME_FORMATTER_ONLY_DAYS)}"
            }
            InteractionType.TROPHY -> "${habboSession.userInformation.username}${9.toChar()}${LocalDateTime.now().format(HabboServer.DATE_TIME_FORMATTER_ONLY_DAYS)}${9.toChar()}${extraData.trim()}"
            else -> ""
        }
    }
}