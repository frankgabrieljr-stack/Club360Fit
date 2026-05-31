// Deploy: `supabase functions deploy payment-due-reminders --project-ref <ref>`
// Schedule daily via Supabase Dashboard → Integrations → Cron, or see supabase/docs/payment-reminders-cron.md
//
// Secrets: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, PAYMENT_REMINDER_CRON_SECRET
// Optional (for push): same FCM/APNs secrets as send-device-push

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type, x-cron-secret",
};

type SettingsRow = {
  client_id: string;
  next_due_date: string | null;
  next_due_amount: string | null;
  next_due_note: string | null;
  due_recurrence: string | null;
};

type ClientRow = {
  id: string;
  user_id: string;
  coach_id: string | null;
  full_name: string | null;
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: cors });
  }

  try {
    const cronSecret = Deno.env.get("PAYMENT_REMINDER_CRON_SECRET");
    const headerSecret = req.headers.get("x-cron-secret");
    const authHeader = req.headers.get("Authorization") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const authorized =
      (cronSecret && headerSecret === cronSecret) ||
      (serviceRole && authHeader === `Bearer ${serviceRole}`);

    if (!authorized) {
      return json(401, { error: "Unauthorized" });
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const admin = createClient(supabaseUrl, serviceRole, {
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const today = isoDay(new Date());
    const tomorrow = isoDay(addDays(new Date(), 1));

    const { data: settingsRows, error: settingsErr } = await admin
      .from("client_payment_settings")
      .select("client_id,next_due_date,next_due_amount,next_due_note,due_recurrence")
      .not("next_due_date", "is", null);

    if (settingsErr) return json(500, { error: settingsErr.message });

    let memberInserted = 0;
    let coachInserted = 0;
    const pushTargets: Array<{
      client_id: string;
      kind: string;
      title: string;
      body: string;
      visible_to_client: boolean;
      dedupe_key: string;
    }> = [];

    for (const row of (settingsRows ?? []) as SettingsRow[]) {
      const due = row.next_due_date?.trim();
      if (!due) continue;

      const { data: clientRaw, error: clientErr } = await admin
        .from("clients")
        .select("id,user_id,coach_id,full_name")
        .eq("id", row.client_id)
        .maybeSingle();

      if (clientErr || !clientRaw) continue;
      const client = clientRaw as ClientRow;
      const name = (client.full_name ?? "").trim() || "Client";
      const amount = (row.next_due_amount ?? "").trim();
      const amountPart = amount ? ` — ${amount}` : "";

      if (due === today) {
        const memberBody = amount
          ? `Amount: ${amount}. Open Payments to confirm or pay.`
          : "Open Payments to confirm or pay.";
        const memberDedupe = `cron_payment_due_today:${row.client_id}:${due}`;
        const inserted = await insertNotification(admin, {
          client_id: row.client_id,
          kind: "payment_reminder",
          title: "Payment due today",
          body: memberBody,
          ref_type: "payment",
          ref_id: row.client_id,
          dedupe_key: memberDedupe,
          visible_to_client: true,
        });
        if (inserted) {
          memberInserted += 1;
          pushTargets.push({
            client_id: row.client_id,
            kind: "payment_reminder",
            title: "Payment due today",
            body: memberBody,
            visible_to_client: true,
            dedupe_key: memberDedupe,
          });
        }

        if (client.coach_id) {
          const coachBody = `${name}${amountPart}`.trim();
          const coachDedupe = `cron_coach_payment_due_today:${row.client_id}:${due}`;
          const coachInsertedRow = await insertNotification(admin, {
            client_id: row.client_id,
            kind: "payment_reminder",
            title: "Payment due today",
            body: coachBody,
            ref_type: "payment",
            ref_id: row.client_id,
            dedupe_key: coachDedupe,
            visible_to_client: false,
          });
          if (coachInsertedRow) {
            coachInserted += 1;
            pushTargets.push({
              client_id: row.client_id,
              kind: "payment_reminder",
              title: "Payment due today",
              body: coachBody,
              visible_to_client: false,
              dedupe_key: coachDedupe,
            });
          }
        }
      } else if (due === tomorrow) {
        const memberBody = `Due tomorrow${amountPart}`;
        const memberDedupe = `cron_payment_due_tomorrow:${row.client_id}:${due}`;
        const inserted = await insertNotification(admin, {
          client_id: row.client_id,
          kind: "payment_reminder",
          title: "Payment due tomorrow",
          body: memberBody,
          ref_type: "payment",
          ref_id: row.client_id,
          dedupe_key: memberDedupe,
          visible_to_client: true,
        });
        if (inserted) {
          memberInserted += 1;
          pushTargets.push({
            client_id: row.client_id,
            kind: "payment_reminder",
            title: "Payment due tomorrow",
            body: memberBody,
            visible_to_client: true,
            dedupe_key: memberDedupe,
          });
        }
      }
    }

    let pushSent = 0;
    const fnBase = `${supabaseUrl}/functions/v1/send-device-push`;
    for (const p of pushTargets) {
      try {
        const res = await fetch(fnBase, {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${serviceRole}`,
            "Content-Type": "application/json",
            "x-cron-secret": cronSecret ?? "",
          },
          body: JSON.stringify({
            client_id: p.client_id,
            kind: p.kind,
            title: p.title,
            body: p.body,
            ref_type: "payment",
            ref_id: p.client_id,
            dedupe_key: p.dedupe_key,
            visible_to_client: p.visible_to_client,
          }),
        });
        if (res.ok) {
          const j = await res.json();
          if (j.sent > 0) pushSent += j.sent;
        }
      } catch {
        /* push best-effort */
      }
    }

    return json(200, {
      ok: true,
      today,
      tomorrow,
      member_notifications: memberInserted,
      coach_notifications: coachInserted,
      push_sent: pushSent,
    });
  } catch (e) {
    return json(500, { error: String(e) });
  }
});

async function insertNotification(
  admin: ReturnType<typeof createClient>,
  row: Record<string, unknown>,
): Promise<boolean> {
  const { error } = await admin.from("client_notifications").insert(row);
  if (!error) return true;
  if (error.code === "23505") return false;
  console.error("insert_notification", error);
  return false;
}

function isoDay(d: Date): string {
  return d.toISOString().slice(0, 10);
}

function addDays(d: Date, days: number): Date {
  const x = new Date(d);
  x.setUTCDate(x.getUTCDate() + days);
  return x;
}

function json(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}
