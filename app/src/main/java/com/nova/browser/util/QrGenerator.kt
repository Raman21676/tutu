package com.nova.browser.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

/**
 * QrGenerator
 *
 * Generates a QR code Bitmap from any String (URL, text, etc.)
 * using ZXing core — no Google Play Services required.
 *
 * Gradle dependency needed (zxing core only, no camera Activity):
 *   implementation 'com.google.zxing:core:3.5.3'
 *
 * Usage:
 *   val bitmap = QrGenerator.generate("https://example.com")
 *   // bitmap is null if generation failed (empty string etc.)
 */
object QrGenerator {

    /**
     * Generates a square QR code bitmap.
     *
     * @param content  The string to encode (typically the current URL).
     * @param sizePx   Side length in pixels. Defaults to 512 for sharp display.
     * @param darkColor  Pixel color for dark modules. Defaults to black.
     * @param lightColor Pixel color for light modules. Defaults to white.
     * @return         A Bitmap, or null if [content] is blank or encoding fails.
     */
    fun generate(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null

        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1          // quiet zone in modules
        )

        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            bitMatrixToBitmap(bitMatrix, darkColor, lightColor)
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun bitMatrixToBitmap(
        matrix: BitMatrix,
        darkColor: Int,
        lightColor: Int
    ): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) darkColor else lightColor
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }
}
