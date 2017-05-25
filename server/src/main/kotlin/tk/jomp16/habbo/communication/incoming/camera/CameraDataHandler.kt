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

package tk.jomp16.habbo.communication.incoming.camera

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession
import java.time.LocalDateTime

@Suppress("unused", "UNUSED_PARAMETER")
class CameraDataHandler {
    @Handler(Incoming.CAMERA_DATA)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.hasPermission("acc_can_use_camera")) return

        ByteArray(habboRequest.readInt()).let {
            habboRequest.readBytes(it)

            val (success, path) = HabboServer.habboGame.cameraManager.createCameraPreview(habboSession, it)

            if (!success) habboSession.sendHabboResponse(Outgoing.CAMERA_FINISH_PUBLISH, false, LocalDateTime.now().plusHours(1).second, "")
            else habboSession.sendHabboResponse(Outgoing.CAMERA_PREVIEW_URL, path)
        }
    }
}