package ttgt.schedule.ui

enum class TimestampType(
    vararg val timestamps: LessonTime
) {
    Normal(
        LessonTime(
            Time(8, 0),
            Time(9, 30)
        ),
        LessonTime(
            Time(9, 40),
            Time(11, 10)
        ),
        LessonTime(
            Time(11, 50),
            Time(13, 20)
        ),
        LessonTime(
            Time(13, 30),
            Time(15, 0)
        ),
        LessonTime(
            Time(15, 10),
            Time(16, 40)
        )
    ),

    ClassHour(
        LessonTime(
            Time(8, 0),
            Time(9, 30)
        ),
        LessonTime(
            Time(9, 40),
            Time(11, 10)
        ),
        LessonTime(
            Time(11, 50),
            Time(13, 20)
        ),
        LessonTime(
            Time(13, 30),
            Time(14, 15)
        ),
        LessonTime(
            Time(14, 25),
            Time(15, 55)
        ),
        LessonTime(
            Time(16, 5),
            Time(17, 35)
        )
    ),

    Saturday(
        LessonTime(
            Time(8, 0),
            Time(9, 30)
        ),
        LessonTime(
            Time(9, 40),
            Time(11, 10)
        ),
        LessonTime(
            Time(11, 20),
            Time(12, 50)
        ),
        LessonTime(
            Time(13, 0),
            Time(14, 30)
        )
    )
}