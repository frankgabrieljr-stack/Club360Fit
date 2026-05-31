-- Recurring payment due dates (coach sets weekly/monthly on client_payment_settings).

ALTER TABLE public.client_payment_settings
  ADD COLUMN IF NOT EXISTS due_recurrence text NOT NULL DEFAULT 'none';

ALTER TABLE public.client_payment_settings
  DROP CONSTRAINT IF EXISTS client_payment_settings_due_recurrence_check;

ALTER TABLE public.client_payment_settings
  ADD CONSTRAINT client_payment_settings_due_recurrence_check
  CHECK (due_recurrence IN ('none', 'weekly', 'monthly'));

COMMENT ON COLUMN public.client_payment_settings.due_recurrence IS
  'After a payment is logged or approved: none = keep date; weekly/monthly = advance next_due_date.';
