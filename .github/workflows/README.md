# GitHub Actions CI Workflows

This directory contains the CI/CD workflows for the DotaOps project.

## Workflows

### backend-ci.yml
Runs backend tests on every PR and push to `main` and `development` branches.

**Jobs:**
1. **unit-tests** — Runs unit tests against H2 in-memory database (always runs).
   - Uses `application-test.properties` with H2 profile
   - Fast feedback, no external dependencies
   
2. **integration-tests** — Runs integration tests against Supabase (only if secrets are configured).
   - Uses `application-integration.properties` with Supabase credentials
   - Skips automatically on forks (where secrets are not available)
   - Tests database connectivity, migrations, and health endpoints

**Required Repository Secrets for integration tests:**
- `SUPABASE_DB_URL`
- `SUPABASE_DB_USER`
- `SUPABASE_DB_PASSWORD`
- `SUPABASE_SERVICE_ROLE_KEY`
- `SUPABASE_JWT_SECRET`

### frontend-ci.yml
Runs frontend checks (lint, typecheck, build) on every PR and push to `main` and `development` branches.

**Jobs:**
1. **test** — Installs deps and runs linting, type checking, and production build.

## Setup Instructions

1. **Add repository secrets** (Settings → Secrets → Actions):
   - Go to your GitHub repository settings
   - Navigate to Secrets and Variables → Actions
   - Create secrets for:
     - `SUPABASE_DB_URL` — JDBC connection string (e.g., `jdbc:postgresql://...`)
     - `SUPABASE_DB_USER` — Database user
     - `SUPABASE_DB_PASSWORD` — Database password
     - `SUPABASE_SERVICE_ROLE_KEY` — Service role JWT token
     - `SUPABASE_JWT_SECRET` — JWT secret

2. **Branch Protection** (Settings → Branches → Add rule for `main`):
   - Enable "Require a pull request before merging"
   - Check "Require status checks to pass before merging"
   - Select:
     - `backend-ci / unit-tests`
     - `backend-ci / integration-tests` (if using Supabase)
     - `frontend-ci / test`

3. **Run workflows locally** (optional):
   ```bash
   # Backend unit tests (H2)
   cd backend && ./mvnw -B test -Dspring.profiles.active=test
   
   # Backend integration tests (requires env vars)
   export SUPABASE_DB_URL=...
   export SUPABASE_DB_USER=...
   export SUPABASE_DB_PASSWORD=...
   cd backend && ./mvnw -B test -Dspring.profiles.active=integration
   ```

## Notes

- **Unit tests** use H2 in-memory database and run for every PR/push.
- **Integration tests** are skipped on forks (where secrets unavailable) and only run if `SUPABASE_DB_URL` is set.
- Both workflows cache Maven and npm dependencies for faster builds.
- Frontend tests run ESLint, TypeScript type-checking, and a production build.
