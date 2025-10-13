package com.sanna.provcalapp.ui.notifications

import androidx.lifecycle.*
import com.sanna.provcalapp.feature.schedule.domain.ScheduleRepository
import com.sanna.provcalapp.feature.schedule.domain.model.MySchedule
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ShiftDayUi(
    val dayName: String,
    val dayNum: String,
    val start: String?,
    val end: String?,
    val total: String?
)

class NotificationsViewModel(
    private val repo: ScheduleRepository
) : ViewModel() {

    private val _next = MutableLiveData<ShiftDayUi?>()
    val next: LiveData<ShiftDayUi?> = _next

    private val _week = MutableLiveData<List<ShiftDayUi>>()
    val week: LiveData<List<ShiftDayUi>> = _week

    fun load() = viewModelScope.launch {
        val schedule = repo.getMySchedule()
        val items = buildSevenDays(schedule)
        _next.value = items.firstOrNull { it.start != null } ?: items.firstOrNull()
        _week.value = items
    }

    // ---- helpers ----
    private val esPE = Locale("es", "PE")
    private val lima = ZoneId.of("America/Lima")
    private val dayNumFmt = DateTimeFormatter.ofPattern("dd")
    private val time12Fmt = DateTimeFormatter.ofPattern("hh:mm a", esPE)

    private fun buildSevenDays(s: MySchedule): List<ShiftDayUi> {
        val now = LocalDate.now(lima)
        val out = mutableListOf<ShiftDayUi>()
        repeat(7) { idx ->
            val date = now.plusDays(idx.toLong())
            val dayName = date.dayOfWeek
                .getDisplayName(java.time.format.TextStyle.FULL, esPE)
                .replaceFirstChar { it.uppercase(esPE) }

            val works = s.workingDaysNames.any { it.equals(dayName, ignoreCase = true) }
            val (start, end, total) =
                if (works) {
                    val st = LocalTime.parse(s.startTime)
                    val en = LocalTime.parse(s.endTime)
                    val hours = durationHours(st, en)
                    val hh = hours.toInt()
                    val mm = ((hours - hh) * 60 + 0.5).toInt()
                    Triple(to12h(st), to12h(en), "%02d:%02d".format(hh, mm))
                } else Triple(null, null, null)

            out += ShiftDayUi(
                dayName = dayName,
                dayNum = dayNumFmt.format(date),
                start = start, end = end, total = total
            )
        }
        return out
    }

    private fun to12h(t: LocalTime) = time12Fmt.format(t)

    private fun durationHours(start: LocalTime, end: LocalTime): Double {
        val today = LocalDate.now(lima)
        val a = LocalDateTime.of(today, start)
        var b = LocalDateTime.of(today, end)
        if (end.isBefore(start)) b = b.plusDays(1)
        val minutes = Duration.between(a, b).toMinutes()
        return minutes / 60.0
    }
}