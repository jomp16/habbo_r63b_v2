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

package ovh.rwx.habbo.game.user.information

/**
 * Flags de UI que o cliente (2016+) guarda num único int e reenvia inteiro em MESSENGER_FRIEND_BAR_STATE.
 * Os valores são os do AS3 (`SessionDataManager.setFriendBarState` = 1, `setRoomToolsState` = 2).
 */
enum class UiFlag(val bit: Int) {
    /** Barra de amigos aberta (expandida). */
    FRIEND_BAR(1),

    /** Painel de room tools aberto. */
    ROOM_TOOLS(2),
}
