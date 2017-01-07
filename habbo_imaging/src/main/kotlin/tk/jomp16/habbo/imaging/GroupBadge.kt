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

package tk.jomp16.habbo.imaging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class GroupBadge(
        private val badgeBases: Collection<Triple<Int, String, String>>,
        private val badgeBaseColors: Collection<Pair<Int, String>>,
        private val badgeSymbols: Collection<Triple<Int, String, String>>,
        private val badgeSymbolColors: Collection<Pair<Int, String>>
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val WIDTH = 39
        const val HEIGHT = 39
    }

    fun getGroupBadge(badgeCode: String): BufferedImage {
        val badgeImage = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val graphics = badgeImage.createGraphics()

        // Anti Aliasing
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

        try {
            val split = badgeCode.split('s')
            val baseCode = split[0].substring(1)
            val symbolsCode = split.drop(1)

            if (baseCode.isNotEmpty()) {
                val data = getParts(baseCode, false)
                val baseData = badgeBases.find { it.first == data[0].toInt() }!!
                val image = ImageIO.read(javaClass.classLoader.getResourceAsStream("base/badgepart_${File(baseData.second).nameWithoutExtension}.png"))

                if (data[1].isNotEmpty()) colorize(image, Color.decode("#${data[1]}"))

                graphics.drawImage(image, badgeImage.width / 2 - image.width / 2, badgeImage.height / 2 - image.height / 2, null)

                if (baseData.third.isNotEmpty()) {
                    val image1 = ImageIO.read(javaClass.classLoader.getResourceAsStream("base/badgepart_${File(baseData.third).nameWithoutExtension}.png"))

                    graphics.drawImage(image1, badgeImage.width / 2 - image.width / 2, badgeImage.height / 2 - image.height / 2, null)
                }
            }

            symbolsCode.forEach {
                val data = getParts(it, true)

                if (data[0].toInt() == 0) return@forEach

                val symbolData = badgeSymbols.find { it.first == data[0].toInt() }!!
                val image = ImageIO.read(javaClass.classLoader.getResourceAsStream("symbol/badgepart_${File(symbolData.second).nameWithoutExtension}.png"))

                if (data[1].isNotEmpty()) colorize(image, Color.decode("#${data[1]}"))

                val position = if (data[2].isNotEmpty()) data[2].toInt() else 0

                var x = 0
                var y = 0

                when (position) {
                    1 -> x = (badgeImage.width - image.width) / 2
                    2 -> x = badgeImage.width - image.width
                    3 -> y = (badgeImage.height / 2) - (image.height / 2)
                    4 -> {
                        x = (badgeImage.width / 2) - (image.width / 2)
                        y = (badgeImage.height / 2) - (image.height / 2)
                    }
                    5 -> {
                        x = badgeImage.width - image.width
                        y = (badgeImage.height / 2) - (image.height / 2)
                    }
                    6 -> y = badgeImage.height - image.height
                    7 -> {
                        x = (badgeImage.width - image.width) / 2
                        y = badgeImage.height - image.height
                    }
                    8 -> {
                        x = badgeImage.width - image.width
                        y = badgeImage.height - image.height
                    }
                }

                graphics.drawImage(image, x, y, null)

                if (symbolData.third.isNotEmpty()) {
                    val image1 = ImageIO.read(javaClass.classLoader.getResourceAsStream("symbol/badgepart_${File(symbolData.third).nameWithoutExtension}.png"))

                    graphics.drawImage(image1, x, y, null)
                }
            }
        } catch (e: Exception) {
            log.error("An exception happened while rendering group badge $badgeCode", e)
        }

        return badgeImage
    }

    private fun colorize(image: BufferedImage, color: Color) {
        (0..image.width - 1).forEach { x ->
            (0..image.height - 1).forEach { y ->
                val color1 = Color(image.getRGB(x, y), true)

                val grayscale = ((color1.red + color1.green + color1.blue).toDouble() / 3) / 0xFF

                image.setRGB(x, y, Color((grayscale * color.red).toInt(), (grayscale * color.green).toInt(), (grayscale * color.blue).toInt(), color1.alpha).rgb)
            }
        }
    }

    private fun getParts(code: String, isSymbol: Boolean): List<String> {
        val tmp = if (isSymbol) badgeSymbols else badgeBases

        val partKey = StringBuilder()

        if (code.startsWith('0')) partKey.append('0').append(tmp.map { it.first }.filter { it < 10 }.find { code.startsWith("0$it") } ?: "0")
        else partKey.append(tmp.map { it.first }.filter { it > 10 }.find { code.startsWith("$it") } ?: "0")

        val partColor = code.substring(partKey.length, if (isSymbol) code.length - 1 else code.length)
        val partPos = if (isSymbol) code.substring(code.length - 1) else "0"
        val color = (if (isSymbol) badgeSymbolColors.find { it.first == partColor.toInt() }?.second else badgeBaseColors.find { it.first == partColor.toInt() }?.second) ?: ""

        return listOf(partKey.toString(), color, partPos)
    }
}