package com.example.flightmobileapp

import com.google.gson.annotations.SerializedName

data class Command(
    @SerializedName("aileron") var aileron: Double,
    @SerializedName("rudder") var rudder: Double,
    @SerializedName("elevator") var elevator: Double,
    @SerializedName("throttle") var throttle: Double
)
