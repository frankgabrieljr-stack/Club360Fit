-- Club360Fit: upcoming due on payment settings + payment history log

ALTER TABLE public.client_payment_settings
  ADD COLUMN IF NOT EXISTS next_due_date date,
  ADD COLUMN IF NOT EXISTS next_due_amount text,
  ADD COLUMN IF NOT EXISTS next_due_note text DEFAULT '';

CREATE TABLE IF NOT EXISTS public.payment_records (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  amount_label text,
  amount_cents int,
  paid_at timestamptz NOT NULL DEFAULT now(),
  method text NOT NULL DEFAULT 'other',
  note text NOT NULL DEFAULT '',
  recorded_by uuid REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payment_records_client_paid
  ON public.payment_records (client_id, paid_at DESC);

ALTER TABLE public.payment_records ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "coach_all_payment_records" ON public.payment_records;
CREATE POLICY "coach_all_payment_records" ON public.payment_records
  FOR ALL USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );

DROP POLICY IF EXISTS "client_select_payment_records" ON public.payment_records;
CREATE POLICY "client_select_payment_records" ON public.payment_records
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );
