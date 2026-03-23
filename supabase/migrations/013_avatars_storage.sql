-- Club360Fit: Storage bucket `avatars` + RLS for profile photos (iOS UserProfileView / Android UserProfileScreen).
-- Path used by apps: `{auth.uid()}/avatar.jpg`
-- Without these policies, uploads fail with: "new row violates row-level security policy"

INSERT INTO storage.buckets (id, name, public)
VALUES ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

-- Ensure public read for getPublicURL() / AsyncImage (no-op if already public)
UPDATE storage.buckets SET public = true WHERE id = 'avatars';

-- Authenticated users may only write under their own user-id folder.
DROP POLICY IF EXISTS "avatars_insert_own" ON storage.objects;
CREATE POLICY "avatars_insert_own"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
  bucket_id = 'avatars'
  AND lower(split_part(name, '/', 1)) = lower(auth.uid()::text)
);

-- Upsert (overwrite) requires UPDATE on storage.objects
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
