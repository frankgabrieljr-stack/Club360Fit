# Supabase setup for Club360Fit

## Run migrations

1. Open [Supabase Dashboard](https://supabase.com/dashboard) → your project → **SQL Editor**.
2. Run migrations in order:
   - `migrations/001_clients_and_schedule_events.sql` (or `20260312_club360fit_schema.sql` if you use that)
   - `migrations/020_clients_coach_id_nullable_for_signup.sql` – if `coach_id` was `NOT NULL`, makes it nullable so signup triggers can create a row before assignment
   - `migrations/021_rls_coach_claim_unassigned_clients.sql` – coaches (`user_metadata.role` = `admin`) can see unassigned clients, claim `coach_id`, and write meal/workout plans before claim
   - `migrations/022_workout_session_note_and_coach_reply.sql` – adds workout note/reply columns + coach UPDATE policy on `workout_session_logs`
   - `migrations/023_profiles_table_and_auth_sync.sql` – **`public.profiles`** (id, email, name, avatar, **role**), RLS, trigger on `auth.users` so **`profiles.role` stays aligned with `raw_user_meta_data.role`**, backfill for existing users
   - `migrations/003_plans_and_progress.sql` – workout_plans, meal_plans, progress_check_ins (and RLS)

This creates:

- **`public.clients`** – each member has a row; `coach_id` may be **NULL** until a coach is assigned (signup intake). Coaches see rows where `coach_id = auth.uid()` (RLS).
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

## Edge Functions

Deploy from the repo (requires [Supabase CLI](https://supabase.com/docs/guides/cli) linked to the project):

```bash
supabase functions deploy set-user-role --project-ref <YOUR_PROJECT_REF>
supabase functions deploy transfer-client --project-ref <YOUR_PROJECT_REF>
```

Hosted projects inject `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` automatically.

| Function | Purpose |
|----------|---------|
| `set-user-role` | Coach updates another user’s **`user_metadata.role`** (`admin` / `client`) — Hub “Account access” UI. Also **upserts `public.profiles`** so PostgREST queries match Auth. Redeploy after pulling changes. |
| `transfer-client` | **Current** coach sets `public.clients.coach_id` to another coach’s auth user id. Target must have `user_metadata.role` = `admin`. Unclaimed clients (`coach_id` null) must be claimed first. |

**Roles:** The apps route coach vs member using the **JWT** (`user_metadata.role`). After a role change, the user should **sign out and sign in** so the token updates. The **`profiles`** table is kept in sync for SQL/API reads (trigger + Edge Function).

## Payments (coach + client)

Run in order (after `clients` exists):

- `migrations/008_payment_history.sql` — `client_payment_settings` upcoming due + `payment_records` + RLS  
- `migrations/009_payment_confirmations.sql` — `payment_confirmations` (client “I paid” → coach approve → `payment_records`) + RLS  

Clients submit confirmations in the app; coaches review under **Client payment confirmations** and **Approve** to add an entry to logged payment history.

- `migrations/010_adherence_retention.sql` — daily habits, workout session logs, in-app notifications, `expected_sessions` on `workout_plans`
- `migrations/011_client_read_schedule_events.sql` — clients can `SELECT` `schedule_events` where `client_id` is their profile (needed for client schedule + adherence nudges)

## Community (peer support)

- `migrations/030_client_community.sql` — app-wide `community_posts` + `community_comments`, plus `fetch_community_member_directory()`
- `migrations/031_community_member_directory_profile.sql` — directory adds `user_id` + `avatar_url` for member community profiles (name/photo/coach only)
- `migrations/032_coach_community_replies.sql` — coaches can browse directory + reply (`is_coach_reply`); members still own posts
- `migrations/033_community_reply_notifications.sql` — on reply, notify post author (member inbox) + assigned coach (Updates); redeploy `send-device-push` for peer push auth
- `migrations/034_meal_photo_meal_slot.sql` — `meal_slot` (breakfast/lunch/dinner/snack/other) for day-grouped meal photo UI
