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

package ovh.rwx.habbo.database.camera

import ovh.rwx.habbo.database.sequence.HiLoSequence
import ovh.rwx.habbo.database.db

object CameraDao {
    val cameraSequence = HiLoSequence("camera_pictures", blockSize = 100)

    fun savePictureDataToDatabase(userId: Int, fileName: String): Int {
        val id = cameraSequence.nextId()
        db {
            update(
                "INSERT INTO `camera_pictures` (`id`, `user_id`, `file_name`) VALUES (:id, :user_id, :file_name)",
                mapOf(
                    "id" to id,
                    "user_id" to userId,
                    "file_name" to fileName
                )
            )
        }
        return id
    }
}
