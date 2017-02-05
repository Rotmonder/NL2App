package me.jackdn.nl2controlpanel

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import org.jetbrains.anko.doAsync

fun View.repeatingListener(period: Long = 100, listener: (View) -> Unit) {
    setOnTouchListener({ view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                view.isPressed = true
            }

            MotionEvent.ACTION_UP -> {
                view.isPressed = false
            }
        }

        doAsync {
            while (view.isPressed) {
                listener(view)
                Thread.sleep(period)
            }
        }

        true
    })
}

fun Button.setAvailable(enabled: Boolean) {
    if (enabled) {
        background.colorFilter = null
    } else {
        background.setColorFilter(Color.DKGRAY, PorterDuff.Mode.MULTIPLY)
    }
}