# Notes Service

A note-taking service used and shared amongst several small teams. It gives users
somewhere to capture their notes and work with them, through a REST API and a simple
browser UI.

> **Status:** early development. User registration and sign-in are in place; the notes
> features themselves are the next piece of work.

## Tech stack

- Java 21, Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation)
- H2 in-memory database (development only — data is lost on restart)
- Maven (wrapper included)
- Plain HTML/CSS/JS front end, no build step

## Running it

```bash
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

Then open <http://localhost:8080/> to register an account and sign in.

Run the tests with:

```bash
./mvnw test
```

## API

| Method | Path             | Auth       | Description                                          |
|--------|------------------|------------|------------------------------------------------------|
| POST   | `/auth/register` | public     | Create an account. Body: `{"email", "password"}`. Returns `201` with `{id, email}`; `409` if the email is taken; `400` on validation errors. |
| GET    | `/auth/me`       | HTTP Basic | Returns the signed-in user's `{id, email}`.           |

Passwords must be 8–72 characters. Emails are case-insensitive.

Errors are returned as JSON (`ApiError`).

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

## Change log

Code changes are recorded in [`FIXES.md`](FIXES.md).
