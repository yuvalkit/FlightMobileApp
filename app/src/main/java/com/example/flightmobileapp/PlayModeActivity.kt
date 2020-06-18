package com.example.flightmobileapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
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
    private var url : String? = null
    private var connected = true
    private var errorId = 0
    private var errorCounter = 0
    private var locker = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.play_mode)
        setSlidersRanges()
        url = intent.getStringExtra("url")
        setScreenshot(MyScreenshot.screenshot)
        setSlidersListeners()
        rudderBar.progress = rudderBar.max / 2
        joystick.setOnMoveListener(this)
    }

    override fun onStop() {
        super.onStop()
        Log.d("EA", "play stop")
        connected = false
    }

    override fun onResume() {
        super.onResume()
        Log.d("EA", "resume")
        connected = true
        GlobalScope.launch {
            getScreenshots()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.d("EA", "back")
        connected = false
        deleteAllErrors()
    }

    private fun getScreenshots() {
        var error = "Failed getting screenshot"
        var operate = { image: Bitmap -> setScreenshot(image) }
        var errOperate = { msg : String -> showError(msg) }
        while (connected) {
            Utils().getScreenshot(url.toString(), operate, errOperate, error)
            sleep(1000)
        }
    }

    private fun setScreenshot(screenshot : Bitmap) {
        runOnUiThread {
            screenshotView.setImageBitmap(screenshot)
        }
    }

    private fun updateAileronAndElevator(angle : Int, strength : Int) {
        var changed = false
        var ratio = strength.toDouble() / 100
        var x = kotlin.math.cos(toRadians(angle.toDouble())) * ratio
        var y = kotlin.math.sin(toRadians(angle.toDouble())) * ratio
        if(hasChanged(x, lastAileron, 2.0)) {
            changed = true
            Log.d("EA", "aileron is changed to ${x}")
            lastAileron = x
        }
        if(hasChanged(y, lastElevator, 2.0)) {
            changed = true
            Log.d("EA", "elevator is changed to ${y}")
            lastElevator = y
        }
        if (changed) {
            sendValuesCommand()
        }
    }

    private fun sendValuesCommand() {
        var command = Command(lastAileron, lastRudder, lastElevator, lastThrottle)
        sendCommand(command)
    }

    private fun sendCommand(command : Command) {
        var error = "Failed trying sending values to simulator"
        var timeout = "Timeout - The Simulator Is Not Responding"
        val gson = GsonBuilder() .setLenient() .create()
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            val api = retrofit.create(Api::class.java)
            api.sendCommand(command).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if(!response.isSuccessful) {
                        var msg = Utils().createMsgError(response)
                        if(msg == "") {
                            showError(error)
                        } else {
                            showError(msg)
                        }
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d("EA", "fail send 2")
                    if(t is SocketTimeoutException) {
                        showError(timeout)
                    } else {
                        showError(error)
                    }
                }
            })
        }
        catch(e: Exception) {
            Log.d("EA", "fail send 3")
            showError("Failed trying to connect with server")
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
                var changed = false
                val value = (progress.toDouble()/100) - minus
                // the bar is rudderBar
                if(minus == 1) {
                    if(hasChanged(value, lastRudder, 2.0)) {
                        changed = true
                        Log.d("EA", "rudder is changed to ${value.toString()}")
                        lastRudder = value
                    }
                } else {
                    if(hasChanged(value, lastThrottle, 1.0)) {
                        changed = true
                        Log.d("EA", "throttle is changed to ${value.toString()}")
                        lastThrottle = value
                    }
                }
                if (changed) {
                    sendValuesCommand()
                }

            }

            override fun onStartTrackingTouch(seek: SeekBar) {}

            override fun onStopTrackingTouch(seek: SeekBar) {
            }
        })
    }

    override fun onMove(angle: Int, strength: Int) {
        updateAileronAndElevator(angle, strength)
    }

    private fun getErrId() : Int = runBlocking<Int> {
        var id = 0
        var execute = GlobalScope.launch {
            locker.lock()
            errorId++
            id = errorId
            locker.unlock()
        }
        execute.join()
        return@runBlocking id
    }

    private fun showError(error : String) {
        var context = this
        var id = getErrId()
        thread(start = true) {
            runOnUiThread {
                Utils().createNewError(context, error, id, play_mode_layout)
            }
            errorCounter++
            sleep(3000)
            var text = findViewById<TextView>(id)
            runOnUiThread {
                play_mode_layout.removeView(text)
            }
            errorCounter--
        }
    }

    private fun deleteAllErrors() {
        var i = 0
        while(errorCounter > 0) {
            var err = findViewById<TextView>(errorId - i)
            runOnUiThread {
                play_mode_layout.removeView(err)
                errorCounter--
                i++
            }
        }
    }
}