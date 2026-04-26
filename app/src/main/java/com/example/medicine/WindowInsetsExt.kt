package com.example.medicine

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun View.applySystemBarInsets(extraTopDp: Int = 16, extraBottomDp: Int = 16) {
    val density = resources.displayMetrics.density
    val extraTopPx = (extraTopDp * density).toInt()
    val extraBottomPx = (extraBottomDp * density).toInt()
    val initialStart = paddingStart
    val initialEnd = paddingEnd

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            left = initialStart,
            top = bars.top + extraTopPx,
            right = initialEnd,
            bottom = bars.bottom + extraBottomPx
        )
        insets
    }
    requestApplyInsets()
}
