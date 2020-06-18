package com.example.flightmobileapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.room.Room
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.play_mode.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Thread.sleep
import java.lang.reflect.InvocationTargetException
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private var urlList = arrayListOf<String>()
    private var urlObjectList = arrayListOf<Button>()
    private var errorId = 0
    private var errorCounter = 0
    private var locker = Mutex()
    private var dbLocker = Mutex()

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

    override fun onPause() {
        super.onPause()
        Log.d("EA", "main pause")
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

    private fun deleteAllErrors() {
        var i = 0
        while(errorCounter > 0) {
            var err = findViewById<TextView>(errorId - i)
            runOnUiThread {
                main_layout.removeView(err)
                errorCounter--
                i++
            }
        }
    }

    private fun tryToConnect(){
        var error = "Failed trying connect with server"
        var url = insertBox.text.toString()

        var operate = {image : Bitmap ->
            MyScreenshot.screenshot = image
            var intent = Intent(this@MainActivity, PlayModeActivity::class.java)
            intent.putExtra("url", url)
            startActivity(intent)
            deleteAllErrors()
        }

        var errOperate = {msg : String -> showError(msg)}
        Utils().getScreenshot(url, operate, errOperate, error)
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

    private fun getErrId() : Int = runBlocking<Int> {
        var id = 0
        var execute = GlobalScope.launch {
            locker.lock()
            errorId++
            id = errorId
            locker.unlock()
        }
        execute.join()
        return@runBlocking id
    }

    private fun showError(error : String) {
        var context = this
        var id = getErrId()
        thread(start = true) {
            runOnUiThread {
                Utils().createNewError(context, error, id, main_layout)
            }
            errorCounter++
            sleep(3000)
            var text = findViewById<TextView>(id)
            runOnUiThread {
                main_layout.removeView(text)
            }
            errorCounter--
        }
    }


}