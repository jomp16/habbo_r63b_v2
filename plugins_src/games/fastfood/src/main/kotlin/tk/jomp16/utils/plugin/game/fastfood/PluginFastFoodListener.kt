/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.utils.plugin.game.fastfood

import tk.jomp16.habbo.HabboServer
import tk.jomp16.utils.plugin.api.PluginListener
import tk.jomp16.utils.plugin.game.fastfood.impl.FastFoodGameHandler

@Suppress("unused")
class PluginFastFoodListener : PluginListener() {
    private val fastFoodGameHandler: FastFoodGameHandler = FastFoodGameHandler()

    override fun onCreate() {
        super.onCreate()

        // Creates and registers a new FastFood game in HabboServer
        HabboServer.habboGame.gameManager.registerGame(fastFoodGameHandler)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Destroys and unregister FastFood game in HabboServer
        HabboServer.habboGame.gameManager.unregisterGame(fastFoodGameHandler)
    }
}