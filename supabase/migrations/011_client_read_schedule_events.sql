-- Allow clients to read schedule rows linked to their client profile (coach still owns rows via user_id).

DROP POLICY IF EXISTS "client_select_own_schedule_events" ON public.schedule_events;
CREATE POLICY "client_select_own_schedule_events" ON public.schedule_events
  FOR SELECT USING (
    client_id IS NOT NULL
    AND client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );
