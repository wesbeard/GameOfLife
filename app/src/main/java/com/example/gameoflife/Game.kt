package com.example.gameoflife

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Button
import com.google.gson.Gson
import java.io.*
import kotlin.random.Random


const val defaultGenerations = 6

class Cell(
    val pos: Int,
    private val col: Int,
    private val row: Int,
    val view: Button
) {

    var alive = false
    var neighbors = mutableListOf<Cell>()
    var generationsRemaining = defaultGenerations

    fun toggleState(activity: Activity, primaryColor: Int, secondaryColor: Int) {
        alive = !alive
        generationsRemaining = defaultGenerations
        when (alive) {
            true -> {
                this.view.setBackgroundColor(primaryColor)
                animate(activity)
            }
            false -> this.view.setBackgroundColor(secondaryColor)
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
    }

    /*
    Animation help from:
    https://stackoverflow.com/questions/15685485/android-shrink-and-grow-sequential-animation
    https://stackoverflow.com/questions/37689903/animators-may-only-be-run-on-looper-threads-android/40508143
     */
    fun animate(activity: Activity) {
        activity.runOnUiThread(Runnable {
            view.animate().scaleX(.7F).scaleY(.7F).setDuration(50).withEndAction(Runnable {
                view.animate().scaleX(1F).scaleY(1F).duration = 150
            })
        })
    }
}

class Grid {
    var cells = mutableMapOf<Pair<Int, Int>, Cell>()
    val width = 20
    val height = 20
    val totalCells = width * height
    var generationsEnabled = true
    var generations = defaultGenerations
    var primaryColor = 0
    var secondaryColor = 0

    fun addCell(pos: Int, col: Int, row: Int, view: Button): Cell {
        val newCell = Cell(pos, col, row, view)
        cells[Pair(col, row)] = newCell
        return newCell
    }

    fun simulate(activity: Activity) {
        var toToggle = mutableListOf<Cell>()

        for (cell in cells.values) {
            var toggled = false

            if (generationsEnabled && cell.alive) {
                if (cell.generationsRemaining <= 0) {
                    toToggle.add(cell)
                    toggled = true
                }
                else {
                    cell.generationsRemaining--
                }
            }

            if (!toggled) {
                // Get number of living neighbors
                var neighborsAlive = 0
                for (neighbor in cell.neighbors) {
                    if (neighbor.alive) {
                        neighborsAlive++
                    }
                }

                // If cell is dead and has three living neighbors bring it to life
                if (!toggled && !cell.alive && neighborsAlive == 3) {
                    toToggle.add(cell)
                    toggled = true
                }

                // If cell is alive and doesn't have 2 or 3 living neighbors kill it
                if (!toggled && cell.alive && !(neighborsAlive == 2 || neighborsAlive == 3)) {
                    toToggle.add(cell)
                    toggled = true
                }

                if ((!toggled && cell.alive) || toggled && !cell.alive) {
                    cell.animate(activity)
                }
            }
        }

        for (cell in toToggle) {
            cell.toggleState(activity, primaryColor, secondaryColor)
        }
    }

    fun randomize(activity: Activity) {
        for (cell in cells.values) {
            if (Random.nextBoolean()) {
                cell.toggleState(activity, primaryColor, secondaryColor)
            }
        }
    }

    fun setGenerationsRemaining() {
        for (cell in cells.values) {
            cell.generationsRemaining = generations
        }
    }

    fun toJson() : String {
        val gson = Gson()
        var gridState = mutableListOf<Int>()

        for (cell in cells.values) {
            if (cell.alive) {
                gridState.add(cell.pos)
            }
        }

        return gson.toJson(gridState)
    }

    fun setFromJson(json: String, activity: Activity) {
        val gson = Gson()
        var gridState = mutableListOf<Int>()
        gridState = gson.fromJson(json, gridState.javaClass)

        for (alivePos in gridState) {
            val col = alivePos % width
            val row = alivePos / height
            cells[Pair(col, row)]?.toggleState(activity, primaryColor, secondaryColor)
        }
    }
}