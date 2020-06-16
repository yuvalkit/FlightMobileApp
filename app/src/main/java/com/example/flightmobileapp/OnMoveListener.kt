package com.example.flightmobileapp

/**
 * Interface definition for a callback to be invoked when a
 * JoystickView's button is moved
 */
interface OnMoveListener {
    /**
     * Called when a JoystickView's button has been moved
     * @param angle current angle
     * @param strength current strength
     */
    fun onMove(angle: Int, strength: Int)
}