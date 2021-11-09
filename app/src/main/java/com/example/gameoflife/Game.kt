package com.example.gameoflife

import android.content.Context
import android.widget.Button
import androidx.core.content.ContextCompat
import com.google.gson.Gson

class Cell(private val pos: Int,
           private val col: Int,
           private val row: Int,
           private val view: Button) {

    var alive = false
    var neighbors = mutableListOf<Cell>()

    fun toggleState(context: Context) {
        alive = !alive
        when (alive) {
            true -> this.view.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
            false -> this.view.setBackgroundColor(ContextCompat.getColor(context, R.color.gray))
        }
    }

    fun calculateNeighbors(grid: Grid) {
        val width = grid.width - 1
        val height = grid.height - 1

        val north = if (row - 1 >= 0) row - 1 else height
        val east = if (col + 1 <= width) col + 1 else 0
        val south = if (row + 1 <= height) row + 1 else 0
        val west = if (col - 1 >= 0) col - 1 else width

        neighbors.add(grid.cells[Pair(col, north)]!!)
        neighbors.add(grid.cells[Pair(east, row)]!!)
        neighbors.add(grid.cells[Pair(col, south)]!!)
        neighbors.add(grid.cells[Pair(west, row)]!!)
        neighbors.add(grid.cells[Pair(east, north)]!!)
        neighbors.add(grid.cells[Pair(west, north)]!!)
        neighbors.add(grid.cells[Pair(east, south)]!!)
        neighbors.add(grid.cells[Pair(west, south)]!!)

        print(neighbors)
    }
}

class Grid {
    var cells = mutableMapOf<Pair<Int, Int>, Cell>()
    val width = 20
    val height = 20
    val totalCells = width * height

    fun addCell(pos: Int, col: Int, row: Int, view: Button): Cell {
        val newCell = Cell(pos, col, row, view)
        cells[Pair(col, row)] = newCell
        return newCell
    }

    fun simulate(context: Context) {
        var toToggle = mutableListOf<Cell>()

        for (cell in cells.values) {

            // Get number of living neighbors
            var neighborsAlive = 0
            for (neighbor in cell.neighbors) {
                if (neighbor.alive) {
                    neighborsAlive++
                }
            }

            // If cell is dead and has three living neighbors bring it to life
            if (!cell.alive && neighborsAlive == 3) {
                toToggle.add(cell)
                continue
            }

            // If cell is alive and doesn't have 2 or 3 living neighbors kill it
            if (cell.alive && !(neighborsAlive == 2 || neighborsAlive == 3)) {
                toToggle.add(cell)
                continue
            }
        }

        for (cell in toToggle) {
            cell.toggleState(context)
        }
    }

    fun toJson() {
        val gson = Gson()

    }

    fun fromJson() {
        val gson = Gson()
    }
}