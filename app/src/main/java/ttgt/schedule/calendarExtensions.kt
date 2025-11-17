package ttgt.schedule

import ttgt.schedule.proto.Date
import java.util.Calendar

var Calendar.year: Int
    get() = get(Calendar.YEAR)
    set(field) = set(Calendar.YEAR, field)

val Calendar.dayOfWeek: Int
    get() = (get(Calendar.DAY_OF_WEEK) - 1).let { if (it == 0) 6 else it - 1 }

private val months = listOf(
    "января",
    "февраля",
    "марта",
    "апреля",
    "мая",
    "инюня",
    "июля",
    "августа",
    "сентября",
    "октября",
    "ноября",
    "декабря"
)

fun Date.sortString() = "$year$month$day"
fun Date.display() = "$day ${months[month]} ${year}г."