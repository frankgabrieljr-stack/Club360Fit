-- Notify post author (and their coach) when someone else replies on a community post.
-- In-app rows are created server-side; clients invoke send-device-push for device delivery.

CREATE OR REPLACE FUNCTION public.trg_community_comment_notify()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_author_client_id uuid;
  v_coach_id uuid;
  v_author_name text;
  v_preview text;
  v_body text;
  v_member_title text := 'New community reply';
  v_coach_title text;
BEGIN
  SELECT p.client_id, p.coach_id, p.author_display_name
  INTO v_author_client_id, v_coach_id, v_author_name
  FROM public.community_posts p
  WHERE p.id = NEW.post_id;

  IF v_author_client_id IS NULL THEN
    RETURN NEW;
  END IF;

  -- Member commenting on their own post: no notification.
  IF NEW.client_id IS NOT NULL AND NEW.client_id = v_author_client_id THEN
    RETURN NEW;
  END IF;

  v_preview := left(trim(NEW.body), 120);
  v_body := coalesce(nullif(trim(NEW.author_display_name), ''), 'Someone')
    || CASE WHEN v_preview = '' THEN '' ELSE ': ' || v_preview END;

  INSERT INTO public.client_notifications (
    client_id, kind, title, body, ref_type, ref_id, visible_to_client, dedupe_key
  ) VALUES (
    v_author_client_id,
    'community_reply',
    v_member_title,
    v_body,
    'community_post',
    NEW.post_id::text,
    true,
    'community_reply:' || NEW.id::text
  )
  ON CONFLICT DO NOTHING;

  -- Coach Updates inbox (skip when the assigned coach is the replier).
  IF v_coach_id IS NOT NULL
     AND (NEW.author_user_id IS NULL OR NEW.author_user_id <> v_coach_id) THEN
    v_coach_title := 'Reply on '
      || coalesce(nullif(trim(v_author_name), ''), 'member')
      || '''s post';
    INSERT INTO public.client_notifications (
      client_id, kind, title, body, ref_type, ref_id, visible_to_client, dedupe_key
    ) VALUES (
      v_author_client_id,
      'community_reply',
      v_coach_title,
      v_body,
      'community_post',
      NEW.post_id::text,
      false,
      'community_reply_coach:' || NEW.id::text
    )
    ON CONFLICT DO NOTHING;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS community_comments_notify ON public.community_comments;
CREATE TRIGGER community_comments_notify
  AFTER INSERT ON public.community_comments
  FOR EACH ROW
  EXECUTE FUNCTION public.trg_community_comment_notify();

COMMENT ON FUNCTION public.trg_community_comment_notify() IS
  'Creates member + coach client_notifications when a community reply is posted (skips self-replies).';
