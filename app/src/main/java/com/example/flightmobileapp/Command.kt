package com.example.flightmobileapp

import com.google.gson.annotations.SerializedName

data class Command (
    @SerializedName("aileron") var Aileron: Double,
    @SerializedName("rudder") var Rudder: Double,
    @SerializedName("elevator") var Elevator: Double,
    @SerializedName("throttle") var Throttle: Double
)