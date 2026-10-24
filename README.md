# Notes Service

## 1. What it is

A note-taking service for small teams. Users register, write personal notes, and share
notes with teams they belong to. Any team member can read and edit the team's notes; only
the owner can delete a note or move it to another team. It's a Spring Boot REST API with a
plain HTML/JS browser UI on top, built as a time-boxed take-home exercise.

**Stack:** Java 21 · Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation) · H2
database (file-based, in `data/`) · Maven · plain HTML/CSS/JS (no build step)

## 2. Choices and scope

- **Java 21 + Spring Boot:** a mature security and data stack, fast to build a correct REST
  API with, and my strongest stack.
- **H2 with file storage:** zero setup for reviewers (no database to install) and data that
  survives restarts. The schema is kept portable, ready to move to PostgreSQL.
- **In scope:** authentication, personal and team notes, team management, search,
  optimistic locking for concurrent edits, and a browser UI for all of it.
- **Deliberately out of scope:** admin/invite-only registration, per-note permissions,
  pagination, and production-grade auth. They're covered in section 9.

## 3. Running it

**Prerequisites:** JDK 21 and free port 8080. Maven isn't required: the wrapper downloads
it on first run, which needs internet access.

```bash
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

Open **<http://localhost:8080/>**. Run the tests (66 tests, MockMvc against the real security
chain and an in-memory database):

```bash
./mvnw test
```

## 4. Demo credentials and a 2-minute walkthrough

When the database has no users at startup, demo data is seeded. The database persists in
`data/` between runs. To reset to the demo state (e.g. after running the walkthrough, or
after changing `schema.sql`), stop the app, delete `data/` and start it again.

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
4. **Concurrent edit:** with **Sprint 12 plan** open in both windows, save a change as Alice,
   then save a different change as Bob. Bob gets a conflict message and a **Reload note**
   button, and his unsaved text stays in the editor.
5. **Alice → Manage teams:** remove Bob. When Bob reloads, the Engineering notes are gone,
   because access is checked on every request.
6. **Carol:** only ever sees her own note. Adding her to Engineering in step 5 instead
   would give her the team's notes immediately.

## 5. API overview

All endpoints except registration use HTTP Basic. **[`requests.http`](requests.http)** has a
runnable example for every endpoint, using alice/bob/carol to show the allowed, 403, 404
and 409 cases. Run it top to bottom in IntelliJ's HTTP client.

| Endpoint | Notes |
|----------|-------|
| `POST /auth/register` | Public. `{email, password}`; password 8–72 chars; `409` if taken |
| `GET /auth/me` | The signed-in user's `{id, email}` |
| `POST /notes` | `{title, content, teamId?}` → `201`; no `teamId` = personal |
| `GET /notes?teamId=&q=` | Notes you can access, newest first; optional team filter and case-insensitive search |
| `GET` / `PUT` / `DELETE /notes/{id}` | Notes include a `version`. `PUT` replaces title, content **and** team and must send the `version` it read (`409` if stale, `400` if missing); `DELETE` is owner-only → `204` |
| `POST /teams` | `{name}` → `201`; you become creator and first member |
| `GET /teams` | Your teams, each with members' `{id, email}` |
| `POST /teams/{id}/members` | `{email}` → `201`; any member can add; `404` unknown email, `409` already a member |
| `DELETE /teams/{id}/members/{userId}` | Leave yourself, or (creator only) remove others → `204`; creator can't be removed (`409`) |

Errors are JSON (`{status, error, message, fieldErrors}`). Validation failures return `400`
with per-field messages.

## 6. Design choices I spent the most time on

**Authentication.** I first explored Google OAuth and email OTP, then deliberately chose
HTTP Basic with BCrypt-hashed passwords. It's stateless, needs no external provider setup for
reviewers, and kept the time on the core features. CSRF is disabled because credentials
travel in a header, never a cookie, and an unknown email gets the same `401` as a wrong
password. The cost is that credentials go with every request. In production I'd use an OIDC
provider issuing short-lived JWTs, or an HttpOnly session cookie with CSRF protection.

**Access model.** The alternative was per-note ACL rows (who can see which note), which would
have to be kept in sync with team membership. Instead, access is worked out from membership
at query time: the "can I see this note?" rule (owner, or member of the note's team) is part
of the repository query. Removing a member therefore revokes access instantly, with nothing
to clean up. No access returns `404`, identical to a non-existent id, so note and team ids
can't be probed. `403` is used only when the caller can already see the resource but isn't
allowed the action. Deleting a note and moving it to another team are owner-only, so a team
member can't take a note away from the team or its owner.

**Concurrent editing.** Team notes are edited by several people, and last-write-wins would
silently lose someone's work. I considered locking a note while someone edits it, but that
needs lock expiry and breaks when a tab is simply closed. Instead every note has a `version`
(JPA `@Version`), and `PUT` must send the version it was based on. `NoteService` compares it
with the stored version and rejects a stale edit with `409`. That check has to live in the
service: the entity is freshly loaded in each request, so `@Version` on its own would never
see the stale read. `@Version` still catches two saves racing in the same moment (also
`409`). On a conflict, the UI keeps the user's unsaved text and offers **Reload note**, so
nothing is overwritten or lost.

## 7. Other design decisions

- **Package-by-layer** (`controller` → `service` → `repository` → `model`, plus `dto`,
  `exception`, `config`). Controllers are thin delegators. All business rules and access
  checks live in services, and only services touch repositories. Controllers return DTOs,
  never entities.
- **`schema.sql` is the source of truth.** Hibernate runs with `ddl-auto=validate`, so the app
  refuses to start if entities drift from the schema. The DDL is idempotent and kept portable
  between H2 and PostgreSQL. For example, `content` is `VARCHAR(20000)` rather than `TEXT`.
- **`PUT` replaces the whole note**, including `teamId`. Omitting it makes the note personal.
  The UI therefore always sends the currently selected team, even when unchanged.
- **Single-file UI.** `static/index.html` holds its HTML, CSS and JS inline. Only `GET /`
  and `GET /index.html` are public in `SecurityConfig`, and `<script src>` / `<link href>`
  requests can't carry the Authorization header. Separate asset files would have needed more
  public paths. Credentials are kept in `sessionStorage` (per tab, cleared on sign-out) and
  re-verified on load. Signing out also wipes the previous user's data from the page.
- **XSS-safe rendering.** All API data is inserted with `textContent` / `createElement`,
  never `innerHTML`, so note titles, content and team names can't inject markup or script.

## 8. Current limitations

- Registration is open to anyone. Adding a member by email reveals whether that email is
  registered (`404`), as registration's `409` already does.
- The database is a local H2 file (`data/`). Changes to `schema.sql` only create missing
  tables, so after changing a table, delete `data/` (there are no migrations yet). The H2
  console (`/h2-console`) is enabled for development.
- Demo seeding is on by default (`app.demo-data.enabled`) and uses a published password.
- Basic credentials sit in `sessionStorage` and are resent on every request; HTTPS is
  assumed.
- `GET /notes` and `GET /teams` return everything, with no paging.

## 9. If I had more time: what I'd change, add, or stop doing

**Change**
- **HTTP Basic → OIDC/JWT:** short-lived tokens (or an HttpOnly session cookie with CSRF
  protection), logout/revocation, ideally via an OIDC provider with MFA.
- **H2 → PostgreSQL + Flyway:** versioned migrations replace `schema.sql`, the H2 console is
  removed, and `data/` resets are no longer needed.
- **Demo settings into a `demo` Spring profile:** seeding and the H2 console would only run
  when that profile is active, never by default.

**Add**
- **Admin- or invite-only registration:** team invites instead of open sign-up, which also
  closes the email-enumeration gap.
- **Pagination and full-text search:** paging and sorting on note and team lists, and
  full-text search instead of `LIKE`.
- **Per-note and read-only sharing:** share a note with individual users, with read-only vs
  edit permissions, alongside team sharing.
- **Team rename/delete and password change.**

**Stop doing**
- **Open self-registration:** anyone can create an account today.
- **Sending credentials on every request:** a token or session would replace the Basic
  header.

## 10. How I work with Claude Code

This project was built with Claude Code. I didn't start from a blank setup: I copied my
standard Claude Code environment from a previous project. It's my default starting point for
any new project, and it sets guidelines and guardrails for how the AI works in the codebase:

- **[`CLAUDE.md`](CLAUDE.md):** engineering principles (least privilege, defence in depth,
  fail safe, auditability), architecture rules, code-commenting conventions, protected files
  (`SecurityConfig.java`, `schema.sql`) that need human review before changes, and a rule to
  stop and ask rather than assume when requirements are unclear.
- **[`PERSONAL-PREFERENCES.md`](PERSONAL-PREFERENCES.md):** my workflow conventions: the
  `devel` → `staging` → `master` branching strategy, version bumping, and post-merge checks.
- **[`.claude/agents/`](.claude/agents):** two agents run by GitHub Actions. The code-review
  agent is started manually (JaCoCo coverage and an OWASP dependency scan run separately on
  every push to `devel`). It reviews the changes on `devel` for design and security issues
  and writes a report with HIGH/MEDIUM/LOW findings. If there are HIGH findings, the
  auto-implementer agent fixes them on a separate branch and opens a pull request for human
  review. It never commits to `devel` directly.

The project-specific parts (the project description, package structure, and schema rules)
were written for this project. Several decisions in this README came out of that setup. For
example, `schema.sql` as the source of truth and the 404-vs-403 policy were checked against
these rules as the code was written. Every code change is logged in [`FIXES.md`](FIXES.md).

## Repository

- **Branches:** `devel` (active), `staging` (tested), `master` (public, default). The version
  is in [`VERSION`](VERSION), mirrored as `app.version`.
- **Change log:** every code change is recorded in [`FIXES.md`](FIXES.md).
- **CI** ([`.github/workflows`](.github/workflows)): JaCoCo coverage and an OWASP dependency
  scan on push to `devel`, plus a manually started AI code-review agent whose HIGH findings
  trigger an auto-fix PR for human review. The workflows need the `ANTHROPIC_API_KEY` and
  `NVD_API_KEY` secrets.
