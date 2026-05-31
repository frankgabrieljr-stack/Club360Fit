# Payment due reminders (server cron)

The `payment-due-reminders` Edge Function scans `client_payment_settings.next_due_date` and creates deduped `client_notifications` rows (member + coach for “due today”), then calls `send-device-push` for device alerts.

## Deploy

```bash
supabase functions deploy payment-due-reminders --project-ref <your-ref>
```

Set secrets (Dashboard → Project Settings → Edge Functions):

- `PAYMENT_REMINDER_CRON_SECRET` — long random string; pass as header `x-cron-secret` from cron
- `SUPABASE_SERVICE_ROLE_KEY` — already present for other functions
- FCM/APNs secrets — same as `send-device-push`

`send-device-push` must accept cron calls: it allows `Authorization: Bearer <service_role>` or matching `x-cron-secret` when `PAYMENT_REMINDER_CRON_SECRET` is set.

## Schedule (Supabase Dashboard)

1. **Integrations → Cron Jobs** (or Database → Extensions: enable `pg_cron` + `pg_net` if using SQL).
2. Create a daily job (e.g. `0 14 * * *` UTC).
3. HTTP POST to:

   `https://<project-ref>.supabase.co/functions/v1/payment-due-reminders`

   Headers:

   - `Content-Type: application/json`
   - `x-cron-secret: <PAYMENT_REMINDER_CRON_SECRET>`

   Body: `{}`

## Apply migration

Run `029_payment_recurrence.sql` so coaches can set `due_recurrence` (`none` | `weekly` | `monthly`).
