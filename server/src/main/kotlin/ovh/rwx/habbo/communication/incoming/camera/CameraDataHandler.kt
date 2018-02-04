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

package ovh.rwx.habbo.communication.incoming.camera

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession
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