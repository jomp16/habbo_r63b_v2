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

package ovh.rwx.habbo.communication.incoming.gamecenter

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class GameCenterInitializeHandler {
    /**
     * O Game Center abriu (997 `_-0-e()`, sem corpo, 2013). Não responde: a tela pede o status de cada jogo ela mesma
     * (GAME_2_GET_ACCOUNT_GAME_STATUS, com as partidas grátis de hoje). Ler um id aqui dava 0, que desde a V33 é o
     * SnowWar: o servidor mandava "SnowWar ilimitado" por cima do contador do dia.
     */
    @Handler(Incoming.INITIALIZE_GAME_CENTER)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) = Unit
}
