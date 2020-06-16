package com.example.flightmobileapp

import android.graphics.Bitmap
import java.io.Serializable

class MyImage(image : Bitmap) : Serializable {
    var screenshot = image
}