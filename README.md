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

Try it:

```bash
curl http://localhost:8080/api/v1/tickets
```

## Environment variables

Every setting has a default for local development, in
`src/main/resources/application.yml`. Set an environment variable to change
it.

| Variable | Default | What it does |
|----------|---------|--------------|
| `DB_USER` | `opsdesk` | The PostgreSQL user name. |
| `DB_PASSWORD` | `opsdesk` | The PostgreSQL password. The default only fits the local Docker database: set a real password this way, never in a committed file. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | The frontend addresses allowed to call the API from a browser, separated by commas. |

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
| GET | `/tickets` | One page of tickets. Takes `page`, `size`, `status`, `priority` and `search`. |
| GET | `/tickets/{id}` | One ticket. |
| POST | `/tickets` | Creates a ticket: `201` with a `Location` header. |
| PATCH | `/tickets/{id}` | Changes some of a ticket's fields. |
| DELETE | `/tickets/{id}` | Deletes a ticket: `204`. |
| GET | `/projects` | One page of projects. |
| GET | `/projects/{id}` | One project. |
| GET | `/users` | One page of users. |

Every error has the contract's shape:
`{ "error": "NOT_FOUND", "message": "...", "fields": { ... } }`.

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
