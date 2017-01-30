package me.jackdn.nl2controlpanel

import android.view.MotionEvent
import android.view.View
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