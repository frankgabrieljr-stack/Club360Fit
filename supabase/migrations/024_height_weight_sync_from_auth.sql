-- =============================================================
-- Migration 024: Height / Weight sync from auth.users → public.clients
-- Purpose: Sync height_cm and weight_kg from auth.users.raw_user_meta_data
--          into public.clients whenever a user upserts their metadata.
-- Additive to 023; does NOT touch public.profiles or sync_profile_from_auth_user.
-- =============================================================

-- -------------------------------------------------------------
-- 0. Drop legacy function names from any earlier drafts (idempotent)
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS sync_clients_metrics_from_auth        ON auth.users;
DROP TRIGGER IF EXISTS sync_clients_metrics_from_auth_user   ON auth.users;
DROP TRIGGER IF EXISTS sync_client_biometrics_from_auth      ON auth.users;

DROP FUNCTION IF EXISTS public.sync_clients_metrics_from_auth()        CASCADE;
DROP FUNCTION IF EXISTS public.sync_clients_metrics_from_auth_user()   CASCADE;
DROP FUNCTION IF EXISTS public.sync_client_biometrics_from_auth()      CASCADE;

-- -------------------------------------------------------------
-- 1. Guard function: parse text → positive integer ≤ 999, or NULL
--    Single function covers both height (cm) and weight (kg).
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.safe_positive_numeric(txt TEXT)
RETURNS NUMERIC
LANGUAGE plpgsql
IMMUTABLE
PARALLEL SAFE
AS $$
DECLARE
  v NUMERIC;
BEGIN
  IF txt IS NULL OR btrim(txt) = '' THEN
    RETURN NULL;
  END IF;

  BEGIN
    v := btrim(txt)::NUMERIC;
  EXCEPTION WHEN others THEN
    RETURN NULL;  -- non-numeric string
  END;

  IF v <= 0 OR v > 999 THEN
    RETURN NULL;
  END IF;

  RETURN v;
END;
$$;

-- -------------------------------------------------------------
-- 2. Backfill: fill height_cm / weight_kg only where column is NULL
--    and metadata has a valid value. IS DISTINCT FROM skips no-ops.
-- -------------------------------------------------------------
UPDATE public.clients c
SET
  height_cm  = CASE
                 WHEN c.height_cm IS NULL
                      AND public.safe_positive_numeric(u.raw_user_meta_data->>'height_cm') IS NOT NULL
                 THEN ROUND(public.safe_positive_numeric(u.raw_user_meta_data->>'height_cm'))::INT
                 ELSE c.height_cm
               END,
  weight_kg  = CASE
                 WHEN c.weight_kg IS NULL
                      AND public.safe_positive_numeric(u.raw_user_meta_data->>'weight_kg') IS NOT NULL
                 THEN ROUND(public.safe_positive_numeric(u.raw_user_meta_data->>'weight_kg'))::INT
                 ELSE c.weight_kg
               END,
  updated_at = CASE
                 WHEN (
                   (c.height_cm IS NULL AND public.safe_positive_numeric(u.raw_user_meta_data->>'height_cm') IS NOT NULL)
                   OR
                   (c.weight_kg IS NULL AND public.safe_positive_numeric(u.raw_user_meta_data->>'weight_kg') IS NOT NULL)
                 ) THEN NOW()
                 ELSE c.updated_at
               END
FROM auth.users u
WHERE c.user_id = u.id
  AND (
    (c.height_cm IS NULL AND public.safe_positive_numeric(u.raw_user_meta_data->>'height_cm') IS NOT NULL)
    OR
    (c.weight_kg IS NULL AND public.safe_positive_numeric(u.raw_user_meta_data->>'weight_kg') IS NOT NULL)
  );

-- -------------------------------------------------------------
-- 3. Trigger function
-- -------------------------------------------------------------
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
  new_h := ROUND(public.safe_positive_numeric(NEW.raw_user_meta_data->>'height_cm'))::INT;
  new_w := ROUND(public.safe_positive_numeric(NEW.raw_user_meta_data->>'weight_kg'))::INT;

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

-- -------------------------------------------------------------
-- 4. Attach trigger (Postgres 14+ EXECUTE FUNCTION syntax)
-- -------------------------------------------------------------
CREATE TRIGGER sync_client_biometrics_from_auth
  AFTER INSERT OR UPDATE OF raw_user_meta_data
  ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.sync_client_biometrics_from_auth();
