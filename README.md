# Notes Service

A note-taking service for small teams. Users register, write personal notes, and share
notes with teams they belong to. Any team member can read and edit the team's notes; only
the owner can delete a note or move it to another team. It's a Spring Boot REST API with a
plain HTML/JS browser UI on top, built as a time-boxed take-home exercise.

**Stack:** Java 21 · Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation) · H2
in-memory database · Maven · plain HTML/CSS/JS (no build step)

## 1. Running it

**Prerequisites:** JDK 21 and free port 8080. Maven isn't required: the wrapper downloads
it on first run, which needs internet access.

```bash
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

Open **<http://localhost:8080/>**. Run the tests (62 tests, MockMvc against the real security
chain and database):

```bash
./mvnw test
```

## 2. Demo credentials and a 2-minute walkthrough

When the database is empty at startup, demo data is seeded. It's in-memory, so restarting
the app resets it.

| Email            | Password      | Team        | Has                                       |
|------------------|---------------|-------------|-------------------------------------------|
| `alice@demo.com` | `password123` | Engineering (creator) | 1 personal note; owns 2 Engineering notes |
| `bob@demo.com`   | `password123` | Engineering | 1 personal note                           |
| `carol@demo.com` | `password123` | —           | 1 personal note                           |

1. **Alice:** sign in. You see 3 notes: her personal note and two Engineering notes. Try the
   **Personal** / **Engineering** filters and search for `sprint`.
2. Open **Sprint 12 plan** and edit it. Then click **New** and create a note shared with
   Engineering.
3. **Bob:** sign in from a private window (sessions are per browser tab). He sees the
   Engineering notes, including Alice's new one, but not her personal note. On **Sprint 12
   plan** he can edit, but the team picker is disabled and there's no Delete.
4. **Alice → Manage teams:** remove Bob. When Bob reloads, the Engineering notes are gone,
   because access is checked on every request.
5. **Carol:** only ever sees her own note. Adding her to Engineering in step 4 instead
   would give her the team's notes immediately.

## 3. API overview

All endpoints except registration use HTTP Basic. **[`requests.http`](requests.http)** has a
runnable example for every endpoint, using alice/bob/carol to show the allowed, 403, 404
and 409 cases. Run it top to bottom in IntelliJ's HTTP client.

| Endpoint | Notes |
|----------|-------|
| `POST /auth/register` | Public. `{email, password}`; password 8–72 chars; `409` if taken |
| `GET /auth/me` | The signed-in user's `{id, email}` |
| `POST /notes` | `{title, content, teamId?}` → `201`; no `teamId` = personal |
| `GET /notes?teamId=&q=` | Notes you can access, newest first; optional team filter and case-insensitive search |
| `GET` / `PUT` / `DELETE /notes/{id}` | `PUT` replaces title, content **and** team; `DELETE` is owner-only → `204` |
| `POST /teams` | `{name}` → `201`; you become creator and first member |
| `GET /teams` | Your teams, each with members' `{id, email}` |
| `POST /teams/{id}/members` | `{email}` → `201`; any member can add; `404` unknown email, `409` already a member |
| `DELETE /teams/{id}/members/{userId}` | Leave yourself, or (creator only) remove others → `204`; creator can't be removed (`409`) |

Errors are JSON (`{status, error, message, fieldErrors}`). Validation failures return `400`
with per-field messages.

## 4. Design decisions

- **Package-by-layer** (`controller` → `service` → `repository` → `model`, plus `dto`,
  `exception`, `config`). Controllers are thin delegators. All business rules and access
  checks live in services, and only services touch repositories. Controllers return DTOs,
  never entities.
- **`schema.sql` is the source of truth.** Hibernate runs with `ddl-auto=validate`, so the app
  refuses to start if entities drift from the schema. The DDL is idempotent and kept portable
  between H2 and PostgreSQL. For example, `content` is `VARCHAR(20000)` rather than `TEXT`.
- **HTTP Basic + BCrypt.** Stateless and deliberately minimal for the scope. CSRF is disabled
  because credentials travel in a header, never a cookie. Unknown email and wrong password
  give the same `401`.
- **Access is derived from team membership at query time.** There are no per-note ACL rows:
  the "can I see this note?" rule is part of the repository query (owner, or member of the
  note's team). Removing a member therefore revokes access instantly, with nothing to clean
  up.
- **404 vs 403.** No access returns `404`, identical to a non-existent id, so note and team
  ids can't be probed. `403` is used only when the caller can already see the resource but
  isn't allowed the action (a member deleting or moving a note, a non-creator removing
  someone).
- **`PUT` replaces the whole note**, including `teamId`. Omitting it makes the note personal.
  The UI therefore always sends the currently selected team, even when unchanged.
- **Single-file UI.** `static/index.html` holds its HTML, CSS and JS inline. Only `GET /`
  and `GET /index.html` are public in `SecurityConfig`, and `<script src>` / `<link href>`
  requests can't carry the Authorization header. Separate asset files would have needed more
  public paths. Credentials are kept in `sessionStorage` (per tab, cleared on sign-out) and
  re-verified on load.
- **XSS-safe rendering.** All API data is inserted with `textContent` / `createElement`,
  never `innerHTML`, so note titles, content and team names can't inject markup or script.

## 5. Known limitations and what I'd do next

**Limitations today**
- Registration is open to anyone. Adding a member by email reveals whether that email is
  registered (`404`), as registration's `409` already does.
- The database is in-memory H2: data is lost on restart. The H2 console (`/h2-console`) is
  enabled for development.
- Demo seeding is on by default (`app.demo-data.enabled`) and uses a published password.
- Basic credentials sit in `sessionStorage` and are resent on every request; HTTPS is
  assumed.
- `GET /notes` and `GET /teams` return everything, with no paging.

**Next steps**
1. **Admin- or invite-only registration.** Team invites instead of open sign-up, which also
   closes the email-enumeration gap.
2. **JWT or session-based auth** instead of Basic: short-lived tokens or an HttpOnly session
   cookie (with CSRF protection), logout/revocation, ideally via an OIDC provider with MFA.
3. **PostgreSQL + Flyway.** Versioned migrations replace `schema.sql`. Remove the H2 console
   and disable demo seeding in production profiles.
4. **Pagination** (and sorting) on note and team lists, plus full-text search instead of
   `LIKE`.
5. **Per-note sharing** with individual users and read-only vs edit permissions, alongside
   team sharing.

## Repository

- **Branches:** `devel` (active), `staging` (tested), `master` (public, default). The version
  is in [`VERSION`](VERSION), mirrored as `app.version`.
- **Change log:** every code change is recorded in [`FIXES.md`](FIXES.md).
- **CI** ([`.github/workflows`](.github/workflows)): JaCoCo coverage and an OWASP dependency
  scan on push to `devel`, plus an AI code-review agent whose HIGH findings trigger an
  auto-fix PR for human review. The workflows need the `ANTHROPIC_API_KEY` and
  `NVD_API_KEY` secrets.
