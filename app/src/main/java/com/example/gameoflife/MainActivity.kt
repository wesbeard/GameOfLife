package com.example.gameoflife

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.slider.Slider
import java.io.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var play: ImageButton
    private lateinit var reset: ImageButton
    private lateinit var next: ImageButton
    private lateinit var slider: Slider
    private lateinit var save: ImageButton
    private lateinit var open: ImageButton
    private lateinit var clone: ImageButton
    private lateinit var randomize: ImageButton
    private lateinit var primaryColorButton: Button
    private lateinit var secondaryColorButton: Button
    private lateinit var generations: SwitchCompat
    private lateinit var generationsSlider: Slider

    private var grid = Grid()
    private var running = false
    private var simSpeed: Long = 500

    companion object {
        fun newIntent(packageContext: Context): Intent {
            return Intent(packageContext, MainActivity::class.java)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 0) {
            return
        }
        when (requestCode) {
            0 -> {
                // Write grid to uri returned by file creation
                write(this@MainActivity, data?.data as Uri)
            }
            1 -> {
                // Open new activity with grid from file
                val openIntent = newIntent(this)
                openIntent.putExtra("grid", read(this@MainActivity, data?.data as Uri))
                startActivity(openIntent)
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "The Game of Life"

        grid.primaryColor = resources.getColor(R.color.green)
        grid.secondaryColor = resources.getColor(R.color.gray)

        recycler = findViewById(R.id.recycler)
        recycler.layoutManager = GridLayoutManager(this@MainActivity, grid.width)
        recycler.adapter = RecyclerAdapter()

        reset = findViewById(R.id.reset)
        reset.setOnClickListener {
            if (running) toggleRunning()
            recycler.adapter = RecyclerAdapter()
        }

        play = findViewById(R.id.play)
        play.setOnClickListener {
            // Start simulation
            toggleRunning()

            play.animate().scaleX(.9F).scaleY(.9F).setDuration(100).withEndAction(Runnable {
                play.animate().scaleX(1F).scaleY(1F).duration = 100
            })

            thread {
                while (running) {
                    grid.simulate(this@MainActivity)
                    Thread.sleep(simSpeed)
                }
            }
        }

        next = findViewById(R.id.next)
        next.setOnClickListener {
            grid.simulate(this@MainActivity)
        }

        slider = findViewById(R.id.slider)
        slider.addOnChangeListener { _, value, _ ->
            simSpeed = (value * 1000).toLong()
        }

        save = findViewById(R.id.save)
        save.setOnClickListener {
            save(this@MainActivity)
        }

        open = findViewById(R.id.open)
        open.setOnClickListener {
            open(this@MainActivity)
        }

        clone = findViewById(R.id.clone)
        clone.setOnClickListener {
            val cloneIntent = newIntent(this)
            cloneIntent.putExtra("grid", grid.toJson())
            startActivity(cloneIntent)
        }

        randomize = findViewById(R.id.randomize)
        randomize.setOnClickListener {
            if (running) toggleRunning()
            grid.randomize(this@MainActivity)
        }

        primaryColorButton = findViewById(R.id.primaryColor)
        primaryColorButton.setOnClickListener {
            launchColorPicker(true)
        }

        secondaryColorButton = findViewById(R.id.secondaryColor)
        secondaryColorButton.setOnClickListener {
            launchColorPicker(false)
        }

        generations = findViewById(R.id.generations)
        generations.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            grid.generationsEnabled = checked
        }

        generationsSlider = findViewById(R.id.generationsSlider)
        generationsSlider.addOnChangeListener { _, value, _ ->
            grid.generations = value.toInt()
            grid.setGenerationsRemaining()
        }
    }

    private fun toggleRunning() {
        running = !running
        when (running) {
            true -> play.setImageResource(R.drawable.pause)
            false -> play.setImageResource(R.drawable.play)
        }
    }

    inner class CellViewHolder(cellView: View) : RecyclerView.ViewHolder(cellView) {
        private val cellView: Button = cellView.findViewById(R.id.cell_button)
        lateinit var cell: Cell

        init {
            this.cellView.setOnClickListener {
                cell.toggleState(this@MainActivity, grid.primaryColor, grid.secondaryColor)
            }
        }

        fun initPosition(position: Int) {
            val col = position % grid.width
            val row = position / grid.height
            cell = grid.addCell(position, col, row, cellView)
        }
    }

    inner class RecyclerAdapter : RecyclerView.Adapter<CellViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
            val inflater: LayoutInflater = LayoutInflater.from(parent.context)
            val cellView: View = inflater.inflate(R.layout.cell, parent, false)
            return CellViewHolder(cellView)
        }

        override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
            holder.initPosition(position)

            // Operations to perform once the last cell is created
            if (position == grid.totalCells - 1) {

                // Set grid to intent extra if it exists
                if (intent.hasExtra("grid")) {
                    val json = intent.getStringExtra("grid")
                    if (json != null) {
                        grid.setFromJson(json, this@MainActivity)
                    }
                }

                // Separate thread for calculating neighbors of each cell
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

    private fun launchColorPicker(primary: Boolean) {

        var defaultColor = when(primary) {
            true -> grid.primaryColor
            false -> grid.secondaryColor
        }

        ColorPickerDialogBuilder
            .with(this)
            .setTitle("Choose a color")
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
            .showAlphaSlider(false)
            .density(12)
            .setPositiveButton(
                "Ok"
            ) { dialog, selectedColor, _ ->
                if (primary) recolorPrimary(selectedColor) else recolorSecondary(selectedColor)
            }
            .setNegativeButton(
                "Cancel"
            ) { _, _ -> }
            .build()
            .show()
    }

    private fun recolorPrimary(newColor: Int) {
        grid.primaryColor = newColor
        primaryColorButton.setBackgroundColor(newColor)
        for (cell in grid.cells.values) {
            if (cell.alive) {
                cell.view.setBackgroundColor(newColor)
            }
        }

        recolorImageButton(play, newColor)
        recolorImageButton(reset, newColor)
        recolorImageButton(next, newColor)
        recolorImageButton(save, newColor)
        recolorImageButton(open, newColor)
        recolorImageButton(clone, newColor)
        recolorImageButton(randomize, newColor)
    }

    private fun recolorSecondary(newColor: Int) {
        grid.secondaryColor = newColor
        secondaryColorButton.setBackgroundColor(newColor)
        for (cell in grid.cells.values) {
            if (!cell.alive) {
                cell.view.setBackgroundColor(newColor)
            }
        }
    }

    // This is some crazy workaround for recoloring an image button
    // Thanks to the legend at https://stackoverflow.com/a/33833926
    // (Other ways of recoloring don't support pre API 21 versions)
    private fun recolorImageButton(button: ImageButton, color: Int) {
        var wrapDrawable = DrawableCompat.wrap(button.background)
        DrawableCompat.setTint(wrapDrawable, color)
        button.setBackgroundDrawable(DrawableCompat.unwrap(wrapDrawable))
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
                        grid.toJson().toByteArray()
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