package com.example.counter

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var countText: TextView
    private lateinit var incrementButton: Button

    private var count = 0
    private val maxCount = 20

    // Define your button color palette
    private val buttonColors = listOf(
        "#6200EE", // purple
        "#03DAC5", // teal
        "#FF5722", // deep orange
        "#4CAF50", // green
        "#03A9F4", // blue
        "#EC407A"  // pink
    )
    private var colorIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        countText = findViewById(R.id.countText)
        incrementButton = findViewById(R.id.incrementButton)

        incrementButton.setOnClickListener {
            count++
            if (count > maxCount) count = 0
            countText.text = count.toString()

            // Update button color
            colorIndex = (colorIndex + 1) % buttonColors.size
            incrementButton.setBackgroundColor(Color.parseColor(buttonColors[colorIndex]))
        }
    }
}