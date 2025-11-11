package ttgt.schedule.api

import com.google.protobuf.Empty
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ttgt.schedule.proto.CommonLesson
import ttgt.schedule.proto.Day
import ttgt.schedule.proto.Lesson
import ttgt.schedule.proto.Override
import ttgt.schedule.proto.Overrides
import ttgt.schedule.proto.Schedule
import ttgt.schedule.proto.SubgroupedLesson
import ttgt.schedule.proto.SubgroupedLessonData
import ttgt.schedule.proto.Week

private fun JsonElement.lesson(): Lesson {
    if (toString() == "null")
        return Lesson
            .newBuilder()
            .setNoLesson(
                Empty
                    .newBuilder()
                    .build()
            )
            .build()

    val obj = jsonObject

    return Lesson.newBuilder()
        .setGroup(obj["group"]?.jsonPrimitive?.content ?: "")
        .let { lesson ->
            obj["commonLesson"]?.let { commonLesson ->
                lesson.setCommonLesson(
                    CommonLesson.newBuilder()
                        .setName(commonLesson.jsonObject["name"]?.jsonPrimitive?.content)
                        .setRoom(commonLesson.jsonObject["room"]?.jsonPrimitive?.content)
                        .setTeacher(commonLesson.jsonObject["teacher"]?.jsonPrimitive?.content)
                        .build()
                )
            } ?: obj["subgroupedLesson"]?.let { subgroupedLesson ->
                lesson.setSubgroupedLesson(
                    SubgroupedLesson.newBuilder()
                        .setName(subgroupedLesson.jsonObject["name"]?.jsonPrimitive?.content)
                        .addAllSubgroups(
                            subgroupedLesson.jsonObject["subgroups"]?.jsonArray?.map { subgroup ->
                                SubgroupedLessonData.newBuilder()
                                    .setRoom(subgroup.jsonObject["room"]?.jsonPrimitive?.content)
                                    .setTeacher(subgroup.jsonObject["teacher"]?.jsonPrimitive?.content)
                                    .build()
                            }
                        )

                        .build()
                )
            } ?: lesson
        }
        .build()
}

suspend fun HttpResponse.overrides(): Overrides {
    val overrides: JsonObject = to()

    return Overrides.newBuilder()
        .addAllOverrides(
            overrides["overrides"]?.jsonArray?.map { override ->
                val override = override.jsonObject
                Override.newBuilder()
                    .setIndex(override["index"]?.jsonPrimitive?.intOrNull ?: 0)
                    .setShouldBe(override["shouldBe"]?.lesson())
                    .setWillBe(override["willBe"]?.lesson())
                    .build()
            } ?: emptyList()
        )
        .setWeekDay(overrides["weekDay"]?.jsonPrimitive?.intOrNull ?: 0)
        .setWeekNum(overrides["weekNum"]?.jsonPrimitive?.intOrNull ?: 0)
        .build()
}

suspend fun HttpResponse.schedule(): Schedule {
    val schedule: JsonObject = to()

    return Schedule.newBuilder()
        .addAllWeeks(
            schedule["weeks"]?.jsonArray?.map { week ->
                Week.newBuilder()
                    .addAllDays(
                        week.jsonObject["days"]?.jsonArray?.map { day ->
                            Day.newBuilder()
                                .addAllLesson(
                                    day.jsonObject["lesson"]?.jsonArray?.map { lesson ->
                                        lesson.lesson()
                                    }
                                )
                                .build()
                        }
                    )
                    .build()
            }
        )
        .build()
}