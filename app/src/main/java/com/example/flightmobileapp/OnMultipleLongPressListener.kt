package com.example.flightmobileapp

/**
 * Interface definition for a callback to be invoked when a JoystickView
 * is touched and held by multiple pointers.
 */
interface OnMultipleLongPressListener {
    /**
     * Called when a JoystickView has been touch and held enough time by multiple pointers.
     */
    fun onMultipleLongPress()
}