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

package ovh.rwx.habbo.game.item.user

import ovh.rwx.habbo.database.item.ItemDao

/**
 * Coletor dos itens criados em UMA tentativa de compra (conteúdo, parceiros de teleporte, caixa de presente). A criação
 * é write-behind (os ids vêm da sequência em memória e as linhas entram na fila), então uma falha no meio deixaria
 * linhas órfãs. [rollback] enfileira a remoção de TUDO que a tentativa criou. Todas as tabelas filhas
 * (`items_limited`, `items_gift_contents`, `items_teleport`, `items_wired`, `items_dimmer`) são `ON DELETE CASCADE`
 * sobre `items`, então apagar o item basta; a ordem com os inserts vem das mesmas `WriteKey.item(id)`.
 */
class CreatedItems {
    private val ids = mutableListOf<Int>()

    /** Ids registrados, na ordem de criação (cópia). */
    val itemIds: List<Int> get() = ids.toList()

    fun record(items: Collection<UserItem>) {
        items.forEach { ids += it.id }
    }

    fun record(item: UserItem) {
        ids += item.id
    }

    /** O plano de remoção: todos os ids registrados, na ordem de criação, sem repetição. Vazio = nada a desfazer. */
    fun rollbackPlan(): List<Int> = ids.distinct()

    /** Desfaz a tentativa (idempotente: esvazia o coletor). [delete] recebe o plano; por padrão, [ItemDao.rollbackCreated]. */
    fun rollback(delete: (List<Int>) -> Unit = ItemDao::rollbackCreated) {
        val plan = rollbackPlan()
        ids.clear()
        if (plan.isNotEmpty()) delete(plan)
    }
}

/** Número LTD "pioneiro" (EarlyBird): o comprador ganha se algum número que RECEBEU está nos primeiros 10% da série. */
fun isLimitedEarlyBird(assignedNumbers: Collection<Int>, limitedTotal: Int): Boolean {
    val threshold = (limitedTotal * 0.1).toInt()
    return assignedNumbers.any { it in 1..threshold }
}
