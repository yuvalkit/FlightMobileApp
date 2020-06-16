package com.example.flightmobileapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private var urlList = arrayListOf<String>()
    private var urlObjectList = arrayListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlObjectList.addAll(listOf(url0, url1, url2, url3, url4))
        disableCapitalLetter()
        joinPopulate()
        showList()
        connectButton.setOnClickListener {
            Log.d("EA","clicked")
            updateList()
            showList()
            tryToConnect()
        }
        clearButton.setOnClickListener {
            urlList.clear()
            showList()
        }
        setButtonsClickEvent()
    }

    private fun joinPopulate()  = runBlocking<Unit> {
        var execute = GlobalScope.launch {
            populateList()
        }
        execute.join()
    }

    private suspend fun populateList() {
        var db = Room.databaseBuilder(applicationContext, MyRoom::class.java, "urlsDB").build()
        urlList.clear()
        var i = 0
        db.urlDAO().readUrl().forEach() {
            urlList.add(it.urlId, it.url)
            i++
        }
        Log.d("EA", "read ${i} objects")
    }

    private suspend fun saveDB() {
        var db = Room.databaseBuilder(applicationContext, MyRoom::class.java, "urlsDB").build()
        db.clearAllTables()
        var i = 0
        for (url in urlList) {
            var entity = MyUrlEntity()
            entity.urlId = i
            entity.url = url
            db.urlDAO().saveUrl(entity)
            i++
        }
        Log.d("EA", "entered ${i} objects")
    }

    override fun onStop() {
        super.onStop()
        CoroutineScope(Dispatchers.IO).launch {
            saveDB()
        }
    }

    private fun showList() {
        makeUrlsInvisible()
        var i = 0
        for (item: String in urlList) {
            when(i) {
                0 -> {
                    url0.text = item
                    url0.visibility = View.VISIBLE
                }
                1 -> {
                    url1.text = item
                    url1.visibility = View.VISIBLE
                }
                2 -> {
                    url2.text = item
                    url2.visibility = View.VISIBLE
                }
                3 -> {
                    url3.text = item
                    url3.visibility = View.VISIBLE
                }
                4 -> {
                    url4.text = item
                    url4.visibility = View.VISIBLE
                }
            }
            i++
        }
    }

    private fun tryToConnect(){
        var successFlag = false
        //var url = insertBox.text.toString()
        var url = "http://10.0.2.2:50242"
        Log.d("EA", "before. url is ${url}")
        val gson = GsonBuilder() .setLenient() .create()
        try {
            Log.d("EA", url)
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            Log.d("EA", "after retrofit")
            val api = retrofit.create(Api::class.java)
            val body = api.getScreenshot().enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("EA", "in response")
                    val stream = response?.body()?.byteStream()
                    Log.d("EA", "after stream")
                    var bitmapImage = BitmapFactory.decodeStream(stream)

                    Log.d("EA", "after bitmap")
                    if(bitmapImage is Bitmap) {
                        var image = MyImage(bitmapImage)
                        val intent = Intent(this@MainActivity, PlayModeActivity::class.java)
                        var b = Bundle()
                        b.putSerializable("image", image)
                        intent.putExtras(b)
                        startActivity(intent)
                    } else {
                        throw Exception()
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d("EA", "failed. respones is ${t}")
                }
            })
        }
        catch(e: Exception) {
            Log.d("EA", "in catch. failed trying getting image")
        }
    }

    private fun updateList() {
        var url = insertBox.text.toString()
        if(urlList.contains(url)) {
            urlList.remove(url)
        }
        urlList.add(0,url)
        if(urlList.size == 6) {
            urlList.removeAt(5);
        }
    }

    private fun makeUrlsInvisible() {
        for(button: Button in urlObjectList) {
            button.visibility = View.INVISIBLE
        }
    }

    private fun disableCapitalLetter(){
        for(button: Button in urlObjectList) {
            button.transformationMethod = null
        }
    }

    private fun setButtonsClickEvent(){
        for(button: Button in urlObjectList) {
            button.setOnClickListener {
                changeUrlText(button)
            }
        }
    }

    private fun changeUrlText(button : Button) {
        if(button.visibility == View.VISIBLE) {
            insertBox.setText(button.text.toString())
        }
    }
}