package com.example.flightmobileapp

interface JoystickListener {
    fun onJoystickMoved (xPercent: Float, yPercent : Float, range : Float)
}