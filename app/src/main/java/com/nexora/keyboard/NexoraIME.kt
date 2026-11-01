package com.nexora.keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
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

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0720"))
            setPadding(dp(6), dp(8), dp(6), dp(8))
        }

        nexoraSymbols.chunked(4).forEach { row ->
            val line = newRow()
            row.forEach { s -> line.addView(newKey(s, 1f) { type(s) }) }
            root.addView(line)
        }

        val bottom = newRow()
        bottom.addView(newKey("⌫", 1f) { backspace() })
        bottom.addView(newKey("␣", 2f) { type(" ") })
        bottom.addView(newKey("⏎", 1f) { enter() })
        root.addView(bottom)

        return root
    }

    private fun newRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
        )
    }

    private fun newKey(label: String, weight: Float, onTap: () -> Unit) =
        TextView(this).apply {
            text = label
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#7DF9FF"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1140"))
                setStroke(dp(1), Color.parseColor("#5B3FFF"))
                cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight)
                .apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
            setOnClickListener { onTap() }
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
}
