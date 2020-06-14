package com.example.flightmobileapp

import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.play_mode.*
import java.lang.Math.*


class PlayModeActivity : AppCompatActivity() {
    private var lastRudder = 100.0
    private var lastThrottle = 100.0
    private var lastAileron = 100.0
    private var lastElevator = 100.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.play_mode)
        setSlidersRanges()
        setSlidersListeners()
        joystick.setOnMoveListener { angle, strength ->
            updateAileronAndElevator(angle, strength)
        }
    }

    private fun updateAileronAndElevator(angle : Int, strength : Int) {
        var ratio = strength.toDouble() / 100
        var x = kotlin.math.cos(toRadians(angle.toDouble())) * ratio
        var y = kotlin.math.sin(toRadians(angle.toDouble())) * ratio
        if(hasChanged(x, lastAileron, 2.0)) {
            Log.d("EA", "aileron is changed to ${x}")
            lastAileron = x
        }
        if(hasChanged(y, lastElevator, 2.0)) {
            Log.d("EA", "elevator is changed to ${y}")
            lastElevator = y
        }
    }

    private fun setSlidersRanges() {
        throttleBar.max = 100
        rudderBar.max = 200
    }

    private fun setSlidersListeners() {
        setSliderListener(throttleBar, 0)
        setSliderListener(rudderBar, 1)
    }

    private fun getAbs(value : Double) : Double {
        if(value < 0) {
            return value * -1
        }
        return value
    }

    private fun hasChanged(newVal : Double, prevVal : Double, range : Double) : Boolean {
        if(getAbs(newVal - prevVal) > 0.01 * range) {
            return true
        }
        return false
    }

    private fun setSliderListener(bar : SeekBar, minus : Int) {
        bar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seek: SeekBar,
                progress: Int, fromUser: Boolean
            ) {
                val value = (progress.toDouble()/100) - minus
                // the bar is rudderBar
                if(minus == 1) {
                    if(hasChanged(value, lastRudder, 2.0)) {
                        Log.d("EA", "rudder is changed to ${value.toString()}")
                        lastRudder = value
                    }
                } else {
                    if(hasChanged(value, lastThrottle, 1.0)) {
                        Log.d("EA", "throttle is changed to ${value.toString()}")
                        lastThrottle = value
                    }
                }

            }

            override fun onStartTrackingTouch(seek: SeekBar) {}

            override fun onStopTrackingTouch(seek: SeekBar) {
            }
        })
    }
}