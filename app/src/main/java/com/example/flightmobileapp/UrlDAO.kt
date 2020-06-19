package com.example.flightmobileapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UrlDAO {
    @Insert
    suspend fun saveUrl(url: MyUrlEntity)

    @Query("SELECT * FROM MyUrlEntity")
    suspend fun readUrl(): List<MyUrlEntity>
}
