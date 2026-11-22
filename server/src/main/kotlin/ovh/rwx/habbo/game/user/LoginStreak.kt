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

package ovh.rwx.habbo.game.user

import java.time.LocalDate

/**
 * Login diário (ACH_Login, "entrar no Hotel por N dias seguidos").
 *
 * Conta ENTRAR (abrir uma sessão) em cada dia. Ficar conectado sem reentrar não conta, então deixar o cliente
 * aberto não farma dias. Um dia sem entrar quebra a sequência.
 */
data class LoginDay(
    /** Primeira entrada do dia: conta +1 no ACH_Login e é enviada ao cliente (AUTHENTICATION_FIRST_LOGIN_OF_DAY). */
    val firstOfDay: Boolean,
    /** A entrada anterior foi ontem: a sequência continua. Só vale com [firstOfDay]. */
    val streakContinues: Boolean,
)

object LoginStreak {
    /** [lastLogin] é a data da entrada anterior (null = nunca entrou), [today] a data da entrada atual. */
    fun evaluate(lastLogin: LocalDate?, today: LocalDate): LoginDay {
        if (lastLogin == null) return LoginDay(firstOfDay = true, streakContinues = false)
        if (!lastLogin.isBefore(today)) return LoginDay(firstOfDay = false, streakContinues = false)
        return LoginDay(firstOfDay = true, streakContinues = lastLogin == today.minusDays(1))
    }
}
