package com.example.gameoflife

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var play: ImageButton
    private lateinit var reset: ImageButton
    private lateinit var next: ImageButton

    private var grid = Grid()
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "The Game of Life"

        recycler = findViewById(R.id.recycler)
        recycler.layoutManager = GridLayoutManager(this@MainActivity, grid.width)
        recycler.adapter = RecyclerAdapter()

        reset = findViewById(R.id.reset)
        reset.setOnClickListener {
            toggleRunning()
            recycler.adapter = RecyclerAdapter()
        }

        play = findViewById(R.id.play)
        play.setOnClickListener {
            // Start simulation
            toggleRunning()
            thread {
                while (running) {
                    grid.simulate(this@MainActivity)
                    Thread.sleep(500)
                }
            }
        }

        next = findViewById(R.id.next)
        next.setOnClickListener {
            grid.simulate(this@MainActivity)
        }
    }

    fun toggleRunning() {
        running = !running
        when(running) {
            true -> play.setImageResource(R.drawable.pause)
            false -> play.setImageResource(R.drawable.play)
        }
    }

    inner class CellViewHolder(cellView: View): RecyclerView.ViewHolder(cellView) {
        private val cellView: Button = cellView.findViewById(R.id.cell_button)
        lateinit var cell: Cell

        init {
            this.cellView.setOnClickListener {
                cell.toggleState(this@MainActivity)
            }
        }

        fun initPosition(position: Int) {
            val col = position % grid.width
            val row = position / grid.height
            cell = grid.addCell(position, col, row, cellView)
        }
    }

    inner class RecyclerAdapter: RecyclerView.Adapter<CellViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
            val inflater: LayoutInflater = LayoutInflater.from(parent.context)
            val cellView: View = inflater.inflate(R.layout.cell, parent, false)
            return CellViewHolder(cellView)
        }

        override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
            holder.initPosition(position)

            if (position == grid.totalCells - 1) {
                // Separate thread for calculating neighbors for each cell
                // It's slow but only has to run once
                thread {
                    for (cell in grid.cells.values) {
                        cell.calculateNeighbors(grid)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return grid.totalCells
        }
    }
}