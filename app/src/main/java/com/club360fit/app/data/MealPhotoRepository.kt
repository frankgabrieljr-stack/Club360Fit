package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

object MealPhotoRepository {

    private val client = SupabaseClient.client
    private val bucket get() = client.storage.from(SupabaseClient.MEAL_PHOTOS_BUCKET)

    fun publicUrlFor(storagePath: String): String = bucket.publicUrl(storagePath)

    suspend fun listForClient(clientId: String): List<MealPhotoLogDto> = withContext(Dispatchers.IO) {
        client.postgrest["meal_photo_logs"]
            .select {
                filter { eq("client_id", clientId) }
                order("log_date", order = Order.DESCENDING)
            }
            .decodeList<MealPhotoLogDto>()
            .sortedWith(
                compareByDescending<MealPhotoLogDto> { it.logDate }
                    .thenByDescending { it.createdAt ?: "" }
            )
    }

    suspend fun uploadAndInsert(
        clientId: String,
        bytes: ByteArray,
        logDate: LocalDate,
        notes: String,
        originalFilename: String
    ): MealPhotoLogDto = withContext(Dispatchers.IO) {
        val safeName = originalFilename.replace("\\s+".toRegex(), "_").ifBlank { "photo.jpg" }
        val path = "$clientId/${System.currentTimeMillis()}_$safeName"
        bucket.upload(path, bytes, upsert = false)
        val row = MealPhotoLogDto(
            clientId = clientId,
            logDate = logDate,
            storagePath = path,
            notes = notes.trim()
        )
        try {
            client.postgrest["meal_photo_logs"].insert(row)
        } catch (e: Exception) {
            try {
                bucket.delete(path)
            } catch (_: Exception) { /* ignore */ }
            throw e
        }
        client.postgrest["meal_photo_logs"]
            .select {
                filter { eq("storage_path", path) }
            }
            .decodeSingle<MealPhotoLogDto>()
    }

    /** Coach updates feedback text; clears both columns when [feedback] is blank. */
    suspend fun updateCoachFeedback(clientId: String, logId: String, feedback: String) =
        withContext(Dispatchers.IO) {
            val trimmed = feedback.trim()
            if (trimmed.isEmpty()) {
                client.postgrest["meal_photo_logs"].update(
                    {
                        set("coach_feedback", null as String?)
                        set("coach_feedback_updated_at", null as String?)
                    }
                ) {
                    filter {
                        eq("id", logId)
                        eq("client_id", clientId)
                    }
                }
            } else {
                client.postgrest["meal_photo_logs"].update(
                    {
                        set("coach_feedback", trimmed)
                        set("coach_feedback_updated_at", Instant.now().toString())
                    }
                ) {
                    filter {
                        eq("id", logId)
                        eq("client_id", clientId)
                    }
                }
            }
        }

    suspend fun deleteOwn(clientId: String, logId: String) = withContext(Dispatchers.IO) {
        val existing = client.postgrest["meal_photo_logs"]
            .select {
                filter { eq("id", logId) }
            }
            .decodeList<MealPhotoLogDto>()
            .firstOrNull() ?: return@withContext
        if (existing.clientId != clientId) return@withContext
        client.postgrest["meal_photo_logs"].delete {
            filter { eq("id", logId) }
        }
        try {
            bucket.delete(existing.storagePath)
        } catch (_: Exception) { /* storage cleanup best-effort */ }
    }
}
