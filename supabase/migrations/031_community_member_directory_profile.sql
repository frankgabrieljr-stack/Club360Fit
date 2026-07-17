-- Extend member directory for community profiles: auth user id + avatar (public fields only).
-- Must DROP first: Postgres cannot change OUT/return row type via CREATE OR REPLACE.

DROP FUNCTION IF EXISTS public.fetch_community_member_directory();

CREATE OR REPLACE FUNCTION public.fetch_community_member_directory()
RETURNS TABLE (
  client_id uuid,
  user_id uuid,
  member_display_name text,
  coach_id uuid,
  coach_display_name text,
  avatar_url text
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT
    c.id AS client_id,
    c.user_id,
    COALESCE(NULLIF(trim(c.full_name), ''), 'Member') AS member_display_name,
    c.coach_id,
    COALESCE(NULLIF(trim(coach.full_name), ''), 'Coach') AS coach_display_name,
    NULLIF(trim(member_profile.avatar_url), '') AS avatar_url
  FROM public.clients c
  LEFT JOIN public.profiles coach ON coach.id = c.coach_id
  LEFT JOIN public.profiles member_profile ON member_profile.id = c.user_id
  WHERE c.coach_id IS NOT NULL
    AND EXISTS (
      SELECT 1 FROM public.clients viewer WHERE viewer.user_id = auth.uid()
    )
  ORDER BY member_display_name ASC;
$$;

GRANT EXECUTE ON FUNCTION public.fetch_community_member_directory() TO authenticated;

COMMENT ON FUNCTION public.fetch_community_member_directory() IS
  'Members can see other members: display name, avatar, assigned coach name (no private health fields).';
