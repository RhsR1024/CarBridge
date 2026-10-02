package com.shilapi.xcertplay.media

/** Fits the negotiated CarPlay canvas inside the current window without changing its aspect ratio. */
data class CarPlayVideoLayout(val left: Float, val top: Float, val width: Float, val height: Float) {
    fun contains(x: Float, y: Float): Boolean =
        x >= left && x <= left + width && y >= top && y <= top + height

    companion object {
        fun fit(canvasWidth: Int, canvasHeight: Int, viewWidth: Int, viewHeight: Int): CarPlayVideoLayout {
            val vw = viewWidth.coerceAtLeast(1).toFloat()
            val vh = viewHeight.coerceAtLeast(1).toFloat()
            val cw = canvasWidth.coerceAtLeast(1).toFloat()
            val ch = canvasHeight.coerceAtLeast(1).toFloat()
            val scale = minOf(vw / cw, vh / ch)
            val width = cw * scale
            val height = ch * scale
            return CarPlayVideoLayout((vw - width) / 2f, (vh - height) / 2f, width, height)
        }
    }
}
