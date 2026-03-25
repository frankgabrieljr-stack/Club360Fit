-- Coach-only notifications: rows the member app should not show (e.g. “client uploaded meal photo”).
-- Clients still SELECT only their rows where visible_to_client = true; coaches see all for coached clients.
-- INSERT/UPDATE: clients may only touch member-visible rows (coach-only rows are coach/coach-insert only).

ALTER TABLE public.client_notifications
  ADD COLUMN IF NOT EXISTS visible_to_client boolean NOT NULL DEFAULT true;

COMMENT ON COLUMN public.client_notifications.visible_to_client IS
  'When false, only the coach sees this row (member SELECT policy hides it).';

DROP POLICY IF EXISTS "client_select_own_notifications" ON public.client_notifications;
CREATE POLICY "client_select_own_notifications" ON public.client_notifications
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
    AND visible_to_client = true
  );

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
