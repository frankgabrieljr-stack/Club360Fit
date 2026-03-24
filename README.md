# Club360Fit — Android & shared backend

This repository contains:

- **Android app** (Gradle project under `app/`)
- **Supabase** migrations and SQL notes under `supabase/`

## iOS app location

The SwiftUI client lives in **`Club360Fit-iOS/`**, but that directory is **ignored by this repo’s Git** (see `.gitignore`). Android and iOS are meant to use **separate remotes** and **separate commit history**.

**Workflow:** Open a terminal in **`Club360Fit-iOS/`** (or the folder that contains your `.git` for iOS), run `git status`, commit, and push to your iOS remote. Do not expect `git push` from the Android repo root to include Swift sources.

Details: **`Club360Fit-iOS/README.md`**.
