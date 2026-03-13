package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MealPlanRepository {
    private val client = SupabaseClient.client

    suspend fun getCurrentPlan(clientId: String): MealPlanDto? = withContext(Dispatchers.IO) {
        client.postgrest["meal_plans"]
            .select {
                filter { eq("client_id", clientId) }
                order("week_start", order = Order.DESCENDING)
                limit(1)
            }
            .decodeList<MealPlanDto>()
            .firstOrNull()
    }

    suspend fun upsertPlan(plan: MealPlanDto) = withContext(Dispatchers.IO) {
        client.postgrest["meal_plans"].upsert(plan)
    }
}
