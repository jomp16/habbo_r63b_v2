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

package ovh.rwx.habbo.game.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.gamedata.ExternalVariables
import java.time.LocalDate

/**
 * Estado do reciclador: os slots vêm do `external_variables` que os clients recebem ([ExternalVariables]); aberto ou
 * fechado é só do servidor (`recycler.open` do `config.yaml`), entregue ao client pelo pacote de status.
 *
 * `recycler.enabled` existe no `external_variables.txt` servido, mas nenhum client AS3 (2011-2026) o lê: não usar.
 * `recycler.ducket_cost`/`recycler.timeout_seconds` (lidos pelo AIR 2025+) ainda não são aplicados: dependem do packet
 * log de 2025 (`docs/HANDOFF_RECYCLER.md`).
 */
object RecyclerSettings {
    fun slotsFor(buildDate: LocalDate): Int =
        RecyclerSlots.forBuildDate(buildDate, RecyclerSlots.fromExternalVariables(ExternalVariables.snapshot()))

    val open: Boolean
        get() = HabboServer.habboConfig.recyclerConfig.open
}
