package com.example.flightmobileapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var urlList = arrayListOf<String>()
    private var urlObjectList = arrayListOf<Button>()

    private fun showList() {
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
        showList()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        urlObjectList.addAll(listOf(url0, url1, url2, url3, url4))
        makeUrlsInvisible()
        disableCapitalLetter()
        connectButton.setOnClickListener {
            updateList()
            tryToConnect()
        }
        setButtonsClickEvent()
    }
}