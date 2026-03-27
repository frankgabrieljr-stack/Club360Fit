// Deploy: `supabase functions deploy set-user-role --project-ref <ref>`
// Secrets (auto in hosted project): SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: cors });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader?.startsWith("Bearer ")) {
      return json(401, { error: "Missing Authorization" });
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!serviceRoleKey) {
      return json(500, { error: "SUPABASE_SERVICE_ROLE_KEY not configured" });
    }

    const userClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    });

    const { data: userData, error: userErr } = await userClient.auth.getUser();
    if (userErr || !userData.user) {
      return json(401, { error: "Invalid session" });
    }

    const callerRole = userData.user.user_metadata?.role;
    if (callerRole !== "admin") {
      return json(403, { error: "Only coach/admin accounts can change roles" });
    }

    const body = (await req.json()) as { target_user_id?: string; role?: string };
    const targetId = body.target_user_id?.trim();
    const newRole = body.role?.trim();
    if (!targetId) {
      return json(400, { error: "target_user_id required" });
    }
    if (newRole !== "admin" && newRole !== "client") {
      return json(400, { error: "role must be admin or client" });
    }

    const adminClient = createClient(supabaseUrl, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const { data: existing, error: fetchErr } = await adminClient.auth.admin.getUserById(targetId);
    if (fetchErr || !existing?.user) {
      return json(404, { error: "User not found" });
    }

    const prev = existing.user.user_metadata ?? {};
    const merged = { ...prev, role: newRole };
    const { error: upErr } = await adminClient.auth.admin.updateUserById(targetId, {
      user_metadata: merged as Record<string, unknown>,
    });
    if (upErr) {
      return json(500, { error: upErr.message });
    }

    // Sync to public.profiles table
    // Derive full_name from metadata (name, full_name, or first_name/last_name)
    const fullName = merged.full_name || merged.name || 
      (merged.first_name && merged.last_name ? `${merged.first_name} ${merged.last_name}` : '') || '';
    
    try {
      const { error: profileErr } = await adminClient
        .from('profiles')
        .upsert({
          id: targetId,
          email: existing.user.email,
          full_name: fullName,
          avatar_url: merged.avatar_url || '',
          role: newRole,
          updated_at: new Date().toISOString(),
        }, {
          onConflict: 'id'
        });
      
      if (profileErr) {
        // Log warning but don't fail - profiles table might not be deployed yet
        console.warn('Failed to update profiles table:', profileErr.message);
      }
    } catch (profileUpdateErr) {
      // Log warning but continue - table sync is not critical for auth update
      console.warn('Error updating profiles table:', profileUpdateErr);
    }

    return json(200, { ok: true });
  } catch (e) {
    return json(500, { error: String(e) });
  }
});

function json(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}
