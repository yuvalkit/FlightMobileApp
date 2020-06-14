package com.example.flightmobileapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.core.view.isVisible
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.lang.Thread.sleep

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
        var url = insertBox.text.toString()
        var screenshot = "${url}/screenshot"

        startActivity(Intent(this, PlayModeActivity::class.java))
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