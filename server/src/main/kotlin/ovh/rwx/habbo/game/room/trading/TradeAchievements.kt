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

package ovh.rwx.habbo.game.room.trading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ovh.rwx.habbo.game.HabboGame
import ovh.rwx.habbo.game.achievement.CreditFurniValue
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Conquistas de troca. Chamado com a troca já concluída (itens transferidos), dentro do lock da troca: o progresso
 * roda em IO para não segurar o lock nem a thread de rede com acesso ao banco.
 */
object TradeAchievements {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Troca sem nenhum item não conta (seria trivial de repetir). */
    fun onTradeCompleted(
        session1: HabboSession, items1: List<UserItem>,
        session2: HabboSession, items2: List<UserItem>,
    ) {
        if (items1.isEmpty() && items2.isEmpty()) return

        scope.launch {
            progress(session1, items1)
            progress(session2, items2)
        }
    }

    /** `given` = itens que o usuário entregou: conta a troca e o valor em moedas que ele negociou (só quem oferece). */
    private fun progress(session: HabboSession, given: List<UserItem>) {
        val achievements = HabboGame.instance?.achievementManager ?: return
        achievements.progress(session, "ACH_TradeCount", 1, accumulate = true)

        val coinValue = given.sumOf { CreditFurniValue.of(it.itemName) }
        if (coinValue > 0) achievements.progress(session, "ACH_CoinValueTraded", coinValue, accumulate = true)
    }
}
