package ttgt.schedule.ui

import kotlinx.serialization.Serializable

@Serializable
sealed class Destination {
    @Serializable
    object Welcome : Destination()

    @Serializable
    object Schedule : Destination()
}