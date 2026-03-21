package com.club360fit.app.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientDto(
    val id: String? = null,

    @SerialName("coach_id")
    val coachId: String? = null,

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("full_name")
    val fullName: String? = null,

    val age: Int? = null,

    @SerialName("height_cm")
    val heightCm: Int? = null,

    @SerialName("weight_kg")
    val weightKg: Int? = null,

    val phone: String? = null,

    @SerialName("medical_conditions")
    val medicalConditions: String? = null,

    @SerialName("food_restrictions")
    val foodRestrictions: String? = null,

    val goal: String? = null,

    /** Always encode so `false` is sent on upsert (kotlinx default skips false → DB never updated). */
    @EncodeDefault(Mode.ALWAYS)
    @SerialName("can_view_nutrition")
    val canViewNutrition: Boolean = false,

    @EncodeDefault(Mode.ALWAYS)
    @SerialName("can_view_workouts")
    val canViewWorkouts: Boolean = false,

    @EncodeDefault(Mode.ALWAYS)
    @SerialName("can_view_payments")
    val canViewPayments: Boolean = false,

    @EncodeDefault(Mode.ALWAYS)
    @SerialName("can_view_events")
    val canViewEvents: Boolean = false,

    @SerialName("last_active")
    val lastActive: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)
