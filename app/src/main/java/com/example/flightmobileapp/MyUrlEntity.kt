package com.example.flightmobileapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class MyUrlEntity {
    @PrimaryKey
    var urlId : Int = 0

    @ColumnInfo(name = "url")
    var url : String = ""
}