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

package ovh.rwx.habbo.game.camera

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.camera.HabboCameraRenderer
import ovh.rwx.habbo.camera.SwfInfo
import ovh.rwx.habbo.camera.json.HabboCamera
import ovh.rwx.habbo.database.camera.CameraDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.ActivityPointType
import java.awt.Color
import java.awt.Image
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.zip.InflaterInputStream
import javax.imageio.ImageIO
import kotlin.io.path.writeText

class CameraManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val cameraDirectory: Path = Paths.get("camera_data")
    val cameraPreviewDirectory: Path = cameraDirectory.resolve("preview")
    val cameraPurchasedDirectory: Path = cameraDirectory.resolve("purchased")
    val cameraNavigatorThumbnailDirectory: Path = cameraDirectory.resolve("navigator-thumbnail")
    private val currentPictureForUsers: ConcurrentHashMap<String, Pair<LocalDateTime, String>> = ConcurrentHashMap()
    private val jacksonJson = jacksonObjectMapper()
    private val habboCameraRenderer = HabboCameraRenderer(HabboServer.habboConfig.cameraConfig.assetsPath)

    fun getSwfInfo(swfName: String): SwfInfo? = habboCameraRenderer.getSwfInfo(swfName)

    fun load() {
        log.info("Loading camera...")

        if (Files.notExists(cameraDirectory)) Files.createDirectory(cameraDirectory)
        if (Files.notExists(cameraPreviewDirectory)) Files.createDirectory(cameraPreviewDirectory)
        if (Files.notExists(cameraPurchasedDirectory)) Files.createDirectory(cameraPurchasedDirectory)
        if (Files.notExists(cameraNavigatorThumbnailDirectory)) Files.createDirectory(cameraNavigatorThumbnailDirectory)

        habboCameraRenderer.load()

        HabboServer.serverScheduledExecutor.scheduleWithFixedDelay({
            try {
                cleanExpiredPreviews()
                cleanOrphanThumbnails()
            } catch (e: Exception) {
                log.error("Error during camera directory maintenance", e)
            }
        }, 0, 5, TimeUnit.SECONDS)

        log.info("Done!")
    }

    private fun cleanExpiredPreviews() {
        Files.walk(cameraPreviewDirectory).use { paths ->
            paths.filter { path -> Files.isRegularFile(path) }.forEach { path ->
                val basicFileAttributes = Files.readAttributes(path, BasicFileAttributes::class.java)
                val localDateTime =
                    LocalDateTime.ofInstant(basicFileAttributes.creationTime().toInstant(), ZoneId.systemDefault())

                if (localDateTime.plusMinutes(HabboServer.habboConfig.cameraConfig.previewTimeoutMinutes)
                        .isBefore(LocalDateTime.now())
                ) {
                    if (!Files.deleteIfExists(path)) log.error("Couldn't delete camera preview: {}", path.fileName)

                    Files.newDirectoryStream(path.parent).use { directoryStream ->
                        if (!directoryStream.iterator().hasNext()) Files.deleteIfExists(path.parent)
                    }
                }
            }
        }
    }

    private fun cleanOrphanThumbnails() {
        Files.walk(cameraNavigatorThumbnailDirectory).use { paths ->
            paths.filter { path -> Files.isRegularFile(path) }
                .forEach { path ->
                    val roomId = path.fileName.toString().removeSuffix(".png").toIntOrNull() ?: return@forEach
                    if (!HabboServer.habboGame.roomManager.rooms.containsKey(roomId)) {
                        if (!Files.deleteIfExists(path)) {
                            log.error("Couldn't delete camera navigator thumbnail: {}", path.fileName)
                        }
                    }
                }
        }
    }

    fun createCameraPreview(habboSession: HabboSession, cameraBytes: ByteArray): Pair<Boolean, String> {
        if (!habboSession.hasPermission("acc_can_use_camera")) return false to ""
        val cameraPreviewUserPath = cameraPreviewDirectory.resolve(habboSession.userInformation.username)

        if (Files.notExists(cameraPreviewUserPath)) Files.createDirectory(cameraPreviewUserPath)
        val uuid = UUID.randomUUID()
        val cameraPreviewPath = cameraPreviewUserPath.resolve("${uuid}.png")
        val cameraPreviewJsonPath = cameraPreviewUserPath.resolve("${uuid}.json")

        val jsonData = extractJsonData(cameraBytes)
        cameraPreviewJsonPath.writeText(jsonData)

        val renderedBytes = renderCameraData(jsonData)
        cameraPreviewPath.toFile().writeBytes(renderedBytes)

        currentPictureForUsers[habboSession.userInformation.username] =
            LocalDateTime.now() to cameraPreviewPath.fileName.toString()

        return true to "preview/${habboSession.userInformation.username}/${cameraPreviewPath.fileName}"
    }

    fun createRoomThumbnail(habboSession: HabboSession, roomId: Int, roomThumbnailBytes: ByteArray): Boolean {
        if (!habboSession.hasPermission("acc_can_use_camera")) return false
        val roomThumbnailPath = cameraNavigatorThumbnailDirectory.resolve("$roomId.png")

        val jsonData = extractJsonData(roomThumbnailBytes)
        val renderedBytes = renderCameraData(jsonData)
        roomThumbnailPath.toFile().writeBytes(renderedBytes)

        return true
    }

    private fun extractJsonData(cameraBytes: ByteArray): String =
        if (isZlibCompressed(cameraBytes)) {
            InflaterInputStream(cameraBytes.inputStream()).readBytes().decodeToString()
        } else {
            cameraBytes.decodeToString()
        }

    private fun renderCameraData(data: String): ByteArray {
        val habboCamera: HabboCamera = jacksonJson.readValue(data)
        val dimmer = extractDimmerSettings(habboCamera.roomId.toInt())
        return habboCameraRenderer.renderToBytes(habboCamera, dimmer.color, dimmer.alpha, dimmer.type)
    }

    private fun extractDimmerSettings(roomId: Int): DimmerSettings {
        val room = HabboServer.habboGame.roomManager.rooms[roomId] ?: return DimmerSettings.DEFAULT
        val dimmerItem =
            room.itemManager.wallItems.values.find { it.furnishing.interactionType == InteractionType.DIMMER }
                ?: return DimmerSettings.DEFAULT

        if (dimmerItem.extraData.isNotEmpty()) {
            val parts = dimmerItem.extraData.split(",")
            // Padrão Sulake para o extraData do dimmer: "Estado,Preset,1,CorHex,Intensidade" (2 = Ligado)
            if (parts.size >= 5 && parts[0] == "2") {
                try {
                    val dimmerType = parts[2].toInt()
                    val dimmerColor = Color.decode(parts[3])
                    val dimmerAlpha = parts[4].toFloat() / 255f
                    return DimmerSettings(dimmerType, dimmerColor, dimmerAlpha)
                } catch (_: Exception) {
                    // Ignora erro no parse do Hex/Float para não falhar a renderização da foto
                }
            }
        }
        return DimmerSettings.DEFAULT
    }

    private fun isZlibCompressed(data: ByteArray): Boolean {
        if (data.size < 2) return false
        val header = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        return header == 0x789C || header == 0x7801 || header == 0x78DA
    }

    fun purchaseCamera(habboSession: HabboSession): Boolean {
        if (!habboSession.hasPermission("acc_can_use_camera")) return false
        val picturePair = currentPictureForUsers.remove(habboSession.userInformation.username) ?: return false
        val picName = picturePair.second.replace(".png", "")
        val createdAt = picturePair.first
        val tmpPath = "${habboSession.userInformation.username}/$picName"
        val previewPicturePath = cameraPreviewDirectory.resolve("$tmpPath.png")
        val previewJsonPath = cameraPreviewDirectory.resolve("$tmpPath.json")
        val purchasedPicturePath = cameraPurchasedDirectory.resolve("$tmpPath.png")
        val photoFurnishing = HabboServer.habboGame.itemManager.furnishings["external_image_wallitem_poster_small"]
            ?: return false

        val creditPrice = HabboServer.habboConfig.cameraConfig.prices.credits
        val pixelPrice = HabboServer.habboConfig.cameraConfig.prices.pixels
        val currentPixels = habboSession.userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.PIXELS, 0)

        if (habboSession.userInformation.credits < creditPrice || currentPixels < pixelPrice) {
            Files.deleteIfExists(previewPicturePath)
            Files.deleteIfExists(previewJsonPath)
            return false
        }

        if (Files.notExists(previewPicturePath) || Files.exists(purchasedPicturePath)) return false
        if (Files.notExists(purchasedPicturePath.parent)) Files.createDirectory(purchasedPicturePath.parent)

        habboSession.userInformation.credits -= creditPrice
        habboSession.userInformation.activityPointsCurrencies.merge(
            ActivityPointType.PIXELS,
            -pixelPrice,
            Int::plus
        )

        Files.move(previewPicturePath, purchasedPicturePath)
        Files.deleteIfExists(previewJsonPath)

        val originalImage = ImageIO.read(purchasedPicturePath.toFile())
        val thumbnailWidth = originalImage.width / 2
        val thumbnailHeight = originalImage.height / 2
        val thumbnailImage = BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_ARGB)

        val graphics = thumbnailImage.createGraphics()
        try {
            graphics.drawImage(originalImage.getScaledInstance(thumbnailWidth, thumbnailHeight, Image.SCALE_SMOOTH), 0, 0, null)
        } finally {
            graphics.dispose()
        }

        ImageIO.write(thumbnailImage, "png", File(cameraPurchasedDirectory.toFile(), "${tmpPath}_small.png"))
        val pictureId = CameraDao.savePictureDataToDatabase(habboSession.userInformation.id, picName)
        val cameraInfoMap = mapOf(
            "w" to "purchased/$tmpPath.png",
            "s" to habboSession.userInformation.id,
            "n" to habboSession.userInformation.username,
            "u" to "$pictureId",
            "t" to "${TimeUnit.SECONDS.toMillis(createdAt.atZone(ZoneOffset.systemDefault()).toEpochSecond())}"
        )
        val jsonExtradata = jacksonJson.writeValueAsString(cameraInfoMap)
        val userItem = ItemDao.addItems(
            habboSession.userInformation.id,
            listOf(ItemPurchaseData(photoFurnishing, jsonExtradata, limited = false, buildersClub = false)),
        ).first()

        habboSession.habboInventory.addItems(listOf(userItem))

        // ACH_CameraPhotoCount: revelar fotos
        HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_CameraPhotoCount", 1, accumulate = true)

        return true
    }

    private data class DimmerSettings(
        val type: Int = 2,
        val color: Color? = null,
        val alpha: Float = 0f
    ) {
        companion object {
            val DEFAULT = DimmerSettings()
        }
    }
}