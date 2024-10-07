package com.example.steptracker

import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.button.MaterialButton
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
import androidx.appcompat.app.AlertDialog
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
    private var stepGoal: Int = 10000 // Default step goal
    private val activityRecognitionRequestCode = 101
    private var currentSteps: Float = 0f
    private var caloriesBurned: Float = 0f
    private var debugMode: Boolean = false // This will be toggled via the Switch

    // UI Elements
    private lateinit var stepCountView: TextView
    private lateinit var caloriesBurnedView: TextView
    private lateinit var stepProgressBar: ProgressBar
    private lateinit var resetButton: MaterialButton
    private lateinit var setStepGoalButton: MaterialButton // Button to set step goal
    private lateinit var debugModeSwitch: SwitchMaterial // Switch for Debug Mode
    private lateinit var progressPercentageView: TextView // View to display progress percentage
    private lateinit var currentStepGoalView: TextView // View to display the current step goal

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
        setStepGoalButton = findViewById(R.id.setStepGoalButton) // Initialize the set step goal button
        debugModeSwitch = findViewById(R.id.debugModeSwitch) // Initialize Debug Mode Switch
        progressPercentageView = findViewById(R.id.progressPercentageView) // Initialize progress percentage view
        currentStepGoalView = findViewById(R.id.currentStepGoalView) // View to display current step goal

        // Set progress bar max steps to stepGoal from SharedPreferences
        loadStepGoal()
        stepProgressBar.max = stepGoal
        currentStepGoalView.text = "Current Step Goal: $stepGoal" // Set initial step goal text

        // Set up the Debug Mode Switch behavior
        debugModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            debugMode = isChecked
            if (debugMode) {
                Toast.makeText(this, "Debug mode enabled: Simulating 5000 steps", Toast.LENGTH_SHORT).show()
                simulateSteps()
            } else {
                Toast.makeText(this, "Debug mode disabled: Real sensor data", Toast.LENGTH_SHORT).show()
                registerSensorListener()
            }
            saveDebugModeState() // Save the debug mode state
        }

        // Handle the custom step goal input dialog
        setStepGoalButton.setOnClickListener {
            showSetStepGoalDialog() // Open dialog to set step goal
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
            if (!debugMode) {
                registerSensorListener()
            }
        } else {
            Toast.makeText(this, "Step Counter Sensor Not Available!", Toast.LENGTH_SHORT).show()
        }
    }

    // Register sensor listener only if not in debug mode
    private fun registerSensorListener() {
        sensorManager.registerListener(this, stepCount, SensorManager.SENSOR_DELAY_UI)
    }

    // Simulate steps in debug mode
    private fun simulateSteps() {
        lifecycleScope.launch(Dispatchers.Default) {
            currentSteps = 5000f // Simulate 5000 steps in debug mode
            Log.d("StepTracker", "Debug mode: Simulating 5000 steps")

            caloriesBurned = currentSteps * stepToCalorieConversionRate // Calculate calories

            withContext(Dispatchers.Main) {
                updateUI()
            }
        }
    }

    // Update the UI elements
    private fun updateUI() {
        stepProgressBar.max = stepGoal  // Update goal max steps

        // Animate progress bar smoothly
        val progressAnimator = ObjectAnimator.ofInt(stepProgressBar, "progress", stepProgressBar.progress, currentSteps.toInt())
        progressAnimator.duration = 1500  // 1.5 second animation
        progressAnimator.interpolator = DecelerateInterpolator()
        progressAnimator.start()

        stepCountView.text = getString(R.string.step_count, currentSteps.toInt())  // Steps: [steps]
        caloriesBurnedView.text = getString(R.string.calories_burned, caloriesBurned)  // Calories: [calories]

        updateProgressPercentage() // Update progress percentage

        // Save the current steps for future sessions
        saveData()
    }

    // Update the progress percentage
    private fun updateProgressPercentage() {
        val progressPercentage = (currentSteps / stepGoal) * 100
        progressPercentageView.text = String.format("%.2f%%", progressPercentage)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (debugMode) return // Skip updating steps if in debug mode

        lifecycleScope.launch(Dispatchers.Default) {
            if (event != null) {
                totalSteps = event.values[0]
                currentSteps = totalSteps - previousTotalSteps
                Log.d("StepTracker", "Real sensor data: $currentSteps steps")
            }

            caloriesBurned = currentSteps * stepToCalorieConversionRate

            withContext(Dispatchers.Main) {
                updateUI()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this case
    }

    // Save the current steps, debug mode, and step goal to SharedPreferences
    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("previousTotalSteps", previousTotalSteps)
        editor.putBoolean("debugMode", debugMode) // Save debug mode state
        editor.putInt("stepGoal", stepGoal) // Save step goal
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

    // Load the step goal from SharedPreferences
    private fun loadStepGoal() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        stepGoal = sharedPreferences.getInt("stepGoal", 10000) // Load or default to 10000 steps
        currentStepGoalView.text = "Current Step Goal: $stepGoal" // Update current step goal view
    }

    // Show dialog to set step goal
    private fun showSetStepGoalDialog() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Enter your step goal"

        AlertDialog.Builder(this)
            .setTitle("Set Step Goal")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newGoal = input.text.toString().toIntOrNull()
                if (newGoal != null && newGoal > 0) {
                    stepGoal = newGoal
                    stepProgressBar.max = stepGoal // Update progress bar max
                    currentStepGoalView.text = "Current Step Goal: $stepGoal" // Update the step goal view
                    Toast.makeText(this, "Step goal set to $stepGoal steps", Toast.LENGTH_SHORT).show()
                    saveData() // Save the step goal
                } else {
                    Toast.makeText(this, "Invalid step goal", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Add new functionality for history tracking
    private fun logDailySteps() {
        val sharedPreferences = getSharedPreferences("stepHistory", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Get today's date
        val calendar = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)


        // Save today's steps in the history
        editor.putFloat(today, currentSteps)
        editor.apply()

        Toast.makeText(this, "Today's steps logged!", Toast.LENGTH_SHORT).show()
    }

    // Function to view step history
    private fun showStepHistory() {
        val sharedPreferences = getSharedPreferences("stepHistory", Context.MODE_PRIVATE)

        val allEntries = sharedPreferences.all
        val historyBuilder = StringBuilder()

        for ((date, steps) in allEntries) {
            historyBuilder.append("$date: ${steps as Float} steps\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Step History")
            .setMessage(historyBuilder.toString())
            .setPositiveButton("OK", null)
            .show()
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
        if (stepCount != null && !debugMode) {
            registerSensorListener()
        } else if (debugMode) {
            simulateSteps() // Simulate steps if in debug mode
        }
    }

    override fun onPause() {
        super.onPause()
        if (!debugMode) {
            sensorManager.unregisterListener(this, stepCount)
        }
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
