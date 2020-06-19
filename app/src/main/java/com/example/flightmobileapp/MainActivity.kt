package com.example.flightmobileapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.lang.Thread.sleep
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private var urlList = arrayListOf<String>()
    private var urlObjectList = arrayListOf<Button>()
    private var errorId = 0
    private var errorCounter = 0
    private var locker = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /** Create a url objects list */
        urlObjectList.addAll(listOf(url0, url1, url2, url3, url4))
        disableCapitalLetter()
        /** Populate the list with urls and show it */
        joinPopulate()
        showList()
        /** When the connect button is clicked */
        connectButton.setOnClickListener {
            updateList()
            showList()
            tryToConnect()
        }
        /** When the clear button is clicked */
        clearButton.setOnClickListener {
            urlList.clear()
            showList()
        }
        setButtonsClickEvent()
    }

    private fun joinPopulate() = runBlocking {
        /** Wait for the execute */
        val execute = GlobalScope.launch {
            populateList()
        }
        execute.join()
    }

    private suspend fun populateList() {
        val db = Room.databaseBuilder(applicationContext, MyRoom::class.java, "urlsDB").build()
        urlList.clear()
        var i = 0
        /** Get all the urls from the database */
        db.urlDAO().readUrl().forEach {
            urlList.add(it.urlId, it.url)
            i++
        }
    }

    private suspend fun saveDB() {
        val db = Room.databaseBuilder(applicationContext, MyRoom::class.java, "urlsDB").build()
        /** Clear the previous urls */
        db.clearAllTables()
        /** Save all the current urls */
        for ((i, url) in urlList.withIndex()) {
            val entity = MyUrlEntity()
            entity.urlId = i
            entity.url = url
            db.urlDAO().saveUrl(entity)
        }
    }

    override fun onPause() {
        super.onPause()
        CoroutineScope(Dispatchers.IO).launch {
            /** Save all the current urls */
            saveDB()
        }
    }

    private fun showList() {
        /** Set all urls to invisible as a start */
        makeUrlsInvisible()
        /** Set the relevant url box with the url string */
        for ((i, item: String) in urlList.withIndex()) {
            when (i) {
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
        }
    }

    private fun deleteAllErrors() {
        var i = 0
        /** Until there are no more errors */
        while (errorCounter > 0) {
            /** Get the error text view and remove it */
            val err = findViewById<TextView>(errorId - i)
            runOnUiThread {
                main_layout.removeView(err)
                errorCounter--
                i++
            }
        }
    }

    private fun tryToConnect() {
        val url = insertBox.text.toString()

        /** If the connection succeeded, go to the play mode activity */
        val operate = { image: Bitmap ->
            MyScreenshot.screenshot = image
            val intent = Intent(this@MainActivity, PlayModeActivity::class.java)
            intent.putExtra("url", url)
            startActivity(intent)
            deleteAllErrors()
        }

        /** If something failed, show the error message */
        val errOperate = { msg: String -> showError(msg) }
        /** Trying to get a screenshot by connecting to the server */
        Utils().getScreenshot(url, operate, errOperate, Utils().connectionError)
    }

    private fun updateList() {
        val url = insertBox.text.toString()
        /** Remove this url if its already exist */
        if (urlList.contains(url)) {
            urlList.remove(url)
        }
        urlList.add(0, url)
        /** Delete the last url if there are more than 5 */
        if (urlList.size == 6) {
            urlList.removeAt(5)
        }
    }

    private fun makeUrlsInvisible() {
        for (button: Button in urlObjectList) {
            button.visibility = View.INVISIBLE
        }
    }

    private fun disableCapitalLetter() {
        for (button: Button in urlObjectList) {
            button.transformationMethod = null
        }
    }

    private fun setButtonsClickEvent() {
        for (button: Button in urlObjectList) {
            button.setOnClickListener {
                changeUrlText(button)
            }
        }
    }

    private fun changeUrlText(button: Button) {
        if (button.visibility == View.VISIBLE) {
            insertBox.setText(button.text.toString())
        }
    }

    private fun getErrId(): Int = runBlocking {
        var id = 0
        val execute = GlobalScope.launch {
            /** Get the error id with locks */
            locker.lock()
            errorId++
            id = errorId
            locker.unlock()
        }
        execute.join()
        return@runBlocking id
    }

    private fun showError(error: String) {
        val context = this
        val id = getErrId()
        thread(start = true) {
            /** Create a new error text view with the given string */
            runOnUiThread {
                Utils().createNewError(context, error, id, main_layout)
            }
            errorCounter++
            /** Show the error for 3 seconds */
            sleep(Utils().errorSleepMilliseconds)
            val text = findViewById<TextView>(id)
            /** Remove the error text view */
            runOnUiThread {
                main_layout.removeView(text)
            }
            errorCounter--
        }
    }
}
