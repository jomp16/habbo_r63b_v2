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

package ovh.rwx.habbo.communication.incoming.catalog.recycler

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.catalog.recycler.CatalogRecycleItemsResultResponse
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.game.catalog.RecyclerAchievements
import ovh.rwx.habbo.game.catalog.RecyclerDraw
import ovh.rwx.habbo.game.catalog.RecyclerSlots
import ovh.rwx.habbo.game.item.user.CreatedItems
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.user.HabboSession
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogRecycleItemsHandler {
    /**
     * A caixa do Ecotron segue o modelo dos presentes: o prêmio é criado agora como item real e fica escondido dentro
     * da caixa (`items_gift_contents`); abrir a caixa o revela com o mesmo id. Um só handler para todas as eras: a
     * quantidade de slots sai da data da build ([RecyclerSlots]), nunca do transporte.
     */
    @Handler(Incoming.CATALOG_RECYCLE_ITEMS)
    @HandlerR63A(IncomingR63A.CATALOG_RECYCLE_ITEMS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        val expectedAmount = RecyclerSlots.forBuildDate(
            habboSession.habboVersion.buildDate.toLocalDate(),
            HabboServer.habboConfig.recyclerConfig.slots
        )
        val amount = habboRequest.readInt()
        val ecotronBoxFurnishing =
            HabboServer.habboGame.itemManager.furnishings[HabboServer.habboConfig.recyclerConfig.giftBox]

        // reciclador fechado recusa também no servidor (o fechamento na UI não basta)
        if (!HabboServer.habboConfig.recyclerConfig.open || amount != expectedAmount || ecotronBoxFurnishing == null) {
            sendResult(habboSession, CatalogRecycleItemsResultResponse.CatalogRecycleItemsResult.FAILURE, 0)

            return
        }
        val itemIds = mutableListOf<Int>()

        repeat(amount) { itemIds += habboRequest.readInt() }

        val draw = HabboServer.habboGame.catalogManager.getRandomRecyclerReward()
        val recycledItemReward = draw?.furnishing
        val roomUser = habboSession.roomUser

        // ids do cliente nunca são confiáveis: distintos, DESTA sessão, reciclável e fora de troca (tudo ou nada)
        val taken = if (recycledItemReward == null || itemIds.distinct().size != amount) null
        else habboSession.habboInventory.takeItemsExactly(itemIds) {
            it.furnishing.allowRecycle && (roomUser == null || !room.tradeManager.isItemInTrade(it.id, roomUser))
        }

        if (taken == null || draw == null || recycledItemReward == null) {
            sendResult(habboSession, CatalogRecycleItemsResultResponse.CatalogRecycleItemsResult.FAILURE, 0)

            return
        }

        val ownerId = habboSession.userInformation.id
        val scope = CreatedItems()
        val box = try {
            val contents = ItemDao.addItems(
                ownerId,
                listOf(ItemPurchaseData(recycledItemReward, "", limited = false, buildersClub = false))
            )
            scope.record(contents)
            val created = ItemDao.addGiftBox(
                ownerId,
                ecotronBoxFurnishing,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                contents
            )
            scope.record(created)

            created
        } catch (e: Exception) {
            // nada foi consumido: desfaz o que foi criado e devolve os itens ao inventário
            log.error("Falha ao criar o prêmio do Ecotron; devolvendo os itens do usuário {}", ownerId, e)
            scope.rollback()
            habboSession.habboInventory.restoreTaken(taken)
            sendResult(habboSession, CatalogRecycleItemsResultResponse.CatalogRecycleItemsResult.FAILURE, 0)

            return
        }

        // só agora os reciclados somem (cliente + banco)
        habboSession.habboInventory.confirmTaken(taken, delete = true)
        habboSession.habboInventory.addItems(listOf(box))

        sendResult(habboSession, CatalogRecycleItemsResultResponse.CatalogRecycleItemsResult.SUCCESS, box.id)

        recordAchievements(habboSession, taken, draw)
    }

    /** Conquistas do Ecotron: só no sucesso (a decisão é pura, em [RecyclerAchievements]). */
    private fun recordAchievements(habboSession: HabboSession, taken: List<UserItem>, draw: RecyclerDraw) {
        try {
            val rewards = HabboServer.habboGame.catalogManager.recyclerRewards
            val progress = RecyclerAchievements.progressFor(
                recycledNames = taken.map { it.furnishing.itemName },
                prizeNames = rewards.values.flatten().toSet(),
                drawnLevel = draw.level,
                populatedLevels = rewards.filterValues { it.isNotEmpty() }.keys
            )
            val achievements = HabboServer.habboGame.achievementManager

            progress.forEach { (name, amount) -> achievements.progress(habboSession, name, amount, accumulate = true) }
        } catch (e: Exception) {
            // a reciclagem já foi entregue: falha de conquista não pode desfazer nem propagar
            log.error("Falha ao registrar as conquistas do Ecotron do usuário {}", habboSession.userInformation.id, e)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(CatalogRecycleItemsHandler::class.java)
    }

    private fun sendResult(
        habboSession: HabboSession,
        result: CatalogRecycleItemsResultResponse.CatalogRecycleItemsResult,
        boxId: Int
    ) {
        habboSession.sendResponse(
            Outgoing.CATALOG_RECYCLE_ITEMS_RESULT,
            OutgoingR63A.CATALOG_RECYCLE_ITEMS_RESULT,
            result,
            boxId
        )
    }
}
