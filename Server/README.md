# Golf Trip Booker — Server

Spring Boot 3.5.7 · Java 17 · MySQL · JdbcClient · **stateless** Spring Security with JWT.

A client asks for a golf trip — a place, a budget per player, a party size, rounds,
nights. A host either books it or declines it with a reason.

## Why JWT instead of a session cookie

The pre-capstone used `formLogin` and a `JSESSIONID`. That does not survive the
client here: React Native has no dependable cookie jar, `credentials: "include"` is
effectively a no-op in Expo Go, and the cookie is gone the moment the app reloads —
so "I stay logged in when I reopen the app" would fail. A bearer token in
`expo-secure-store` survives, and it also sidesteps the CORS rule that forbids a
wildcard origin whenever `allowCredentials(true)` is set, which matters because the
phone reaches the server at your LAN address, not `localhost`.

## What you need installed

Nothing beyond what the pre-capstone already needed:

| | Check it |
|---|---|
| A JDK (17 or newer) | `java -version` |
| Maven | IntelliJ bundles it. Standalone: `mvn -v` |
| MySQL 8, running | `mysql --version` and `mysqladmin ping` |

The pom adds four artifacts — `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.6) and
`spring-security-test`. Maven downloads them on the first build, so the only
requirement is an internet connection the first time. In IntelliJ that is the
Maven panel's **Reload All Maven Projects**; from a terminal, `mvn clean install`.

## Setup

### 1. Build the databases

Run these in MySQL Workbench or the `mysql` CLI, in order:

```
database/production-schema.sql
database/test-schema.sql
database/initial-data.sql      <- seed data, production DB only
```

`test-schema.sql` uses `delimiter //` to define `set_known_good_state()`. That is a
client command, not server SQL — Workbench and the `mysql` CLI both understand it,
but pasting it into a tool that does not will fail on the procedure body.

### 2. Environment variables

Four of them. The app reads `DB_*` in `application.properties`; `JWT_SECRET` has a
development default so the server starts without it, but set it anyway — the
default is in version control, which is the one place a signing key must never be.

```bash
export DB_URL=jdbc:mysql://localhost:3306/golf_trip_booker
export DB_USERNAME=root
export DB_PASSWORD='your-password'
export JWT_SECRET="$(openssl rand -base64 48)"   # any 32+ bytes; openssl ships with macOS
```

**Tests point at a different database.** Same three `DB_*` names, one different value:

```bash
export DB_URL=jdbc:mysql://localhost:3306/golf_trip_booker_test
```

### 3. Setting them in IntelliJ

Two run configurations, because the app and the tests need different `DB_URL`s:

- **Run → Edit Configurations → `Main`** → *Environment variables* → the four above,
  pointing at `golf_trip_booker`.
- **Run → Edit Configurations → Edit configuration templates… → JUnit** →
  *Environment variables* → the same three `DB_*`, pointing at
  `golf_trip_booker_test`.

Setting the JUnit **template** rather than one test class means every test you
write from here on inherits the test database automatically. Getting this wrong is
the classic way to have `set_known_good_state()` wipe your seed data.

If a test fails with `Unknown database 'golf_trip_booker_test'` or a repository
test suddenly finds zero rows, this is why: the run configuration is pointed at the
wrong schema.

### 4. First run

`Main.java` starts on port 8080. Because `server.address=0.0.0.0` is set — so a
phone running Expo Go can reach it — macOS will pop a firewall prompt the first
time asking whether to allow incoming connections for Java. Allow it, or the phone
will time out later while `localhost` keeps working and hides the problem.

All nine seed accounts use the password `1234`. It is deliberately short so signing
in during a demo is not four seconds of thumbing a symbol on a phone keyboard. Worth
knowing that `UserService` would refuse it at registration: a real sign-up needs eight
characters with a digit, a letter and a symbol. Seeded rows go in underneath that check.

| Username | Role | Notes |
|---|---|---|
| `zach` | CLIENT | Request 51 is a live proposal with one refusal already behind it. Start a demo here. |
| `dana` | CLIENT | One pending, one past trip |
| `mo` | CLIENT | One pending, one past trip |
| `kyle` `brett` `tyler` `jenn` `ray` | CLIENT | The rest of the seeded history that Explore reads |
| `tourpro` | HOST | The only host. Sees the queue. |

### Reaching the server from Expo Go

`localhost` on a phone is the phone. Find your machine's LAN address
(`ipconfig getifaddr en0` on a Mac) and point the client at
`http://192.168.x.x:8080`. `server.address=0.0.0.0` is already set so the server
listens on that interface, and the CORS patterns in `application.properties`
already cover the private ranges. A native Expo build sends no `Origin` header at
all, so CORS only ever matters for the Vite web client and `expo start --web`.

## Endpoints

Everything except register and login needs `Authorization: Bearer <token>`.

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | public | 201 `UserResponse`. Always a CLIENT. |
| POST | `/api/auth/login` | public | 200 `{ token, user }` · 401 on a bad password |
| GET | `/api/auth/me` | required | 200 `UserResponse` · 401 |
| GET | `/api/auth/refresh` | required | 200 `{ token, user }` — a fresh token on app launch |
| GET | `/api/destination` | required | The picker on the New Request screen |
| GET | `/api/destination/{id}` | required | 200 · 404 |
| GET | `/api/request` | client | My Trips. Scoped to the token. |
| GET | `/api/request/{id}` | owner or host | 200 · 403 someone else's · 404 |
| POST | `/api/request` | client | 201 `TripRequestResponse` |
| PUT | `/api/request/{id}` | owner, pending | 204 *(post-MVP edit)* |
| DELETE | `/api/request/{id}` | owner, pending | 204 — sets status CANCELLED, keeps the row |
| GET | `/api/explore` | required | Booked trips, anonymised *(post-MVP)* |
| GET | `/api/host/request` | HOST | The queue, oldest first |
| GET | `/api/host/request/{id}` | HOST | One request plus its budget ceiling |
| POST | `/api/host/request/{id}/booking` | HOST | 201 — books it and flips the request |
| POST | `/api/host/request/{id}/decline` | HOST | 204 — reason required |
| PUT | `/api/host/booking/{id}` | HOST | 204 *(post-MVP correction)* |

`/api/explore` takes two optional filters: `maxCostPerPlayer` and `destinationId`.
No parameters means "any budget, anywhere".

Error bodies are always a JSON **array of strings**, so the client can do
`setErrors(payload)` without branching. Status codes come from `ResultType` via
`ErrorResponse.build`: `INVALID` → 400, `FORBIDDEN` → 403, `NOT_FOUND` → 404.

There is no `/logout`. Nothing is stored server side, so logging out is the client
deleting its token.

## How the security stories are enforced

**"I can't make myself a host"** — three layers, any one of which would do:
`RegisterForm` has no `role` field, `UserService.create` takes no role parameter,
and `UserJdbcClientRepository.create` writes the literal `'CLIENT'` rather than
reading the argument. `UserJdbcClientRepositoryTest.createIgnoresARoleOfHost` is
the test that proves the last one.

**"I can't see or change another client's request"** — no endpoint takes a user id.
`GET /api/request` reads the id off the authenticated user; every single-request
route loads the row and compares its owner to the caller. A stranger gets 403 rather
than 404, because the request does exist — they simply may not have it.
`TripRequestServiceTest.findByIdIsForbiddenForAnotherClient` covers it.

**"I can't reach host features"** — `SecurityConfig` gates the whole `/api/host/**`
branch with `hasRole("HOST")`, and every method in `BookingService` checks the role
again, so a routing mistake cannot quietly open the door.

**The owner is never read from the body.** A trip request's client comes from the
JWT and a booking's host comes from the JWT.
`TripRequestServiceTest.createIgnoresTheClientFromTheRequestBody` and
`BookingServiceTest.bookRecordsTheAuthenticatedHost` are the proofs.

## Business rules worth knowing

- A request starts `PENDING` and moves exactly once. `BOOKED`, `DECLINED`, and
  `CANCELLED` are terminal, which is why cancelling a booked trip is a 400.
- A booking's total cannot exceed `budgetPerPlayer × playerCount`. That is the
  "Ceiling $4,000" on the host's booking form. Over it, the host declines instead.
- A booking cannot list more courses than the client asked for rounds. Fewer is
  fine — the same course twice is a real itinerary.
- Cancelling does not delete. The host's record of what was asked for survives.

## Notes on the schema

`booking.courses` is one text column with a course per line. The post-MVP story
"pick each course and round date individually" is exactly what turns that column
into its own table; `BookingResponse` already sends courses as a JSON array, so
that change will not reach the client.

One correction from the ERD: it drew the foreign key as
`trip_request.request_id → booking.request_id`. MySQL will not accept that — the
child row is the booking — so `production-schema.sql` puts the FK on
`booking.request_id → trip_request.request_id`, with a `unique` constraint holding
"one booking per request".

## Tests

```bash
export DB_URL=jdbc:mysql://localhost:3306/golf_trip_booker_test
mvn test
```

Repository tests run against the real test database and call
`set_known_good_state()` before each method. `TestDataHelper` mirrors that
procedure — change one, change the other in the same commit. Service tests mock
the repositories with `@MockitoBean` and focus on the guards and validation.
