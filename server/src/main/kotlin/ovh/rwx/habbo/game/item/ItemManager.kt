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
import io.github.classgraph.ClassGraph
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.sequence.HiLoSequence
import ovh.rwx.habbo.game.item.logic.DefaultFurnitureLogic
import ovh.rwx.habbo.game.item.logic.FurnitureLogic
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.stuff.LimitedStuffData
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.item.wired.WiredDefaultDataFactory
import ovh.rwx.habbo.game.item.wired.WiredItem
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import kotlin.reflect.full.companionObjectInstance
import ovh.rwx.habbo.game.item.xml.FurniXMLHandler
import ovh.rwx.habbo.game.item.xml.FurniXMLInfo
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.kotlin.urlUserAgent
import ovh.rwx.habbo.util.ReplacingInputStream
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.io.FileOutputStream
import java.lang.reflect.Constructor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.SAXParserFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.full.companionObject

class ItemManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val itemSequence = HiLoSequence("items", blockSize = 1000)
    val furniXMLInfos: ConcurrentHashMap<String, FurniXMLInfo> = ConcurrentHashMap()
    val furnishings: ConcurrentHashMap<String, Furnishing> = ConcurrentHashMap()
    val oldGiftWrapper: MutableList<Furnishing> = CopyOnWriteArrayList()
    val newGiftWrapper: MutableList<Furnishing> = CopyOnWriteArrayList()
    val teleportLinks: ConcurrentHashMap<Int, Int> = ConcurrentHashMap()
    val roomTeleportLinks: ConcurrentHashMap<Int, Int> = ConcurrentHashMap()
    val furniInteractor: ConcurrentHashMap<InteractionType, ItemInteractor> = ConcurrentHashMap()
    private val furniLogic: ConcurrentHashMap<InteractionType, FurnitureLogic> = ConcurrentHashMap()
    private val furniLogicByName: ConcurrentHashMap<String, FurnitureLogic> = ConcurrentHashMap()
    private val defaultFurnitureLogic: FurnitureLogic = DefaultFurnitureLogic()
    private val wiredItems: ConcurrentHashMap<InteractionType, Constructor<out WiredItem>> = ConcurrentHashMap()
    private val wiredDefaultData: ConcurrentHashMap<InteractionType, WiredDefaultDataFactory> = ConcurrentHashMap()

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

        loadFurniXml()

        furnishings += ItemDao.getFurnishings(furniXMLInfos).associateBy { it.itemName }
        oldGiftWrapper += furnishings.filterKeys { it.startsWith("present_gen") }.values
        newGiftWrapper += furnishings.filterKeys { it.startsWith("present_wrap*") }.values
        teleportLinks += ItemDao.getTeleportLinks()

        roomTeleportLinks.putAll(ItemDao.getLinkedTeleport(teleportLinks.keys))

        val interactors: List<ItemInteractor>
        val furnitureLogics: List<FurnitureLogic>
        val wiredItemsInteractor: List<Class<*>>

        ClassGraph().enableAllInfo().acceptPackages("ovh.rwx.habbo").scan().use { scanResult ->
            interactors = scanResult.getSubclasses(ItemInteractor::class.java).loadClasses()
                .map { it.asSubclass(ItemInteractor::class.java).getConstructor().newInstance() }

            furnitureLogics = scanResult.getSubclasses(FurnitureLogic::class.java).loadClasses()
                .map { it.asSubclass(FurnitureLogic::class.java).getConstructor().newInstance() }

            wiredItemsInteractor = scanResult.getClassesWithAnnotation(WiredItemInteractor::class.java).loadClasses()
        }

        interactors.forEach { interactor ->
            interactor.interactionType.forEach { furniInteractor[it] = interactor }
        }

        furnitureLogics.forEach { logic ->
            logic.interactionTypes.forEach { furniLogic[it] = logic }
            logic.itemNames.forEach { furniLogicByName[it] = logic }
        }

        wiredItemsInteractor.forEach { wiredItemClasses ->
            val annotation = wiredItemClasses.getAnnotation(WiredItemInteractor::class.java)

            // Registro tipado do WiredData default: resolvido UMA vez no load — companion
            // sem factory é warn de boot, não null silencioso em runtime
            val defaultDataFactory = wiredItemClasses.kotlin.companionObjectInstance as? WiredDefaultDataFactory
            if (defaultDataFactory == null) {
                log.warn("Wired {} sem factory de WiredData default (companion não implementa WiredDefaultDataFactory)", wiredItemClasses.simpleName)
            }

            annotation.interactionType.forEach {
                @Suppress("UNCHECKED_CAST")
                wiredItems[it] =
                    (wiredItemClasses as Class<WiredItem>).getConstructor(Room::class.java, RoomItem::class.java)
                defaultDataFactory?.let { factory -> wiredDefaultData[it] = factory }
            }
        }

        val missingItems = furniXMLInfos.keys.minus(furnishings.keys).sorted()
        if (missingItems.isNotEmpty()) {
            recordMissingItems(missingItems)
            insertMissingItems(missingItems)
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

    private fun loadFurniXml() {
        if (furniXMLInfos.isNotEmpty()) return

        urlUserAgent(HabboServer.habboConfig.furnidataXml).inputStream.use { rawStream ->
            var stream = rawStream
            for (replacement in XML_SANITIZATION_REPLACEMENTS) {
                stream = ReplacingInputStream(stream, replacement, "")
            }
            stream.buffered().use { bufferedStream ->
                val saxParser = SAXParserFactory.newInstance().newSAXParser()
                val handler = FurniXMLHandler()
                saxParser.parse(bufferedStream, handler)
                furniXMLInfos += handler.furniXMLInfos.associateBy { it.itemName }
            }
        }
    }

    private fun recordMissingItems(missingItems: List<String>) {
        FileOutputStream("MISSING_ITEMS.txt", true).bufferedWriter().use { writer ->
            writer.appendLine()
            writer.appendLine("================")
            writer.appendLine(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            writer.appendLine()
            missingItems.forEach { writer.appendLine(it) }
            writer.appendLine()
            writer.appendLine("================")
            writer.flush()
        }
    }

    private fun insertMissingItems(missingItems: List<String>) {
        ItemDao.insertMissingFurnishings(furniXMLInfos, missingItems)
    }

    fun getAffectedTiles(x: Int, y: Int, rotation: Int, width: Int, length: Int): List<Vector2> {
        val list = ArrayList<Vector2>(width * length)

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

    fun getWiredDefaultData(interactionType: InteractionType): WiredData? =
        wiredDefaultData[interactionType]?.getDefaultWiredData()

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

    companion object {
        private val XML_SANITIZATION_REPLACEMENTS = listOf("&#25;", "&#28;", "&#29;")
    }
}
