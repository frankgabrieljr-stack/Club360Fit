// Deploy: `supabase functions deploy send-device-push --project-ref <ref>`
// Required secrets:
// - SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY
// - Android FCM HTTP v1: FCM_PROJECT_ID, FCM_CLIENT_EMAIL, FCM_PRIVATE_KEY
// - iOS APNs token auth: APNS_TEAM_ID, APNS_KEY_ID, APNS_PRIVATE_KEY, APNS_BUNDLE_ID
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";
import { SignJWT, importPKCS8 } from "https://esm.sh/jose@5.9.6";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type, x-cron-secret",
};

type PushPayload = {
  client_id?: string;
  kind?: string;
  title?: string;
  body?: string;
  ref_type?: string | null;
  ref_id?: string | null;
  dedupe_key?: string | null;
  visible_to_client?: boolean;
};

type ClientRow = {
  id: string;
  user_id: string;
  coach_id: string | null;
};

type TokenRow = {
  id: string;
  platform: "android_fcm" | "ios_apns";
  token: string;
  environment: "sandbox" | "production";
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
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!serviceRoleKey) {
      return json(500, { error: "SUPABASE_SERVICE_ROLE_KEY not configured" });
    }

    const adminClient = createClient(supabaseUrl, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });
    const bearerToken = authHeader.replace(/^Bearer\s+/i, "");
    const cronSecret = Deno.env.get("PAYMENT_REMINDER_CRON_SECRET");
    const cronHeader = req.headers.get("x-cron-secret");
    const isCronCall =
      !!cronSecret && !!cronHeader && cronHeader === cronSecret;
    const isServiceRoleCall = bearerToken === serviceRoleKey;

    const payload = (await req.json()) as PushPayload;
    const clientId = payload.client_id?.trim();
    const title = payload.title?.trim();
    if (!clientId) return json(400, { error: "client_id required" });
    if (!title) return json(400, { error: "title required" });

    const { data: clientRowRaw, error: clientErr } = await adminClient
      .from("clients")
      .select("id,user_id,coach_id")
      .eq("id", clientId)
      .maybeSingle();

    if (clientErr) return json(500, { error: clientErr.message });
    const clientRow = clientRowRaw as ClientRow | null;
    if (!clientRow) return json(404, { error: "Client not found" });

    if (!isCronCall && !isServiceRoleCall) {
      const { data: userData, error: userErr } = await adminClient.auth.getUser(bearerToken);
      if (userErr || !userData.user) {
        return json(401, { error: "Invalid session" });
      }
      const callerId = userData.user.id.toLowerCase();
      const memberUserId = clientRow.user_id.toLowerCase();
      const coachUserId = clientRow.coach_id?.toLowerCase() ?? null;
      const callerRole = userData.user.user_metadata?.role;
      const callerCanNotify =
        callerId === memberUserId ||
        callerId === coachUserId ||
        callerRole === "admin" ||
        callerRole === "coach" ||
        (await canNotifyCommunityReply(adminClient, callerId, clientId, payload));
      if (!callerCanNotify) {
        return json(403, { error: "Caller is not related to this client" });
      }
    }

    return await deliverPush(adminClient, payload, clientRow);
  } catch (e) {
    return json(500, { error: String(e) });
  }
});

/** Peer/coach replied on a community post owned by `clientId` — allow push to that member/coach. */
async function canNotifyCommunityReply(
  adminClient: ReturnType<typeof createClient>,
  callerId: string,
  clientId: string,
  payload: PushPayload,
): Promise<boolean> {
  if ((payload.kind ?? "").toLowerCase() !== "community_reply") return false;
  const postId = payload.ref_id?.trim();
  if (!postId) return false;

  const { data: post, error: postErr } = await adminClient
    .from("community_posts")
    .select("id,client_id")
    .eq("id", postId)
    .maybeSingle();
  if (postErr || !post) return false;
  if ((post.client_id as string)?.toLowerCase() !== clientId.toLowerCase()) return false;

  const { data: comments, error: commentErr } = await adminClient
    .from("community_comments")
    .select("id")
    .eq("post_id", postId)
    .eq("author_user_id", callerId)
    .limit(1);
  if (commentErr) return false;
  return (comments?.length ?? 0) > 0;
}

async function deliverPush(
  adminClient: ReturnType<typeof createClient>,
  payload: PushPayload,
  clientRow: ClientRow,
): Promise<Response> {
  try {
    const clientId = payload.client_id?.trim();
    const title = payload.title?.trim();
    const body = payload.body?.trim() ?? "";
    if (!clientId || !title) {
      return json(400, { error: "client_id and title required" });
    }

    const recipientUserId = payload.visible_to_client === false
      ? clientRow.coach_id
      : clientRow.user_id;
    if (!recipientUserId) {
      return json(200, { ok: true, skipped: "no_recipient" });
    }

    if (payload.dedupe_key) {
      const { error: dedupeErr } = await adminClient
        .from("push_notification_dedupe")
        .insert({ dedupe_key: payload.dedupe_key });
      if (dedupeErr) {
        if (dedupeErr.code === "23505") {
          return json(200, { ok: true, skipped: "duplicate" });
        }
        return json(500, { error: dedupeErr.message });
      }
    }

    const { data: tokens, error: tokenErr } = await adminClient
      .from("push_device_tokens")
      .select("id,platform,token,environment")
      .eq("user_id", recipientUserId)
      .eq("enabled", true);

    if (tokenErr) return json(500, { error: tokenErr.message });

    let sent = 0;
    const errors: string[] = [];
    for (const token of (tokens ?? []) as TokenRow[]) {
      try {
        if (token.platform === "android_fcm") {
          await sendFcm(token.token, title, body, payload);
        } else if (token.platform === "ios_apns") {
          await sendApns(token.token, token.environment, title, body, payload);
        }
        sent += 1;
      } catch (e) {
        const message = e instanceof Error ? e.message : String(e);
        errors.push(`${token.platform}:${message}`);
        if (message.includes("UNREGISTERED") || message.includes("BadDeviceToken")) {
          await adminClient
            .from("push_device_tokens")
            .update({ enabled: false, updated_at: new Date().toISOString() })
            .eq("id", token.id);
        }
      }
    }

    console.log("device_push_result", {
      recipientUserId,
      tokenCount: tokens?.length ?? 0,
      sent,
      errors,
      kind: payload.kind ?? null,
      refType: payload.ref_type ?? null,
    });

    return json(200, { ok: true, sent, errors });
  } catch (e) {
    return json(500, { error: String(e) });
  }
});

async function sendFcm(token: string, title: string, body: string, payload: PushPayload) {
  const projectId = Deno.env.get("FCM_PROJECT_ID");
  const clientEmail = Deno.env.get("FCM_CLIENT_EMAIL");
  const privateKey = normalizePrivateKey(Deno.env.get("FCM_PRIVATE_KEY"));
  if (!projectId || !clientEmail || !privateKey) {
    throw new Error("FCM secrets not configured");
  }

  const accessToken = await googleAccessToken(clientEmail, privateKey);
  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token,
          notification: { title, body },
          data: pushData(payload),
          android: { priority: "HIGH" },
        },
      }),
    },
  );

  if (!response.ok) {
    throw new Error(await response.text());
  }
}

async function googleAccessToken(clientEmail: string, privateKeyPem: string): Promise<string> {
  const key = await importPKCS8(privateKeyPem, "RS256");
  const assertion = await new SignJWT({
    scope: "https://www.googleapis.com/auth/firebase.messaging",
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(clientEmail)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt()
    .setExpirationTime("1h")
    .sign(key);

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const json = await response.json();
  if (!response.ok || !json.access_token) {
    throw new Error(`FCM auth failed: ${JSON.stringify(json)}`);
  }
  return json.access_token as string;
}

async function sendApns(
  token: string,
  environment: "sandbox" | "production",
  title: string,
  body: string,
  payload: PushPayload,
) {
  const teamId = Deno.env.get("APNS_TEAM_ID");
  const keyId = Deno.env.get("APNS_KEY_ID");
  const privateKey = normalizePrivateKey(Deno.env.get("APNS_PRIVATE_KEY"));
  const bundleId = Deno.env.get("APNS_BUNDLE_ID");
  if (!teamId || !keyId || !privateKey || !bundleId) {
    throw new Error("APNs secrets not configured");
  }

  const key = await importPKCS8(privateKey, "ES256");
  const jwt = await new SignJWT({})
    .setProtectedHeader({ alg: "ES256", kid: keyId })
    .setIssuer(teamId)
    .setIssuedAt()
    .sign(key);
  const host = environment === "sandbox"
    ? "https://api.sandbox.push.apple.com"
    : "https://api.push.apple.com";

  const response = await fetch(`${host}/3/device/${token}`, {
    method: "POST",
    headers: {
      "authorization": `bearer ${jwt}`,
      "apns-topic": bundleId,
      "apns-push-type": "alert",
      "apns-priority": "10",
      "content-type": "application/json",
    },
    body: JSON.stringify({
      aps: {
        alert: { title, body },
        sound: "default",
      },
      data: pushData(payload),
    }),
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }
}

function pushData(payload: PushPayload): Record<string, string> {
  const kind = payload.kind ?? "";
  const refType = payload.ref_type ?? "";
  const deepLink =
    kind === "payment_reminder" ||
      kind === "payment" ||
      kind === "payment_confirmation" ||
      refType === "payment"
      ? "payments"
      : "";
  return {
    client_id: payload.client_id ?? "",
    kind,
    ref_type: refType,
    ref_id: payload.ref_id ?? "",
    deep_link: deepLink,
  };
}

function normalizePrivateKey(value?: string | null): string | null {
  if (!value) return null;
  return value.replace(/\\n/g, "\n");
}

function json(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}
