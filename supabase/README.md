# Supabase setup for Club360Fit

## Run migrations

1. Open [Supabase Dashboard](https://supabase.com/dashboard) → your project → **SQL Editor**.
2. Copy the contents of `migrations/001_clients_and_schedule_events.sql` and run it.

This creates:

- **`public.clients`** – clients owned by the logged-in admin (`coach_id`). RLS limits rows to `coach_id = auth.uid()`.
- **`public.schedule_events`** – schedule events owned by the logged-in user (`user_id`). RLS limits rows to `user_id = auth.uid()`.

## If you already have a `clients` table

If the table was created without `coach_id`, run before the full migration (or in place of the `create table` for clients):

```sql
alter table public.clients add column if not exists coach_id uuid references auth.users(id);
-- Backfill: set coach_id to a valid user id, then:
-- alter table public.clients alter column coach_id set not null;
```

Then add the RLS policies from the migration file.
