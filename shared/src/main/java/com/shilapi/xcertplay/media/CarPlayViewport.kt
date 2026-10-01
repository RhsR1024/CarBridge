package com.shilapi.xcertplay.media

/** One fit rectangle for both video rendering and touch coordinates after a local resize. */
data class CarPlayViewport(val left: Float, val top: Float, val width: Float, val height: Float) {
    fun contains(x: Float, y: Float): Boolean = x >= left && x <= left + width && y >= top && y <= top + height

    companion object {
        fun fit(sourceWidth: Int, sourceHeight: Int, viewWidth: Int, viewHeight: Int): CarPlayViewport {
            val vw = viewWidth.coerceAtLeast(1).toFloat()
            val vh = viewHeight.coerceAtLeast(1).toFloat()
            val sw = sourceWidth.coerceAtLeast(1).toFloat()
            val sh = sourceHeight.coerceAtLeast(1).toFloat()
            val scale = minOf(vw / sw, vh / sh)
            val width = sw * scale
            val height = sh * scale
            return CarPlayViewport((vw - width) / 2f, (vh - height) / 2f, width, height)
        }
    }
}
