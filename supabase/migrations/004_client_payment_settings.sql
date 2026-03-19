-- Club360Fit: per-client payment settings (Venmo/Zelle)
-- Run in Supabase SQL Editor. Safe to run multiple times.

CREATE TABLE IF NOT EXISTS public.client_payment_settings (
  client_id  uuid PRIMARY KEY REFERENCES public.clients(id) ON DELETE CASCADE,
  venmo_url  text,
  zelle_email text,
  zelle_phone text,
  note       text DEFAULT '',
  updated_at timestamptz DEFAULT now()
);

ALTER TABLE public.client_payment_settings ENABLE ROW LEVEL SECURITY;

-- Admin/coach can read/write settings for clients they coach.
DROP POLICY IF EXISTS "coach_rw_client_payment_settings" ON public.client_payment_settings;
CREATE POLICY "coach_rw_client_payment_settings" ON public.client_payment_settings
  FOR ALL USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );

-- Client can read their own payment settings.
DROP POLICY IF EXISTS "client_r_client_payment_settings" ON public.client_payment_settings;
CREATE POLICY "client_r_client_payment_settings" ON public.client_payment_settings
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

