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

package ovh.rwx.habbo.config

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Quartos virtuais de stress test: existem só na memória (sem linha no banco), com ids
 * determinísticos (`VirtualRooms.ID_BASE + 1 .. + count`) para os bots do SDK saberem onde entrar.
 */
data class VirtualRoomsConfig(
    @param:JsonProperty("enabled")
    val enabled: Boolean = false,
    /** Quantos quartos virtuais criar no boot. */
    @param:JsonProperty("count")
    val count: Int = 10,
    /** Capacidade de cada quarto (o navigator mostra este máximo). */
    @param:JsonProperty("max_users")
    val maxUsers: Int = 1000,
    @param:JsonProperty("name_prefix")
    val namePrefix: String = "[STRESS] Sala",
    /** Semente do sorteio de modelos/categorias: mesma semente = mesma distribuição a cada boot. */
    @param:JsonProperty("seed")
    val seed: Long = 1,
)
