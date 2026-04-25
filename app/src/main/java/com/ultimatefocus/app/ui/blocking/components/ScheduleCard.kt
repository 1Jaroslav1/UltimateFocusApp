/**
 * Single-row card renderer for BlockingSchedulesListScreen. Shows the
 * schedule name, formatted time range, and seven short weekday labels with
 * inactive days dimmed — matches the acceptance copy for ULT-24's list
 * view: "name, the time when it is blocked, and a list of days when it is
 * blocked by showing slightly highlighted short weekday names."
 *
 * The "Active now" badge appears when the predicate is currently true so
 * the user understands why the editor's save/delete buttons may be disabled
 * on entry (ULT-25 self-unblock-prevention).
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimatefocus.app.domain.model.BlockingSchedule
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ScheduleCard(
    schedule: BlockingSchedule,
    isCurrentlyActive: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("schedule_card_${schedule.id}")
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = schedule.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("schedule_card_name_${schedule.id}"),
                )
                if (isCurrentlyActive) {
                    Text(
                        text = "Active now",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.testTag("schedule_card_active_${schedule.id}"),
                    )
                }
            }
            Text(
                text = "${schedule.start.format(TIME_FORMATTER)} – ${schedule.end.format(TIME_FORMATTER)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
            DayPills(active = schedule.days)
        }
    }
}

@Composable
private fun DayPills(active: Set<DayOfWeek>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        DayOfWeek.entries.forEach { day ->
            val isActive = day in active
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .testTag("schedule_card_day_${day.name.lowercase()}_${if (isActive) "on" else "off"}"),
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    },
                )
            }
        }
    }
}
