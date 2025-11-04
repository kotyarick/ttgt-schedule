package ttgt.schedule

import java.util.Calendar

var Calendar.year: Int
    get() = get(Calendar.YEAR)
    set(field) = set(Calendar.YEAR, field)

val Calendar.dayOfWeek: Int
    get() = (get(Calendar.DAY_OF_WEEK) - 1).let { if (it == 0) 6 else it - 1 }