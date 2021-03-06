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

package ovh.rwx.habbo.database.landing

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.landing.LandingPromo
import ovh.rwx.habbo.game.landing.LandingReward

object LandingDao {
    fun getLandingPromos(): List<LandingPromo> = HabboServer.database {
        select("SELECT * FROM `landing_promos`") {
            LandingPromo(
                    it.int("id"),
                    it.string("header"),
                    it.string("body"),
                    it.string("button"),
                    it.boolean("show_button"),
                    it.string("button_link"),
                    it.string("image")
            )
        }
    }

    fun getLandingReward(): LandingReward? = HabboServer.database {
        select("SELECT * FROM `landing_reward` LIMIT 1") {
            LandingReward(
                    it.int("id"),
                    it.string("item_name"),
                    it.int("total_amount"),
                    it.string("random_rewards").split(',')
            )
        }.firstOrNull()
    }
}