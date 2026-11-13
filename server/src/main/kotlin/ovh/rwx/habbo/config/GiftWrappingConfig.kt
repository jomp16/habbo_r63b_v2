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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Embrulho de presentes (catalog.gift_wrapping no config.yaml). Os tipos de caixa/fita (e a janela de release de cada um)
 * vêm da tabela `catalog_gift_wrapping_types`; aqui só ficam o liga/desliga e o preço (em créditos) do embrulho.
 * A caixa padrão (sem embrulho) é sempre grátis.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GiftWrappingConfig(
    @param:JsonProperty("enabled")
    val enabled: Boolean = true,
    @param:JsonProperty("price")
    val price: Int = 1,
)
