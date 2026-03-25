-- Deep links for in-app notifications (tap → screen).
ALTER TABLE public.client_notifications
  ADD COLUMN IF NOT EXISTS ref_type text,
  ADD COLUMN IF NOT EXISTS ref_id text;

COMMENT ON COLUMN public.client_notifications.ref_type IS 'Optional target: meal_photo, workout, meal_plan, schedule, payment, progress, habit';
COMMENT ON COLUMN public.client_notifications.ref_id IS 'Optional row id (e.g. meal_photo_logs.id)';
