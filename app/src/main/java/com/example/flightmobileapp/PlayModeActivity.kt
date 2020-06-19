package com.example.flightmobileapp

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.play_mode.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Math.toRadians
import java.lang.Thread.sleep
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

class PlayModeActivity : AppCompatActivity(), OnMoveListener {
    private var lastRudder = 0.0
    private var lastThrottle = 0.0
    private var lastAileron = 0.0
    private var lastElevator = 0.0
    private var url: String? = null
    private var keepSending = true
    private var errorId = 0
    private var errorCounter = 0
    private var locker = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.play_mode)
        setSlidersRanges()
        /** Get the url string from the main activity intent */
        url = intent.getStringExtra("url")
        /** Set the given screenshot */
        setScreenshot(MyScreenshot.screenshot)
        setSlidersListeners()
        /** Set the rudder bar to 0 as a start (this is 100 before the adjustment) */
        rudderBar.progress = rudderBar.max / 2
        joystick.setOnMoveListener(this)
    }

    override fun onStop() {
        super.onStop()
        /** Stop getting screenshots */
        keepSending = false
    }

    override fun onResume() {
        super.onResume()
        /** Continue getting screenshots */
        keepSending = true
        GlobalScope.launch {
            getScreenshots()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        /** Stop getting screenshots and delete all errors */
        keepSending = false
        deleteAllErrors()
    }

    private fun getScreenshots() {
        /** Set the screenshot image on success */
        val operate = { image: Bitmap -> setScreenshot(image) }

        /** Show error message on fail */
        val errOperate = { msg: String -> showError(msg) }
        /** While needs to keep sending requests to the server */
        while (keepSending) {
            /** Send a screenshot GET request every 1 second */
            Utils().getScreenshot(url.toString(), operate, errOperate, Utils().screenshotError)
            sleep(1000)
        }
    }

    private fun setScreenshot(screenshot: Bitmap) {
        runOnUiThread {
            /** Set the screenshot image on the view layout */
            screenshotView.setImageBitmap(screenshot)
        }
    }

    private fun updateAileronAndElevator(angle: Int, strength: Int) {
        var changed = false
        val ratio = strength.toDouble() / 100

        /** Get the x and y values from the angle and strength of the joystick */
        val x = kotlin.math.cos(toRadians(angle.toDouble())) * ratio
        val y = kotlin.math.sin(toRadians(angle.toDouble())) * ratio
        /** If the aileron changed more than 1% */
        if (hasChanged(x, lastAileron, 2.0)) {
            changed = true
            lastAileron = x
        }
        /** If the elevator changed more than 1% */
        if (hasChanged(y, lastElevator, 2.0)) {
            changed = true
            lastElevator = y
        }
        /** If something changed more than 1%, send all values to the server */
        if (changed) {
            sendValuesCommand()
        }
    }

    private fun sendValuesCommand() {
        /** Create a new command with all the values and send it to server */
        val command = Command(lastAileron, lastRudder, lastElevator, lastThrottle)
        sendCommand(command)
    }

    private fun sendCommand(command: Command) {
        val gson = GsonBuilder().setLenient().create()
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl(url.toString())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            val api = retrofit.create(Api::class.java)
            /** Send command request to the server */
            api.sendCommand(command).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    /** Show error if the response is invalid */
                    checkResponse(response)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    failureError(t)
                }
            })
        } catch (e: Exception) {
            /** If this is a connection error */
            showError(Utils().connectionError)
        }
    }

    private fun failureError(t: Throwable) {
        /** If this is a timeout failure */
        if (t is SocketTimeoutException) {
            showError(Utils().timeoutError)
        } else {
            showError(Utils().valuesError)
        }
    }

    private fun checkResponse(response: Response<ResponseBody>) {
        /** If the response is not with status 200-300 */
        if (!response.isSuccessful) {
            /** Get the error from the response */
            val msg = Utils().getMessageFromResponse(response)
            /** If failed, show values error, else shows that error */
            if (msg == "") {
                showError(Utils().valuesError)
            } else {
                showError(msg)
            }
        }
    }

    private fun setSlidersRanges() {
        /** Throttle range is [0,1] so set max to 100 progress */
        throttleBar.max = 100
        /** Rudder range is [-1,1] so set max to 200 progress */
        rudderBar.max = 200
    }

    private fun setSlidersListeners() {
        setSliderListener(throttleBar, 0)
        setSliderListener(rudderBar, 1)
    }

    private fun getAbs(value: Double): Double {
        /** Return the absolute value */
        if (value < 0) {
            return value * -1
        }
        return value
    }

    private fun hasChanged(newVal: Double, prevVal: Double, range: Double): Boolean {
        /** If the values are different in more than 1% of the given range */
        if (getAbs(newVal - prevVal) > 0.01 * range) {
            return true
        }
        return false
    }

    private fun setSliderListener(bar: SeekBar, minus: Int) {
        bar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seek: SeekBar,
                progress: Int, fromUser: Boolean
            ) {
                /** Send the values to the server if they changed more than 1% */
                sendValuesIfChanged(progress, minus)
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })
    }

    private fun sendValuesIfChanged(progress: Int, minus: Int) {
        var changed = false

        /** Adjust the progress value to the simulator values range */
        val value = (progress.toDouble() / 100) - minus
        /** If the minus is 1, this is the rudder bar */
        if (minus == 1) {
            /** If the rudder changed in more than 1% */
            if (hasChanged(value, lastRudder, 2.0)) {
                changed = true
                lastRudder = value
            }
        } else {
            /** If the throttle changed in more than 1% */
            if (hasChanged(value, lastThrottle, 1.0)) {
                changed = true
                lastThrottle = value
            }
        }
        /** If something changed more than 1%, send all values to the server */
        if (changed) {
            sendValuesCommand()
        }
    }

    override fun onMove(angle: Int, strength: Int) {
        /** Update these values when the joystick moves */
        updateAileronAndElevator(angle, strength)
    }

    private fun getErrId(): Int = runBlocking {
        var id = 0
        val execute = GlobalScope.launch {
            /** Get the error id with locks */
            locker.lock()
            errorId++
            id = errorId
            locker.unlock()
        }
        execute.join()
        return@runBlocking id
    }

    private fun showError(error: String) {
        val context = this
        val id = getErrId()
        thread(start = true) {
            /** Create a new error text view with the given string */
            runOnUiThread {
                Utils().createNewError(context, error, id, play_mode_layout)
            }
            errorCounter++
            /** Show the error for 3 seconds */
            sleep(Utils().errorSleepMilliseconds)
            val text = findViewById<TextView>(id)
            /** Remove the error text view */
            runOnUiThread {
                play_mode_layout.removeView(text)
            }
            errorCounter--
        }
    }

    private fun deleteAllErrors() {
        var i = 0
        /** Until there are no more errors */
        while (errorCounter > 0) {
            /** Get the error text view and remove it */
            val err = findViewById<TextView>(errorId - i)
            runOnUiThread {
                play_mode_layout.removeView(err)
                errorCounter--
                i++
            }
        }
    }
}
