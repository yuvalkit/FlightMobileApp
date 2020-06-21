package com.example.flightmobileapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.gson.GsonBuilder
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.SocketTimeoutException

class Utils {
    /** All error messages */
    var connectionError = "Failed connecting to server"
    val screenshotError = "Failed getting screenshot from server"
    val valuesError = "Failed sending values to server"
    val timeoutError = "Timeout - the server is not responding"
    val invalidScreenshotError = "Got invalid screenshot"
    val errorSleepMilliseconds = 3000L

    fun createNewError(context: Context, error: String, id: Int, layout: ConstraintLayout) {
        /** Create a text view with the given error */
        val textView = createTextView(context, error, id)
        /** Add the text view to the given layout */
        layout.addView(textView)
        val set = ConstraintSet()
        set.clone(layout)
        /** Set the text view constraints */
        set.connect(
            id, ConstraintSet.LEFT,
            ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0
        )
        set.connect(
            id, ConstraintSet.RIGHT,
            ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0
        )
        set.connect(
            id, ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0
        )
        set.connect(
            id, ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0
        )
        set.applyTo(layout)
    }

    private fun createTextView(context: Context, error: String, id: Int): TextView {
        val textView = TextView(context)
        val params: ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(
            /** Text view width */
            1000,
            /** Text view height */
            400
        )
        /** Set text view parameters */
        textView.id = id
        textView.gravity = Gravity.CENTER
        textView.layoutParams = params
        textView.text = error
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F)
        textView.setTextColor(Color.WHITE)
        textView.setBackgroundColor(Color.RED)
        textView.setTypeface(textView.typeface, Typeface.BOLD_ITALIC)
        textView.typeface = Typeface.MONOSPACE
        textView.elevation = 10f
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        return textView
    }

    @Throws(Exception::class)
    fun getScreenshot(
        api : Api,
        function: (Bitmap) -> Unit,
        errOperate: (String) -> Unit,
        errMsg: String
    ) {
        try {
            /** Send GET screenshot request */
            api.getScreenshot().enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    /** Analyze the received response */
                    analyzeResponse(response, function, errOperate, errMsg)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    failureError(t, errOperate, errMsg)
                }
            })
        } catch (e: Exception) {
            errOperate(connectionError)
        }
    }

    private fun analyzeResponse(
        response: Response<ResponseBody>,
        function: (Bitmap) -> Unit,
        errOperate: (String) -> Unit,
        errMsg: String
    ) {
        /** If the response is with 200-300 status */
        if (response.isSuccessful) {
            /** Get the image from the response */
            val stream = response.body()?.byteStream()
            val bitmapImage = BitmapFactory.decodeStream(stream)
            /** If this is a valid image */
            if (bitmapImage is Bitmap) {
                function(bitmapImage)
            } else {
                errOperate(invalidScreenshotError)
            }
        } else {
            /** Get the error message from the response */
            val msg = getMessageFromResponse(response)
            if (msg == "") {
                errOperate(errMsg)
            } else {
                errOperate(msg)
            }
        }
    }

    private fun failureError(t: Throwable, errOperate: (String) -> Unit, errMsg: String) {
        /** If this is a timeout failure */
        if (t is SocketTimeoutException) {
            errOperate(timeoutError)
        } else {
            errOperate(errMsg)
        }
    }

    fun getMessageFromResponse(response: Response<ResponseBody>): String {
        /** Getting the message string from the given response */
        val reader: BufferedReader?
        val sb = StringBuilder()
        try {
            reader =
                BufferedReader(
                    InputStreamReader(
                        response.errorBody()!!.byteStream()
                    )
                )
            var line: String?
            try {
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
            } catch (e: IOException) {
                return ""
            }
        } catch (e: IOException) {
            return ""
        }
        return sb.toString()
    }
}
