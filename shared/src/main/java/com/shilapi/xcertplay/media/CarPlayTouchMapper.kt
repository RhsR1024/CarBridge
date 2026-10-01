package com.shilapi.xcertplay.media

import android.view.MotionEvent
import com.shilapi.xcertplay.airplay.AirPlayContact

/** Converts Android MotionEvents into normalized CarPlay touch contacts. */
object CarPlayTouchMapper {
    private const val MAX_CONTACTS = 2

    fun contacts(event: MotionEvent, viewWidth: Int, viewHeight: Int,
                 viewport: CarPlayViewport = CarPlayViewport.fit(viewWidth, viewHeight, viewWidth, viewHeight)): List<AirPlayContact> {
        val width = viewport.width.coerceAtLeast(1f)
        val height = viewport.height.coerceAtLeast(1f)
        val action = event.actionMasked
        val liftedIndex = if (action == MotionEvent.ACTION_POINTER_UP) event.actionIndex else -1
        val allUp = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
        val count = minOf(MAX_CONTACTS, event.pointerCount)
        val contacts = ArrayList<AirPlayContact>(count)
        for (index in 0 until count) {
            contacts.add(
                AirPlayContact(
                    id = index,
                    x = ((event.getX(index).toDouble() - viewport.left) / width).coerceIn(0.0, 1.0),
                    y = ((event.getY(index).toDouble() - viewport.top) / height).coerceIn(0.0, 1.0),
                    down = !allUp && index != liftedIndex,
                ),
            )
        }
        return contacts
    }
}
