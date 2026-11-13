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

import ovh.rwx.habbo.game.item.user.UserItem
import java.util.concurrent.ConcurrentHashMap

/**
 * O conteúdo de uma caixa recém-criada, em memória, até o vínculo (`items_gift_contents`) chegar ao banco. A criação é
 * write-behind: abrir uma caixa recém-comprada/reciclada lendo só o banco achava a caixa vazia (conteúdo ainda na fila).
 * A entrada sai quando o vínculo é gravado ([persisted]) ou a caixa é aberta ([remove]); depois disso o banco manda.
 */
object GiftContentsStore {
    private val pending = ConcurrentHashMap<Int, List<UserItem>>()

    fun register(boxId: Int, contents: List<UserItem>) {
        pending[boxId] = contents
    }

    fun peek(boxId: Int): List<UserItem>? = pending[boxId]

    fun persisted(boxId: Int) {
        pending.remove(boxId)
    }

    fun remove(boxId: Int) {
        pending.remove(boxId)
    }

    /** Só para testes. */
    internal fun size(): Int = pending.size
}
