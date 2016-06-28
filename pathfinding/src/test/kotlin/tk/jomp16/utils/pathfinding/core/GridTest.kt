/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.utils.pathfinding.core

import org.junit.Assert
import org.junit.Test
import java.util.*

class GridTest {
    @Test
    fun generateWithoutIsWalkableFunction() {
        val width = 10
        val height = 20
        val grid = Grid(width, height)

        Assert.assertEquals("should have correct size", width, grid.width)
        Assert.assertEquals("should have correct size", height, grid.height)

        (0..height - 1).forEach { y ->
            (0..width - 1).forEach { x ->
                Assert.assertTrue("should set all nodes walkable attribute", grid.isWalkable(grid, x, y))
            }
        }

        //println(AStarFinder().findPath(grid, 1, 1, 9, 15))
    }

    @Test
    fun generateWithIsWalkableFunction() {
        val matrix = arrayOf(
                arrayOf(1, 0, 0, 1),
                arrayOf(0, 1, 0, 0),
                arrayOf(0, 1, 0, 0),
                arrayOf(0, 0, 0, 0),
                arrayOf(1, 0, 0, 1)
        )

        val width = matrix[0].size
        val height = matrix.size

        var invertResult = false

        val grid = Grid(width, height) { grid, x, y ->
            grid.isInside(x, y) && if (invertResult) matrix[y][x] == 0 else matrix[y][x] == 1
        }

        Assert.assertEquals("should have correct size", width, grid.width)
        Assert.assertEquals("should have correct size", height, grid.height)

        (0..height - 1).forEach { y ->
            (0..width - 1).forEach { x ->
                Assert.assertEquals("should return correct answer for position validity query", matrix[y][x] == 1, grid.isWalkable(grid, x, y))
            }
        }

        val asserts = arrayOf(
                arrayOf(0, 0, true),
                arrayOf(0, height - 1, true),
                arrayOf(width - 1, 0, true),
                arrayOf(width - 1, height - 1, true),
                arrayOf(-1, -1, false),
                arrayOf(0, -1, false),
                arrayOf(-1, 0, false),
                arrayOf(0, height, false),
                arrayOf(width, 0, false),
                arrayOf(width, height, false)
        )

        asserts.forEach {
            Assert.assertEquals("should return correct answer for position validity query", it[2] as Boolean, grid.isInside(it[0] as Int, it[1] as Int))
        }

        invertResult = true

        Assert.assertEquals("should return correct neighbors", listOf(grid.getNodeAt(0, 2)), grid.getNeighbors(grid.getNodeAt(0, 1), DiagonalMovement.NEVER))
        Assert.assertEquals("should return correct neighbors", listOf(grid.getNodeAt(1, 0), grid.getNodeAt(2, 1), grid.getNodeAt(3, 1)),
                grid.getNeighbors(grid.getNodeAt(2, 0), DiagonalMovement.IF_AT_MOST_ONE_OBSTACLE).sortedWith(Comparator { a, b -> a.x * 100 + a.y - b.x * 100 - b.y })
        )
    }
}