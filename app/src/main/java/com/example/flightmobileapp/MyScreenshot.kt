package com.example.flightmobileapp

import android.graphics.Bitmap

class MyScreenshot(screenshotInput: Bitmap) {
    companion object {
        lateinit var screenshot: Bitmap
    }

    init {
        screenshot = screenshotInput
    }
}
