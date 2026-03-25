-- Fix for deployments that already applied 015 before INSERT/UPDATE were tightened.
-- Idempotent: same policies as current 015; safe to run if policies already match.

DROP POLICY IF EXISTS "client_insert_own_notifications" ON public.client_notifications;
CREATE POLICY "client_insert_own_notifications" ON public.client_notifications
  FOR INSERT WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
    AND visible_to_client = true
  );

DROP POLICY IF EXISTS "client_update_own_notifications" ON public.client_notifications;
CREATE POLICY "client_update_own_notifications" ON public.client_notifications
  FOR UPDATE USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
    AND visible_to_client = true
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
    AND visible_to_client = true
  );
