package com.karthikkilari.expiryradar

import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.*
import org.junit.Test

class BarcodeOfflineTest {
    @Test
    fun decodesEan13WithoutAProductService() {
        val code = "5901234123457"
        val matrix = MultiFormatWriter().encode(code, BarcodeFormat.EAN_13, 400, 160)
        val pixels =
            IntArray(matrix.width * matrix.height) { i ->
                if (matrix[i % matrix.width, i / matrix.width]) 0xff000000.toInt()
                else 0xffffffff.toInt()
            }
        val result =
            MultiFormatReader()
                .decode(
                    BinaryBitmap(
                        HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels))
                    )
                )
        assertEquals(code, result.text)
        assertEquals(BarcodeFormat.EAN_13, result.barcodeFormat)
    }
}
