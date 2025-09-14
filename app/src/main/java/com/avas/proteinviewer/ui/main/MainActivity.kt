package com.avas.proteinviewer.ui.main

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * Main Activity - equivalent to iPhone ContentView
 */
class MainActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple TextView
        val textView = TextView(this).apply {
            text = "Protein Viewer - Android Version\nHello World!"
            textSize = 18f
        }
        
        setContentView(textView)
    }
}