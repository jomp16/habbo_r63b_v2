/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.landing

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.landing.LandingDao

class LandingManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    @Volatile
    var landingPromos: List<LandingPromo> = emptyList()
        private set
    @Volatile
    var landingReward: LandingReward? = null
        private set

    fun load() {
        log.info("Loading landing...")

        // Lê antes de trocar; promos como snapshot imutável atômico (sem clear/refill)
        val promos = LandingDao.getLandingPromos().toList()
        val reward = LandingDao.getLandingReward()

        landingPromos = promos
        landingReward = reward

        log.info("Loaded {} landing promos!", landingPromos.size)
        log.info("Loaded landing reward, data=$landingReward")
    }
}