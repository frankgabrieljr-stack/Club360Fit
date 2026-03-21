-- Club360Fit: normalize meal_photo_logs.notes (no NULLs; consistent with app + JSON decoding)
-- Safe to run multiple times.

UPDATE public.meal_photo_logs
SET notes = ''
WHERE notes IS NULL;

ALTER TABLE public.meal_photo_logs
  ALTER COLUMN notes SET DEFAULT '';

ALTER TABLE public.meal_photo_logs
  ALTER COLUMN notes SET NOT NULL;
