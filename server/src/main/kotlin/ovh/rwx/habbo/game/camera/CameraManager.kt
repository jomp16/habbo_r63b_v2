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

package ovh.rwx.habbo.game.camera

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.camera.CameraDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.kotlin.ip
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class CameraManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val cameraDirectory: Path = Paths.get("camera_data")
    val cameraPreviewDirectory: Path = cameraDirectory.resolve("preview")
    val cameraPurchasedDirectory: Path = cameraDirectory.resolve("purchased")
    val cameraNavigatorThumbnailDirectory: Path = cameraDirectory.resolve("navigator-thumbnail")
    private val currentPictureForUsers: MutableMap<String, Pair<LocalDateTime, String>> = mutableMapOf()
    private val jacksonJson = jacksonObjectMapper()

    fun load() {
        log.info("Starting CameraManager...")

        if (Files.notExists(cameraDirectory)) Files.createDirectory(cameraDirectory)
        if (Files.notExists(cameraPreviewDirectory)) Files.createDirectory(cameraPreviewDirectory)
        if (Files.notExists(cameraPurchasedDirectory)) Files.createDirectory(cameraPurchasedDirectory)
        if (Files.notExists(cameraNavigatorThumbnailDirectory)) Files.createDirectory(cameraNavigatorThumbnailDirectory)

        HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
            Files.walk(cameraPreviewDirectory).use {
                it.filter { Files.isRegularFile(it) }.forEach { path ->
                    val basicFileAttributes = Files.readAttributes(path, BasicFileAttributes::class.java)
                    val localDateTime = LocalDateTime.ofInstant(basicFileAttributes.creationTime().toInstant(), ZoneId.systemDefault())

                    if (localDateTime.plusMinutes(HabboServer.habboConfig.cameraConfig.previewTimeoutMinutes).isBefore(LocalDateTime.now())) {
                        // expired image, delete this now
                        if (!Files.deleteIfExists(path)) log.error("Couldn't delete camera preview: {}", path.fileName)

                        Files.newDirectoryStream(path.parent).use {
                            if (!it.iterator().hasNext()) Files.deleteIfExists(path.parent)
                        }
                    }
                }
            }
            // Delete old navigator thumbnail that room doesn't exists anymore
            Files.walk(cameraNavigatorThumbnailDirectory).use {
                it.filter { Files.isRegularFile(it) }
                        .filter { path -> !HabboServer.habboGame.roomManager.rooms.keys.contains(path.fileName.toString().replace(".png", "").toInt()) }
                        .forEach { path ->
                            // No more room fam, remove this image.
                            if (!Files.deleteIfExists(path)) log.error("Couldn't delete camera navigator thumbnail: {}", path.fileName)
                        }
            }
        }, 0, 5, TimeUnit.SECONDS)

        log.info("Done!")
    }

    fun createCameraPreview(habboSession: HabboSession, cameraBytes: ByteArray): Pair<Boolean, String> {
        if (!habboSession.hasPermission("acc_can_use_camera")) return false to ""
        val cameraPreviewUserPath = cameraPreviewDirectory.resolve(habboSession.userInformation.username)

        if (Files.notExists(cameraPreviewUserPath)) Files.createDirectory(cameraPreviewUserPath)
        val cameraPreviewPath = cameraPreviewUserPath.resolve("${UUID.randomUUID()}.png")

        cameraPreviewPath.toFile().writeBytes(cameraBytes)

        currentPictureForUsers[habboSession.userInformation.username] = LocalDateTime.now() to cameraPreviewPath.fileName.toString()

        return true to "preview/${habboSession.userInformation.username}/${cameraPreviewPath.fileName}"
    }

    fun createRoomThumbnail(habboSession: HabboSession, roomId: Int, roomThumbnailBytes: ByteArray): Boolean {
        if (!habboSession.hasPermission("acc_can_use_camera")) return false
        val roomThumbnailPath = cameraNavigatorThumbnailDirectory.resolve("$roomId.png")

        roomThumbnailPath.toFile().writeBytes(roomThumbnailBytes)

        return true
    }

    fun purchaseCamera(habboSession: HabboSession): Boolean {
        if (!habboSession.hasPermission("acc_can_use_camera")) return false
        if (!currentPictureForUsers.containsKey(habboSession.userInformation.username)) return false
        val picturePair = currentPictureForUsers.remove(habboSession.userInformation.username) ?: return false
        val picName = picturePair.second.replace(".png", "")
        val createdAt = picturePair.first
        val tmpPath = "${habboSession.userInformation.username}/$picName"
        val previewPicturePath = cameraPreviewDirectory.resolve("$tmpPath.png")
        val purchasedPicturePath = cameraPurchasedDirectory.resolve("$tmpPath.png")
        val photoFurnishing = HabboServer.habboGame.itemManager.furnishings["external_image_wallitem_poster_small"] ?: return false

        if (habboSession.userInformation.credits < HabboServer.habboConfig.cameraConfig.prices.credits || habboSession.userInformation.pixels < HabboServer.habboConfig.cameraConfig.prices.pixels) {
            Files.delete(previewPicturePath)

            return false
        }

        if (Files.notExists(previewPicturePath) || Files.exists(purchasedPicturePath)) return false
        if (Files.notExists(purchasedPicturePath.parent)) Files.createDirectory(purchasedPicturePath.parent)

        habboSession.userInformation.credits -= HabboServer.habboConfig.cameraConfig.prices.credits
        habboSession.userInformation.pixels -= HabboServer.habboConfig.cameraConfig.prices.pixels

        Files.move(previewPicturePath, purchasedPicturePath)
        val thumbnailImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)

        thumbnailImage.createGraphics().drawImage(ImageIO.read(purchasedPicturePath.toFile()).getScaledInstance(100, 100, 0), 0, 0, null)

        ImageIO.write(thumbnailImage, "png", File(cameraPurchasedDirectory.toFile(), "${tmpPath}_small.png"))
        val pictureId = CameraDao.savePictureDataToDatabase(habboSession.userInformation.id, habboSession.channel.ip(), picName, createdAt)
        val cameraInfoMap = mapOf(
                "w" to "purchased/$tmpPath.png",
                "s" to habboSession.userInformation.id,
                "n" to habboSession.userInformation.username,
                "u" to "$pictureId",
                "t" to "${TimeUnit.SECONDS.toMillis(createdAt.atZone(ZoneOffset.systemDefault()).toEpochSecond())}"
        )
        val jsonExtradata = jacksonJson.writeValueAsString(cameraInfoMap)
        val userItem = ItemDao.addItems(habboSession.userInformation.id, listOf(photoFurnishing), listOf(jsonExtradata)).first()

        habboSession.habboInventory.addItems(listOf(userItem))

        return true
    }
}