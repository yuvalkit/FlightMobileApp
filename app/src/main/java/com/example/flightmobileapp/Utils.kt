package com.example.flightmobileapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
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
import java.lang.Exception
import java.lang.reflect.InvocationTargetException

class Utils {
    fun createNewError(context : Context, error : String, id : Int, layout : ConstraintLayout) {
        val textView: TextView = TextView(context)
        var params : ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(
            1000, // This will define text view width
            400 // This will define text view height
        )
        textView.id = id
        textView.gravity = Gravity.CENTER
        textView.layoutParams = params
        textView.text = error
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,20F)
        textView.setTextColor(Color.WHITE)
        textView.setBackgroundColor(Color.RED)
        textView.setTypeface(textView.typeface, Typeface.BOLD_ITALIC)
        textView.typeface = Typeface.MONOSPACE
        textView.elevation = 10f
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        layout.addView(textView)
        val set = ConstraintSet()
        set.clone(layout)
        set.connect(id, ConstraintSet.LEFT,
            ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0)
        set.connect(id, ConstraintSet.RIGHT,
            ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0)
        set.connect(id, ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        set.connect(id, ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        set.applyTo(layout)
    }

    @Throws(Exception::class)
    fun getScreenshot(url :String, function : (Bitmap) -> Unit,errOperate : () -> Unit) {
        try {
            val gson = GsonBuilder() .setLenient() .create()
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            val api = retrofit.create(Api::class.java)
            api.getScreenshot().enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if(response.isSuccessful) {
                        val stream = response?.body()?.byteStream()
                        var bitmapImage = BitmapFactory.decodeStream(stream)
                        if (bitmapImage is Bitmap) {
                            function(bitmapImage)
                        } else {
                            Log.d("EA", "fail 1")
                            errOperate()
                        }
                    } else {
                        Log.d("EA", "fail 2")
                        errOperate()
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d("EA", "fail 3")
                    errOperate()
                }
            })
        }
        catch (e : Exception) {
            errOperate()
        }
    }
}