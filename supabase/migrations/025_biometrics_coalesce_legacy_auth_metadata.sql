-- =============================================================
-- Migration 025: Seamless height/weight from auth metadata
-- Extends 024: same columns and trigger name; adds fallback to
-- legacy signup keys `height` / `weight` when `height_cm` /
-- `weight_kg` are absent (older apps only wrote the short keys).
-- Idempotent: CREATE OR REPLACE only; safe to re-run.
-- =============================================================

-- Prefer canonical keys; otherwise use legacy keys (same units: cm / kg).
CREATE OR REPLACE FUNCTION public.auth_meta_height_cm_int(meta JSONB)
RETURNS INT
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
  SELECT COALESCE(
    (ROUND(public.safe_positive_numeric(meta->>'height_cm')))::INT,
    (ROUND(public.safe_positive_numeric(meta->>'height')))::INT
  );
$$;

CREATE OR REPLACE FUNCTION public.auth_meta_weight_kg_int(meta JSONB)
RETURNS INT
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
  SELECT COALESCE(
    (ROUND(public.safe_positive_numeric(meta->>'weight_kg')))::INT,
    (ROUND(public.safe_positive_numeric(meta->>'weight')))::INT
  );
$$;

CREATE OR REPLACE FUNCTION public.sync_client_biometrics_from_auth()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public
LANGUAGE plpgsql
AS $$
DECLARE
  new_h INT;
  new_w INT;
BEGIN
  new_h := public.auth_meta_height_cm_int(NEW.raw_user_meta_data);
  new_w := public.auth_meta_weight_kg_int(NEW.raw_user_meta_data);

  IF new_h IS NULL AND new_w IS NULL THEN
    RETURN NEW;
  END IF;

  UPDATE public.clients
  SET
    height_cm  = CASE
                   WHEN COALESCE(new_h, height_cm) IS DISTINCT FROM height_cm
                   THEN COALESCE(new_h, height_cm)
                   ELSE height_cm
                 END,
    weight_kg  = CASE
                   WHEN COALESCE(new_w, weight_kg) IS DISTINCT FROM weight_kg
                   THEN COALESCE(new_w, weight_kg)
                   ELSE weight_kg
                 END,
    updated_at = CASE
                   WHEN COALESCE(new_h, height_cm) IS DISTINCT FROM height_cm
                     OR COALESCE(new_w, weight_kg)  IS DISTINCT FROM weight_kg
                   THEN NOW()
                   ELSE updated_at
                 END
  WHERE user_id = NEW.id;

  RETURN NEW;
END;
$$;

-- Backfill: NULL columns only, using canonical OR legacy metadata keys.
UPDATE public.clients c
SET
  height_cm  = CASE
                 WHEN c.height_cm IS NULL
                      AND public.auth_meta_height_cm_int(u.raw_user_meta_data) IS NOT NULL
                 THEN public.auth_meta_height_cm_int(u.raw_user_meta_data)
                 ELSE c.height_cm
               END,
  weight_kg  = CASE
                 WHEN c.weight_kg IS NULL
                      AND public.auth_meta_weight_kg_int(u.raw_user_meta_data) IS NOT NULL
                 THEN public.auth_meta_weight_kg_int(u.raw_user_meta_data)
                 ELSE c.weight_kg
               END,
  updated_at = CASE
                 WHEN (
                   (c.height_cm IS NULL AND public.auth_meta_height_cm_int(u.raw_user_meta_data) IS NOT NULL)
                   OR
                   (c.weight_kg IS NULL AND public.auth_meta_weight_kg_int(u.raw_user_meta_data) IS NOT NULL)
                 ) THEN NOW()
                 ELSE c.updated_at
               END
FROM auth.users u
WHERE c.user_id = u.id
  AND (
    (c.height_cm IS NULL AND public.auth_meta_height_cm_int(u.raw_user_meta_data) IS NOT NULL)
    OR
    (c.weight_kg IS NULL AND public.auth_meta_weight_kg_int(u.raw_user_meta_data) IS NOT NULL)
  );
