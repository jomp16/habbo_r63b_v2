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

package ovh.rwx.habbo.game.room.wired

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItem
import ovh.rwx.habbo.game.item.wired.WiredMoveEntry
import ovh.rwx.habbo.game.item.wired.condition.WiredCondition
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.selector.WiredSelector
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerAtGivenTime
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerPeriodically
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerPeriodicallyLong
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.slide.flushWiredMovements
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Vector2
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class WiredHandler(val room: Room) {
    private val wiredStack: MutableMap<Vector2, MutableMap<Int, WiredItem>> = ConcurrentHashMap()

    fun addWiredItem(vector2: Vector2, wiredItem: WiredItem) {
        if (!wiredStack.containsKey(vector2)) wiredStack[vector2] = mutableMapOf()

        wiredStack[vector2]!![wiredItem.roomItem.id] = wiredItem
    }

    fun removeWiredItem(vector2: Vector2, roomItem: RoomItem): WiredItem? = wiredStack[vector2]?.remove(roomItem.id)

    /**
     * O fluxo Wired 2.0 deve ser:
     * 1. Seletores (Definição de alvos no Contexto)
     * 2. Trigger (Ativação)
     * 3. Ordenação por Z (Base para o topo)
     * 4. Condições (Filtros e Validações)
     * 5. Efeitos (Ações nos alvos)
     */
    fun triggerWired(triggerClass: KClass<out WiredTrigger>, roomUser: RoomUser?, data: Any?): List<WiredTrigger> {
        val triggeredWireds = mutableListOf<WiredTrigger>()
        val batchedMovements: MutableList<WiredMoveEntry> = mutableListOf()

        wiredStack.values.forEach { wiredStackMap ->
            // Filtramos todos os triggers do tipo solicitado nesta pilha
            val triggersInStack = wiredStackMap.values
                .filterIsInstance(triggerClass.java)

            triggersInStack.forEach { trigger ->
                val wiredContext = WiredContext(
                    triggererUser = roomUser,
                    trigger = trigger
                )

                // Pegamos a pilha inteira ORDENADA pelo Z para respeitar a lógica visual
                val sortedStack = wiredStackMap.values.sortedBy { it.roomItem.position.z }

                // PROCESSAMOS OS SELETORES PRIMEIRO!
                // Eles preenchem o context.targetFurnis e context.targetUsers
                sortedStack.filterIsInstance<WiredSelector>().forEach { selector ->
                    selector.onSelect(wiredContext)
                }

                // AGORA avaliamos o Trigger!
                // O Trigger agora pode usar wiredContext.resolveFurniSources(this)
                // para saber se o 'data' (ex: o mobi pisado) está na lista do Seletor.
                if (trigger.onTrigger(wiredContext, data)) {
                    triggeredWireds.add(trigger)
                    lightWired(trigger)

                    // Acendemos os seletores que participaram da ativação
                    sortedStack.filterIsInstance<WiredSelector>().forEach { lightWired(it) }

                    // 3. Processamos as Condições (Validam se a pilha prossegue)
                    // No 2.0, usamos 'all' porque todas precisam ser verdadeiras
                    val conditionsPassed = sortedStack.filterIsInstance<WiredCondition>().all { condition ->
                        condition.onCondition(wiredContext).also { passed ->
                            if (passed) lightWired(condition)
                        }
                    }

                    // Se as condições passarem, executamos os efeitos
                    if (conditionsPassed) {
                        // Processamos os Efeitos (Ações finais)
                        sortedStack.filterIsInstance<WiredEffect>()
                            .takeWhile { !wiredContext.cancelled }
                            .forEach { effect ->
                                lightWired(effect)

                                effect.handle(wiredContext, roomUser)
                            }

                        // NOVO: Faz o flush dos movimentos após todos os efeitos rodarem
                        if (wiredContext.batchedMovements.isNotEmpty()) {
                            batchedMovements += wiredContext.batchedMovements
                            wiredContext.batchedMovements.clear() // Limpa para evitar reenvio
                        }
                    }
                }
            }
        }

        room.flushWiredMovements(batchedMovements)

        return triggeredWireds
    }

    fun lightWired(wiredItem: WiredItem) {
        if (wiredItem.roomItem.extraData == "1") return

        wiredItem.roomItem.extraData = "1"
        wiredItem.roomItem.update(updateDb = false, updateClient = true)
        wiredItem.roomItem.requestCycles(1)
    }

    fun resetTimers() {
        wiredStack.values.forEach { wiredStackMap ->
            wiredStackMap.values.forEach { wiredItem ->
                when (wiredItem) {
                    is WiredTriggerPeriodically -> wiredItem.resetTimer()
                    is WiredTriggerPeriodicallyLong -> wiredItem.resetTimer()
                    is WiredTriggerAtGivenTime -> wiredItem.resetTimer()
                }
            }
        }
    }

    fun resetTriggerer(triggerClass: KClass<out WiredTrigger>) {
        wiredStack.values
            .flatMap { it.values }
            .filterIsInstance(triggerClass.java)
            .forEach { it.resetTriggered() }
    }

    fun saveWired(roomItem: RoomItem, habboRequest: HabboRequest, habboAir: Boolean = false): Boolean {
        val vector2 = roomItem.position.vector2
        val wiredItem = wiredStack[vector2]?.get(roomItem.id) ?: return false

        if (wiredItem.saveWired(habboRequest, habboAir)) {
            roomItem.update(updateDb = true, updateClient = false)

            return true
        }

        return false
    }
}