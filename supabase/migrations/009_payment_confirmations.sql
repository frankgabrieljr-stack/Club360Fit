-- Club360Fit: client "I paid" confirmations → coach approves → payment_records

CREATE TABLE IF NOT EXISTS public.payment_confirmations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  amount_label text,
  note text NOT NULL DEFAULT '',
  method text NOT NULL DEFAULT 'venmo',
  submitted_at timestamptz NOT NULL DEFAULT now(),
  status text NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'declined')),
  reviewed_at timestamptz,
  reviewed_by uuid REFERENCES auth.users(id) ON DELETE SET NULL,
  payment_record_id uuid REFERENCES public.payment_records(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_payment_confirmations_client_status
  ON public.payment_confirmations (client_id, status);

CREATE INDEX IF NOT EXISTS idx_payment_confirmations_submitted
  ON public.payment_confirmations (submitted_at DESC);

ALTER TABLE public.payment_confirmations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "client_insert_payment_confirmations" ON public.payment_confirmations;
CREATE POLICY "client_insert_payment_confirmations" ON public.payment_confirmations
  FOR INSERT WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "client_select_own_payment_confirmations" ON public.payment_confirmations;
CREATE POLICY "client_select_own_payment_confirmations" ON public.payment_confirmations
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "coach_select_payment_confirmations" ON public.payment_confirmations;
CREATE POLICY "coach_select_payment_confirmations" ON public.payment_confirmations
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );

DROP POLICY IF EXISTS "coach_update_payment_confirmations" ON public.payment_confirmations;
CREATE POLICY "coach_update_payment_confirmations" ON public.payment_confirmations
  FOR UPDATE USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );
