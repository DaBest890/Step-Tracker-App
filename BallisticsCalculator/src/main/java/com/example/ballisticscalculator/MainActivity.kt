package com.example.ballisticscalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val switchUnits = findViewById<SwitchCompat>(R.id.switchUnits)

        // Set initial text based on the default state
        switchUnits.text = if (switchUnits.isChecked) "Imperial" else "Metric"

        // Set up a listener to change the text based on the toggle state
        switchUnits.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                switchUnits.text = "Imperial"
            } else {
                switchUnits.text = "Metric"
            }
        }
    }
}
