package com.example.flightmobileapp

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class Utils {
    public fun createNewError(context : Context, error : String, id : Int, layout : ConstraintLayout) {
        val textView: TextView = TextView(context)
        var params : ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(
            1000, // This will define text view width
            400 // This will define text view height
        )
        textView.id = id
        textView.gravity = Gravity.CENTER
        textView.layoutParams = params
        textView.text = error
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,20F)
        textView.setTextColor(Color.WHITE)
        textView.setBackgroundColor(Color.RED)
        textView.setTypeface(textView.typeface, Typeface.BOLD_ITALIC)
        textView.typeface = Typeface.MONOSPACE
        textView.elevation = 10f
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        layout.addView(textView)
        val set = ConstraintSet()
        set.clone(layout)
        set.connect(id, ConstraintSet.LEFT,
            ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0)
        set.connect(id, ConstraintSet.RIGHT,
            ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0)
        set.connect(id, ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        set.connect(id, ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        set.applyTo(layout)
    }
}