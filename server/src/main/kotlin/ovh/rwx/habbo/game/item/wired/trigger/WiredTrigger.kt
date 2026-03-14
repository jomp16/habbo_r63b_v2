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

package ovh.rwx.habbo.game.item.wired.trigger

import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItem
import ovh.rwx.habbo.game.room.Room

/**
 * Base class para todos os triggers Wired.
 * Usa generics para garantir type safety nos dados do trigger.
 */
abstract class WiredTrigger<in T : WiredTriggerData>(
    room: Room,
    roomItem: RoomItem
) : WiredItem(room, roomItem) {

    /**
     * Método principal que é chamado quando o trigger é ativado.
     * Recebe dados tipados em vez de Any?, garantindo type safety.
     * 
     * @param wiredContext Contexto do Wired com alvos, usuário acionador, etc.
     * @param data Dados do trigger, tipados conforme a subclasse
     * @return true se o trigger foi ativado, false caso contrário
     */
    abstract fun onTrigger(wiredContext: WiredContext, data: T): Boolean

    /**
     * Método opcional para resetar o estado do trigger.
     * Sobrescrever quando necessário.
     */
    open fun resetTriggered() {}
}