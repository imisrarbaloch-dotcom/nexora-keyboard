package com.nexora.keyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SetupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B0720"))
            setPadding(48, 48, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "◈ Nexora Keyboard ◈"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#7DF9FF"))
        })

        root.addView(Button(this).apply {
            text = "1. Keyboard enable karo"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        })

        root.addView(Button(this).apply {
            text = "2. Nexora select karo"
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        })

        setContentView(root)
    }
}
