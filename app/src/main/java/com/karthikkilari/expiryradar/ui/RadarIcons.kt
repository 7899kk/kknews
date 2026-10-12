package com.karthikkilari.expiryradar.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object RadarIcons {
    val Inventory =
        ImageVector.Builder("Inventory", 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(3f, 7f)
                    lineTo(12f, 2f)
                    lineTo(21f, 7f)
                    lineTo(21f, 18f)
                    lineTo(12f, 23f)
                    lineTo(3f, 18f)
                    close()
                    moveTo(5f, 8f)
                    lineTo(11f, 11f)
                    lineTo(11f, 20f)
                    lineTo(5f, 17f)
                    close()
                    moveTo(13f, 11f)
                    lineTo(19f, 8f)
                    lineTo(19f, 17f)
                    lineTo(13f, 20f)
                    close()
                }
            }
            .build()
    val Calendar =
        ImageVector.Builder("Calendar", 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(3f, 4f)
                    lineTo(6f, 4f)
                    lineTo(6f, 2f)
                    lineTo(8f, 2f)
                    lineTo(8f, 4f)
                    lineTo(16f, 4f)
                    lineTo(16f, 2f)
                    lineTo(18f, 2f)
                    lineTo(18f, 4f)
                    lineTo(21f, 4f)
                    lineTo(21f, 22f)
                    lineTo(3f, 22f)
                    close()
                    moveTo(5f, 9f)
                    lineTo(5f, 20f)
                    lineTo(19f, 20f)
                    lineTo(19f, 9f)
                    close()
                    moveTo(8f, 12f)
                    lineTo(11f, 12f)
                    lineTo(11f, 15f)
                    lineTo(8f, 15f)
                    close()
                }
            }
            .build()
    val Chart =
        ImageVector.Builder("Insights", 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(3f, 12f)
                    lineTo(7f, 12f)
                    lineTo(7f, 21f)
                    lineTo(3f, 21f)
                    close()
                    moveTo(10f, 3f)
                    lineTo(14f, 3f)
                    lineTo(14f, 21f)
                    lineTo(10f, 21f)
                    close()
                    moveTo(17f, 8f)
                    lineTo(21f, 8f)
                    lineTo(21f, 21f)
                    lineTo(17f, 21f)
                    close()
                }
            }
            .build()
    val Scanner =
        ImageVector.Builder("Scanner", 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(2f, 2f)
                    lineTo(9f, 2f)
                    lineTo(9f, 4f)
                    lineTo(4f, 4f)
                    lineTo(4f, 9f)
                    lineTo(2f, 9f)
                    close()
                    moveTo(15f, 2f)
                    lineTo(22f, 2f)
                    lineTo(22f, 9f)
                    lineTo(20f, 9f)
                    lineTo(20f, 4f)
                    lineTo(15f, 4f)
                    close()
                    moveTo(2f, 15f)
                    lineTo(4f, 15f)
                    lineTo(4f, 20f)
                    lineTo(9f, 20f)
                    lineTo(9f, 22f)
                    lineTo(2f, 22f)
                    close()
                    moveTo(20f, 15f)
                    lineTo(22f, 15f)
                    lineTo(22f, 22f)
                    lineTo(15f, 22f)
                    lineTo(15f, 20f)
                    lineTo(20f, 20f)
                    close()
                    moveTo(6f, 11f)
                    lineTo(18f, 11f)
                    lineTo(18f, 13f)
                    lineTo(6f, 13f)
                    close()
                }
            }
            .build()
}
