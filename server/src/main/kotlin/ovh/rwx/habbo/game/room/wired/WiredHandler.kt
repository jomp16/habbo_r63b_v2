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
import ovh.rwx.habbo.game.item.wired.addon.WiredAddon
import ovh.rwx.habbo.game.item.wired.addon.addons.WiredAddonExecuteInOrder
import ovh.rwx.habbo.game.item.wired.addon.addons.WiredAddonOrEval
import ovh.rwx.habbo.game.item.wired.addon.addons.WiredAddonRandom
import ovh.rwx.habbo.game.item.wired.addon.addons.WiredAddonUnseen
import ovh.rwx.habbo.game.item.wired.condition.WiredCondition
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.selector.WiredSelector
import ovh.rwx.habbo.game.item.wired.trigger.SignalTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerAtGivenTime
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerAtTimeLong
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerPeriodically
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerPeriodicallyLong
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.slide.flushWiredMovements
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.user.HabboSession
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
     * 2. Addons (Modificadores globais/locais e limites)
     * 3. Trigger (Ativação)
     * 4. Condições (Filtros e Validações - AND padrão, ou OR com WiredAddonOrEval)
     * 5. Efeitos (Ações nos alvos, com suporte a Random/Unseen/Order)
     */
    fun <T : WiredTriggerData> triggerWired(
        triggerClass: KClass<out WiredTrigger<T>>,
        roomUser: RoomEntity?,
        data: T,
        parentContext: WiredContext? = (data as? SignalTriggerData)?.context
    ): List<WiredTrigger<T>> {
        val triggeredWireds = mutableListOf<WiredTrigger<T>>()
        val batchedMovements: MutableList<WiredMoveEntry> = mutableListOf()

        if (parentContext != null && parentContext.executionDepth >= 25) {
            room.wiredErrorLogger.logError(
                "RECURSION_LIMIT",
                "EXECUTION",
                Exception("Wired execution depth limit exceeded (25)")
            )
            return emptyList()
        }

        // 1. Cast the class once
        @Suppress("UNCHECKED_CAST")
        val targetClass = triggerClass.java as Class<WiredTrigger<T>>

        val startTime = System.currentTimeMillis()
        var executionCost = 0.0

        wiredStack.values.forEach { wiredStackMap ->
            // Filtramos todos os triggers do tipo solicitado nesta pilha
            val triggersInStack = wiredStackMap.values.filterIsInstance(targetClass)

            if (triggersInStack.isEmpty()) return@forEach

            triggersInStack.forEach { trigger ->
                val wiredContext = WiredContext(
                    triggererUser = roomUser,
                    trigger = trigger,
                    executionDepth = (parentContext?.executionDepth ?: 0) + 1
                )

                if (parentContext != null) {
                    wiredContext.variables.putAll(parentContext.variables)
                    wiredContext.placeholders.putAll(parentContext.placeholders)
                }

                // Pegamos a pilha inteira ORDENADA pelo Z para respeitar a lógica visual
                val sortedStack = wiredStackMap.values.sortedBy { it.roomItem.position.z }

                // Custo base por componentes
                executionCost += 1.0 // Trigger base cost
                executionCost += sortedStack.filterIsInstance<WiredSelector>().size * 2.0
                executionCost += sortedStack.filterIsInstance<WiredAddon>().size * 1.5
                executionCost += sortedStack.filterIsInstance<WiredCondition>().size * 2.0
                executionCost += sortedStack.filterIsInstance<WiredEffect>().size * 3.0

                try {
                    // 1. PROCESSAMOS OS SELETORES PRIMEIRO!
                    // Eles preenchem o context.targetFurnis e context.targetUsers
                    sortedStack.filterIsInstance<WiredSelector>().forEach { selector ->
                        selector.onSelect(wiredContext)
                    }

                    // 2. PROCESSAMOS OS ADDONS (Limites de execução, placeholders, filtros, etc.)
                    sortedStack.filterIsInstance<WiredAddon>().forEach { addon ->
                        addon.onAddon(wiredContext)
                    }

                    if (wiredContext.cancelled) return@forEach

                    // 3. AGORA avaliamos o Trigger!
                    if (trigger.onTrigger(wiredContext, data)) {
                        triggeredWireds.add(trigger)
                        lightWired(trigger)

                        // Acendemos os seletores e addons que participaram da ativação
                        sortedStack.filterIsInstance<WiredSelector>().forEach { lightWired(it) }
                        sortedStack.filterIsInstance<WiredAddon>().forEach { lightWired(it) }

                        // 4. Processamos as Condições (Validam se a pilha prossegue)
                        val hasOrEval = sortedStack.any { it is WiredAddonOrEval }
                        val conditions = sortedStack.filterIsInstance<WiredCondition>()
                        val conditionsPassed = conditions.isEmpty() || if (hasOrEval) {
                            conditions.any { condition ->
                                condition.onCondition(wiredContext).also { passed ->
                                    if (passed) lightWired(condition)
                                }
                            }
                        } else {
                            conditions.all { condition ->
                                condition.onCondition(wiredContext).also { passed ->
                                    if (passed) lightWired(condition)
                                }
                            }
                        }

                        // Se as condições passarem, executamos os efeitos
                        if (conditionsPassed) {
                            val allEffects = sortedStack.filterIsInstance<WiredEffect>()
                            val randomAddon = sortedStack.filterIsInstance<WiredAddonRandom>().firstOrNull()
                            val unseenAddon = sortedStack.filterIsInstance<WiredAddonUnseen>().firstOrNull()
                            val orderAddon = sortedStack.filterIsInstance<WiredAddonExecuteInOrder>().firstOrNull()

                            val effectsToExecute = when {
                                randomAddon != null -> randomAddon.selectEffects(allEffects)
                                unseenAddon != null -> unseenAddon.selectEffect(allEffects)?.let { listOf(it) }
                                    ?: emptyList()

                                orderAddon != null -> orderAddon.selectEffect(allEffects)?.let { listOf(it) }
                                    ?: emptyList()

                                else -> allEffects
                            }

                            // Processamos os Efeitos (Ações finais)
                            effectsToExecute
                                .takeWhile { !wiredContext.cancelled }
                                .forEach { effect ->
                                    if ((effect.roomItem.wiredData?.delay ?: 0) <= 0) {
                                        lightWired(effect)
                                    }

                                    effect.handle(wiredContext, roomUser)
                                }

                            // Faz o flush dos movimentos após todos os efeitos rodarem
                            if (wiredContext.batchedMovements.isNotEmpty()) {
                                batchedMovements += wiredContext.batchedMovements
                                wiredContext.batchedMovements.clear() // Limpa para evitar reenvio
                            }
                        }
                    }
                } catch (e: Exception) {
                    room.wiredErrorLogger.logError(e.javaClass.simpleName, "EXECUTION", e)
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        room.wiredPerformanceMonitor.recordExecution(executionCost, duration)

        room.flushWiredMovements(batchedMovements)

        return triggeredWireds
    }

    fun lightItem(roomItem: RoomItem) {
        if (roomItem.extraData == "1") return

        roomItem.extraData = "1"
        roomItem.update(updateDb = false, updateClient = true)
        roomItem.requestTicks(1)
    }

    fun lightWired(wiredItem: WiredItem) {
        lightItem(wiredItem.roomItem)
    }

    fun resetTimers() {
        wiredStack.values.forEach { wiredStackMap ->
            wiredStackMap.values.forEach { wiredItem ->
                when (wiredItem) {
                    is WiredTriggerPeriodically -> wiredItem.resetTriggered()
                    is WiredTriggerPeriodicallyLong -> wiredItem.resetTriggered()
                    is WiredTriggerAtGivenTime -> wiredItem.resetTriggered()
                    is WiredTriggerAtTimeLong -> wiredItem.resetTriggered()
                }
            }
        }
    }

    fun <T : WiredTriggerData> resetTriggerer(triggerClass: KClass<out WiredTrigger<T>>) {
        @Suppress("UNCHECKED_CAST")
        val targetClass = triggerClass.java as Class<WiredTrigger<T>>

        wiredStack.values
            .flatMap { it.values }
            .filterIsInstance(targetClass)
            .forEach { it.resetTriggered() }
    }

    fun saveWired(roomItem: RoomItem, habboRequest: HabboRequest, habboSession: HabboSession): Boolean {
        val vector2 = roomItem.position.vector2
        val wiredItem = wiredStack[vector2]?.get(roomItem.id) ?: return false

        if (wiredItem.saveWired(habboRequest, habboSession)) {
            roomItem.update(updateDb = true, updateClient = false)

            return true
        }

        return false
    }
}