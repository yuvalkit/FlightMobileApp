package com.example.flightmobileapp

import android.graphics.Bitmap
import java.io.Serializable

class MyScreenshot(screenshotInput : Bitmap) : Serializable {
    companion object {
        lateinit var screenshot: Bitmap
    }
    init {
        screenshot = screenshotInput
    }
}