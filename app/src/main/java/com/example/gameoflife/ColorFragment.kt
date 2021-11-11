package com.example.gameoflife

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class ColorFragment : Fragment() {

    private lateinit var primaryColorButton: Button
    private lateinit var secondaryColorButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_color, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        primaryColorButton = view.findViewById(R.id.primaryColor)
        primaryColorButton.setOnClickListener {
            (activity as MainActivity).launchColorPicker(true)
        }

        secondaryColorButton = view.findViewById(R.id.secondaryColor)
        secondaryColorButton.setOnClickListener {
            (activity as MainActivity).launchColorPicker(false)
        }
    }
}