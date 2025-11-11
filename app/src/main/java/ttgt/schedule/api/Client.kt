package ttgt.schedule.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ttgt.schedule.proto.ProfileType
import ttgt.schedule.proto.Profile
import ttgt.schedule.proto.Profiles
import ttgt.schedule.proto.UserData

suspend inline fun <reified T> HttpResponse.to(): T = Json.decodeFromString(bodyAsText())

@Serializable
data class Update(
    val versionCode: Int,
    val changelog: String
)

@Serializable
data class Items(
    val groups: List<String>,
    val teachers: List<String>
)

object Client {
    val http = HttpClient(CIO) {
        defaultRequest {
            host = "ttgt-api-isxb.onrender.com"
        }
    }

    val downloadUpdateUrl = "https://ttgt-api-isxb.onrender.com/schedule/android/download"

    suspend fun items(): Items = http.get("/schedule/items").to()
    suspend fun schedule(itemName: String) = http.get("/schedule/$itemName/schedule".encodeURLPath()).schedule()
    suspend fun overrides(itemName: String) = http.get("/schedule/$itemName/overrides".encodeURLPath()).overrides()
    suspend fun updates(): Update = http.get("/schedule/android/updates").to()
}

fun Profiles.profile(lastUsed: ProfileType?): Profile? = when (lastUsed) {
    ProfileType.STUDENT -> student
    ProfileType.TEACHER -> teacher
    else -> {
        when {
            hasTeacher() -> teacher
            hasStudent() -> student
            else -> null
        }
    }
}

fun UserData.Builder.editProfile(
    lastUsed: ProfileType,
    block: Profile.Builder.() -> Profile.Builder
): UserData.Builder =
    setProfiles(
        profiles
            .toBuilder()
            .let { profilesBuilder ->
                when (lastUsed) {
                    ProfileType.STUDENT -> profilesBuilder
                        .setStudent(
                            profilesBuilder
                                .student
                                .toBuilder()
                                .block()
                                .build()
                        )

                    ProfileType.TEACHER -> profilesBuilder
                        .setTeacher(
                            profilesBuilder
                                .teacher
                                .toBuilder()
                                .block()
                                .build()
                        )

                    else -> null
                }
            }
            ?.build()
            ?: profiles
    )