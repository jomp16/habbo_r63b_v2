/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.utils.pathfinding.core.finders

import tk.jomp16.utils.pathfinding.IFinder
import tk.jomp16.utils.pathfinding.core.*
import tk.jomp16.utils.pathfinding.core.heuristics.ManhattanHeuristic
import tk.jomp16.utils.pathfinding.core.heuristics.OctileHeuristic
import java.util.*
import kotlin.comparisons.compareBy

class AStarFinder(
        private val diagonalMovement: DiagonalMovement = DiagonalMovement.ONLY_WHEN_NO_OBSTACLES,
        private val heuristic: IHeuristic = if (diagonalMovement == DiagonalMovement.NEVER) OctileHeuristic() else ManhattanHeuristic()
) : IFinder {
    override fun findPath(grid: Grid, startX: Int, startY: Int, endX: Int, endY: Int): List<Path> {
        val openList: Queue<Node> = PriorityQueue(compareBy(Node::f))
        val closedList: MutableList<Node> = mutableListOf()

        val startNode = grid.getNodeAt(startX, startY)
        val endNode = grid.getNodeAt(endX, endY)
        var node: Node
        var neighbors: List<Node>

        // set the 'g' and 'f' value of the start node to be 0
        startNode.g = 0.toDouble()
        startNode.f = 0.toDouble()

        // push the start node into the open list
        openList.offer(startNode)

        // while the open list is not empty
        while (openList.isNotEmpty()) {
            // pop the position of node which has the minimum 'f' value.
            node = openList.poll()

            if (!closedList.contains(node)) closedList.add(node)

            // if reached the end position, construct the path and return it
            if (node == endNode) {
                val path: MutableList<Path> = mutableListOf()

                var target: Node? = endNode

                while (target != startNode) {
                    if (target != null) {
                        path.add(Path(target.x, target.y))

                        target = target.parent
                    }
                }

                return path.reversed()
            }

            // get neighbours of the current node
            neighbors = grid.getNeighbors(node, diagonalMovement)

            neighbors.forEach { neighbor ->
                if (closedList.contains(neighbor)) return@forEach

                val x = neighbor.x
                val y = neighbor.y

                // get the distance between current node and the neighbor
                // and calculate the next g score
                val ng = node.g + if (x - node.x == 0 || y - node.y == 0) 1.toDouble() else Math.sqrt(2.toDouble())

                // check if the neighbor has not been inspected yet, or
                // can be reached with smaller cost from the current node
                if (!openList.contains(neighbor) || ng < neighbor.g) {
                    neighbor.g = ng
                    neighbor.h = heuristic.getCost(grid, Math.abs((x - endX).toDouble()).toInt(), Math.abs((y - endY).toDouble().toInt()))
                    neighbor.f = neighbor.g + neighbor.h
                    neighbor.parent = node

                    if (!openList.contains(neighbor)) openList.offer(neighbor)
                }
            }
        }

        return emptyList()
    }
}