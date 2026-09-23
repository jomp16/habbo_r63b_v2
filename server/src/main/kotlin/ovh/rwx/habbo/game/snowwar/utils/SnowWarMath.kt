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

package ovh.rwx.habbo.game.snowwar.utils

import kotlin.math.abs
import kotlin.math.min

object SnowWarMath {
    const val TILE_SIZE = 3200
    const val TILE_HALFWIDTH = 1600
    const val SUBTURN_MOVEMENT = 534
    const val VELOCITY_DIVISOR = 255
    const val QUICK_THROW_PLANAR_VELOCITY = 2000
    const val SNOWBALL_RADIUS = 400
    const val AVATAR_RADIUS = 1600
    const val AVATAR_COLLISION_HEIGHT = 5000
    const val TREE_RADIUS = TILE_SIZE - SNOWBALL_RADIUS - 1
    const val TREE_COLLISION_HEIGHT = TILE_SIZE
    const val TREE_MAX_HITS = 3
    const val MACHINE_RADIUS = 1200
    const val INITIAL_HEALTH = 5
    const val MAX_SNOWBALLS = 5
    const val STUNNED_TIMER = 100
    const val INVINCIBILITY_TIMER = 60
    const val CREATING_TIMER = 20
    const val MACHINE_SNOWBALL_GENERATOR_TIME = 100
    const val MACHINE_MAX_SNOWBALL_CAPACITY = 5
    const val PILE_MAX_SNOWBALL_CAPACITY = 12
    const val TRAJECTORY_QUICK = 0
    const val TRAJECTORY_SHORT_LOB = 1
    const val TRAJECTORY_LONG_LOB = 2
    const val TRAJECTORY_DEFAULT = 3
    const val SHORT_LOB_MAX_RANGE = 60_000
    const val LONG_LOB_MAX_RANGE = 100_000
    const val DEFAULT_THROW_TO_LOB_CUTOFF_RANGE = 42_000

    val TABLE = intArrayOf(
        0, 16, 22, 27, 32, 35, 39, 42, 45, 48, 50, 53, 55, 57, 59, 61, 64, 65, 67, 69, 71, 73, 75, 76, 78, 80, 81, 83,
        84, 86, 87, 89, 90, 91, 93, 94, 96, 97, 98, 99, 101, 102, 103, 104, 106, 107, 108, 109, 110, 112, 113, 114, 115,
        116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 128, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137,
        138, 139, 140, 141, 142, 143, 144, 144, 145, 146, 147, 148, 149, 150, 150, 151, 152, 153, 154, 155, 155, 156,
        157, 158, 159, 160, 160, 161, 162, 163, 163, 164, 165, 166, 167, 167, 168, 169, 170, 170, 171, 172, 173, 173,
        174, 175, 176, 176, 177, 178, 178, 179, 180, 181, 181, 182, 183, 183, 184, 185, 185, 186, 187, 187, 188, 189,
        189, 190, 191, 192, 192, 193, 193, 194, 195, 195, 196, 197, 197, 198, 199, 199, 200, 201, 201, 202, 203, 203,
        204, 204, 205, 206, 206, 207, 208, 208, 209, 209, 210, 211, 211, 212, 212, 213, 214, 214, 215, 215, 216, 217,
        217, 218, 218, 219, 219, 220, 221, 221, 222, 222, 223, 224, 224, 225, 225, 226, 226, 227, 227, 228, 229, 229,
        230, 230, 231, 231, 232, 232, 233, 234, 234, 235, 235, 236, 236, 237, 237, 238, 238, 239, 240, 240, 241, 241,
        242, 242, 243, 243, 244, 244, 245, 245, 246, 246, 247, 247, 248, 248, 249, 249, 250, 250, 251, 251, 252, 252,
        253, 253, 254, 254, 255
    )

    val COMPONENT = intArrayOf(
        0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8,
        8, 8, 9, 9, 9, 9, 10, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12, 12, 12, 13, 13, 13, 13, 13, 14, 14, 14, 14,
        15, 15, 15, 15, 15, 16, 16, 16, 16, 16, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 19, 19, 19, 19, 19, 20, 20, 20,
        20, 20, 21, 21, 21, 21, 21, 22, 22, 22, 22, 22, 23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 25, 25, 25, 25, 25,
        26, 26, 26, 26, 26, 26, 27, 27, 27, 27, 27, 28, 28, 28, 28, 28, 28, 29, 29, 29, 29, 29, 29, 30, 30, 30, 30, 30,
        30, 31, 31, 31, 31, 31, 31, 32, 32, 32, 32, 32, 32, 33, 33, 33, 33, 33, 33, 34, 34, 34, 34, 34, 34, 34, 35, 35,
        35, 35, 35, 35, 36, 36, 36, 36, 36, 36, 36, 37, 37, 37, 37, 37, 37, 37, 38, 38, 38, 38, 38, 38, 38, 39, 39, 39,
        39, 39, 39, 39, 39, 40, 40, 40, 40, 40, 40, 40, 41, 41, 41, 41, 41, 41, 41, 41, 42, 42, 42, 42, 42, 42, 42, 42,
        43, 43, 43, 43, 43, 43, 43, 43, 44, 44, 44, 44, 44, 44, 44, 44, 44, 45, 45, 45, 45, 45
    )

    val BASE_VEL_X = intArrayOf(
        0, 4, 8, 13, 17, 22, 26, 31, 35, 40, 44, 48, 53, 57, 61, 66, 70, 74, 79, 83, 87, 91, 95, 100, 104, 108, 112,
        116, 120, 124, 127, 131, 135, 139, 143, 146, 150, 154, 157, 161, 164, 167, 171, 174, 177, 181, 184, 187, 190,
        193, 196, 198, 201, 204, 207, 209, 212, 214, 217, 219, 221, 223, 226, 228, 230, 232, 233, 235, 237, 238, 240,
        242, 243, 244, 246, 247, 248, 249, 250, 251, 252, 252, 253, 254, 254, 255, 255, 255, 255, 255, 256, 255, 255,
        255, 255, 255, 254, 254, 253, 252, 252, 251, 250, 249, 248, 247, 246, 244, 243, 242, 240, 238, 237, 235, 233,
        232, 230, 228, 226, 223, 221, 219, 217, 214, 212, 209, 207, 204, 201, 198, 196, 193, 190, 187, 184, 181, 177,
        174, 171, 167, 164, 161, 157, 154, 150, 146, 143, 139, 135, 131, 127, 124, 120, 116, 112, 108, 104, 100, 95, 91,
        87, 83, 79, 74, 70, 66, 61, 57, 53, 48, 44, 40, 35, 31, 26, 22, 17, 13, 8, 4, 0, -4, -8, -13, -17, -22, -26,
        -31, -35, -40, -44, -48, -53, -57, -61, -66, -70, -74, -79, -83, -87, -91, -95, -100, -104, -108, -112, -116,
        -120, -124, -128, -131, -135, -139, -143, -146, -150, -154, -157, -161, -164, -167, -171, -174, -177, -181,
        -184, -187, -190, -193, -196, -198, -201, -204, -207, -209, -212, -214, -217, -219, -221, -223, -226, -228,
        -230, -232, -233, -235, -237, -238, -240, -242, -243, -244, -246, -247, -248, -249, -250, -251, -252, -252,
        -253, -254, -254, -255, -255, -255, -255, -255, -256, -255, -255, -255, -255, -255, -254, -254, -253, -252,
        -252, -251, -250, -249, -248, -247, -246, -244, -243, -242, -240, -238, -237, -235, -233, -232, -230, -228,
        -226, -223, -221, -219, -217, -214, -212, -209, -207, -204, -201, -198, -196, -193, -190, -187, -184, -181,
        -177, -174, -171, -167, -164, -161, -157, -154, -150, -146, -143, -139, -135, -131, -128, -124, -120, -116,
        -112, -108, -104, -100, -95, -91, -87, -83, -79, -74, -70, -66, -61, -57, -53, -48, -44, -40, -35, -31, -26,
        -22, -17, -13, -8, -4
    )

    val BASE_VEL_Y = intArrayOf(
        -256, -255, -255, -255, -255, -255, -254, -254, -253, -252, -252, -251, -250, -249, -248, -247, -246, -244,
        -243, -242, -240, -238, -237, -235, -233, -232, -230, -228, -226, -223, -221, -219, -217, -214, -212, -209,
        -207, -204, -201, -198, -196, -193, -190, -187, -184, -181, -177, -174, -171, -167, -164, -161, -157, -154,
        -150, -146, -143, -139, -135, -131, -128, -124, -120, -116, -112, -108, -104, -100, -95, -91, -87, -83, -79,
        -74, -70, -66, -61, -57, -53, -48, -44, -40, -35, -31, -26, -22, -17, -13, -8, -4, 0, 4, 8, 13, 17, 22, 26, 31,
        35, 40, 44, 48, 53, 57, 61, 66, 70, 74, 79, 83, 87, 91, 95, 100, 104, 108, 112, 116, 120, 124, 127, 131, 135,
        139, 143, 146, 150, 154, 157, 161, 164, 167, 171, 174, 177, 181, 184, 187, 190, 193, 196, 198, 201, 204, 207,
        209, 212, 214, 217, 219, 221, 223, 226, 228, 230, 232, 233, 235, 237, 238, 240, 242, 243, 244, 246, 247, 248,
        249, 250, 251, 252, 252, 253, 254, 254, 255, 255, 255, 255, 255, 256, 255, 255, 255, 255, 255, 254, 254, 253,
        252, 252, 251, 250, 249, 248, 247, 246, 244, 243, 242, 240, 238, 237, 235, 233, 232, 230, 228, 226, 223, 221,
        219, 217, 214, 212, 209, 207, 204, 201, 198, 196, 193, 190, 187, 184, 181, 177, 174, 171, 167, 164, 161, 157,
        154, 150, 146, 143, 139, 135, 131, 128, 124, 120, 116, 112, 108, 104, 100, 95, 91, 87, 83, 79, 74, 70, 66, 61,
        57, 53, 48, 44, 40, 35, 31, 26, 22, 17, 13, 8, 4, 0, -4, -8, -13, -17, -22, -26, -31, -35, -40, -44, -48, -53,
        -57, -61, -66, -70, -74, -79, -83, -87, -91, -95, -100, -104, -108, -112, -116, -120, -124, -128, -131, -135,
        -139, -143, -146, -150, -154, -157, -161, -164, -167, -171, -174, -177, -181, -184, -187, -190, -193, -196,
        -198, -201, -204, -207, -209, -212, -214, -217, -219, -221, -223, -226, -228, -230, -232, -233, -235, -237,
        -238, -240, -242, -243, -244, -246, -247, -248, -249, -250, -251, -252, -252, -253, -254, -254, -255, -255,
        -255, -255, -255
    )

    fun tileToWorld(num: Int): Int = num * TILE_SIZE

    fun worldToTile(num: Int): Int = (num + TILE_HALFWIDTH) / TILE_SIZE

    fun circlesOverlap(x1: Int, y1: Int, radius1: Int, x2: Int, y2: Int, radius2: Int): Boolean {
        val radius = radius1 + radius2
        val deltaX = abs(x1 - x2)
        val deltaY = abs(y1 - y2)
        return !(deltaX > radius || deltaY > radius) && (deltaX.toLong() * deltaX) + (deltaY.toLong() * deltaY) < (radius.toLong() * radius)
    }

    fun fastSqrt(x: Int): Int {
        if (x >= 65536) {
            if (x >= 16777216) {
                if (x >= 268435456) {
                    return if (x >= 1073741824) {
                        TABLE[x / 16777216] * 256
                    } else {
                        TABLE[x / 4194304] * 128
                    }
                } else {
                    return if (x >= 67108864) {
                        TABLE[x / 1048576] * 64
                    } else {
                        TABLE[x / 262144] * 32
                    }
                }
            } else {
                if (x >= 1048576) {
                    return if (x >= 4194304) {
                        TABLE[x / 65536] * 16
                    } else {
                        TABLE[x / 16384] * 8
                    }
                } else {
                    return if (x >= 262144) {
                        TABLE[x / 4096] * 4
                    } else {
                        TABLE[x / 1024] * 2
                    }
                }
            }
        } else {
            if (x >= 256) {
                if (x >= 4096) {
                    return if (x >= 16384) {
                        TABLE[x / 256]
                    } else {
                        TABLE[x / 64] / 2
                    }
                } else {
                    return if (x >= 1024) {
                        TABLE[x / 16] / 4
                    } else {
                        TABLE[x / 4] / 8
                    }
                }
            } else {
                if (x >= 0) {
                    return TABLE[x] / 16
                }
            }
        }
        return -1
    }

    fun validateDirection360(value: Int): Int {
        if (value > 359) return value % 360
        if (value < 0) return 360 + (value % 360)
        return value
    }

    fun validateDirection8(value: Int): Int {
        return value and 7
    }

    fun direction360To8(angle360: Int): Int {
        val validated = validateDirection360(angle360 - 22)
        return validateDirection8((validated / 45) + 1)
    }

    fun getAngleFromComponents(x: Int, y: Int): Int {
        return validateDirection360(getAngleFromComponentsMaths(x, y))
    }

    private fun getAngleFromComponentsMaths(xInput: Int, yInput: Int): Int {
        var x = xInput
        var y = yInput
        if (abs(x) <= abs(y)) {
            if (y == 0) y = 1
            x *= 256
            var temp = x / y
            if (temp < 0) temp = -temp
            if (temp > 255) temp = 255

            return if (y < 0) {
                if (x > 0) COMPONENT[temp] else 360 - COMPONENT[temp]
            } else {
                if (x > 0) 180 - COMPONENT[temp] else 180 + COMPONENT[temp]
            }
        } else {
            if (x == 0) x = 1
            y *= 256
            var temp = y / x
            if (temp < 0) temp = -temp
            if (temp > 255) temp = 255

            return if (y < 0) {
                if (x > 0) 90 - COMPONENT[temp] else 270 + COMPONENT[temp]
            } else {
                if (x > 0) 90 + COMPONENT[temp] else 270 - COMPONENT[temp]
            }
        }
    }

    fun getBaseVelX(direction: Int): Int {
        return BASE_VEL_X[validateDirection360(direction)]
    }

    fun getBaseVelY(direction: Int): Int {
        return BASE_VEL_Y[validateDirection360(direction)]
    }

    fun calculateFlightPathWorld(
        startWorldX: Int,
        startWorldY: Int,
        targetWorldX: Int,
        targetWorldY: Int,
        trajectory: Int
    ): IntArray {
        val output = IntArray(5)
        val deltaX = (targetWorldX - startWorldX) / 200
        val deltaY = (targetWorldY - startWorldY) / 200

        output[0] = getAngleFromComponents(deltaX, deltaY)

        val distanceSquared = (deltaX * deltaX) + (deltaY * deltaY)
        val distanceToTarget = fastSqrt(distanceSquared) * 200
        var resolvedTrajectory = trajectory

        if (resolvedTrajectory == TRAJECTORY_DEFAULT) {
            resolvedTrajectory = when {
                distanceToTarget <= DEFAULT_THROW_TO_LOB_CUTOFF_RANGE -> TRAJECTORY_QUICK
                distanceToTarget <= SHORT_LOB_MAX_RANGE -> TRAJECTORY_SHORT_LOB
                else -> TRAJECTORY_LONG_LOB
            }
        }

        val cappedDistance: Int
        if (resolvedTrajectory == TRAJECTORY_QUICK) {
            output[1] = 10
            output[3] = QUICK_THROW_PLANAR_VELOCITY
        } else if (resolvedTrajectory == TRAJECTORY_SHORT_LOB) {
            cappedDistance = min(distanceToTarget, SHORT_LOB_MAX_RANGE)
            output[1] = (cappedDistance * 0.000559).toInt()
            output[3] = if (output[1] == 0) 0 else cappedDistance / output[1]
        } else {
            cappedDistance = min(distanceToTarget, LONG_LOB_MAX_RANGE)
            output[1] = (cappedDistance * 0.0007072135785007072).toInt()
            output[3] = if (output[1] == 0) 0 else cappedDistance / output[1]
        }

        output[2] = output[1] / 2
        output[4] = resolvedTrajectory

        return output
    }
}
