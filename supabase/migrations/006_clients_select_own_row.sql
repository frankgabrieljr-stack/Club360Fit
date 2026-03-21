-- Club360Fit: clients must be able to read their own row in public.clients.
-- Meal photo storage + meal_photo_logs RLS policies use:
--   SELECT id FROM public.clients WHERE user_id = auth.uid()
-- The original clients policies only allowed SELECT when auth.uid() = coach_id,
-- so end-user clients saw zero rows from that subquery and uploads failed with
-- "new row violates row-level security policy" on storage INSERT.

DROP POLICY IF EXISTS "Clients can read own client row" ON public.clients;
CREATE POLICY "Clients can read own client row"
  ON public.clients FOR SELECT
  USING (auth.uid() = user_id);
