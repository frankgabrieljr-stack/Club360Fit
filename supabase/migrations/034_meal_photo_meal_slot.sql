-- Meal photo slot (breakfast / lunch / dinner / snack) for day-grouped client + coach UI.

ALTER TABLE public.meal_photo_logs
  ADD COLUMN IF NOT EXISTS meal_slot text NOT NULL DEFAULT 'other';

UPDATE public.meal_photo_logs
SET meal_slot = 'other'
WHERE meal_slot IS NULL
   OR meal_slot NOT IN ('breakfast', 'lunch', 'dinner', 'snack', 'other');

ALTER TABLE public.meal_photo_logs
  DROP CONSTRAINT IF EXISTS meal_photo_logs_meal_slot_check;

ALTER TABLE public.meal_photo_logs
  ADD CONSTRAINT meal_photo_logs_meal_slot_check
  CHECK (meal_slot IN ('breakfast', 'lunch', 'dinner', 'snack', 'other'));

COMMENT ON COLUMN public.meal_photo_logs.meal_slot IS
  'Which meal of the day this photo is for: breakfast, lunch, dinner, snack, or other.';

CREATE INDEX IF NOT EXISTS idx_meal_photo_logs_client_date_slot
  ON public.meal_photo_logs (client_id, log_date DESC, meal_slot);

-- Coaches still may only update feedback columns (not meal_slot / core fields).
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
     OR OLD.created_at IS DISTINCT FROM NEW.created_at
     OR OLD.meal_slot IS DISTINCT FROM NEW.meal_slot THEN
    RAISE EXCEPTION 'Coaches may only update coach feedback fields';
  END IF;
  RETURN NEW;
END;
$$;
