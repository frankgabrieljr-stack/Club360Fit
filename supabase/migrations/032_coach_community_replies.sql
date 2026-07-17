-- Coach community participation: browse feed + reply with Coach badge (no coach posts).
-- Makes community_comments.client_id optional for coach replies; adds author_user_id + is_coach_reply.

ALTER TABLE public.community_comments
  ALTER COLUMN client_id DROP NOT NULL;

ALTER TABLE public.community_comments
  ADD COLUMN IF NOT EXISTS author_user_id uuid REFERENCES auth.users(id) ON DELETE SET NULL;

ALTER TABLE public.community_comments
  ADD COLUMN IF NOT EXISTS is_coach_reply boolean NOT NULL DEFAULT false;

-- Backfill author_user_id for existing member comments when possible.
UPDATE public.community_comments c
SET author_user_id = cl.user_id
FROM public.clients cl
WHERE c.client_id = cl.id
  AND c.author_user_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_community_comments_author_user
  ON public.community_comments (author_user_id)
  WHERE author_user_id IS NOT NULL;

-- Members: force non-coach replies tied to their clients row.
DROP POLICY IF EXISTS "client_insert_community_comments" ON public.community_comments;
CREATE POLICY "client_insert_community_comments" ON public.community_comments
  FOR INSERT TO authenticated
  WITH CHECK (
    is_coach_reply = false
    AND client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
    AND author_user_id = auth.uid()
    AND EXISTS (SELECT 1 FROM public.community_posts p WHERE p.id = community_comments.post_id)
  );

-- Coaches/admins: reply as coach (no clients row required).
DROP POLICY IF EXISTS "coach_insert_community_comments" ON public.community_comments;
CREATE POLICY "coach_insert_community_comments" ON public.community_comments
  FOR INSERT TO authenticated
  WITH CHECK (
    public.club360_is_coach_or_admin()
    AND is_coach_reply = true
    AND client_id IS NULL
    AND author_user_id = auth.uid()
    AND EXISTS (SELECT 1 FROM public.community_posts p WHERE p.id = community_comments.post_id)
  );

-- Coaches can delete their own coach replies.
DROP POLICY IF EXISTS "coach_delete_own_community_comments" ON public.community_comments;
CREATE POLICY "coach_delete_own_community_comments" ON public.community_comments
  FOR DELETE TO authenticated
  USING (
    is_coach_reply = true
    AND author_user_id = auth.uid()
  );

-- Any authenticated coach/admin can read all comments (for Community inbox).
DROP POLICY IF EXISTS "coach_select_community_comments" ON public.community_comments;
CREATE POLICY "coach_select_community_comments" ON public.community_comments
  FOR SELECT TO authenticated
  USING (
    public.club360_is_coach_or_admin()
    OR post_id IN (
      SELECT id FROM public.community_posts WHERE coach_id = auth.uid()
    )
  );

-- Directory: coaches can browse members too (not only clients-with-clients-row).
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
    AND (
      EXISTS (SELECT 1 FROM public.clients viewer WHERE viewer.user_id = auth.uid())
      OR public.club360_is_coach_or_admin()
    )
  ORDER BY member_display_name ASC;
$$;

GRANT EXECUTE ON FUNCTION public.fetch_community_member_directory() TO authenticated;

COMMENT ON FUNCTION public.fetch_community_member_directory() IS
  'Members and coaches can browse the directory: name, avatar, assigned coach (no private health fields).';

-- Fill coach reply display name from profiles when needed.
CREATE OR REPLACE FUNCTION public.community_comments_fill_author_name()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  resolved text;
BEGIN
  IF NEW.author_user_id IS NULL THEN
    NEW.author_user_id := auth.uid();
  END IF;

  IF NEW.is_coach_reply THEN
    SELECT COALESCE(NULLIF(trim(full_name), ''), 'Coach')
      INTO resolved
    FROM public.profiles
    WHERE id = NEW.author_user_id;

    IF length(trim(COALESCE(NEW.author_display_name, ''))) = 0
       OR NEW.author_display_name IN ('Member', 'Coach') THEN
      NEW.author_display_name := COALESCE(resolved, 'Coach');
    END IF;
    NEW.client_id := NULL;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS community_comments_fill_author_name ON public.community_comments;
CREATE TRIGGER community_comments_fill_author_name
  BEFORE INSERT OR UPDATE OF author_user_id, is_coach_reply, author_display_name ON public.community_comments
  FOR EACH ROW
  EXECUTE FUNCTION public.community_comments_fill_author_name();

COMMENT ON COLUMN public.community_comments.is_coach_reply IS
  'True when a coach/admin replied; shown with a Coach badge in the app.';
COMMENT ON COLUMN public.community_comments.author_user_id IS
  'auth.users id of the comment author (member or coach).';
