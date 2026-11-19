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

package ovh.rwx.habbo.game.gamecenter

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.gamecenter.GameDailyPlaysDao
import ovh.rwx.habbo.game.user.HabboSession
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/** Quantas partidas de um jogo o usuário jogou em [date]. */
data class DailyPlays(val date: LocalDate, val plays: Int) {
    /** As partidas de [today]: as de outro dia não contam. */
    fun on(today: LocalDate) = if (date == today) plays else 0

    companion object {
        /**
         * Quantas partidas grátis ainda restam hoje: o limite menos as já jogadas, nunca negativo; -1 (ilimitado)
         * quando o limite é negativo. O cliente troca o "Jogue agora" pela compra do HC com 0.
         */
        fun freeGamesLeft(limit: Int, playedToday: Int): Int = if (limit < 0) -1 else (limit - playedToday).coerceAtLeast(0)
    }
}

/**
 * As partidas grátis do dia em cada jogo do Game Center (users_game_daily_plays, V35). O limite vem de
 * game.snowwar.free_games_daily_regular / _hc (HC); o dia vira à meia-noite do servidor. Cache por usuário e jogo,
 * gravado pelo write-behind a cada partida contada.
 */
object GameDailyPlaysService {
    private data class Key(val userId: Int, val gameTypeId: Int)

    private val cache = ConcurrentHashMap<Key, DailyPlays>()

    private fun current(userId: Int, gameTypeId: Int): DailyPlays =
        cache.computeIfAbsent(Key(userId, gameTypeId)) {
            GameDailyPlaysDao.get(userId, gameTypeId) ?: DailyPlays(LocalDate.now(), 0)
        }

    fun playedToday(userId: Int, gameTypeId: Int): Int = current(userId, gameTypeId).on(LocalDate.now())

    fun freeGamesLeft(habboSession: HabboSession, gameTypeId: Int): Int {
        val cfg = HabboServer.habboConfig.gameConfig.snowwar
        val limit = if (habboSession.habboSubscription.hasHabboClub) cfg.freeGamesDailyHc else cfg.freeGamesDailyRegular
        return DailyPlays.freeGamesLeft(limit, playedToday(habboSession.userInformation.id, gameTypeId))
    }

    /** Conta uma partida de hoje. */
    fun record(userId: Int, gameTypeId: Int) {
        val today = LocalDate.now()
        current(userId, gameTypeId) // carrega antes do compute (sem I/O dentro do lock do mapa)
        val updated = cache.compute(Key(userId, gameTypeId)) { _, old -> DailyPlays(today, (old?.on(today) ?: 0) + 1) }!!
        GameDailyPlaysDao.save(userId, gameTypeId, updated.date, updated.plays)
    }
}
