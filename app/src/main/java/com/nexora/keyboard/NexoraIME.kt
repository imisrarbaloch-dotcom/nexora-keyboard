package com.nexora.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

class NexoraIME : InputMethodService() {

    private class Page(val name: String, val keys: List<String>)

    // Glyphs live in assets/fonts/nexora.ttf on private-use codepoints:
    // letters U+E000.., digits U+E100.. (see glyph_map.json next to the font).
    private fun glyphs(first: Int, count: Int): List<String> =
        (0 until count).map { (first + it).toChar().toString() }

    // New letters later: add another Page (or raise the count) after rebuilding nexora.ttf.
    private val pages = listOf(
        Page("ABC", glyphs(LETTERS_START, 15)),
        Page("123", glyphs(DIGITS_START, 10))
    )
    private var pageIndex = 0
    private val columns = 5
    private val maxRows = pages.maxOf { (it.keys.size + columns - 1) / columns }

    private val glyphFont: Typeface by lazy {
        try {
            Typeface.createFromAsset(assets, "fonts/nexora.ttf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }

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

    override fun onCreateInputView(): View = buildView()

    private fun buildView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colBoard)
            setPadding(dp(6), dp(8), dp(6), dp(8))
        }

        val rows = pages[pageIndex].keys.chunked(columns)
        rows.forEach { row ->
            val line = newRow()
            row.forEach { s ->
                line.addView(
                    newKey(s, 1f, AudioManager.FX_KEYPRESS_STANDARD, glyphFont, GLYPH_SIZE) { type(s) }
                )
            }
            // Short last row: empty cells so every key keeps the same width.
            repeat(columns - row.size) { line.addView(newSpacer()) }
            root.addView(line)
        }
        // Keep the board the same height on every page.
        repeat(maxRows - rows.size) { root.addView(newRow()) }

        val next = pages[(pageIndex + 1) % pages.size]
        val bottom = newRow()
        bottom.addView(newKey("⌫", 1f, AudioManager.FX_KEYPRESS_DELETE, Typeface.DEFAULT_BOLD, 22f) { backspace() })
        bottom.addView(newKey(next.name, 1f, AudioManager.FX_KEYPRESS_STANDARD, Typeface.DEFAULT_BOLD, 16f) {
            pageIndex = (pageIndex + 1) % pages.size
            setInputView(buildView())
        })
        bottom.addView(newKey("␣", 2f, AudioManager.FX_KEYPRESS_SPACEBAR, Typeface.DEFAULT_BOLD, 22f) { type(" ") })
        bottom.addView(newKey("⏎", 1f, AudioManager.FX_KEYPRESS_RETURN, Typeface.DEFAULT_BOLD, 22f) { enter() })
        root.addView(bottom)

        return root
    }

    private fun newRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        )
    }

    private fun newSpacer() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
    }

    private fun newKey(
        label: String,
        weight: Float,
        soundFx: Int,
        face: Typeface,
        size: Float,
        onTap: () -> Unit
    ): TextView {
        val bg = GradientDrawable().apply {
            setColor(colKey)
            setStroke(dp(1), colStroke)
            cornerRadius = dp(10).toFloat()
        }
        return TextView(this).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
            typeface = face
            includeFontPadding = false
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
        if (!ic.performEditorAction(EditorInfo.IME_ACTION_DONE)) {
            ic.commitText("\n", 1)
        }
    }

    companion object {
        const val PREFS = "nexora_prefs"
        const val KEY_SOUND = "touch_sound"
        const val LETTERS_START = 0xE000
        const val DIGITS_START = 0xE100
        const val GLYPH_SIZE = 50f
    }
}
