/**
 * Composable that renders a circular battery arc around the current time and date.
 * No icons — purely text and a thin arc drawn on a Canvas.
 * Covers: ULT-6
 */
package com.ultimatefocus.app.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ClockWidget(
    currentTime: LocalDateTime,
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val primaryColor = MaterialTheme.colorScheme.onBackground
    val dimColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 3.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            drawArc(
                color = dimColor.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            if (batteryLevel in 0..100) {
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * (batteryLevel / 100f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentTime.format(timeFormatter),
                style = MaterialTheme.typography.displayLarge,
                color = primaryColor,
                fontWeight = FontWeight.Light,
            )
            Text(
                text = currentTime.format(dateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = dimColor,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = if (isCharging) "$batteryLevel% +" else "$batteryLevel%",
                style = MaterialTheme.typography.labelMedium,
                color = dimColor,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
