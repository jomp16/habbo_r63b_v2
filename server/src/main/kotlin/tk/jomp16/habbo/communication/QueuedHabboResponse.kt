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

package tk.jomp16.habbo.communication

import java.util.*

class QueuedHabboResponse {
    val headerIds: MutableList<Pair<Int, Array<out Any>>> = ArrayList() // Do not change this, LinkedList is the only that keeps the insertion order

    fun add(headerId: Int, args: Array<out Any>): QueuedHabboResponse {
        headerIds.add(headerId to args)

        return this
    }

    operator fun plusAssign(pair: Pair<Int, Array<out Any>>) {
        add(pair.first, pair.second)
    }
}