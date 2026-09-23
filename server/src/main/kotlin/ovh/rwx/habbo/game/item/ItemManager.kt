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

package ovh.rwx.habbo.game.item

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.logic.DefaultFurnitureLogic
import ovh.rwx.habbo.game.item.logic.FurnitureLogic
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.stuff.LimitedStuffData
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.item.wired.WiredItem
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.xml.FurniXMLHandler
import ovh.rwx.habbo.game.item.xml.FurniXMLInfo
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.batchInsertAndGetGeneratedKeys
import ovh.rwx.habbo.kotlin.urlUserAgent
import ovh.rwx.habbo.util.ReplacingInputStream
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.io.FileOutputStream
import java.lang.reflect.Constructor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.SAXParserFactory
import kotlin.reflect.full.companionObject

class ItemManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val furniXMLInfos: MutableMap<String, FurniXMLInfo> = mutableMapOf()
    val furnishings: MutableMap<String, Furnishing> = mutableMapOf()
    val oldGiftWrapper: MutableList<Furnishing> = mutableListOf()
    val newGiftWrapper: MutableList<Furnishing> = mutableListOf()
    val teleportLinks: MutableMap<Int, Int> = mutableMapOf()
    val roomTeleportLinks: MutableMap<Int, Int> = mutableMapOf()
    val furniInteractor: MutableMap<InteractionType, ItemInteractor> = mutableMapOf()
    private val furniLogic: MutableMap<InteractionType, FurnitureLogic> = mutableMapOf()
    private val furniLogicByName: MutableMap<String, FurnitureLogic> = mutableMapOf()
    private val defaultFurnitureLogic: FurnitureLogic = DefaultFurnitureLogic()
    private val wiredItems: MutableMap<InteractionType, Constructor<out WiredItem>> = mutableMapOf()

    fun load() {
        log.info("Loading furnishings...")

        oldGiftWrapper.clear()
        newGiftWrapper.clear()
        furniInteractor.clear()
        furniLogic.clear()
        furniLogicByName.clear()
        teleportLinks.clear()
        roomTeleportLinks.clear()
        wiredItems.clear()

        if (furniXMLInfos.isEmpty()) {
            urlUserAgent(HabboServer.habboConfig.furnidataXml).inputStream.use { inputStream ->
                ReplacingInputStream(inputStream, "&#25;", "").use { replacingFileInputOne ->
                    ReplacingInputStream(replacingFileInputOne, "&#28;", "").use { replacingFileInputTwo ->
                        ReplacingInputStream(replacingFileInputTwo, "&#29;", "").use { replacingFileInputFinal ->
                            replacingFileInputFinal.buffered().use { bufferedInputStream ->
                                val saxParser = SAXParserFactory.newInstance().newSAXParser()
                                val handler = FurniXMLHandler()

                                saxParser.parse(bufferedInputStream, handler)

                                furniXMLInfos += handler.furniXMLInfos.associateBy { furniXMLInfo -> furniXMLInfo.itemName }
                            }
                        }
                    }
                }
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

        val furnitureLogics = HabboServer.reflections.getSubTypesOf(FurnitureLogic::class.java)

        furnitureLogics.map { it.getConstructor().newInstance() }.forEach { logic ->
            logic.interactionTypes.forEach { furniLogic[it] = logic }
            logic.itemNames.forEach { furniLogicByName[it] = logic }
        }

        val wiredItemsInteractor = HabboServer.reflections.getTypesAnnotatedWith(WiredItemInteractor::class.java)

        wiredItemsInteractor.forEach { wiredItemClasses ->
            val wiredItemInteractor = wiredItemClasses.getAnnotation(WiredItemInteractor::class.java)

            wiredItemInteractor.interactionType.forEach {
                @Suppress("UNCHECKED_CAST")
                wiredItems[it] =
                    (wiredItemClasses as Class<WiredItem>).getConstructor(Room::class.java, RoomItem::class.java)
            }
        }

        val missingItems = furniXMLInfos.keys.minus(furnishings.keys).sorted()

        if (missingItems.isNotEmpty()) {
            FileOutputStream("MISSING_ITEMS.txt", true).bufferedWriter().use {
                it.apply {
                    appendLine()
                    appendLine("================")
                    appendLine(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    appendLine()

                    missingItems.forEach { s ->
                        appendLine(s)
                    }

                    appendLine()
                    appendLine("================")
                }

                it.flush()
            }

            HabboServer.database {
                batchInsertAndGetGeneratedKeys(
                    javaClass.classLoader.getResource("sql/furnishings/insert_furnishings.sql").readText().trim(),
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
                            "interaction_type" to (InteractionType.fromString(it).let { t ->
                                if (t != InteractionType.NOT_FOUND && t != InteractionType.DEFAULT) t.type else "default"
                            }),
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
        log.info("Loaded {} item furniture logics!", furnitureLogics.size)
        log.info("Loaded {} wired interactors!", wiredItemsInteractor.size)
    }

    fun getAffectedTiles(x: Int, y: Int, rotation: Int, width: Int, length: Int): List<Vector2> {
        val list: MutableList<Vector2> = mutableListOf()

        for (i in 0 until width) {
            val x1 = if (rotation == 0 || rotation == 4) x + i else x
            val y1 = if (rotation == 2 || rotation == 6) y + i else y

            for (j in 0 until length) {
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
        userItem.itemName,
        userItem.extraData,
        Vector3(0, 0, 0.toDouble()),
        0,
        "",
        userItem.limited,
        userItem.buildersClub
    )

    fun getWiredInstance(room: Room, roomItem: RoomItem): WiredItem? =
        wiredItems[roomItem.furnishing.interactionType]?.newInstance(room, roomItem)

    fun getWiredDefaultData(interactionType: InteractionType): WiredData? {
        return try {
            val wiredClass = wiredItems[interactionType]?.declaringClass
            val companionObject = wiredClass?.kotlin?.companionObject
            val companionInstance = companionObject?.objectInstance
            val getDefaultWiredDataMethod = companionObject?.members?.find { it.name == "getDefaultWiredData" }
            getDefaultWiredDataMethod?.call(companionInstance) as? WiredData
        } catch (_: Exception) {
            null
        }
    }

    fun getFurnitureLogic(furnishing: Furnishing): FurnitureLogic =
        furniLogicByName[furnishing.itemName]
            ?: furniLogic[furnishing.interactionType]
            ?: defaultFurnitureLogic

    fun writeExtradata(
        habboResponse: HabboResponse,
        extraData: String,
        furnishing: Furnishing,
        limitedItemData: LimitedItemData?,
        magicRemove: Boolean = false,
        inventory: Boolean = false,
    ) {
        if (habboResponse.outgoingR63A != null || !habboResponse.isVersionAtLeast(2012, 1, 13)) {
            if (!inventory) {
                habboResponse.writeInt(0)
            }
            val legacy = getFurnitureLogic(furnishing)
                .parseStuffData(extraData, furnishing, limitedItemData, magicRemove)
                .legacyValue
            habboResponse.writeUTF(legacy)
            return
        }

        if (!inventory && limitedItemData != null) {
            LimitedStuffData(extraData, limitedItemData.limitedNumber, limitedItemData.limitedTotal)
                .write(habboResponse, inventory = false)

            return
        }

        getFurnitureLogic(furnishing)
            .parseStuffData(extraData, furnishing, limitedItemData, magicRemove)
            .write(habboResponse, inventory)
    }

    fun correctExtradataCatalog(habboSession: HabboSession, extraData: String, furnishing: Furnishing): String? {
        return getFurnitureLogic(furnishing).correctCatalogExtraData(habboSession, extraData, furnishing)
    }
}