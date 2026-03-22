-- Coach feedback on client meal photos (text + timestamp). Clients read; coaches update.

ALTER TABLE public.meal_photo_logs
  ADD COLUMN IF NOT EXISTS coach_feedback text,
  ADD COLUMN IF NOT EXISTS coach_feedback_updated_at timestamptz;

COMMENT ON COLUMN public.meal_photo_logs.coach_feedback IS 'Coach notes on portion, balance, etc.';
COMMENT ON COLUMN public.meal_photo_logs.coach_feedback_updated_at IS 'Last time coach_feedback was saved';

-- Coaches may update feedback on their clients' meal photo rows (RLS restricts to coached clients).
DROP POLICY IF EXISTS "coach_update_meal_photo_logs_feedback" ON public.meal_photo_logs;
CREATE POLICY "coach_update_meal_photo_logs_feedback" ON public.meal_photo_logs
  FOR UPDATE TO authenticated
  USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );

-- Optional: prevent coaches from changing core row data (only feedback columns).
CREATE OR REPLACE FUNCTION public.meal_photo_logs_coach_update_guard()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  is_coach_for_row boolean;
BEGIN
  IF TG_OP <> 'UPDATE' THEN
    RETURN NEW;
  END IF;
  SELECT EXISTS (
    SELECT 1 FROM public.clients c
    WHERE c.id = NEW.client_id AND c.coach_id = auth.uid()
  ) INTO is_coach_for_row;
  IF NOT is_coach_for_row THEN
    RETURN NEW;
  END IF;
  IF OLD.id IS DISTINCT FROM NEW.id
     OR OLD.client_id IS DISTINCT FROM NEW.client_id
     OR OLD.log_date IS DISTINCT FROM NEW.log_date
     OR OLD.storage_path IS DISTINCT FROM NEW.storage_path
     OR OLD.notes IS DISTINCT FROM NEW.notes
     OR OLD.created_at IS DISTINCT FROM NEW.created_at THEN
    RAISE EXCEPTION 'Coaches may only update coach feedback fields';
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS meal_photo_logs_coach_update_guard ON public.meal_photo_logs;
CREATE TRIGGER meal_photo_logs_coach_update_guard
  BEFORE UPDATE ON public.meal_photo_logs
  FOR EACH ROW
  EXECUTE FUNCTION public.meal_photo_logs_coach_update_guard();
