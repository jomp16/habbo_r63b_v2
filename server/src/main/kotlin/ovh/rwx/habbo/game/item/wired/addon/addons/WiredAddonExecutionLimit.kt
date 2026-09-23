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

package ovh.rwx.habbo.game.item.wired.addon.addons

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.addon.WiredAddon
import ovh.rwx.habbo.game.item.wired.addon.WiredAddonType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_EXECUTION_LIMIT)
class WiredAddonExecutionLimit(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.EXECUTION_LIMIT
    private var maxExecutions = 1
    private var timeWindowPulses = 1
    private val executionTimestamps = mutableListOf<Long>()

    init {
        setData()
    }

    override fun setData() {
        roomItem.wiredData?.let {
            maxExecutions = it.options.getOrElse(0) { 1 }
            timeWindowPulses = it.options.getOrElse(1) { 1 }
        }
    }

    override fun onAddon(wiredContext: WiredContext) {
        val now = System.currentTimeMillis()
        val windowMs = timeWindowPulses * 500L
        executionTimestamps.removeIf { now - it > windowMs }

        if (executionTimestamps.size >= maxExecutions) {
            wiredContext.cancelled = true
        } else {
            executionTimestamps.add(now)
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(1, 1), "")
        }
    }
}
