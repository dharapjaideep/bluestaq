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
