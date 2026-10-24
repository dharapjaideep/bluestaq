## 2026-10-24 — Add HTTP Basic authentication (register, me) with H2 user store
- `src/main/resources/application.properties` — TEMPORARY in-memory H2, `ddl-auto=create-drop`,
  show-sql off, open-in-view off, H2 console at `/h2-console`.
- `src/main/java/com/bluestaq/demo/config/SecurityConfig.java` — new (protected file, needs
  human review). Stateless HTTP Basic; CSRF disabled (header-based credentials, no cookies);
  public `POST /auth/register` and TEMPORARY public `/h2-console/**`; everything else
  authenticated; same-origin frames for the console. BCrypt `PasswordEncoder` bean.
- `src/main/java/com/bluestaq/demo/user/User.java`, `UserRepository.java` — new; `users` table,
  emails normalised (trim + lower-case, `Locale.ROOT`) on save and lookup.
- `src/main/java/com/bluestaq/demo/auth/AppUserDetailsService.java` — new; loads users by
  email, case-insensitive.
- `src/main/java/com/bluestaq/demo/auth/AuthService.java`, `AuthController.java`,
  `RegisterRequest.java`, `UserResponse.java`, `EmailAlreadyRegisteredException.java` — new;
  `POST /auth/register` (201, `{id, email}`, 409 on duplicate incl. concurrent-insert race),
  `GET /auth/me`. The DTO never exposes the password hash.
- `src/main/java/com/bluestaq/demo/web/ApiExceptionHandler.java`, `ApiError.java` — new; JSON
  errors for validation (400), malformed body (400) and duplicate email (409).
- `src/test/java/com/bluestaq/demo/auth/AuthControllerTest.java` — new; 13 end-to-end MockMvc
  tests covering register success/duplicate/validation and `/auth/me` 401/200/401.

## 2026-10-24 — Cap registration password length at 72 characters
- `src/main/java/com/bluestaq/demo/auth/RegisterRequest.java` — `@Size(min = 8, max = 72)`;
  BCrypt only uses the first 72 bytes, so longer passwords now get a clear 400.
- `src/test/java/com/bluestaq/demo/auth/AuthControllerTest.java` — tests for a 72-char password
  (accepted, can log in) and a 73-char password (400, nothing saved).

## 2026-10-24 — schema.sql owns the schema; Hibernate validates only
- `src/main/resources/schema.sql` — new (protected file). `users` table matching the `User`
  entity: identity `id`, `email` NOT NULL with named unique constraint `uk_users_email`,
  `password_hash` NOT NULL, `created_at` TIMESTAMP WITH TIME ZONE NOT NULL. Idempotent.
- `src/main/resources/application.properties` — `ddl-auto` changed from `create-drop` to
  `validate` (fail safe: startup fails if entities drift from schema.sql);
  `spring.sql.init.mode=always` so the script also runs on a non-embedded database.
- `src/test/java/com/bluestaq/demo/user/SchemaTest.java` — new; asserts `validate` mode, the
  named unique constraint from schema.sql exists, and the database itself rejects duplicate
  emails and a null password hash.
- `CLAUDE.md` — architecture rule: schema.sql owns the schema, new entities get their table
  in schema.sql first, statements idempotent/non-destructive/portable.

## 2026-10-24 — Restructure from package-by-feature to package-by-layer
- Moved (package + imports updated, no logic changes) under `src/main/java/com/bluestaq/demo/`:
  `auth/AuthController` → `controller/`; `auth/AuthService`, `auth/AppUserDetailsService` →
  `service/`; `user/UserRepository` → `repository/`; `user/User` → `model/`;
  `auth/RegisterRequest`, `auth/UserResponse`, `web/ApiError` → `dto/`;
  `auth/EmailAlreadyRegisteredException`, `web/ApiExceptionHandler` → `exception/`.
  Removed the empty `auth`, `user` and `web` packages.
- Tests: `auth/AuthControllerTest` → `controller/`; `user/SchemaTest` → `repository/`.
- `config/SecurityConfig.java`, `src/main/resources/schema.sql` — updated class references in
  comments only.
- `CLAUDE.md` — added "Code structure" section describing the layer packages.

## 2026-10-24 — Single-page web UI for sign-in and registration
- `src/main/resources/static/index.html` — new; plain HTML/JS page served at `/`: sign in
  (checks credentials via `GET /auth/me` with HTTP Basic), "Register account" form with
  per-field validation errors and 409 handling, signed-in view, sign out. Credentials are held
  in memory only (a reload signs out).
- `src/main/java/com/bluestaq/demo/config/SecurityConfig.java` (protected — approved by user) —
  public `GET /` and `GET /index.html` only; HTTP Basic entry point now returns a plain 401
  without `WWW-Authenticate`, so browsers don't show their native login popup over the page.
- `src/test/java/com/bluestaq/demo/config/SecurityConfigTest.java` — new; page is public via GET
  only, other paths still need auth, 401s carry no `WWW-Authenticate` header.
- `CLAUDE.md` — Code structure: where the static UI lives and that public paths need
  SecurityConfig changes.

## 2026-10-24 — Version tracking for the fresh repository
- `VERSION` — new; repo-root version file, starting at `0.1.0`.
- `src/main/resources/application.properties` — added `app.version=0.1.0`, kept in sync with
  `VERSION` per the version management rules in `PERSONAL-PREFERENCES.md`.

## 2026-10-24 — Core notes domain: teams, memberships, notes CRUD and search
- `src/main/resources/schema.sql` (protected — change requested by user) — new `teams`,
  `team_members` (composite PK) and `notes` tables with foreign keys and lookup indexes.
  `notes.content` is `VARCHAR(20000)` rather than `TEXT` so the DDL is portable between H2
  and PostgreSQL.
- `model/Team.java`, `model/Note.java` — new entities; team membership mapped as a
  `@ManyToMany` over `team_members`.
- `repository/TeamRepository.java`, `repository/NoteRepository.java` — new; membership check
  and "accessible notes" queries (access rule applied in the query itself).
- `service/NoteService.java` — new; create/list/get/update/delete with the access rules:
  owner or team member can read/edit; only the owner can delete or change the team (403);
  assigning to a team you're not in is a 400; no access is a 404. Search escapes LIKE wildcards.
- `controller/NoteController.java` — new; `/notes` endpoints, thin delegator.
- `dto/NoteRequest.java`, `dto/NoteResponse.java` — new request/response records.
- `exception/NoteNotFoundException.java`, `NoteOwnerOnlyException.java`,
  `InvalidTeamException.java` — new; `ApiExceptionHandler` maps them to 404/403/400 and maps
  bad path/query parameter types to 400.
- `src/test/java/com/bluestaq/demo/controller/NoteControllerTest.java` — new; 17 MockMvc tests
  for owner CRUD, member permissions, 404 for non-members, team assignment, listing and search.
- `README.md` — notes endpoints, access rules, data model, branches/versions.

## 2026-10-24 — Demo data seeder and example HTTP requests
- `config/DemoDataSeeder.java` — new `CommandLineRunner`; runs only when
  `app.demo-data.enabled=true` (off if the property is missing) and delegates to the service.
- `service/DemoDataService.java` — new; when the database has no users, creates
  alice/bob/carol@demo.com (BCrypt-hashed `password123`), team "Engineering" (alice, bob), one
  personal note each and two Engineering notes owned by alice. Kept in a service because only
  services may use repositories.
- `src/main/resources/application.properties` — `app.demo-data.enabled=true` (marked TEMPORARY).
- `src/test/resources/config/application.properties` — new; sets `app.demo-data.enabled=false`
  for all tests.
- `src/test/java/com/bluestaq/demo/service/DemoDataServiceTest.java` — new; seeding into an empty
  database, no-op when users exist, seeder bean absent in tests.
- `requests.http` — new; example requests for register, `/auth/me` and every `/notes` endpoint,
  showing the sharing rules with alice, bob and carol.
- `README.md` — demo credentials, how seeding is controlled, `requests.http`.

## 2026-10-24 — Team management endpoints
- `controller/TeamController.java` — new; `POST /teams`, `GET /teams`,
  `POST /teams/{id}/members`, `DELETE /teams/{id}/members/{userId}`; thin delegator.
- `service/TeamService.java` — new; creator becomes first member; any member can add an
  existing user (404 unknown email, 409 already a member, PK violation from a concurrent add
  also mapped to 409); members can remove themselves, only the creator can remove others (403),
  the creator can't be removed (409); non-members get 404 on every team endpoint.
- `model/Team.java` — `removeMember`, `hasMember`, `isCreatedBy` helpers.
- `repository/TeamRepository.java` — `findAllByMember` query.
- `dto/TeamRequest.java`, `dto/AddMemberRequest.java`, `dto/TeamResponse.java` — new.
- `exception/TeamNotFoundException.java`, `UserNotFoundException.java`,
  `TeamCreatorOnlyException.java`, `TeamMembershipConflictException.java` — new;
  `ApiExceptionHandler` maps them to 404/404/403/409.
- `src/test/java/com/bluestaq/demo/controller/TeamControllerTest.java` — new; 16 tests incl.
  removed member losing access to team notes immediately.
- `NoteControllerTest.java` — Javadoc no longer says team endpoints don't exist.
- `requests.http` — team section showing add/remove rules and access revocation.
- `README.md` — team endpoints and membership rules; status updated.

## 2026-10-24 — Adapt GitHub workflows copied from another project
- `pom.xml` — added `jacoco-maven-plugin` 0.8.15 (agent + report bound to the test phase) and
  `dependency-check-maven` 13.0.0 (data dir under `target/` to match the workflow cache, HTML +
  JSON reports); without them the coverage and dependency-scan workflows could not run.
- `.github/workflows/test-coverage.yml` — `mvn test` (report is now bound to the test phase);
  removed `SPRING_PROFILES_ACTIVE=test`, a profile this project doesn't have.
- `.github/workflows/auto-implement.yml` — PR permission narrowed from `gh pr create *` to
  `gh pr create --base devel *`: master is the default branch, so an unqualified PR would
  target it (least privilege / defence in depth with the agent prompt).
- `.claude/agents/auto-implementer.md` — states the exact `gh pr create --base devel ...` form
  the workflow permits.
- `README.md` — CI workflows section and required secrets.

## 2026-10-24 — Browser UI for notes and teams
- `src/main/resources/static/index.html` — extended from sign-in/register into the full UI:
  sidebar filters (All / Personal / each team), search wired to `?q=`, note list with team
  badge and updated time, note editor (create/view/edit/delete; team dropdown; always sends the
  current `teamId` on save; team change and delete disabled for non-owners, server 403 still
  handled), and a teams panel (create, members, add by email, remove/leave only where allowed).
  API errors are shown inline; 401 returns to sign-in. Credentials moved from memory to
  `sessionStorage` (tab-scoped, cleared on sign-out) so a reload keeps the session; stored
  credentials are re-verified via `/auth/me` on load. Kept as one self-contained file so
  `SecurityConfig` (protected) needs no new public paths for separate .js/.css files. All
  API data is rendered with `textContent` (no `innerHTML`), so note/team text can't inject HTML.
- `README.md` — "Browser UI" section; status updated.

## 2026-10-24 — README rewrite for reviewers
- `README.md` — restructured: overview, running, demo credentials + walkthrough, API overview
  (pointing to `requests.http`), design decisions, limitations and next steps. Removed
  duplicated sections and an outdated curl example. No code changes.

## 2026-10-24 — README "How I work with Claude Code"; agent project name
- `README.md` — new section 6 describing the Claude Code setup (CLAUDE.md,
  PERSONAL-PREFERENCES.md, `.claude/agents/`) carried over from a previous project.
- `.claude/agents/code-review.md` — identity line said "the Notify project" (left over from the
  previous project); now "the Notes Service project".
- `README.md` — section 6 now says the code-review agent is started manually (matches
  `code-review.yml`'s `workflow_dispatch` trigger), not run on every push to `devel`.
- `CLAUDE.md`, `PERSONAL-PREFERENCES.md` — version rules pointed at `application.yml` (left over
  from the previous project); now `src/main/resources/application.properties`.
