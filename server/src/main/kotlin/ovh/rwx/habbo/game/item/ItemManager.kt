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
import ovh.rwx.habbo.database.sequence.HiLoSequence
import ovh.rwx.habbo.game.item.logic.DefaultFurnitureLogic
import ovh.rwx.habbo.game.item.logic.FurnitureLogic
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.stuff.LegacyStuffData
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
import ovh.rwx.habbo.util.ClassIndex
import ovh.rwx.habbo.util.ReplacingInputStream
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.io.FileOutputStream
import java.lang.reflect.Constructor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.SAXParserFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.companionObject

class ItemManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val itemSequence = HiLoSequence("items", blockSize = 1000)
    val furniXMLInfos: ConcurrentHashMap<String, FurniXMLInfo> = ConcurrentHashMap()
    val furnishings: ConcurrentHashMap<String, Furnishing> = ConcurrentHashMap()
    @Volatile
    var oldGiftWrapper: List<Furnishing> = emptyList()
        private set
    @Volatile
    var newGiftWrapper: List<Furnishing> = emptyList()
        private set
    val teleportLinks: ConcurrentHashMap<Int, Int> = ConcurrentHashMap()
    val roomTeleportLinks: ConcurrentHashMap<Int, Int> = ConcurrentHashMap()
    val furniInteractor: ConcurrentHashMap<InteractionType, ItemInteractor> = ConcurrentHashMap()
    private val furniLogic: ConcurrentHashMap<InteractionType, FurnitureLogic> = ConcurrentHashMap()
    private val furniLogicByName: ConcurrentHashMap<String, FurnitureLogic> = ConcurrentHashMap()
    private val defaultFurnitureLogic: FurnitureLogic = DefaultFurnitureLogic()
    private val wiredItems: ConcurrentHashMap<InteractionType, Constructor<out WiredItem>> = ConcurrentHashMap()
    private val wiredDefaultData: ConcurrentHashMap<InteractionType, WiredDefaultDataFactory> = ConcurrentHashMap()

    /**
     * (Re)carrega os itens. TODO o estado novo (inclusive a instanciação dos interactors via [ClassIndex]) é construído em
     * coleções locais e só depois publicado nas coleções ativas (putAll + retainAll, ou troca de
     * referência @Volatile), então os leitores nunca veem estado vazio/parcial. Quem muta em runtime
     * (teleportLinks/roomTeleportLinks, testes) continua usando os mesmos ConcurrentHashMap.
     */
    @Synchronized
    fun load() {
        log.info("Loading furnishings...")

        // furnidata.xml relido a cada load (boot e reload) num mapa local; se o download/parse falhar, o
        // load lança antes de publicar qualquer coisa e o estado atual continua valendo
        val newXml = fetchFurniXml()

        var newFurnishings = ItemDao.getFurnishings(newXml).associateBy { it.itemName }
        val newTeleportLinks = ItemDao.getTeleportLinks().toMap()
        val newRoomTeleportLinks = ItemDao.getLinkedTeleport(newTeleportLinks.keys).toMap()

        val interactors: List<ItemInteractor> = ClassIndex.classesOf(ClassIndex.Kind.ITEM_INTERACTOR)
            .map { it.asSubclass(ItemInteractor::class.java).getConstructor().newInstance() }

        val furnitureLogics: List<FurnitureLogic> = ClassIndex.classesOf(ClassIndex.Kind.FURNITURE_LOGIC)
            .map { it.asSubclass(FurnitureLogic::class.java).getConstructor().newInstance() }

        val wiredItemsInteractor: List<Class<*>> = ClassIndex.classesOf(ClassIndex.Kind.WIRED_ITEM)

        val newInteractors = HashMap<InteractionType, ItemInteractor>()
        interactors.forEach { interactor ->
            interactor.interactionType.forEach { newInteractors[it] = interactor }
        }

        val newLogic = HashMap<InteractionType, FurnitureLogic>()
        val newLogicByName = HashMap<String, FurnitureLogic>()
        furnitureLogics.forEach { logic ->
            logic.interactionTypes.forEach { newLogic[it] = logic }
            logic.itemNames.forEach { newLogicByName[it] = logic }
        }

        val newWiredItems = HashMap<InteractionType, Constructor<out WiredItem>>()
        val newWiredDefaultData = HashMap<InteractionType, WiredDefaultDataFactory>()
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
                newWiredItems[it] =
                    (wiredItemClasses as Class<WiredItem>).getConstructor(Room::class.java, RoomItem::class.java)
                defaultDataFactory?.let { factory -> newWiredDefaultData[it] = factory }
            }
        }

        val missingItems = newXml.keys.minus(newFurnishings.keys).sorted()
        if (missingItems.isNotEmpty()) {
            recordMissingItems(missingItems)
            ItemDao.insertMissingFurnishings(newXml, missingItems)
            log.info("Added more {} items to database!", newXml.size - newFurnishings.size)

            newFurnishings = ItemDao.getFurnishings(newXml).associateBy { it.itemName }
        }

        // ---- publicação: nenhum mapa ativo é esvaziado; tudo já está pronto ----
        furniXMLInfos.putAll(newXml)
        furniXMLInfos.keys.retainAll(newXml.keys)
        furnishings.putAll(newFurnishings)
        newInteractors.forEach { (k, v) -> furniInteractor[k] = v }
        furniInteractor.keys.retainAll(newInteractors.keys)
        furniLogic.putAll(newLogic)
        furniLogic.keys.retainAll(newLogic.keys)
        furniLogicByName.putAll(newLogicByName)
        furniLogicByName.keys.retainAll(newLogicByName.keys)
        wiredItems.putAll(newWiredItems)
        wiredItems.keys.retainAll(newWiredItems.keys)
        wiredDefaultData.putAll(newWiredDefaultData)
        teleportLinks.putAll(newTeleportLinks)
        teleportLinks.keys.retainAll(newTeleportLinks.keys)
        roomTeleportLinks.putAll(newRoomTeleportLinks)
        roomTeleportLinks.keys.retainAll(newRoomTeleportLinks.keys)
        oldGiftWrapper = newFurnishings.filterKeys { it.startsWith("present_gen") }.values.toList()
        newGiftWrapper = newFurnishings.filterKeys { it.startsWith("present_wrap*") }.values.toList()

        log.info("Loaded {} furnishings from XML!", furniXMLInfos.size)
        log.info("Loaded {} furnishings!", furnishings.size)
        log.info("Loaded {} teleport links!", teleportLinks.size / 2)
        log.info("Loaded {} item interactors!", furniInteractor.size)
        log.info("Loaded {} item furniture logics!", furnitureLogics.size)
        log.info("Loaded {} wired interactors!", wiredItemsInteractor.size)
    }

    private fun fetchFurniXml(): Map<String, FurniXMLInfo> =
        urlUserAgent(HabboServer.habboConfig.furnidataXml).inputStream.use { rawStream ->
            var stream = rawStream
            for (replacement in XML_SANITIZATION_REPLACEMENTS) {
                stream = ReplacingInputStream(stream, replacement, "")
            }
            stream.buffered().use { bufferedStream ->
                val saxParser = SAXParserFactory.newInstance().newSAXParser()
                val handler = FurniXMLHandler()
                saxParser.parse(bufferedStream, handler)
                handler.furniXMLInfos.associateBy { it.itemName }
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
            val legacy = getFurnitureLogic(furnishing)
                .parseStuffData(extraData, furnishing, limitedItemData, magicRemove)
                .legacyValue

            if (habboResponse.isVersionAtLeast(2011, 9, 8)) {
                // A partir de RELEASE63-35846 (08/09/2011) o cliente lê um StuffData tipado: o "extra" (só no objeto
                // do quarto; no cliente é o id da música de discos de jukebox, 0 para o resto), o tipo (0 = legado:
                // o dado é só uma string) e a string. É o LegacyStuffData que o WIN63 já usa.
                LegacyStuffData(legacy).write(habboResponse, inventory)
            } else {
                // Até 02/09/2011: o "extra" (só no quarto) e a string, sem tipo
                if (!inventory) habboResponse.writeInt(0)
                habboResponse.writeUTF(legacy)
            }
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
