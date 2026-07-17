-- App-wide peer community: members support each other and can see who coaches whom.
-- Private fields (weight, medical, etc.) stay on clients and are NOT exposed here.

CREATE TABLE IF NOT EXISTS public.community_posts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  coach_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  author_display_name text NOT NULL DEFAULT 'Member',
  coach_display_name text NOT NULL DEFAULT 'Coach',
  category text NOT NULL DEFAULT 'tip'
    CHECK (category IN ('tip', 'win', 'question', 'encouragement')),
  body text NOT NULL
    CHECK (char_length(trim(body)) >= 1 AND char_length(body) <= 2000),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_community_posts_created
  ON public.community_posts (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_community_posts_coach_created
  ON public.community_posts (coach_id, created_at DESC);

CREATE TABLE IF NOT EXISTS public.community_comments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id uuid NOT NULL REFERENCES public.community_posts(id) ON DELETE CASCADE,
  client_id uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  author_display_name text NOT NULL DEFAULT 'Member',
  body text NOT NULL
    CHECK (char_length(trim(body)) >= 1 AND char_length(body) <= 1000),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_community_comments_post_created
  ON public.community_comments (post_id, created_at ASC);

ALTER TABLE public.community_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.community_comments ENABLE ROW LEVEL SECURITY;

-- Any signed-in member (has a clients row) can read the feed.
DROP POLICY IF EXISTS "client_select_cohort_community_posts" ON public.community_posts;
DROP POLICY IF EXISTS "client_select_community_posts" ON public.community_posts;
CREATE POLICY "client_select_community_posts" ON public.community_posts
  FOR SELECT TO authenticated
  USING (
    EXISTS (SELECT 1 FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "client_insert_own_community_posts" ON public.community_posts;
CREATE POLICY "client_insert_own_community_posts" ON public.community_posts
  FOR INSERT TO authenticated
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
    AND coach_id = (SELECT coach_id FROM public.clients WHERE user_id = auth.uid())
    AND coach_id IS NOT NULL
  );

DROP POLICY IF EXISTS "client_update_own_community_posts" ON public.community_posts;
CREATE POLICY "client_update_own_community_posts" ON public.community_posts
  FOR UPDATE TO authenticated
  USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "client_delete_own_community_posts" ON public.community_posts;
CREATE POLICY "client_delete_own_community_posts" ON public.community_posts
  FOR DELETE TO authenticated
  USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "coach_select_community_posts" ON public.community_posts;
CREATE POLICY "coach_select_community_posts" ON public.community_posts
  FOR SELECT TO authenticated
  USING (
    coach_id = auth.uid()
    OR public.club360_is_coach_or_admin()
  );

-- Comments: any member can read/reply across the app.
DROP POLICY IF EXISTS "client_select_cohort_community_comments" ON public.community_comments;
DROP POLICY IF EXISTS "client_select_community_comments" ON public.community_comments;
CREATE POLICY "client_select_community_comments" ON public.community_comments
  FOR SELECT TO authenticated
  USING (
    EXISTS (SELECT 1 FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "client_insert_cohort_community_comments" ON public.community_comments;
DROP POLICY IF EXISTS "client_insert_community_comments" ON public.community_comments;
CREATE POLICY "client_insert_community_comments" ON public.community_comments
  FOR INSERT TO authenticated
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
    AND EXISTS (SELECT 1 FROM public.community_posts p WHERE p.id = community_comments.post_id)
  );

DROP POLICY IF EXISTS "client_delete_own_community_comments" ON public.community_comments;
CREATE POLICY "client_delete_own_community_comments" ON public.community_comments
  FOR DELETE TO authenticated
  USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "coach_select_community_comments" ON public.community_comments;
CREATE POLICY "coach_select_community_comments" ON public.community_comments
  FOR SELECT TO authenticated
  USING (
    public.club360_is_coach_or_admin()
    OR post_id IN (
      SELECT id FROM public.community_posts WHERE coach_id = auth.uid()
    )
  );

GRANT SELECT, INSERT, UPDATE, DELETE ON public.community_posts TO authenticated;
GRANT SELECT, INSERT, DELETE ON public.community_comments TO authenticated;

-- Privacy-safe member directory: name + coach only (no biometrics / medical / contact).
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

-- Fill coach_display_name from profiles when the client cannot read other profiles via RLS.
CREATE OR REPLACE FUNCTION public.community_posts_fill_coach_name()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  resolved text;
BEGIN
  SELECT COALESCE(NULLIF(trim(full_name), ''), 'Coach')
    INTO resolved
  FROM public.profiles
  WHERE id = NEW.coach_id;

  IF resolved IS NULL OR length(trim(COALESCE(NEW.coach_display_name, ''))) = 0 THEN
    NEW.coach_display_name := COALESCE(resolved, 'Coach');
  ELSIF NEW.coach_display_name = 'Coach' AND resolved IS NOT NULL THEN
    NEW.coach_display_name := resolved;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS community_posts_fill_coach_name ON public.community_posts;
CREATE TRIGGER community_posts_fill_coach_name
  BEFORE INSERT OR UPDATE OF coach_id, coach_display_name ON public.community_posts
  FOR EACH ROW
  EXECUTE FUNCTION public.community_posts_fill_coach_name();

COMMENT ON TABLE public.community_posts IS
  'App-wide peer feed. coach_id / coach_display_name show which coach the author trains with.';
COMMENT ON TABLE public.community_comments IS
  'Replies on community_posts; any member can encourage another.';
COMMENT ON FUNCTION public.fetch_community_member_directory() IS
  'Members can see other members: display name, avatar, assigned coach name (no private health fields).';
