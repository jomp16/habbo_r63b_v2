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

package ovh.rwx.habbo.game.catalog.gift

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.room.GiftOpenedData
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.game.catalog.RecyclerAchievements
import ovh.rwx.habbo.game.item.GiftData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.TimeUnit

/**
 * Abertura de presente (e da caixa do Ecotron): o conteúdo são itens reais, escondidos na caixa, e saem com os MESMOS
 * ids. A abertura é reivindicada (uma só por caixa), re-validada depois do atraso da animação e termina com exatamente
 * uma resposta PresentOpened.
 */
object GiftOpener {
    private val log = LoggerFactory.getLogger(GiftOpener::class.java)
    private const val MAGIC_REMOVE_DELAY_SECONDS = 3L

    /** Caixas em abertura (a reivindicação atômica contra clique duplo). */
    private val opening = OpeningClaims()

    private val ioScope = CoroutineScope(Dispatchers.IO)

    /** O que sai de uma caixa: itens reais (modelo novo) ou um registro legado de `items_gift` (cria itens novos). */
    private data class Loaded(val contents: List<UserItem>, val legacy: GiftData?)

    fun open(habboSession: HabboSession, room: Room, box: RoomItem) {
        if (!GiftOpenRules.preflight(box.userId, habboSession.userInformation.id, room.itemManager.items[box.id] === box)) return
        if (!opening.tryClaim(box.id)) return // já em abertura: o clique duplo não faz nada

        val animated = box.furnishing.itemName != HabboServer.habboConfig.recyclerConfig.giftBox

        if (animated) {
            box.magicRemove = true
            box.update(updateDb = false, updateClient = true)
        }

        try {
            ioScope.launch {
                val loaded = try {
                    load(box.id)
                } catch (e: Exception) {
                    log.error("Falha ao carregar o conteúdo do presente {}", box.id, e)
                    null
                }

                if (loaded == null) {
                    abort(box, room)
                    return@launch
                }

                val finish = Runnable { finish(habboSession, room, box, loaded) }

                try {
                    if (animated) {
                        HabboServer.serverScheduledExecutor.schedule(finish, MAGIC_REMOVE_DELAY_SECONDS, TimeUnit.SECONDS)
                    } else {
                        HabboServer.serverScheduledExecutor.execute(finish)
                    }
                } catch (e: Exception) {
                    // ex.: RejectedExecutionException no desligamento: solta a reivindicação e desfaz o magicRemove
                    log.error("Falha ao agendar a abertura do presente {}", box.id, e)
                    abort(box, room)
                }
            }
        } catch (e: Exception) {
            log.error("Falha ao iniciar a abertura do presente {}", box.id, e)
            abort(box, room)
        }
    }

    private fun load(boxId: Int): Loaded? {
        val contents = ItemDao.getGiftContents(boxId)
        if (contents.isNotEmpty()) return Loaded(contents, null)

        val legacy = ItemDao.getGiftData(boxId) ?: return null

        return Loaded(emptyList(), legacy)
    }

    private fun abort(box: RoomItem, room: Room) {
        try {
            if (box.magicRemove) {
                box.magicRemove = false
                if (room.itemManager.items[box.id] === box) box.update(updateDb = false, updateClient = true)
            }
        } finally {
            opening.release(box.id)
        }
    }

    /**
     * Ordem (importa): reivindicação (em [open]) -> revalida -> remove a caixa do quarto ANTES de materializar o legado
     * -> libera o conteúdo / apaga a caixa / apaga o legado -> coloca no quarto ou vai ao inventário -> exatamente UMA
     * resposta PresentOpened -> libera a reivindicação no `finally`. As decisões são de [GiftOpenRules].
     */
    private fun finish(habboSession: HabboSession, room: Room, box: RoomItem, loaded: Loaded) {
        // Estágios: removed = a caixa saiu do quarto; committed = o conteúdo já foi criado/liberado (daqui em diante
        // recolocar a caixa duplicaria itens, então não se recoloca)
        var removed = false
        var committed = false
        try {
            // Revalida depois do atraso: sessão viva, a caixa ainda é desta sala e deste dono (não foi recolhida)
            // (e caixa vazia, legado com amount <= 0: não abre, nada é removido)
            val decision = GiftOpenRules.validate(
                channelActive = habboSession.channel.isActive,
                boxOwnerId = box.userId,
                sessionUserId = habboSession.userInformation.id,
                boxStillInRoom = room.itemManager.items[box.id] === box,
                contentsCount = loaded.contents.size,
                legacyAmount = loaded.legacy?.amount,
            )
            if (decision != OpenDecision.PROCEED) {
                abort(box, room)
                return
            }

            val roomUser = habboSession.roomUser?.takeIf { habboSession.currentRoom === room }

            // a caixa sai do quarto ANTES de criar os itens do legado: se a remoção falhar, nada foi criado (sem duplicar)
            removed = true // antes da chamada: se removeItem lançar no meio, a caixa também precisa ser recolocada
            if (!room.itemManager.removeItem(roomUser, box)) {
                removed = false
                abort(box, room)
                return
            }

            val contents = materialize(habboSession.userInformation.id, loaded)

            // legado: itens novos já existem; apaga o registro NA HORA (antes de qualquer outra coisa que possa lançar)
            // para não reabrir e duplicar
            loaded.legacy?.let {
                committed = true
                ItemDao.deleteGiftData(it.id, box.id)
            }

            ItemDao.releaseGiftContents(box.id, contents.map { it.id })
            committed = true
            ItemDao.deleteItems(listOf(box.id))

            val first = contents.first()
            val placed = tryPlaceInRoom(room, roomUser, box, contents)

            if (!placed) {
                habboSession.habboInventory.addItems(contents)
            }

            habboSession.sendResponse(
                Outgoing.ROOM_ITEM_OPEN_GIFT_RESULT,
                OutgoingR63A.ROOM_ITEM_OPEN_GIFT_RESULT,
                GiftOpenedData(first.furnishing, first.id, placed)
            )

            // só a caixa do Ecotron conta (presente normal não), e só no sucesso, depois da única resposta
            if (box.furnishing.itemName == HabboServer.habboConfig.recyclerConfig.giftBox) {
                try {
                    HabboServer.habboGame.achievementManager.progress(
                        habboSession, RecyclerAchievements.RECYCLE_BOX_OPENER, 1, accumulate = true
                    )
                } catch (e: Exception) {
                    log.error("Falha ao registrar a conquista da caixa do Ecotron {}", box.id, e)
                }
            }
        } catch (e: Exception) {
            log.error("Falha ao abrir o presente {}", box.id, e)
            if (removed && !committed) restoreBox(habboSession, room, box)
        } finally {
            opening.release(box.id)
        }
    }

    /**
     * Nada foi entregue: recoloca a MESMA caixa na posição de antes. `removeItem` não mexe na linha do banco (a caixa
     * continua no quarto lá), então só o estado em memória/client precisa voltar. Se o tile já foi ocupado, a caixa
     * reaparece quando o quarto recarregar.
     */
    private fun restoreBox(habboSession: HabboSession, room: Room, box: RoomItem) {
        try {
            box.magicRemove = false
            val roomUser = habboSession.roomUser?.takeIf { habboSession.currentRoom === room }
            val back = room.itemManager.setFloorItem(
                box, box.position.vector2, box.rotation, roomUser, overrideZ = box.position.z
            )
            if (!back) log.error("Não foi possível recolocar a caixa de presente {} no quarto {}", box.id, room.roomData.id)
        } catch (e: Exception) {
            log.error("Falha ao recolocar a caixa de presente {}", box.id, e)
        }
    }

    /** Itens reais, ou (legado) itens novos criados agora para o dono. */
    private fun materialize(ownerId: Int, loaded: Loaded): List<UserItem> {
        loaded.legacy?.let { legacy ->
            return ItemDao.addItems(
                ownerId,
                List(legacy.amount) { ItemPurchaseData(legacy.furnishing, legacy.extradata, legacy.limited, buildersClub = false) }
            )
        }

        return loaded.contents
    }

    /** Um único mobi de chão cabe no lugar da caixa (mesma posição/rotação): coloca no quarto com o mesmo id. */
    private fun tryPlaceInRoom(
        room: Room,
        roomUser: ovh.rwx.habbo.game.room.user.RoomUser?,
        box: RoomItem,
        contents: List<UserItem>
    ): Boolean {
        if (GiftOpenRules.placement(contents.map { it.furnishing.type }) != GiftPlacement.TRY_PLACE_IN_ROOM) return false
        val content = contents.single()

        return try {
            val roomItem = HabboServer.habboGame.itemManager.getRoomItemFromUserItem(room.roomData.id, content)
            room.itemManager.setFloorItem(roomItem, box.position.vector2, box.rotation, roomUser)
        } catch (e: Exception) {
            log.warn("Não foi possível colocar o conteúdo {} no quarto; vai para o inventário", content.id, e)
            false
        }
    }
}
