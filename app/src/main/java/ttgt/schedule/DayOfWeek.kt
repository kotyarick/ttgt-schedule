package ttgt.schedule

enum class DayOfWeek(val nameRes: Int) {
    MONDAY(R.string.monday),
    TUESDAY(R.string.tuesday),
    WEDNESDAY(R.string.wednesday),
    THURSDAY(R.string.thursday),
    FRIDAY(R.string.friday),
    SATURDAY(R.string.saturday),
    SUNDAY(R.string.sunday);

    companion object {
        fun from(input: java.time.DayOfWeek) = when (input) {
            java.time.DayOfWeek.MONDAY -> MONDAY
            java.time.DayOfWeek.TUESDAY -> TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> THURSDAY
            java.time.DayOfWeek.FRIDAY -> FRIDAY
            java.time.DayOfWeek.SATURDAY -> SATURDAY
            java.time.DayOfWeek.SUNDAY -> SUNDAY
        }
    }
}