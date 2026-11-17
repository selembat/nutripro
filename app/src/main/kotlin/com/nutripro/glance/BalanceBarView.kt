package com.nutripro.glance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding

private const val TRACK_WIDTH_PX = 360
private const val TRACK_HEIGHT_PX = 36

/**
 * A horizontal balance track: a center zero-tick, with a filled segment running from
 * center out to the current balance's position (right = ahead/credit, left = owed/behind),
 * ending in a round knob. Drawn onto a bitmap rather than laid out with Glance primitives,
 * since Glance's Row/Box model has no arbitrary positioning or shape drawing of its own -
 * the same limitation that forced the old FillBar this replaces to be segmented.
 */
@Composable
fun BalanceBarView(
    label: String,
    signedValueText: String,
    unitText: String,
    ratio: Float,
    color: Color,
) {
    val colorArgb = color.toArgb()
    val bitmap = remember(ratio, colorArgb) { drawBalanceTrack(ratio, colorArgb) }

    FieldSurface {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CaptionText(text = label, fontSize = 10.sp)
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxWidth().height(16.dp).padding(vertical = 4.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetricText(text = signedValueText, color = color, fontSize = 20.sp)
                CaptionText(text = unitText, color = color, fontSize = 11.sp)
            }
        }
    }
}

private fun drawBalanceTrack(ratio: Float, colorArgb: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(TRACK_WIDTH_PX, TRACK_HEIGHT_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val midY = TRACK_HEIGHT_PX / 2f
    val centerX = TRACK_WIDTH_PX / 2f
    val trackTop = midY - 6f
    val trackBottom = midY + 6f

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = 0xFF555555.toInt()
    }
    canvas.drawRoundRect(RectF(4f, trackTop, TRACK_WIDTH_PX - 4f, trackBottom), 6f, 6f, trackPaint)

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        color = 0xFF777777.toInt()
    }
    canvas.drawLine(centerX, trackTop - 5f, centerX, trackBottom + 5f, tickPaint)

    val clamped = ratio.coerceIn(-1f, 1f)
    val maxHalfWidth = centerX - 16f
    val endX = centerX + clamped * maxHalfWidth

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorArgb
    }
    canvas.drawRoundRect(
        RectF(minOf(centerX, endX), trackTop + 1f, maxOf(centerX, endX), trackBottom - 1f),
        5f, 5f, fillPaint,
    )
    canvas.drawCircle(endX, midY, 9f, fillPaint)

    return bitmap
}
