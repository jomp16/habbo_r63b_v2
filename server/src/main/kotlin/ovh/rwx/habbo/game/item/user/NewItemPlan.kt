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

/** Um mobi a criar para o jogador (já com o extra data sanitizado). */
data class NewItemSpec(
    val itemName: String,
    val extraData: String,
    val limited: Boolean,
    val buildersClub: Boolean,
)

/**
 * Resultado de [planNewItems]: o que será gravado e devolvido ao chamador.
 *
 * @property requested os itens pedidos, na MESMA ordem dos [NewItemSpec] (os chamadores usam o índice).
 * @property teleportPartners o parceiro de cada teletransporte pedido, na ordem em que os teletransportes aparecem.
 * @property teleportLinks pares (pedido, parceiro) a ligar em `items_teleport`.
 */
data class NewItemPlan(
    val requested: List<UserItem>,
    val teleportPartners: List<UserItem>,
    val teleportLinks: List<Pair<Int, Int>>,
) {
    /** Tudo o que existe depois da criação: os pedidos e, no fim, os parceiros (não desloca os índices dos pedidos). */
    val all: List<UserItem> get() = requested + teleportPartners
}

/**
 * Regra pura (sem banco nem servidor) da criação de mobis novos. Teletransporte é um PAR: cada teletransporte
 * criado traz um parceiro ligado a ele, então comprar 1 entrega 2 itens e comprar N entrega 2N. Quem pede N
 * unidades NÃO deve duplicar a entrada: o parceiro nasce aqui, uma única vez.
 *
 * @param isTeleport diz se um mobi (pelo nome) é teletransporte; passe `{ false }` para não criar parceiros.
 * @param nextId fonte de ids; os ids dos pedidos são todos alocados antes dos dos parceiros.
 */
fun planNewItems(
    userId: Int,
    specs: List<NewItemSpec>,
    isTeleport: (itemName: String) -> Boolean,
    nextId: () -> Int,
): NewItemPlan {
    val requested = specs.map { UserItem(nextId(), userId, it.itemName, it.extraData, it.limited, it.buildersClub) }

    val partners = ArrayList<UserItem>()
    val links = ArrayList<Pair<Int, Int>>()
    requested.forEach { item ->
        if (isTeleport(item.itemName)) {
            val partner = UserItem(nextId(), userId, item.itemName, item.extraData, limited = false, buildersClub = false)
            partners += partner
            links += item.id to partner.id
        }
    }

    return NewItemPlan(requested, partners, links)
}
