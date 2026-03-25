-- Allow coaches to mark notifications read for clients they coach (Hub “Updates” / bell badge).

DROP POLICY IF EXISTS "coach_update_client_notifications" ON public.client_notifications;
CREATE POLICY "coach_update_client_notifications" ON public.client_notifications
  FOR UPDATE TO authenticated
  USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );
