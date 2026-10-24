# CLAUDE.md

## What this project is
A note-taking service used and shared amongst several small teams. It gives users
somewhere to capture their notes and work with them, through a REST API and a simple
browser UI.
Spring Boot 4.1.1 / Java 21 REST service

## Engineering Principles

These principles guide development throughout 
the project. However, the Review Agent audits 
every commit against them automatically, flagging 
findings by severity; HIGH findings trigger the 
Auto-Implementer, which opens a pull request for 
human review before any fix is merged.

### Least privilege
Every process, Agent, workflow, and service account must have 
only the minimum permissions needed to do its job. Nothing more.

Apply this to:
- GitHub Actions workflows: scope --allowedTools explicitly, 
  never use broad Bash access when specific commands suffice,
  scope file write permissions to specific directories
- Spring Security: never grant broader access than the specific 
  endpoint or role requires
- API scopes: request only the OAuth/API scopes the app actually uses
- Database access: controllers never touch repositories directly,
  services own data access
- Agent permissions: read-only Agents get no write tools,
  write-scoped Agents are limited to specific paths

Every workflow, configuration, or infrastructure 
change documents what permissions are granted 
and why, and flags any permission that could 
be narrowed.

### Defence in depth
Never rely on a single layer of protection. Apply at least two 
independent controls for any sensitive operation:
- Prompt-level instruction AND CLI permission flag
- Security config AND input validation
- Environment variable AND secret manager

### Fail safe
When uncertain, fail closed rather than open. An explicit error 
is always preferable to silent degradation or unexpected access.

### Auditability
Every automated change must be traceable:
- Automated commits must identify the Agent that made them
- FIXES.md must be updated for every source code change
- Workflow runs must produce a report even if findings are empty

## Code structure

Package-by-layer under `com.bluestaq.demo`. All new code — including code for new
features — goes into the matching layer package; do not create per-feature packages.

| Package      | Contains                                                            | Example                                      |
|--------------|---------------------------------------------------------------------|----------------------------------------------|
| `controller` | `@RestController`s — thin delegators, no business logic             | `AuthController`                             |
| `service`    | Business logic, authorization checks, transactions                  | `AuthService`, `AppUserDetailsService`       |
| `repository` | Spring Data repositories — only services use them                   | `UserRepository`                             |
| `model`      | JPA entities; each has its table in `schema.sql` first              | `User`                                       |
| `dto`        | Request/response DTOs (records) and API error bodies; controllers never return entities | `RegisterRequest`, `UserResponse`, `ApiError` |
| `exception`  | Custom exceptions and the `@RestControllerAdvice` handler           | `EmailAlreadyRegisteredException`, `ApiExceptionHandler` |
| `config`     | Security and other `@Configuration` classes                         | `SecurityConfig`                             |

Dependencies point downwards only: `controller` → `service` → `repository` → `model`.

The browser UI is a single static page, `src/main/resources/static/index.html` (plain
HTML/CSS/JS, no build step), served at `/`. It only calls the REST API; any new public
static path must be added to `SecurityConfig` explicitly (protected file).
Tests mirror the main package of the class under test (e.g.
`src/test/java/com/bluestaq/demo/controller/AuthControllerTest.java`).

## Conventions
- **Fix log**: whenever code fixes are applied, append a dated entry to `FIXES.md` in the project root using the format `## YYYY-MM-DD — <short description>`, listing each file changed and the reason.

## Code commenting conventions
- Every class must have a Javadoc comment explaining its purpose and responsibility.
- Every public method must have a Javadoc comment explaining what it does, its parameters, and what it returns.
- Complex or non-obvious logic must have inline comments explaining **WHY**, not just what.
- Security-relevant code must always have a comment explaining the security decision (e.g. why something is exempt from CSRF, why a scope is required, etc.).
- **TEMPORARY** code must always have a comment explaining what it is and when to remove it.

# Agent Instructions

When running as an agent on this project:

### Before making any changes
- Run `mvn compile` first to confirm the project builds cleanly
- Report any compile errors before proceeding

### Architecture rules
- All business logic must live in the service layer — controllers are thin delegators only
- Never add tokens, passwords or other credentials to entity classes
- `src/main/resources/schema.sql` owns the database schema; Hibernate runs with
  `ddl-auto=validate` only — never change it to `update`/`create`/`create-drop`.
  Every new entity gets its table added to schema.sql **first**, then the entity is
  written to match it. Statements must be idempotent (`CREATE TABLE IF NOT EXISTS`),
  non-destructive (never `DROP`/`TRUNCATE`) and portable between H2 and PostgreSQL.

### Files that require human review before changes
- SecurityConfig.java — any changes here affect all authentication and authorization
- schema.sql — changes here affect the database schema for all environments

### After making changes
- Run `mvn test` and report the full results before finishing
- If any tests fail, attempt to fix them before reporting done
- Append an entry to FIXES.md describing what was changed and why

### Handling uncertainty and assumptions

Before starting any task, if any of the following are unclear, STOP and ask:
- Which files or classes should be modified
- What the expected behaviour should be (inputs, outputs, edge cases)
- Whether a new file/class is needed or an existing one should be extended
- Any data model changes (new fields, new tables)

Do NOT assume and proceed. List your questions as a numbered list and wait
for answers before writing any code.

When you do make a necessary assumption (minor style choices etc.),
state it explicitly: "I'm assuming X — let me know if that's wrong."

### When you don't know how to proceed

If you encounter any of the following, STOP and report it rather than guessing:

- A library, framework, or API you haven't seen before in this codebase
- A requirement that conflicts with an existing design decision
- A task that would require changing a protected file (SecurityConfig.java,
  schema.sql)
- A situation where two reasonable approaches exist and the choice
  significantly affects the architecture

Report it like this:
"I'm not sure how to proceed here because [reason].
The options I can see are: [option A] or [option B].
Which would you prefer, or would you like to discuss?"

## Personal Preferences
Read PERSONAL-PREFERENCES.md for personal workflow conventions.

## MANDATORY: Version management on git operations

BEFORE every git push to devel OR merge
devel → staging → master:

1. Read the version management section in
   PERSONAL-PREFERENCES.md
2. Apply the version bump rules:
   - Push to devel → increment patch
   - Merge to master → increment minor, reset patch
3. Update VERSION file (repo root)
4. Update app.version in application.yml
5. Include these changes in the commit

DO NOT push or merge without completing
these steps. This is not optional.
