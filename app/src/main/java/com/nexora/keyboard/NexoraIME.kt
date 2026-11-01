package com.nexora.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class NexoraIME : InputMethodService() {

    private val nexoraSymbols = listOf(
        "◈", "⟁", "⌬", "⟡",
        "⊗", "⧖", "⌖", "⋇",
        "⟊", "⧫", "⋉", "◉",
        "∇", "∫", "Σ", "Δ"
    )

    // Black theme colors
    private val colBoard = Color.parseColor("#000000")
    private val colKey = Color.parseColor("#101010")
    private val colKeyPressed = Color.parseColor("#1B2B45")
    private val colStroke = Color.parseColor("#2A2A2A")
    private val colStrokePressed = Color.parseColor("#2F7BFF")
    private val colText = Color.parseColor("#FFFFFF")

    private val audio by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun soundOn(): Boolean =
        getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_SOUND, true)

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colBoard)
            setPadding(dp(6), dp(8), dp(6), dp(8))
        }

        nexoraSymbols.chunked(4).forEach { row ->
            val line = newRow()
            row.forEach { s ->
                line.addView(newKey(s, 1f, AudioManager.FX_KEYPRESS_STANDARD) { type(s) })
            }
            root.addView(line)
        }

        val bottom = newRow()
        bottom.addView(newKey("⌫", 1f, AudioManager.FX_KEYPRESS_DELETE) { backspace() })
        bottom.addView(newKey("␣", 2f, AudioManager.FX_KEYPRESS_SPACEBAR) { type(" ") })
        bottom.addView(newKey("⏎", 1f, AudioManager.FX_KEYPRESS_RETURN) { enter() })
        root.addView(bottom)

        return root
    }

    private fun newRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
        )
    }

    private fun newKey(label: String, weight: Float, soundFx: Int, onTap: () -> Unit): TextView {
        val bg = GradientDrawable().apply {
            setColor(colKey)
            setStroke(dp(1), colStroke)
            cornerRadius = dp(10).toFloat()
        }
        return TextView(this).apply {
            text = label
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(colText)
            background = bg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight)
                .apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }

            // Light press animation + touch sound. Returns false so the click still fires.
            setOnTouchListener { v, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        bg.setColor(colKeyPressed)
                        bg.setStroke(dp(1), colStrokePressed)
                        v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(60).start()
                        if (soundOn()) audio.playSoundEffect(soundFx, 0.6f)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        bg.setColor(colKey)
                        bg.setStroke(dp(1), colStroke)
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }
                }
                false
            }
            setOnClickListener { onTap() }
        }
    }

    private fun type(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun backspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun enter() {
        val ic = currentInputConnection ?: return
        if (!ic.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)) {
            ic.commitText("\n", 1)
        }
    }

    companion object {
        const val PREFS = "nexora_prefs"
        const val KEY_SOUND = "touch_sound"
    }
}
