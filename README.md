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

Try it. Every endpoint except register and login needs a token, so register
first and copy the `token` from the answer:

```bash
curl -H "Content-Type: application/json" -d '{"name":"Your Name","email":"you@example.com","password":"at least 8 characters"}' http://localhost:8080/api/v1/auth/register
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/tickets
```

Without the header, the answer is `401` with `"error": "UNAUTHENTICATED"`.

## Environment variables

Every setting has a default for local development, in
`src/main/resources/application.yml`. Set an environment variable to change
it.

| Variable | Default | What it does |
|----------|---------|--------------|
| `DB_USER` | `opsdesk` | The PostgreSQL user name. |
| `DB_PASSWORD` | `opsdesk` | The PostgreSQL password. The default only fits the local Docker database: set a real password this way, never in a committed file. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | The frontend addresses allowed to call the API from a browser, separated by commas. |
| `SQL_LOG` | `info` | Set to `debug` to print every SQL statement Hibernate runs. |
| `JWT_SECRET` | a local-only value | Signs login tokens. At least 32 characters; the app does not start with a shorter one. Anyone who knows it can make a token for any user, so set a long random value anywhere but your own computer. |

For example, in Git Bash:

```bash
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:4173 ./mvnw spring-boot:run
```

## Run the tests

```bash
./mvnw test
```

The database must be running, because one test starts the whole application,
which connects to it. The other tests need no database:

| Test | What it checks |
|------|----------------|
| `OpsdeskBackendApplicationTests` | The application starts and connects to PostgreSQL. |
| `TicketServiceTest` | A new ticket is saved as `OPEN`; deleting a missing ticket throws not-found and deletes nothing. The repository is a Mockito mock. |
| `TextTest` | Text is trimmed exactly as the frontend trims it, including non-breaking spaces. |

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
stored in `app_user`, not in `ticket`. Counted with `SQL_LOG=debug` for
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
  common/    shared pieces: the global exception handler, the error and page
             shapes, query parameter checks, dates and text trimming
  config/    CorsConfig, which lets the frontend call the API
  seed/      DataSeeder, which adds sample projects and users to an empty
             database
```

Each feature has three layers: a **controller** for HTTP, a **service** for
the business rules and a **repository** for the database. Controllers only
ever return DTOs, never entities.
