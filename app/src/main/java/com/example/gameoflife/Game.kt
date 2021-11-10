package com.example.gameoflife

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Button
import com.google.gson.Gson
import java.io.*
import kotlin.random.Random

const val defaultGenerations = 6

class Cell(val pos: Int,
           val col: Int,
           val row: Int,
           val view: Button) {

    var alive = false
    var neighbors = mutableListOf<Cell>()
    var generationsRemaining = defaultGenerations

    fun toggleState(context: Context, primaryColor: Int, secondaryColor: Int) {
        alive = !alive
        generationsRemaining = defaultGenerations
        when (alive) {
            true -> this.view.setBackgroundColor(primaryColor)
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

    fun simulate(context: Context) {
        var toToggle = mutableListOf<Cell>()

        for (cell in cells.values) {

            if (generationsEnabled && cell.alive) {
                if (cell.generationsRemaining <= 0) {
                    toToggle.add(cell)
                    continue
                }
                else {
                    cell.generationsRemaining--
                }
            }

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
            cell.toggleState(context, primaryColor, secondaryColor)
        }
    }

    fun randomize(context: Context) {
        for (cell in cells.values) {
            if (Random.nextBoolean()) {
                cell.toggleState(context, primaryColor, secondaryColor)
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

    fun setFromJson(json: String, context: Context) {
        val gson = Gson()
        var gridState = mutableListOf<Int>()
        gridState = gson.fromJson(json, gridState.javaClass)

        for (alivePos in gridState) {
            val col = alivePos % width
            val row = alivePos / height
            cells[Pair(col, row)]?.toggleState(context, primaryColor, secondaryColor)
        }
    }

    /*
    File I/O help from:
    https://developer.android.com/training/data-storage/shared/documents-files#kotlin
    https://gist.github.com/neonankiti/05922cf0a44108a2e2732671ed9ef386
     */

    fun save(activity: Activity) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }

        activity.startActivityForResult(intent, 0)
    }

    fun write(activity: Activity, uri: Uri) {
        try {
            activity.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use {
                    it.write(
                        toJson().toByteArray()
                    )
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun open(activity: Activity) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }

        var result = activity.startActivityForResult(intent, 1)

        print(result.toString())
    }

    fun read(activity: Activity, uri: Uri): String {
        val stringBuilder = StringBuilder()
        activity.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }
}