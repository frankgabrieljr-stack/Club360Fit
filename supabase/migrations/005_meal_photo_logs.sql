-- Club360Fit: meal photo logs (clients upload daily meal photos for coach review)
-- Run in Supabase SQL Editor after creating Storage bucket (see below).

CREATE TABLE IF NOT EXISTS public.meal_photo_logs (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id    uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  log_date     date NOT NULL,
  storage_path text NOT NULL,
  notes        text NOT NULL DEFAULT '',
  created_at   timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_meal_photo_logs_client_date
  ON public.meal_photo_logs (client_id, log_date DESC);

ALTER TABLE public.meal_photo_logs ENABLE ROW LEVEL SECURITY;

-- Client: insert/select/delete own rows only
DROP POLICY IF EXISTS "client_insert_meal_photo_logs" ON public.meal_photo_logs;
CREATE POLICY "client_insert_meal_photo_logs" ON public.meal_photo_logs
  FOR INSERT WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "client_select_meal_photo_logs" ON public.meal_photo_logs;
CREATE POLICY "client_select_meal_photo_logs" ON public.meal_photo_logs
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "client_delete_meal_photo_logs" ON public.meal_photo_logs;
CREATE POLICY "client_delete_meal_photo_logs" ON public.meal_photo_logs
  FOR DELETE USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

-- Coach: read-only for clients they coach
DROP POLICY IF EXISTS "coach_select_meal_photo_logs" ON public.meal_photo_logs;
CREATE POLICY "coach_select_meal_photo_logs" ON public.meal_photo_logs
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );

-- Storage: public bucket (matches app publicUrl() like transformations bucket).
INSERT INTO storage.buckets (id, name, public)
VALUES ('meal-photos', 'meal-photos', true)
ON CONFLICT (id) DO NOTHING;

-- Clients may only upload/delete under their own client_id folder prefix.
DROP POLICY IF EXISTS "meal_photos_insert_own_client" ON storage.objects;
CREATE POLICY "meal_photos_insert_own_client"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
  bucket_id = 'meal-photos'
  AND split_part(name, '/', 1) IN (
    SELECT id::text FROM public.clients WHERE user_id = auth.uid()
  )
);

DROP POLICY IF EXISTS "meal_photos_delete_own_client" ON storage.objects;
CREATE POLICY "meal_photos_delete_own_client"
ON storage.objects FOR DELETE TO authenticated
USING (
  bucket_id = 'meal-photos'
  AND split_part(name, '/', 1) IN (
    SELECT id::text FROM public.clients WHERE user_id = auth.uid()
  )
);
