-- Club360Fit: habits, workout session logs, in-app notifications, expected_sessions on plans

-- Expected sessions per week for completion % (coach-editable in app)
ALTER TABLE public.workout_plans
  ADD COLUMN IF NOT EXISTS expected_sessions int NOT NULL DEFAULT 4
  CHECK (expected_sessions >= 1 AND expected_sessions <= 14);

-- One row per client per day: water (done/not), steps, sleep hours
CREATE TABLE IF NOT EXISTS public.daily_habit_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  log_date date NOT NULL,
  water_done boolean NOT NULL DEFAULT false,
  steps int,
  sleep_hours numeric(4,2),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (client_id, log_date)
);

CREATE INDEX IF NOT EXISTS idx_daily_habit_logs_client_date
  ON public.daily_habit_logs (client_id, log_date DESC);

ALTER TABLE public.daily_habit_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "client_rw_own_habit_logs" ON public.daily_habit_logs;
CREATE POLICY "client_rw_own_habit_logs" ON public.daily_habit_logs
  FOR ALL USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "coach_select_habit_logs" ON public.daily_habit_logs;
CREATE POLICY "coach_select_habit_logs" ON public.daily_habit_logs
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );

-- Client marks a workout session completed on a calendar day (week tracked for metrics)
CREATE TABLE IF NOT EXISTS public.workout_session_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  session_date date NOT NULL,
  week_start date NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (client_id, session_date)
);

CREATE INDEX IF NOT EXISTS idx_workout_session_logs_client_week
  ON public.workout_session_logs (client_id, week_start);

ALTER TABLE public.workout_session_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "client_rw_own_session_logs" ON public.workout_session_logs;
CREATE POLICY "client_rw_own_session_logs" ON public.workout_session_logs
  FOR ALL USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "coach_select_session_logs" ON public.workout_session_logs;
CREATE POLICY "coach_select_session_logs" ON public.workout_session_logs
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );

-- In-app notifications (missed workout nudges, etc.)
CREATE TABLE IF NOT EXISTS public.client_notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  kind text NOT NULL DEFAULT 'info',
  title text NOT NULL DEFAULT '',
  body text NOT NULL DEFAULT '',
  dedupe_key text,
  read_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_client_notifications_dedupe
  ON public.client_notifications (client_id, dedupe_key)
  WHERE dedupe_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_client_notifications_unread
  ON public.client_notifications (client_id, read_at);

ALTER TABLE public.client_notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "client_select_own_notifications" ON public.client_notifications;
CREATE POLICY "client_select_own_notifications" ON public.client_notifications
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "client_insert_own_notifications" ON public.client_notifications;
CREATE POLICY "client_insert_own_notifications" ON public.client_notifications
  FOR INSERT WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "client_update_own_notifications" ON public.client_notifications;
CREATE POLICY "client_update_own_notifications" ON public.client_notifications
  FOR UPDATE USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "coach_select_client_notifications" ON public.client_notifications;
CREATE POLICY "coach_select_client_notifications" ON public.client_notifications
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );
