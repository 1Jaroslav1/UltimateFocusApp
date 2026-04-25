/**
 * Seven-day filter-chip row used by BlockingScheduleEditorScreen. Each chip
 * carries a localized short weekday label and a stable testTag
 * (`day_chip_<dayOfWeek>`) so instrumented tests can toggle days reliably.
 *
 * Tapping toggles the day's membership in the supplied set; the parent
 * editor owns the canonical Set<DayOfWeek>.
 *
 * Covers: ULT-24
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DayChipRow(
    selectedDays: Set<DayOfWeek>,
    onToggleDay: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("day_chip_row"),
    ) {
        DayOfWeek.entries.forEach { day ->
            val selected = day in selectedDays
            FilterChip(
                selected = selected,
                onClick = { onToggleDay(day) },
                label = {
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        fontSize = 14.sp,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("day_chip_${day.name.lowercase()}"),
            )
        }
    }
}
