package com.example.steptracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCount: Sensor? = null
    private var totalSteps: Float = 0f
    private var previousTotalSteps: Float = 0f
    private val ACTIVITY_RECOGNITION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check and request the Activity Recognition permission for Android 10 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), ACTIVITY_RECOGNITION_REQUEST_CODE)
            } else {
                // Permission already granted, initialize step tracking
                initStepCounter()
            }
        } else {
            // No need for the permission on devices below Android 10
            initStepCounter()
        }
    }

    // Initialize step counter after permission is granted
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
        if (event != null) {
            totalSteps = event.values[0]
            val currentSteps = totalSteps - previousTotalSteps
            // You can update the UI or display the steps here (e.g., in a TextView)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle changes in sensor accuracy here
    }

    private fun resetSteps() {
        previousTotalSteps = totalSteps
        // Save this value so that steps are reset properly when the app restarts
    }

    override fun onResume() {
        super.onResume()
        // Re-register sensor listener when the activity is resumed
        if (stepCount != null) {
            sensorManager.registerListener(this, stepCount, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the sensor listener to conserve resources when activity is paused
        sensorManager.unregisterListener(this, stepCount)
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize step tracking
                initStepCounter()
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Permission denied to access step counter", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
