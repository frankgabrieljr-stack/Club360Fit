-- Fix avatar uploads failing RLS when Swift sends uppercase UUID in the path:
--   `split_part(name, '/', 1) = auth.uid()::text` is CASE-SENSITIVE.
--   UUID.uuidString on Apple platforms is often UPPERCASE; auth.uid()::text is lowercase.
-- Use case-insensitive match (or re-run equivalent policies in Dashboard).

DROP POLICY IF EXISTS "avatars_insert_own" ON storage.objects;
CREATE POLICY "avatars_insert_own"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
  bucket_id = 'avatars'
  AND lower(split_part(name, '/', 1)) = lower(auth.uid()::text)
);

DROP POLICY IF EXISTS "avatars_update_own" ON storage.objects;
CREATE POLICY "avatars_update_own"
ON storage.objects FOR UPDATE TO authenticated
USING (
  bucket_id = 'avatars'
  AND lower(split_part(name, '/', 1)) = lower(auth.uid()::text)
)
WITH CHECK (
  bucket_id = 'avatars'
  AND lower(split_part(name, '/', 1)) = lower(auth.uid()::text)
);

DROP POLICY IF EXISTS "avatars_delete_own" ON storage.objects;
CREATE POLICY "avatars_delete_own"
ON storage.objects FOR DELETE TO authenticated
USING (
  bucket_id = 'avatars'
  AND lower(split_part(name, '/', 1)) = lower(auth.uid()::text)
);
