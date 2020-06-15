package com.example.flightmobileapp

import androidx.room.Database
import androidx.room.RoomDatabase

@Database (entities = [(MyUrlEntity::class)], version = 1)
abstract class MyRoom : RoomDatabase(){
    abstract fun urlDAO() : UrlDAO

}