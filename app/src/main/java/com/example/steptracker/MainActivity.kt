package com.example.steptracker

import com.google.android.material.switchmaterial.SwitchMaterial
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCount: Sensor? = null
    private var totalSteps: Float = 0f
    private var previousTotalSteps: Float = 0f
    private val activityRecognitionRequestCode = 101
    private var currentSteps: Float = 0f
    private var caloriesBurned: Float = 0f
    private var debugMode: Boolean = false // This will be toggled via the Switch

    // UI Elements
    private lateinit var stepCountView: TextView
    private lateinit var caloriesBurnedView: TextView
    private lateinit var stepProgressBar: ProgressBar
    private lateinit var resetButton: Button
    private lateinit var debugModeSwitch: SwitchMaterial // Switch for Debug Mode

    // Constants
    private val stepToCalorieConversionRate: Float = 0.04f  // Approximation: 0.04 calories per step

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        stepCountView = findViewById(R.id.stepCount)
        caloriesBurnedView = findViewById(R.id.caloriesBurned)
        stepProgressBar = findViewById(R.id.stepProgressBar)
        resetButton = findViewById(R.id.resetButton)
        debugModeSwitch = findViewById(R.id.debugModeSwitch) // Initialize Debug Mode Switch

        // Set progress bar max steps
        stepProgressBar.max = 10000  // Goal: 10,000 steps

        // Set up the Debug Mode Switch behavior
        debugModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            debugMode = isChecked
            if (debugMode) {
                Toast.makeText(this, "Debug mode enabled: Simulating 5000 steps", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Debug mode disabled: Real sensor data", Toast.LENGTH_SHORT).show()
            }
            // Save the debug mode state
            saveDebugModeState()
        }

        // Load previous total steps and debug mode state from SharedPreferences
        loadData()

        // Request Activity Recognition permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), activityRecognitionRequestCode)
            } else {
                initStepCounter() // Permission already granted
            }
        } else {
            initStepCounter() // No permission needed for older versions
        }

        // Handle reset button click
        resetButton.setOnClickListener {
            resetSteps()
        }
    }

    // Initialize the step counter sensor
    private fun initStepCounter() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCount = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepCount != null) {
            sensorManager.registerListener(this, stepCount, SensorManager.SENSOR_DELAY_UI)
        } else {
            Toast.makeText(this, "Step Counter Sensor Not Available!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        lifecycleScope.launch(Dispatchers.Default) {
            if (debugMode) {
                // Simulate steps for testing (5000 steps)
                currentSteps = 5000f
                Log.d("StepTracker", "Debug mode: Simulating 5000 steps")
            } else if (event != null) {
                // Use real sensor data in production
                totalSteps = event.values[0]
                currentSteps = totalSteps - previousTotalSteps
                Log.d("StepTracker", "Real sensor data: $currentSteps steps")
            }

            // Calculate calories burned based on steps
            caloriesBurned = currentSteps * stepToCalorieConversionRate

            // Update UI on the main thread
            withContext(Dispatchers.Main) {
                stepProgressBar.max = 10000  // Update goal max steps if needed

                // Animate progress bar smoothly
                val progressAnimator = ObjectAnimator.ofInt(stepProgressBar, "progress", stepProgressBar.progress, currentSteps.toInt())
                progressAnimator.duration = 1500  // 1.5 second animation
                progressAnimator.interpolator = DecelerateInterpolator()
                progressAnimator.start()

                Log.d("StepTracker", "ProgressBar updated: ${currentSteps.toInt()} steps")

                // Update UI with step count and calories burned
                stepCountView.text = getString(R.string.step_count, currentSteps.toInt())  // Steps: [steps]
                caloriesBurnedView.text = getString(R.string.calories_burned, caloriesBurned)  // Calories: [calories]

                // Save the current steps for future sessions
                saveData()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this case
    }

    // Save the current steps and debug mode to SharedPreferences
    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("previousTotalSteps", previousTotalSteps)
        editor.putBoolean("debugMode", debugMode) // Save debug mode state
        editor.apply()
    }

    // Save debug mode state only
    private fun saveDebugModeState() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("debugMode", debugMode)
        editor.apply()
    }

    // Load saved data from SharedPreferences
    private fun loadData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        previousTotalSteps = sharedPreferences.getFloat("previousTotalSteps", 0f)
        debugMode = sharedPreferences.getBoolean("debugMode", false) // Load debug mode state
        debugModeSwitch.isChecked = debugMode // Update switch UI
    }

    // Reset the step count and progress bar
    private fun resetSteps() {
        previousTotalSteps = totalSteps
        saveData()
        stepProgressBar.progress = 0  // Reset progress bar
        stepCountView.text = getString(R.string.step_count, 0)
        caloriesBurnedView.text = getString(R.string.calories_burned, 0f)
        Toast.makeText(this, "Steps reset!", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (stepCount != null) {
            sensorManager.registerListener(this, stepCount, SensorManager.SENSOR_DELAY_UI)
        } else {
            stepCountView.text = getString(R.string.step_count, currentSteps.toInt())
            caloriesBurnedView.text = getString(R.string.calories_burned, caloriesBurned)
            stepProgressBar.visibility = View.GONE
            Toast.makeText(this, "Step Counter Sensor Not Available!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this, stepCount)
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == activityRecognitionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initStepCounter() // Permission granted
            } else {
                Toast.makeText(this, "Permission denied to access step counter", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
