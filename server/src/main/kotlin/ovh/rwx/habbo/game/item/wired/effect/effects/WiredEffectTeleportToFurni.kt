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

package ovh.rwx.habbo.game.item.wired.effect.effects

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_TELEPORT_TO)
class WiredEffectTeleportToFurni(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    init {
        setData()
    }

    override fun code() = WiredEffectType.TELEPORT.code

    override val requiresItems = true
    override val requiresUsers = true

    override fun onEffect(wiredContext: WiredContext) {
        val targetUsers = wiredContext.getEffectiveUsers(this)

        if (targetUsers.isEmpty()) return

        val targetFurnis = wiredContext.getEffectiveFurnis(this)
        if (targetFurnis.isEmpty()) return

        targetUsers.forEach { user ->
            val oldPos = user.currentVector3.copy()

            val currentHighestItem = room.roomGamemap.getHighestItem(oldPos.vector2)

            // Tenta não teleportar para o mesmo item que o usuário já está, a menos que seja a única opção
            var selectedItem = targetFurnis.filter { it.id != currentHighestItem?.id }.randomOrNull()
            if (selectedItem == null) selectedItem = targetFurnis.randomOrNull()
            if (selectedItem == null) return@forEach // Pula este usuário se der erro

            // Encontra um espaço livre no mobi selecionado
            val validTile = selectedItem.affectedTiles.shuffled().firstOrNull { tile ->
                !room.roomGamemap.isBlocked(tile)
            } ?: return@forEach // Pula o usuário se a cadeira/mobi estiver lotada

            // Remove o usuário da posição atual
            user.teleportTo(validTile)
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}