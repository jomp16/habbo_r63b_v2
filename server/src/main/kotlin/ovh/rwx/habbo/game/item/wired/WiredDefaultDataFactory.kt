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

import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.wired.WiredDefaultDataFactory

/**
 * Companion de [WiredItem] que produz o [WiredData] default de itens novos — o registro
 * tipado do wired. Implementado pelo companion de cada classe @WiredItemInteractor e
 * resolvido uma única vez no load do ItemManager (antes: reflexão Kotlin por nome de
 * método com catch-all — um companion faltando era null silencioso, não erro de load).
 */
interface WiredDefaultDataFactory {
    fun getDefaultWiredData(): WiredData
}
