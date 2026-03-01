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

package ovh.rwx.habbo.game.item.wired

/**
 * Define as origens permitidas para a seleção de Mobis (Furnis) em um Wired.
 * Baseado no mapeamento do cliente: wiredfurni.params.sources.furni.*
 */
enum class WiredFurniSource(val code: Int) {
    /** * wiredfurni.params.sources.furni.0
     * Aplica o efeito no mobi que disparou o evento (ex: a alavanca puxada).
     */
    TRIGGERING_ITEM(0),

    /** * wiredfurni.params.sources.furni.100
     * Usa os mobis que o usuário selecionou manualmente (ficaram piscando no quarto).
     */
    SELECTED_ITEMS(100),

    /** * wiredfurni.params.sources.furni.101
     * Usa a segunda lista de mobis selecionados manualmente (stuffIds2).
     */
    SECONDARY_SELECTED_ITEMS(101),

    /** * wiredfurni.params.sources.furni.200
     * Integração com "Wired Selectors" (filtra mobis dinamicamente).
     */
    SELECTOR_ITEMS(200),

    /** * wiredfurni.params.sources.furni.201
     * Integração com "Wired Variables / Signals" (puxa o mobi salvo em um sinal).
     */
    SIGNAL_ITEMS(201),

    /** * wiredfurni.params.sources.furni.900
     * Aplica o efeito em absolutamente todos os mobis do quarto.
     */
    ALL_ROOM_ITEMS(900);

    companion object {
        fun fromCode(code: Int): WiredFurniSource? = entries.find { it.code == code }
    }
}