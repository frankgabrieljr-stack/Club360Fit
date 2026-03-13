-- Club360Fit: real clients and schedule_events collections
-- Run this in Supabase Dashboard → SQL Editor (or via Supabase CLI).
-- If you already have a public.clients table without coach_id, run first:
--   alter table public.clients add column if not exists coach_id uuid references auth.users(id);
--   update public.clients set coach_id = (select id from auth.users limit 1) where coach_id is null;
--   alter table public.clients alter column coach_id set not null;  -- only if you backfilled

-- =============================================================================
-- 1. CLIENTS TABLE
-- =============================================================================
-- Each row is a client linked to an auth user (user_id). coach_id = admin who owns this client.
create table if not exists public.clients (
  id uuid primary key default gen_random_uuid(),
  coach_id uuid not null references auth.users(id) on delete cascade,
  user_id uuid not null,
  full_name text,
  age int,
  height_cm int,
  weight_kg int,
  phone text,
  medical_conditions text,
  food_restrictions text,
  goal text,
  can_view_nutrition boolean default false,
  can_view_workouts boolean default false,
  can_view_payments boolean default false,
  can_view_events boolean default false,
  last_active text,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- RLS: admins see only clients they own (coach_id = current user)
alter table public.clients enable row level security;

create policy "Users can read own coached clients"
  on public.clients for select
  using (auth.uid() = coach_id);

create policy "Users can insert clients they coach"
  on public.clients for insert
  with check (auth.uid() = coach_id);

create policy "Users can update own coached clients"
  on public.clients for update
  using (auth.uid() = coach_id);

create policy "Users can delete own coached clients"
  on public.clients for delete
  using (auth.uid() = coach_id);

-- =============================================================================
-- 2. SCHEDULE_EVENTS TABLE
-- =============================================================================
-- Events belong to the logged-in user (admin). Optional client_id links to a client.
create table if not exists public.schedule_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  date date not null,
  time text default '',
  notes text default '',
  client_id uuid null,
  is_completed boolean default false,
  created_at timestamptz default now()
);

alter table public.schedule_events enable row level security;

create policy "Users can read own schedule events"
  on public.schedule_events for select
  using (auth.uid() = user_id);

create policy "Users can insert own schedule events"
  on public.schedule_events for insert
  with check (auth.uid() = user_id);

create policy "Users can update own schedule events"
  on public.schedule_events for update
  using (auth.uid() = user_id);

create policy "Users can delete own schedule events"
  on public.schedule_events for delete
  using (auth.uid() = user_id);
