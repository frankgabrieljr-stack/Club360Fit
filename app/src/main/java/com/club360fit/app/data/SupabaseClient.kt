package com.club360fit.app.data

import com.club360fit.app.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Supabase client singleton. URL uses project ID from Supabase dashboard.
 * Anon key is read from BuildConfig (set via local.properties).
 * For profile avatars, create a public bucket named "avatars" in Supabase Storage.
 */
object SupabaseClient {

    private const val SUPABASE_URL = "https://mjkrokpctcieahxtxvxq.supabase.co"
    const val AVATARS_BUCKET = "avatars"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}
