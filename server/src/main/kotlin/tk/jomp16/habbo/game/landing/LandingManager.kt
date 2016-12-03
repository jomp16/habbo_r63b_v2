/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.game.landing

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.database.landing.LandingDao

class LandingManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val landingPromos: MutableList<LandingPromo> = mutableListOf()

    init {
        log.info("Loading landing...")

        landingPromos += LandingDao.getLandingPromos()

        log.info("Loaded {} landing promos!", landingPromos.size)
    }
}