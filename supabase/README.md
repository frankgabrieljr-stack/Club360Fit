# Supabase setup for Club360Fit

## Run migrations

1. Open [Supabase Dashboard](https://supabase.com/dashboard) → your project → **SQL Editor**.
2. Run migrations in order:
   - `migrations/001_clients_and_schedule_events.sql` (or `20260312_club360fit_schema.sql` if you use that)
   - `migrations/003_plans_and_progress.sql` – workout_plans, meal_plans, progress_check_ins (and RLS)

This creates:

- **`public.clients`** – clients owned by the logged-in admin (`coach_id`). RLS limits rows to `coach_id = auth.uid()`.
- **`public.schedule_events`** – schedule events owned by the logged-in user (`user_id`). RLS limits rows to `user_id = auth.uid()`.

After **003_plans_and_progress.sql**:

- **`public.workout_plans`** / **`public.meal_plans`** – one row per client per week; coaches full access, clients read-only for their own.
- **`public.progress_check_ins`** – progress tracker (date, weight, workout/meals done, notes); coaches full access, clients can read and add their own.

## If you already have a `clients` table

If the table was created without `coach_id`, run before the full migration (or in place of the `create table` for clients):

```sql
alter table public.clients add column if not exists coach_id uuid references auth.users(id);
-- Backfill: set coach_id to a valid user id, then:
-- alter table public.clients alter column coach_id set not null;
```

Then add the RLS policies from the migration file.

## Payments (coach + client)

Run in order (after `clients` exists):

- `migrations/008_payment_history.sql` — `client_payment_settings` upcoming due + `payment_records` + RLS  
- `migrations/009_payment_confirmations.sql` — `payment_confirmations` (client “I paid” → coach approve → `payment_records`) + RLS  

Clients submit confirmations in the app; coaches review under **Client payment confirmations** and **Approve** to add an entry to logged payment history.
