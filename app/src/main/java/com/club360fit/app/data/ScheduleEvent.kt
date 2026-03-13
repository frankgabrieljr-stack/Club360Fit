package com.club360fit.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

@Serializable
data class ScheduleEvent(
    val id: String? = null,

    @SerialName("user_id")
    val userId: String = "",

    val title: String = "",

    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = LocalDate.now(),

    val time: String = "",
    val notes: String = "",

    @SerialName("client_id")
    val clientId: String? = null,

    @SerialName("is_completed")
    val isCompleted: Boolean = false,

    @SerialName("created_at")
    val createdAt: String? = null
) {
    val isPastDue: Boolean
        get() = !isCompleted && date.isBefore(LocalDate.now())

    val isUpcoming: Boolean
        get() = !isCompleted && !date.isBefore(LocalDate.now())
}

/** Serialises java.time.LocalDate as ISO-8601 "yyyy-MM-dd" for Postgres date column. */
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate =
        LocalDate.parse(decoder.decodeString())
}
