-- ================================================================
-- Club360Fit – initial schema migration
-- Run in Supabase SQL Editor (or supabase db push if using CLI).
--
-- NOTE – if public.clients already exists WITHOUT coach_id:
--   1. ALTER TABLE public.clients
--        ADD COLUMN IF NOT EXISTS coach_id uuid
--        REFERENCES auth.users(id) ON DELETE CASCADE;
--   2. UPDATE public.clients SET coach_id = user_id WHERE coach_id IS NULL;
--   3. ALTER TABLE public.clients ALTER COLUMN coach_id SET NOT NULL;
--   Then re-run the policy section below.
-- ================================================================

-- ── clients ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.clients (
  id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  coach_id             uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  user_id              uuid NOT NULL,
  full_name            text,
  age                  int,
  height_cm            int,
  weight_kg            int,
  phone                text,
  medical_conditions   text,
  food_restrictions    text,
  goal                 text,
  can_view_nutrition   boolean DEFAULT false,
  can_view_workouts    boolean DEFAULT false,
  can_view_payments    boolean DEFAULT false,
  can_view_events      boolean DEFAULT false,
  last_active          text,
  created_at           timestamptz DEFAULT now(),
  updated_at           timestamptz DEFAULT now()
);

ALTER TABLE public.clients ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "coaches_select_clients" ON public.clients;
DROP POLICY IF EXISTS "coaches_insert_clients" ON public.clients;
DROP POLICY IF EXISTS "coaches_update_clients" ON public.clients;
DROP POLICY IF EXISTS "coaches_delete_clients" ON public.clients;

CREATE POLICY "coaches_select_clients" ON public.clients FOR SELECT USING (auth.uid() = coach_id);
CREATE POLICY "coaches_insert_clients" ON public.clients FOR INSERT WITH CHECK (auth.uid() = coach_id);
CREATE POLICY "coaches_update_clients" ON public.clients FOR UPDATE USING (auth.uid() = coach_id);
CREATE POLICY "coaches_delete_clients" ON public.clients FOR DELETE USING (auth.uid() = coach_id);

-- ── schedule_events ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.schedule_events (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  title        text NOT NULL,
  date         date NOT NULL,
  time         text DEFAULT '',
  notes        text DEFAULT '',
  client_id    uuid,
  is_completed boolean DEFAULT false,
  created_at   timestamptz DEFAULT now()
);

ALTER TABLE public.schedule_events ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "owner_select_events" ON public.schedule_events;
DROP POLICY IF EXISTS "owner_insert_events" ON public.schedule_events;
DROP POLICY IF EXISTS "owner_update_events" ON public.schedule_events;
DROP POLICY IF EXISTS "owner_delete_events" ON public.schedule_events;

CREATE POLICY "owner_select_events"  ON public.schedule_events FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "owner_insert_events"  ON public.schedule_events FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "owner_update_events"  ON public.schedule_events FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "owner_delete_events"  ON public.schedule_events FOR DELETE USING (auth.uid() = user_id);
