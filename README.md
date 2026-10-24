# Notes Service

A note-taking service used and shared amongst several small teams. It gives users
somewhere to capture their notes and work with them, through a REST API and a simple
browser UI.

> **Status:** early development. User registration, sign-in, the notes API (personal
> and team notes, with search), team management and a browser UI for all of it are in
> place.

## Tech stack

- Java 21, Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation)
- H2 in-memory database (development only — data is lost on restart)
- Maven (wrapper included)
- Plain HTML/CSS/JS front end, no build step

## Running it

```bash
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

Then open <http://localhost:8080/> to register an account, or sign in with a demo account.

### Browser UI

Open <http://localhost:8080/> and sign in (e.g. `alice@demo.com` / `password123`), or
register a new account. The UI is a single page, `src/main/resources/static/index.html`
(plain HTML/CSS/JS, no build step), that only calls the REST API:

- **Notes:** a sidebar filter (All, Personal, or one of your teams), search, and a list
  showing each note's team (or "Personal") and when it was last updated.
- **Editor:** create, view, edit and delete notes. "Shared with" picks Personal or one of
  your teams. If you don't own a note, the team picker is disabled and Delete is hidden.
  The server enforces the same rules, and the UI shows its 403 if one comes back.
- **Teams:** "Manage teams" lets you create a team, see its members, add members by email
  and remove members. Remove/Leave buttons only appear where you're allowed to use them.
- **Errors** (400, 403, 404, 409) appear as inline messages. A 401 sends you back to the
  sign-in screen.

Credentials are kept in `sessionStorage` for the current tab only: a page reload keeps you
signed in, and signing out or closing the tab clears them. Sign in as `bob@demo.com` in
another browser profile to watch sharing between users.

### Demo data

On startup, if the database has no users, demo data is created (`DemoDataSeeder`):

| Email            | Password      | Team        | Notes                                    |
|------------------|---------------|-------------|------------------------------------------|
| `alice@demo.com` | `password123` | Engineering | 1 personal note; owns 2 Engineering notes |
| `bob@demo.com`   | `password123` | Engineering | 1 personal note; can read and edit the Engineering notes |
| `carol@demo.com` | `password123` | —           | 1 personal note; can't see any Engineering notes |

The database is in-memory, so restarting the app resets the demo data. Seeding is controlled
by `app.demo-data.enabled` in `application.properties`: it is off unless set to `true`, and
it is off during tests. Turn it off before any real deployment, because the demo password is
published here.

### Trying the API

[`requests.http`](requests.http) has example requests for registration, `/auth/me` and every
`/notes` and `/teams` endpoint. It uses alice, bob and carol to show the sharing and
membership rules (allowed, 403, 404 and 409 cases). Run it top to bottom in IntelliJ's HTTP client: later requests reuse ids
captured by earlier ones.

Run the tests with:

```bash
./mvnw test
```

## API

| Method | Path             | Auth       | Description                                          |
|--------|------------------|------------|------------------------------------------------------|
| POST   | `/auth/register` | public     | Create an account. Body: `{"email", "password"}`. Returns `201` with `{id, email}`; `409` if the email is taken; `400` on validation errors. |
| GET    | `/auth/me`       | HTTP Basic | Returns the signed-in user's `{id, email}`.           |
| POST   | `/notes`         | HTTP Basic | Create a note. Body: `{"title", "content", "teamId"?}`. Returns `201`. |
| GET    | `/notes`         | HTTP Basic | Notes you can access, most recently updated first. Optional `?teamId=` filter and `?q=` case-insensitive search on title and content. |
| GET    | `/notes/{id}`    | HTTP Basic | One note.                                            |
| PUT    | `/notes/{id}`    | HTTP Basic | Replace a note's title, content and team. Omitting `teamId` makes the note personal. |
| DELETE | `/notes/{id}`    | HTTP Basic | Delete a note (owner only). Returns `204`.           |
| POST   | `/teams`         | HTTP Basic | Create a team. Body: `{"name"}`. You become its creator and first member. Returns `201`. |
| GET    | `/teams`         | HTTP Basic | Teams you're a member of, each with its members' `{id, email}`. |
| POST   | `/teams/{id}/members` | HTTP Basic | Add an existing user. Body: `{"email"}`. Returns `201` with the updated team; `404` for an unknown email; `409` if already a member. |
| DELETE | `/teams/{id}/members/{userId}` | HTTP Basic | Remove a member. Returns `204`.  |

Passwords must be 8–72 characters. Emails are case-insensitive. Note titles are required
(max 255 characters); content is optional (max 20,000 characters).

Errors are returned as JSON (`ApiError`).

### Note access rules

A note belongs to its owner and, optionally, to one team (`teamId` null = personal note).
The rules are enforced in `NoteService`:

| Situation                                               | Result |
|---------------------------------------------------------|--------|
| Owner: read, edit, delete, move between their own teams | allowed |
| Member of the note's team: read and edit                | allowed |
| Member of the note's team: delete, or change `teamId`   | `403`  |
| Anyone else (including a missing note id)               | `404` — existence of other users' notes is never revealed |
| Assigning a note to a team you're not in (or that doesn't exist) | `400` on `teamId` |

### Team membership rules

Teams are small and trusted, so any member can add people. Enforced in `TeamService`:

| Situation                                        | Result |
|--------------------------------------------------|--------|
| Any member adds an existing user                 | allowed |
| A member removes themselves                      | allowed |
| The creator removes another member               | allowed |
| A non-creator removes someone else               | `403`  |
| Anyone tries to remove the creator               | `409` (every team keeps at least its creator) |
| Non-member, or a team id that doesn't exist      | `404` on every `/teams/{id}` endpoint |

Note access is worked out from team membership on every request, so a removed member
loses access to the team's notes immediately. Notes they own stay in the team, and they
can still see those because they own them.

### Example

```bash
curl -u alice@example.com:correct-horse -H 'Content-Type: application/json'      -d '{"title":"Sprint plan","content":"Ship notes API"}' http://localhost:8080/notes
curl -u alice@example.com:correct-horse 'http://localhost:8080/notes?q=sprint'
```

## Project structure

Package-by-layer under `com.bluestaq.demo`:

| Package      | Responsibility                                             |
|--------------|------------------------------------------------------------|
| `controller` | REST endpoints — thin delegators only                      |
| `service`    | Business logic, authorization checks, transactions         |
| `repository` | Spring Data repositories (used only by services)           |
| `model`      | JPA entities                                               |
| `dto`        | Request/response records and API error bodies              |
| `exception`  | Custom exceptions and the global exception handler         |
| `config`     | Security and other configuration                           |

The browser UI is `src/main/resources/static/index.html`, served at `/`.

Data model: `users`, `teams`, `team_members` (composite key) and `notes` (owner required,
team optional).

The database schema is owned by `src/main/resources/schema.sql`; Hibernate runs with
`ddl-auto=validate`, so startup fails if the entities drift from the schema.

## Security notes and trade-offs

Authentication is deliberately minimal for this stage: stateless HTTP Basic with
BCrypt-hashed passwords. CSRF protection is disabled because credentials travel in a
header, not a cookie.

In production this would be replaced with a proper identity provider (e.g. OAuth 2.0 /
OpenID Connect, optionally with MFA/OTP), served over HTTPS only, and backed by a
persistent database such as PostgreSQL. The H2 console at `/h2-console` is a development
convenience and must be removed before any real deployment.

## CI workflows

GitHub Actions in [`.github/workflows`](.github/workflows):

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| Test Coverage Guard | push to `devel` | `mvn test` with JaCoCo; prints line coverage |
| Dependency Security Scan | push to `devel` | OWASP dependency-check against the NVD database |
| Code Review Agent | manual | Claude reviews recent commits against the Engineering Principles in `CLAUDE.md` and commits a report to `reports/` |
| Auto-Implementer | called when the review finds HIGH issues | Claude fixes them on a `fix/auto-*` branch and opens a PR against `devel` for human review |

Required repository secrets: `ANTHROPIC_API_KEY` and `NVD_API_KEY`.

## Branches and versions

- `devel` — active development; `staging` — tested, demo-ready; `master` — public (default).
- The current version is in [`VERSION`](VERSION) (mirrored as `app.version` in
  `application.properties`).

## Change log

Code changes are recorded in [`FIXES.md`](FIXES.md).
