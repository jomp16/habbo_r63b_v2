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

package ovh.rwx.habbo.database.item

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.writebehind.WriteKey
import ovh.rwx.habbo.game.catalog.gift.GiftContentsStore
import ovh.rwx.habbo.game.item.*
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.user.NewItemSpec
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.item.user.planNewItems
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredUserSource
import ovh.rwx.habbo.game.item.xml.FurniXMLInfo
import ovh.rwx.habbo.game.room.dimmer.RoomDimmer
import ovh.rwx.habbo.kotlin.toIntList
import ovh.rwx.habbo.util.Vector3

object ItemDao {
    private val log = LoggerFactory.getLogger(javaClass)

    // cache in memory all the information about items fam
    private val limitedItemDatas: MutableMap<Int, LimitedItemData?> = mutableMapOf()
    private val wiredDatas: MutableMap<Int, WiredData> = java.util.concurrent.ConcurrentHashMap()
    private val selectWiredDataSql: String by lazy {
        javaClass.classLoader.getResource("sql/items/wired/select_wired_data.sql")!!.readText()
    }
    private val roomDimmers: MutableMap<Int, RoomDimmer?> = mutableMapOf()

    fun getFurnishings(furniXMLInfos: Map<String, FurniXMLInfo>): List<Furnishing> = db {
        query<FurnishingRowDto>(javaClass.classLoader.getResource("sql/furnishings/select_furnishings.sql").readText())
            .map { it.toDomain(furniXMLInfos) }
    }

    /**
     * Boot: insere mobílias presentes no furnidata e ausentes do banco (sync de catálogo no start).
     * Só pode rodar no boot — em runtime use o auto-enfileiramento do WriteBehindManager.
     */
    fun insertMissingFurnishings(furniXMLInfos: Map<String, FurniXMLInfo>, missingItems: List<String>) {
        val insertSql = javaClass.classLoader.getResource("sql/furnishings/insert_furnishings.sql")?.readText()?.trim()
            ?: return
        db {
            batchInsertAndGetGeneratedKeys(
                insertSql,
                missingItems.map { itemName ->
                    val xmlInfo = furniXMLInfos[itemName]
                    val isWall = xmlInfo?.wallFurni == true
                    val interactionType = InteractionType.fromString(itemName).let { t ->
                        if (t != InteractionType.NOT_FOUND && t != InteractionType.DEFAULT) t.type else "default"
                    }
                    mapOf(
                        "item_name" to itemName,
                        "type" to if (!isWall) "s" else "i",
                        "stack_height" to "1",
                        "can_stack" to true,
                        "allow_recycle" to true,
                        "allow_trade" to true,
                        "allow_marketplace_sell" to true,
                        "allow_gift" to true,
                        "allow_inventory_stack" to true,
                        "interaction_type" to interactionType,
                        "interaction_modes_count" to 1,
                        "vending_ids" to "0"
                    )
                }
            )
        }
    }

    fun getRoomItems(roomId: Int): Map<Int, RoomItem> = db {
        query<RoomItemRowDto>(
            javaClass.classLoader.getResource("sql/items/room/select_room_items.sql").readText(),
            mapOf("room_id" to roomId)
        ).associateBy({ it.id }, { it.toDomain() })
    }

    fun getUserItems(userId: Int): Map<Int, UserItem> = db {
        query<UserItemRowDto>(
            javaClass.classLoader.getResource("sql/items/user/select_user_items.sql").readText(),
            mapOf("user_id" to userId)
        ).associateBy({ it.id }, { it.toDomain() })
    }

    fun getAllUserItems(userId: Int): List<UserItem> = db {
        query<UserItemRowDto>(
            javaClass.classLoader.getResource("sql/items/user/select_all_user_items.sql").readText(),
            mapOf("user_id" to userId)
        ).map { it.toDomain() }
    }

    data class UserItemWithRoom(val userItem: UserItem, val roomId: Int?)

    fun getAllUserItemsWithRoom(userId: Int): List<UserItemWithRoom> = db {
        query<UserItemRowDto>(
            javaClass.classLoader.getResource("sql/items/user/select_all_user_items.sql").readText(),
            mapOf("user_id" to userId)
        ).map { UserItemWithRoom(it.toDomain(), it.roomId) }
    }

    fun getLimitedData(itemId: Int): LimitedItemData? {
        if (!limitedItemDatas.containsKey(itemId)) {
            val limitedItemData = db {
                queryOne<LimitedItemData>(
                    "sql/items/limited/select_limited.sql",
                    mapOf("item_id" to itemId)
                )
            }

            limitedItemDatas[itemId] = limitedItemData
        }

        return limitedItemDatas[itemId]
    }

    /**
     * Carrega em UMA query o wired_data de todos os itens informados, populando o cache.
     * Evita o N+1 do `RoomItem.wiredData` (lazy) sendo resolvido na thread do quarto
     * no primeiro acionamento de cada wired.
     */
    fun preloadWiredData(itemIds: Collection<Int>) {
        if (itemIds.isEmpty()) return
        val idsString = itemIds.joinToString(",")
        db {
            query<WiredRowDto>("SELECT * FROM `items_wired` WHERE `item_id` IN ($idsString)")
        }.forEach { wiredDatas[it.itemId] = it.toDomain() }
    }

    fun getWiredData(itemId: Int): WiredData? {
        if (!wiredDatas.containsKey(itemId)) {
            val wiredData = db {
                queryOne<WiredRowDto>(
                    selectWiredDataSql,
                    mapOf("item_id" to itemId)
                )?.toDomain()
            }

            if (wiredData != null) {
                wiredDatas[itemId] = wiredData
            }
        }

        return wiredDatas[itemId]
    }

    fun getTeleportLinks(): List<Pair<Int, Int>> = db {
        query<TeleportLinkDto>(javaClass.classLoader.getResource("sql/items/teleport/select_teleport_links.sql").readText())
            .map { it.teleportOneId to it.teleportTwoId }
    }

    fun getLinkedTeleport(teleportIds: Set<Int>): List<Pair<Int, Int>> = db {
        if (teleportIds.isEmpty()) return@db emptyList()
        query<LinkedTeleportDto>(
            "sql/items/teleport/select_room_id_from_linked_teleport.sql",
            mapOf("teleport_ids" to teleportIds)
        ).map { it.teleportId to (it.roomId ?: 0) }
    }

    fun deleteItems(itemIds: List<Int>) = WriteBehindManager.queue(WriteKey.items(itemIds)) {
        db {
            batchUpdate(
                javaClass.classLoader.getResource("sql/items/item/delete_item.sql").readText(),
                itemIds.map {
                    mapOf(
                        "id" to it
                    )
                }
            )
        }
    }

    private fun removeRoomItems(roomItemsToRemove: Collection<RoomItem>) {
        if (roomItemsToRemove.isEmpty()) return

        // snapshot: extrai extra_data antes de enfileirar — a entidade pode mutar até o flush
        val snapshot = roomItemsToRemove.map {
            mapOf(
                "room_id" to null,
                "extra_data" to HabboServer.habboGame.itemManager
                    .getFurnitureLogic(it.furnishing)
                    .sanitizeForDatabase(it.extraData),
                "id" to it.id
            )
        }

        WriteBehindManager.queue(WriteKey.items(roomItemsToRemove.map { it.id })) {
            db {
                batchUpdate(
                    javaClass.classLoader.getResource("sql/items/item/update_item_room.sql").readText(),
                    snapshot
                )
            }
        }
    }

    fun updateItemsOwner(itemIds: List<Int>, userId: Int) {
        if (itemIds.isEmpty()) return

        WriteBehindManager.queue(WriteKey.items(itemIds)) {
            db {
                batchUpdate(
                    javaClass.classLoader.getResource("sql/items/item/update_item_owner.sql").readText(),
                    itemIds.map {
                        mapOf(
                            "user_id" to userId,
                            "id" to it
                        )
                    }
                )
            }
        }
    }

    fun getRoomDimmer(roomItem: RoomItem): RoomDimmer? {
        if (!roomDimmers.containsKey(roomItem.id)) {
            val roomDimmer = db {
                queryOne<RoomDimmerRowDto>(
                    javaClass.classLoader.getResource("sql/items/dimmer/select_dimmer.sql").readText(),
                    mapOf("item_id" to roomItem.id)
                )?.toDomain(roomItem)
            }

            roomDimmers[roomItem.id] = roomDimmer
        }
        val roomDimmer = roomDimmers[roomItem.id] ?: return null

        roomDimmer.roomItem = roomItem

        return roomDimmer
    }

    fun saveDimmer(roomDimmer: RoomDimmer) {
        // snapshot: o dimmer é mutável e pode mudar antes do flush
        val enabled = roomDimmer.enabled
        val currentPreset = roomDimmer.currentPreset
        val presetOne = roomDimmer.presets[0].toString()
        val presetTwo = roomDimmer.presets[1].toString()
        val presetThree = roomDimmer.presets[2].toString()
        val id = roomDimmer.id
        val itemId = roomDimmer.roomItem.id

        // a linha do dimmer pertence ao mobi: ordena com a inserção/remoção do próprio mobi
        WriteBehindManager.queue(WriteKey.item(itemId)) {
            db {
                update(
                    javaClass.classLoader.getResource("sql/items/dimmer/update_dimmer.sql").readText(),
                    mapOf(
                        "enabled" to enabled,
                        "current_preset" to currentPreset,
                        "preset_one" to presetOne,
                        "preset_two" to presetTwo,
                        "preset_three" to presetThree,
                        "id" to id
                    )
                )
            }
        }
    }

    /** Write-behind: enfileira no Dispatchers.IO — nunca chamar db{} síncrono em thread Netty. */
    fun saveWireds(wireds: List<RoomItem>) {
        val wiredData = wireds.mapNotNull { it.wiredData }

        if (wiredData.isEmpty()) return

        WriteBehindManager.queue(WriteKey.items(wireds.filter { it.wiredData != null }.map { it.id })) {
            db {
                batchUpdate(
                    javaClass.classLoader.getResource("sql/items/wired/update_wired_data.sql").readText(),
                    wiredData.map {
                        mapOf(
                            "delay" to it.delay,
                            "items" to it.items.joinToString(","),
                            "message" to it.message,
                            "options" to it.options.joinToString(","),
                            "extradata" to it.extradata,
                            "is_filter" to it.filter,
                            "is_inverse" to it.inverse,
                            "furni_sources" to it.furniSources.map { source -> source.code }.joinToString(","),
                            "user_sources" to it.userSources.map { source -> source.code }.joinToString(","),
                            "stuff_ids2" to it.stuffIds2.joinToString(","),
                            "variable_ids" to it.variableIds.joinToString(","),
                            "id" to it.id
                        )
                    }
                )
            }
        }
    }

    fun addItems(
        userId: Int,
        itemPurchaseDatas: List<ItemPurchaseData>,
        ignoreSpecialHandling: Boolean = false
    ): List<UserItem> {
        val sanitizedPurchaseDatas = itemPurchaseDatas.map { data ->
            val sanitizedExtraData = HabboServer.habboGame.itemManager
                .getFurnitureLogic(data.furnishing)
                .sanitizeForDatabase(data.extraData)
            if (sanitizedExtraData != data.extraData) data.copy(extraData = sanitizedExtraData) else data
        }

        // A regra de criação (inclusive o par de teletransporte) é pura e testada em NewItemPlanTest: o parceiro nasce
        // aqui, uma vez, e volta no fim da lista (não desloca os índices dos pedidos) para entrar no inventário em
        // memória e na resposta da compra — senão existiria só no banco até o jogador relogar.
        val itemManager = HabboServer.habboGame.itemManager
        val plan = planNewItems(
            userId,
            sanitizedPurchaseDatas.map { NewItemSpec(it.furnishing.itemName, it.extraData, it.limited, it.buildersClub) },
            isTeleport = { name ->
                !ignoreSpecialHandling && itemManager.furnishings[name]?.interactionType == InteractionType.TELEPORT
            },
            nextId = { itemManager.itemSequence.nextId() },
        )
        val userItems = plan.all

        // cada item novo é uma linha: os filhos (wired, dimmer, teleporte, presente, limitado) declaram o mesmo item e rodam depois
        WriteBehindManager.queue(WriteKey.items(userItems.map { it.id })) {
            batchInsertItems(userItems)
        }

        try {
            if (plan.teleportLinks.isNotEmpty()) {
                // o par de teleportes referencia os DOIS itens novos
                WriteBehindManager.queue(plan.teleportLinks.flatMap { listOf(WriteKey.item(it.first), WriteKey.item(it.second)) }) {
                    insertTeleportLinks(plan.teleportLinks)
                }
                plan.teleportLinks.forEach { (one, two) ->
                    itemManager.teleportLinks[one] = two
                    itemManager.roomTeleportLinks[one] = 0
                    itemManager.teleportLinks[two] = one
                    itemManager.roomTeleportLinks[two] = 0
                }
            }

            if (!ignoreSpecialHandling) {
                // teletransportes já foram tratados acima (par); o resto tem dado especial (wired, dimmer, presente...)
                plan.requested.forEach { userItem ->
                    if (userItem.furnishing.interactionType != InteractionType.TELEPORT) addSpecialItemData(userItem)
                }
            }
        } catch (e: Exception) {
            // os ids nunca chegam ao chamador: ninguém mais consegue desfazer, então remove tudo que já entrou na fila
            rollbackCreated(userItems.map { it.id })
            throw e
        }

        return userItems
    }

    /**
     * Desfaz uma criação: tira da memória (vínculos de teleporte, conteúdo de caixa pendente) e enfileira a remoção das
     * linhas de `items` com as MESMAS `WriteKey.item(id)` dos inserts (e dos filhos), então o delete roda depois de
     * cada insert enfileirado para o mesmo item. Os filhos (`items_limited`, `items_gift_contents`, `items_teleport`,
     * `items_wired`, `items_dimmer`) saem por `ON DELETE CASCADE`. Os ids vêm da sequência em memória e nunca se
     * repetem, e `items_limited` só tem UNIQUE em `item_id` (não em `limited_num`): reaproveitar o número LTD de uma
     * tentativa falha por outro comprador não colide, mesmo que o delete ainda esteja na fila.
     */
    fun rollbackCreated(itemIds: Collection<Int>) {
        if (itemIds.isEmpty()) return

        val itemManager = HabboServer.habboGame.itemManager
        itemIds.forEach { id ->
            GiftContentsStore.remove(id)
            itemManager.teleportLinks.remove(id)
            itemManager.roomTeleportLinks.remove(id)
        }

        deleteItems(itemIds.toList())
    }

    /** Liga cada par em `items_teleport` (uma linha por sentido). Os dois itens de cada par já precisam existir. */
    fun insertTeleportLinks(links: List<Pair<Int, Int>>) {
        if (links.isEmpty()) return
        db {
            batchUpdate(
                "INSERT INTO `items_teleport` (`teleport_one_id`, `teleport_two_id`) VALUES (:teleport_one_id, :teleport_two_id)",
                links.flatMap { (one, two) ->
                    listOf(
                        mapOf("teleport_one_id" to one, "teleport_two_id" to two),
                        mapOf("teleport_one_id" to two, "teleport_two_id" to one)
                    )
                }
            )
        }
    }

    /**
     * Cria a caixa de presente [boxFurnishing] (dono [ownerId]) com o conteúdo REAL dentro: os itens de [contents] já
     * existem (mesmos ids) e ficam escondidos do inventário enquanto ligados à caixa em `items_gift_contents`.
     * O conteúdo fica também em [GiftContentsStore] até o vínculo ser gravado (a abertura nunca lê um banco defasado).
     */
    fun addGiftBox(ownerId: Int, boxFurnishing: Furnishing, boxExtraData: String, contents: List<UserItem>): UserItem {
        val box = addItems(ownerId, listOf(ItemPurchaseData(boxFurnishing, boxExtraData, false, buildersClub = false))).first()

        try {
            linkGiftContents(box.id, contents)
        } catch (e: Exception) {
            // a caixa é desta função (o chamador não a conhece): desfaz só ela; os conteúdos são do chamador
            rollbackCreated(listOf(box.id))
            throw e
        }

        return box
    }

    /** Liga a caixa aos itens de conteúdo. Declara a caixa e todos os conteúdos (FKs para os dois lados). */
    fun linkGiftContents(boxId: Int, contents: List<UserItem>) {
        if (contents.isEmpty()) return

        GiftContentsStore.register(boxId, contents)

        val contentIds = contents.map { it.id }

        WriteBehindManager.queue(WriteKey.items(contentIds + boxId)) {
            db {
                batchUpdate(
                    javaClass.classLoader.getResource("sql/items/gift/insert_gift_contents.sql").readText(),
                    contentIds.map { mapOf("gift_item_id" to boxId, "item_id" to it) }
                )
            }

            GiftContentsStore.persisted(boxId)
        }
    }

    /** Conteúdo da caixa: da memória enquanto o vínculo não foi gravado; senão do banco (bloqueante, fora das threads de jogo). */
    fun getGiftContents(boxId: Int): List<UserItem> = GiftContentsStore.peek(boxId) ?: db {
        query<UserItemRowDto>(
            javaClass.classLoader.getResource("sql/items/gift/select_gift_content_items.sql").readText(),
            mapOf("gift_item_id" to boxId)
        ).map { it.toDomain() }
    }

    /** Solta o conteúdo da caixa (os itens voltam a aparecer no inventário/sala). Ordenado com a caixa e os conteúdos. */
    fun releaseGiftContents(boxId: Int, contentIds: List<Int>) {
        GiftContentsStore.remove(boxId)

        WriteBehindManager.queue(WriteKey.items(contentIds + boxId)) {
            db {
                update(
                    javaClass.classLoader.getResource("sql/items/gift/delete_gift_contents.sql").readText(),
                    mapOf("gift_item_id" to boxId)
                )
            }
        }
    }

    fun getGiftData(itemId: Int): GiftData? = db {
        queryOne<GiftDataRowDto>(
            javaClass.classLoader.getResource("sql/items/gift/select_gift.sql").readText(),
            mapOf("item_id" to itemId)
        )?.toDomain()
    }

    fun addLimitedItem(itemId: Int, limitedNumber: Int, limitedTotal: Int): Int {
        WriteBehindManager.queue(WriteKey.item(itemId)) {
            db {
                update(
                    "INSERT INTO `items_limited` (`item_id`, `limited_num`, `limited_total`) VALUES (:item_id, :limited_num, :limited_total)",
                    mapOf("item_id" to itemId, "limited_num" to limitedNumber, "limited_total" to limitedTotal)
                )
            }
        }
        return itemId
    }

    /** [id] é a linha de `items_gift`; [itemId] o mobi-presente, que o chamador também apaga (a ordem entre os dois é preservada). */
    fun deleteGiftData(id: Int, itemId: Int) = WriteBehindManager.queue(WriteKey.item(itemId)) {
        db {
            update(
                javaClass.classLoader.getResource("sql/items/gift/delete_gift.sql").readText(),
                mapOf(
                    "id" to id
                )
            )
        }
    }

    fun addRoomItemInventory(roomItems: List<RoomItem>) {
        val roomItemsGrouped = roomItems.groupBy { it.userId }

        roomItemsGrouped.keys.forEach { userId ->
            val habboSession = HabboServer.habboSessionManager.getHabboSessionById(userId)

            habboSession?.habboInventory?.addItems(
                (roomItemsGrouped[userId]
                    ?: error("Can't find the room items!")).map {
                    UserItem(
                        it.id,
                        it.userId,
                        it.itemName,
                        HabboServer.habboGame.itemManager
                            .getFurnitureLogic(it.furnishing)
                            .sanitizeForDatabase(it.extraData),
                        it.limited,
                        it.buildersClub
                    )
                })
        }

        removeRoomItems(roomItems)
    }

    private fun addSpecialItemData(userItem: UserItem) {
        if (userItem.furnishing.interactionType.isWired) {
            val defaultWiredData =
                HabboServer.habboGame.itemManager.getWiredDefaultData(userItem.furnishing.interactionType)
                    ?: WiredData(0, 0, emptyList(), "", emptyList(), "")

            WriteBehindManager.queue(WriteKey.item(userItem.id)) {
                db {
                    update(
                        "INSERT INTO `items_wired` (`item_id`, `delay`, `items`, `message`, `options`, `extradata`) VALUES (:item_id, :delay, :items, :message, :options, :extradata)",
                        mapOf(
                            "item_id" to userItem.id,
                            "delay" to defaultWiredData.delay,
                            "items" to defaultWiredData.items.joinToString(","),
                            "message" to defaultWiredData.message,
                            "options" to defaultWiredData.options.joinToString(","),
                            "extradata" to defaultWiredData.extradata
                        )
                    )
                }
            }
            return
        }

        when (userItem.furnishing.interactionType) {
            // TELEPORT: tratado em addItems/addTeleportPartner (o parceiro precisa voltar na lista devolvida)

            InteractionType.DIMMER -> {
                WriteBehindManager.queue(WriteKey.item(userItem.id)) {
                    db {
                        update(
                            "INSERT INTO `items_dimmer` (`item_id`, `enabled`, `current_preset`, `preset_one`, `preset_two`, `preset_three`) VALUES (:item_id, :enabled, :current_preset, :preset_one, :preset_two, :preset_three)",
                            mapOf(
                                "item_id" to userItem.id,
                                "enabled" to false,
                                "current_preset" to 1,
                                "preset_one" to "#000000,255,0",
                                "preset_two" to "#000000,255,0",
                                "preset_three" to "#000000,255,0"
                            )
                        )
                    }
                }
            }

            else -> {}
        }
    }


    /**
     * Transfere itens entre dois usuários em uma transação atômica.
     * 
     * GARANTIAS DE SEGURANÇA:
     * - Todos os itens são transferidos ou nenhum é (atomicidade via transação SQL)
     * - Valida que o usuário atual é o dono do item antes de transferir
     * - Previne race conditions através de transação SQL
     *
     * @param user1Id ID do usuário 1
     * @param user2Id ID do usuário 2
     * @param user1Items IDs dos itens que user1 está dando (vão para user2)
     * @param user2Items IDs dos itens que user2 está dando (vão para user1)
     * @throws Exception se algum item não for encontrado ou não pertencer ao usuário
     */
    fun transferTradeItems(
        user1Id: Int,
        user2Id: Int,
        user1Items: List<Int>,
        user2Items: List<Int>
    ) {
        if (user1Items.isEmpty() && user2Items.isEmpty()) return

        db {
            // Prepara os parâmetros para batchUpdate
            // Cada item terá seu próprio UPDATE com validação de proprietário
            val allUpdates = mutableListOf<Map<String, Any>>()

            // Adiciona atualizações para itens do user1 (vão para user2)
            user1Items.forEach { itemId ->
                allUpdates.add(
                    mapOf(
                        "new_user_id" to user2Id,
                        "item_id" to itemId,
                        "old_user_id" to user1Id
                    )
                )
            }

            // Adiciona atualizações para itens do user2 (vão para user1)
            user2Items.forEach { itemId ->
                allUpdates.add(
                    mapOf(
                        "new_user_id" to user1Id,
                        "item_id" to itemId,
                        "old_user_id" to user2Id
                    )
                )
            }

            if (allUpdates.isEmpty()) return@db

            // Executa todas as atualizações em batch
            // A query valida que o item pertence ao usuário antes de atualizar
            val updateSql = javaClass.classLoader
                .getResource("sql/items/trade/transfer_trade_items.sql")
                ?.readText()!!

            val rowsAffectedList = batchUpdate(updateSql, allUpdates)
            val totalRowsAffected = rowsAffectedList.sum()

            // Verifica se todos os itens foram atualizados
            if (totalRowsAffected != allUpdates.size) {
                throw IllegalStateException(
                    "Esperado atualizar ${allUpdates.size} itens, mas apenas $totalRowsAffected foram afetados. " +
                            "Possível tentativa de exploit ou item não encontrado."
                )
            }
        }
    }

    fun batchInsertItems(items: List<UserItem>) {
        if (items.isEmpty()) return
        try {
            db {
                batchInsert(
                    "INSERT INTO `items` (`id`, `user_id`, `item_name`, `extra_data`, `wall_pos`, `is_builders_club`) VALUES (:id, :userId, :itemName, :extraData, '', :buildersClub)",
                    items
                )
            }
        } catch (e: Exception) {
            log.error("Write-Behind error bulk inserting items (count: ${items.size})", e)
        }
    }
}


data class ItemPurchaseData(
    val furnishing: Furnishing,
    val extraData: String,
    val limited: Boolean,
    val buildersClub: Boolean,
)

data class FurnishingRowDto(
    val itemName: String,
    val type: String,
    val interactionType: String,
    val stackHeight: String,
    val canStack: Boolean,
    val allowRecycle: Boolean,
    val allowTrade: Boolean,
    val allowMarketplaceSell: Boolean,
    val allowGift: Boolean,
    val allowInventoryStack: Boolean,
    val vendingIds: String
) {
    fun toDomain(furniXMLInfos: Map<String, FurniXMLInfo>): Furnishing {
        val xml = furniXMLInfos[itemName] ?: error("Can't find the XML info for the furni!")
        val itemType = ItemType.fromString(type)
        val interaction = InteractionType.fromString(interactionType)
        return Furnishing(
            itemName,
            xml.spriteId,
            xml.offerId,
            itemType,
            stackHeight.split(';').map { s -> s.trim().toDouble() },
            canStack,
            xml.canSitOn,
            xml.canLayOn,
            interaction != InteractionType.GATE && interaction != InteractionType.TELEPORT && xml.canStandOn,
            allowRecycle,
            allowTrade,
            allowMarketplaceSell,
            allowGift,
            allowInventoryStack,
            interaction,
            vendingIds.split(',').filter { it.isNotBlank() }.map { s -> s.trim().toInt() }
        )
    }
}

data class RoomItemRowDto(
    val id: Int,
    val userId: Int,
    val roomId: Int,
    val itemName: String,
    val extraData: String,
    val x: Int,
    val y: Int,
    val z: Double,
    val rot: Int,
    val wallPos: String,
    val isLimited: Boolean,
    val isBuildersClub: Boolean
) {
    fun toDomain() = RoomItem(
        id, userId, roomId, itemName, extraData,
        Vector3(x, y, z), rot, wallPos, isLimited, isBuildersClub
    )
}

data class UserItemRowDto(
    val id: Int,
    val userId: Int,
    val itemName: String,
    val extraData: String,
    val isLimited: Boolean,
    val isBuildersClub: Boolean,
    val roomId: Int? = null
) {
    fun toDomain() = UserItem(id, userId, itemName, extraData, isLimited, isBuildersClub)
}

data class WiredRowDto(
    val id: Int,
    val itemId: Int = 0,
    val delay: Int,
    val items: String,
    val message: String,
    val options: String,
    val extradata: String,
    val isFilter: Boolean,
    val isInverse: Boolean,
    val furniSources: String = "",
    val userSources: String = "",
    val stuffIds2: String = "",
    val variableIds: String? = null
) {
    fun toDomain() = WiredData(
        id = id,
        delay = delay,
        items = items.toIntList(),
        message = message,
        options = options.toIntList(),
        extradata = extradata,
        filter = isFilter,
        inverse = isInverse,
        furniSources = furniSources.toIntList().mapNotNull { code -> WiredFurniSource.fromCode(code) },
        userSources = userSources.toIntList().mapNotNull { code -> WiredUserSource.fromCode(code) },
        stuffIds2 = stuffIds2.toIntList(),
        variableIds = variableIds?.split(",")?.filter { s -> s.isNotEmpty() } ?: emptyList()
    )
}

data class TeleportLinkDto(val teleportOneId: Int, val teleportTwoId: Int)
data class LinkedTeleportDto(val teleportId: Int, val roomId: Int? = 0)

data class RoomDimmerRowDto(
    val id: Int,
    val enabled: Boolean,
    val currentPreset: Int,
    val presetOne: String,
    val presetTwo: String,
    val presetThree: String
) {
    fun toDomain(roomItem: RoomItem) = RoomDimmer(
        id,
        roomItem,
        enabled,
        currentPreset,
        mutableListOf(
            RoomDimmer.generatePreset(presetOne),
            RoomDimmer.generatePreset(presetTwo),
            RoomDimmer.generatePreset(presetThree)
        )
    )
}

data class GiftDataRowDto(
    val id: Int,
    val itemName: String,
    val amount: Int,
    val extradata: String,
    val isLimited: Boolean
) {
    fun toDomain() = GiftData(id, itemName, amount, extradata, isLimited)
}
