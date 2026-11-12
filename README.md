# OpsDesk backend

The REST API for OpsDesk, an internal operations dashboard for an IT support
team. It stores support tickets, projects and users in PostgreSQL and serves
them to the React frontend,
[opsdesk-frontend](https://github.com/Jesutofunmiabby/opsdesk-frontend).

Every URL, request and response follows the API contract,
[`API_CONTRACT.md`](https://github.com/Jesutofunmiabby/opsdesk-frontend/blob/main/API_CONTRACT.md)
in the frontend repository. If the code and the contract disagree, the
contract wins and the code is fixed.

Built with Java 25, Spring Boot 4, Spring Data JPA (Hibernate) and Maven.

## What you need

- **Java 25.** Check with `java -version`. It was built with Eclipse Temurin 25.
- **Docker**, to run PostgreSQL. Check with `docker --version`, and make sure
  Docker Desktop is running.

Maven itself is not needed: the project comes with the Maven wrapper
(`mvnw`), which downloads the right version the first time it runs.

## Start the database

The first time, create and start a PostgreSQL 16 container called
`opsdesk-db`:

```bash
docker run --name opsdesk-db -e POSTGRES_USER=opsdesk -e POSTGRES_PASSWORD=opsdesk -e POSTGRES_DB=opsdesk -p 5432:5432 -d postgres:16
```

After that, for example after a restart, just start it again:

```bash
docker start opsdesk-db
```

`docker ps` should list `opsdesk-db` on port 5432. These credentials are for
your own computer only; never use them anywhere else.

To look inside the database:

```bash
docker exec -it opsdesk-db psql -U opsdesk
```

Then `\dt` lists the tables, `SELECT * FROM ticket;` shows the tickets, and
`\q` quits.

## Run the backend

```bash
./mvnw spring-boot:run
```

On Windows in Command Prompt or PowerShell, use `mvnw.cmd spring-boot:run`.

It starts on <http://localhost:8080>. Hibernate creates or updates the tables
on startup, and the first time it adds four sample projects and four sample
users so the frontend has something to show. Stop it with Ctrl+C.

### In Docker

The `Dockerfile` builds the JAR with the Maven wrapper on
`eclipse-temurin:25-jdk`, then runs it on the smaller
`eclipse-temurin:25-jre` as an ordinary user, not root. To start the backend
together with the database and the frontend, use `docker compose up --build`
in [opsdesk-system](https://github.com/Jesutofunmiabby/opsdesk-system).

To build and run only this image, against a database it can reach:

```bash
docker build -t opsdesk-backend .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod -e DB_URL=... -e DB_PASSWORD=... -e JWT_SECRET=... opsdesk-backend
```

`mvnw` must keep Unix line endings (set in `.gitattributes`) and its
executable flag in Git, or the build fails on Linux.

## Health check

`GET /actuator/health` needs no login and answers only whether the app is
working, for example for a Docker health check:

```bash
curl http://localhost:8080/actuator/health
```

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

It is `200` with `UP` when the app and its database are working, and `503`
while the app is still starting or when the database cannot be reached. No
other Actuator endpoint is turned on, because they can show settings and
secrets.

## Register and log in

Every endpoint except register and login needs a token.

1. **Register** once. The answer is `201` with a `token`, and the new account
   is always a `USER`:

   ```bash
   curl -H "Content-Type: application/json" -d '{"name":"Your Name","email":"you@example.com","password":"at least 8 characters"}' http://localhost:8080/api/v1/auth/register
   ```

2. **Log in** whenever you need a new token (for example after it expires, or
   after your role changes). The answer is `200` with a `token`:

   ```bash
   curl -H "Content-Type: application/json" -d '{"email":"you@example.com","password":"at least 8 characters"}' http://localhost:8080/api/v1/auth/login
   ```

   A wrong password and an unknown email both give `401` with the same
   message, so nobody can find out which emails have accounts.

3. **Send the token** in the `Authorization` header on every other request:

   ```bash
   curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/tickets
   ```

   Without the header, the answer is `401` with `"error": "UNAUTHENTICATED"`.

To make an account an ADMIN, see [Accounts and the first admin](#accounts-and-the-first-admin).

## Profiles: development and production

The same code runs with different settings depending on the Spring profile,
chosen with the `SPRING_PROFILES_ACTIVE` environment variable:

| File | Used | What is in it |
|------|------|---------------|
| `application.yml` | always | Shared settings, with defaults that are safe on your own computer only. |
| `application-dev.yml` | `dev` (the default when `SPRING_PROFILES_ACTIVE` is not set) | Prints every SQL statement and our own `DEBUG` log lines. |
| `application-prod.yml` | `prod` | Quiet logs (`INFO`), no SQL output, and no default for `JWT_SECRET` or `DB_PASSWORD`. |

A profile file is read after `application.yml`, so its values win.

In `prod`, if `JWT_SECRET` or `DB_PASSWORD` is missing or empty, the app stops
at startup with a message that names it:

```
java.lang.IllegalStateException: The prod profile needs these environment variables, but they are missing or empty: JWT_SECRET, DB_PASSWORD. Set them (see .env.example) and start again.
```

`./mvnw spring-boot:run` uses `dev`. To try `prod` locally, in Git Bash:

```bash
SPRING_PROFILES_ACTIVE=prod DB_PASSWORD=opsdesk JWT_SECRET=$(openssl rand -base64 48) ./mvnw spring-boot:run
```

## Environment variables

Every setting is read from an environment variable. Most have a default for
local development, in `src/main/resources/application.yml`.
[`.env.example`](.env.example) lists them all with example values.

| Variable | Default | What it does |
|----------|---------|--------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` or `prod` (see [Profiles](#profiles-development-and-production)). |
| `DB_URL` | `jdbc:postgresql://localhost:5432/opsdesk` | Where the database is. Inside Docker Compose it is `jdbc:postgresql://db:5432/opsdesk`. |
| `DB_USER` | `opsdesk` | The PostgreSQL user name. |
| `DB_PASSWORD` | `opsdesk` in `dev`; **none in `prod`** | The PostgreSQL password. The default only fits the local Docker database: set a real password this way, never in a committed file. |
| `ALLOWED_ORIGIN` | `http://localhost:5173` | The frontend address allowed to call the API from a browser. Several can be given, separated by commas. |
| `JWT_SECRET` | a local-only value in `dev`; **none in `prod`** | Signs login tokens. At least 32 characters; the app does not start with a shorter one. Anyone who knows it can make a token for any user, so set a long random value anywhere but your own computer. |

For example, in Git Bash:

```bash
ALLOWED_ORIGIN=http://localhost:5173,http://localhost:4173 ./mvnw spring-boot:run
```

To run with your own `JWT_SECRET`, make a long random one and pass it in the
same way (it is never written to a file):

```bash
JWT_SECRET=$(openssl rand -base64 48) ./mvnw spring-boot:run
```

Changing the secret logs everyone out: tokens signed with the old one stop
working, so everyone has to log in again.

## Run the tests

```bash
./mvnw test
```

On Windows in Command Prompt or PowerShell, use `mvnw.cmd test`. To run one
test class, add `-Dtest=`, for example `./mvnw test -Dtest=TicketSecurityIntegrationTest`.

The tests do not need PostgreSQL or Docker. The tests that start the whole
application use the `test` profile (`@ActiveProfiles("test")`), which
`src/test/resources/application-test.yml` points at an in-memory H2 database.
H2 is built from the entities when the tests start and thrown away when they
finish. The sample users in it have no password, and there are no tickets, so
each integration test registers its own users and creates its own tickets.

| Test | Kind | What it checks |
|------|------|----------------|
| `OpsdeskBackendApplicationTests` | Whole app on H2 | The application starts. |
| `TicketSecurityIntegrationTest` | Whole app on H2, MockMvc | With real tokens from register and login: no token is `401`, a USER deleting a ticket is `403` and the ticket stays, an ADMIN deleting is `204` and the ticket is gone, and a USER's list only has the tickets they created. Nothing is mocked. |
| `TicketControllerTest` | MockMvc, service mocked | The ticket endpoints through the real security chain: `401` without a login, `403` for a USER delete and an AGENT assign, `204` for an ADMIN delete, `200` for a MANAGER assign, and bad bodies and list parameters give the contract's error messages. |
| `AuthControllerTest` | MockMvc, service mocked | Register and login need no token; bad fields give the contract's messages; `/auth/me` is `401` without a token or with a broken one, and never shows the password hash. |
| `UserControllerTest` | MockMvc, service mocked | The "List users" row: a MANAGER can, an AGENT or a USER gets `403`. |
| `TicketPermissionTest` | Mockito | The ownership rules in `TicketService`: who can edit which ticket, a USER cannot see someone else's ticket, the creator comes from the login, and only staff can be assigned. |
| `TicketServiceTest` | Mockito | A new ticket is saved as `OPEN`; a token for a deleted account cannot create one; deleting a missing ticket throws not-found and deletes nothing. |
| `AuthServiceTest` | Mockito | Passwords are saved as BCrypt hashes; a taken email is a conflict; a wrong password and an unknown email give the same error. |
| `JwtServiceTest` | Plain JUnit | A token reads back as the same user and role; changed, expired and wrongly signed tokens are rejected; a secret under 32 bytes stops the app. |
| `QueryParamsTest` | Plain JUnit | The list's `sort` and id parameters, and their error messages. |
| `TicketResponseTest` | Plain JUnit | The ticket JSON shows the assignee's and creator's id and name, or `null`. |
| `TextTest` | Plain JUnit | Text is trimmed exactly as the frontend trims it, including non-breaking spaces. |

"MockMvc" sends real HTTP requests to the controllers without starting a
server. "Mockito" replaces the database with a mock, so only the class's own
rules are tested.

The permission rules in [Who can do what](#who-can-do-what) are checked by
these tests:

| Rule | Test |
|------|------|
| A USER cannot edit someone else's ticket | `TicketPermissionTest.aUserCanOnlyEditTheirOwnTicketWhileItIsOpen` |
| An AGENT can edit a ticket assigned to them, and no other | `TicketPermissionTest.anAgentCanOnlyEditTicketsAssignedToThem` |
| A USER cannot edit their own ticket once it is not `OPEN` | `TicketPermissionTest.aUserCanOnlyEditTheirOwnTicketWhileItIsOpen` |
| An ADMIN or MANAGER can edit any ticket | `TicketPermissionTest.adminAndManagerCanEditAnyTicket` |
| No token is `401` | `TicketSecurityIntegrationTest.noTokenIs401` |
| Only an ADMIN can delete | `TicketSecurityIntegrationTest.aUserCannotDeleteATicket`, `anAdminCanDeleteATicket` |
| A USER only sees their own tickets | `TicketSecurityIntegrationTest.aUsersListOnlyContainsTheirOwnTickets` |

To check that a test really guards its rule, break the rule on purpose and run
the tests again. For example, removing the USER filter in
`TicketService.findPage` makes `aUsersListOnlyContainsTheirOwnTickets` fail with
`JSON path "$.totalItems" expected:<1> but was:<2>`.

## Endpoints

Everything is under `/api/v1`. See the contract for the request and response
of each one.

| Method | Path | What it does |
|--------|------|--------------|
| GET | `/tickets` | One page of tickets. Takes `page`, `size`, `sort`, `status`, `priority`, `assigneeId` and `q`, for example `?status=OPEN&q=vpn&sort=createdAt,desc`. `sort` takes `id`, `title`, `createdAt` or `updatedAt`, then optionally `,asc` or `,desc`; the default is oldest first. A USER only ever gets tickets they created. |
| GET | `/tickets/{id}` | One ticket. |
| POST | `/tickets` | Creates a ticket: `201` with a `Location` header. |
| PATCH | `/tickets/{id}` | Changes some of a ticket's fields. |
| PATCH | `/tickets/{id}/assignee` | Assigns a ticket: `{"assigneeId": 3}`, or `null` to unassign. Only an ADMIN, MANAGER or AGENT can be assigned. |
| DELETE | `/tickets/{id}` | Deletes a ticket: `204`. |
| GET | `/projects` | One page of projects. |
| GET | `/projects/{id}` | One project. |
| GET | `/users` | One page of users. |
| POST | `/auth/register` | Creates an account, always with the role `USER`: `201` with a token. No token needed. |
| POST | `/auth/login` | `200` with a token. No token needed. |
| GET | `/auth/me` | The logged-in user. |

Every endpoint except register and login needs the header
`Authorization: Bearer <token>`, and answers `401` without a valid one.
A token lasts 24 hours (`opsdesk.jwt.expiry` in `application.yml`).

Every error has the contract's shape:
`{ "error": "NOT_FOUND", "message": "...", "fields": { ... } }`.

## Accounts and the first admin

Register always makes a `USER`, and the sample users have no password, so the
first admin is made by hand. Register an account, then in psql
(`docker exec -it opsdesk-db psql -U opsdesk`):

```sql
UPDATE app_user SET role = 'ADMIN' WHERE email = 'you@example.com';
```

Log in again afterwards: the role is stored in the token, so an old token
still has the old role.

If your database was created before the four roles existed, it only allows
`AGENT` and `ADMIN`, and register fails. Run this once in psql to fix it:

```sql
ALTER TABLE app_user DROP CONSTRAINT app_user_role_check;
ALTER TABLE app_user ADD CONSTRAINT app_user_role_check CHECK (role IN ('ADMIN', 'MANAGER', 'AGENT', 'USER'));
```

## Who can do what

**Authentication** answers "who are you?", and **authorization** answers
"what are you allowed to do?". In OpsDesk, logging in with your email and
password is authentication, and only an ADMIN being allowed to delete a
ticket is authorization.

Every user has one of four roles. This table is the rule for each action:

| Action | ADMIN | MANAGER | AGENT | USER |
|--------|-------|---------|-------|------|
| View tickets | all | all | all | own only |
| Create ticket | yes | yes | yes | yes |
| Edit ticket | any | any | assigned | own, if OPEN |
| Assign ticket | yes | yes | no | no |
| Delete ticket | yes | no | no | no |
| List users | yes | yes | no | no |

How the API applies it:

- A missing or invalid token is `401 UNAUTHENTICATED`. Logged in but not
  allowed is `403 FORBIDDEN`.
- A USER only sees tickets they created. Anyone else's ticket is
  `404 NOT_FOUND` for them, the same as a ticket that does not exist.
- "Assigned" means the ticket's `assigneeId` is the AGENT's id; "own" means
  its `createdById` is the USER's id. The creator is always taken from the
  login, never from the request body.

## Database queries and the N+1 problem

Each ticket in the JSON shows its assignee's and creator's names, which are
stored in `app_user`, not in `ticket`. Counted from the SQL that the `dev` profile prints, for
`GET /tickets?size=20&sort=id,desc` (20 tickets, 5 different users on the
page):

| | SQL statements |
|---|---|
| Before: tickets only, users loaded when their name is read | **7**: 1 for the page, 1 count, then 1 per user (5) |
| After: `@EntityGraph(attributePaths = {"assignee", "createdBy"})` | **2**: 1 for the page with the users joined in, 1 count |

Before the fix the count grows with the page: 20 tickets with 40 different
users would be 42 statements. After it, it is always 2. (The "before" was
measured with `spring.jpa.open-in-view=true`, Spring Boot's default; with
this project's `false`, reading a name after the query fails instead.)

The `ticket` table also has indexes on `status`, `priority`, `assignee_id`
and `created_by_id`, the columns the list filters by.

## How the code is organised

```
src/main/java/com/opsdesk/opsdeskbackend/
  ticket/    Ticket entity, TicketController, TicketService, TicketRepository,
             the request and response DTOs, and the list filters
  project/   Project entity, controller, service, repository and DTO
  user/      AppUser entity (the table is app_user, because user is reserved
             in PostgreSQL), controller, service, repository and DTO
  auth/      AuthController and AuthService for register, login and /auth/me,
             and their request and response DTOs
  security/  SecurityConfig (the filter chain and which paths need a token),
             JwtService (makes and checks tokens), JwtAuthFilter (reads the
             Authorization header on each request), the JSON 401 answer, and
             AuthUser, the logged-in user's id and role
  common/    shared pieces: the global exception handler, the error and page
             shapes, query parameter checks, dates and text trimming
  config/    CorsConfig, which lets the frontend call the API
  seed/      DataSeeder, which adds sample projects and users to an empty
             database
```

Each feature has three layers: a **controller** for HTTP, a **service** for
the business rules and a **repository** for the database. Controllers only
ever return DTOs, never entities.
