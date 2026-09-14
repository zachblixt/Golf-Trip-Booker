# Golf Trip Booker — Server

Spring Boot 3.5.7 · Java 17 · MySQL · JdbcClient · **stateless** Spring Security with JWT.

A client asks for a golf trip — a place, a budget per player, a party size, rounds,
nights. A host either books it, counters with a proposal, or declines it with a reason.

## Why JWT instead of a session cookie

The pre-capstone used `formLogin` and a `JSESSIONID`. That does not survive the
client here: React Native has no dependable cookie jar, `credentials: "include"` is
effectively a no-op in Expo Go, and the cookie is gone the moment the app reloads —
so "I stay logged in when I reopen the app" would fail. A bearer token in
`expo-secure-store` survives, and it also sidesteps the CORS rule that forbids a
wildcard origin whenever `allowCredentials(true)` is set, which matters because the
phone reaches the server at your LAN address, not `localhost`.
## Running it

The whole stack, API and a seeded MySQL, comes up from the repository root:

```bash
docker compose up --build
```

That builds this service from `Dockerfile`, starts MySQL, and runs
`production-schema.sql`, `test-schema.sql` and `initial-data.sql` into it on first
boot. Nothing needs to be installed locally but Docker. See the root `README.md`
for the details, including how to force a re-seed.

To run it from an IDE against your own MySQL instead, the app reads `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET` from the environment. Run the three
scripts in `database/` yourself first, in that order, and point JUnit at
`golf_trip_booker_test` rather than `golf_trip_booker`.

## Seed accounts

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

## Reaching the server from Expo Go

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

## Drafting a proposal

`POST /api/host/request/{id}/proposal/draft` returns a proposal for the host to edit,
built from the confirmed trips already taken at that destination. It writes nothing.

Optional: with no `ANTHROPIC_API_KEY` the app starts normally, every screen works, and the
endpoint declines with a plain sentence. Drafting is a convenience on top of a form a host
can always fill in by hand, and it must never become load-bearing.

**It cannot book anything**, and that is structural rather than a promise:
`ProposalDraftService` holds no write path. What it returns goes to a screen, a host edits
it, and what they send travels through `BookingService.propose` exactly as a hand-typed
proposal always has. The draft is validated first by `BookingService.validateProposal`,
which runs the same rules `propose()` does, so a suggestion the server would reject on
submit is never shown.

**No history, no draft.** The confirmed trips at a destination are the menu: the prompt
names them as the only courses and lodging it may choose from. A request for a place nobody
has booked, or a destination with no confirmed trips, declines rather than generating,
because the only other source of a course name is invention.

**The price is computed in Java, not asked for.** The first version let the model price the
trip. Given request 53 (8 players, 3 rounds, 2 nights, $400 per player) it returned exactly
$3,200 -- eight times the budget to the dollar -- cited the least similar past trip as its
basis, and described a calculation that had not happened. That is ordinary behaviour: put a
budget and a request for a number in the same prompt and the budget comes back.

`PriceGuide` now takes the median cost per player per round across confirmed trips at that
destination and multiplies by the rounds requested. The prompt receives a finished number
and is told not to recalculate it; the budget is relabelled as context; comparables arrive
sorted by shape, with the one matching rounds and nights labelled as the closest. The same
request now prices at $2,848 from the trip it actually resembles.

`PriceGuide` prices on rounds and ignores nights. With a handful of trips per destination
there is not enough data to separate a lodging rate from a green fee, so a destination whose
trips vary widely in nights will estimate worse.

`LlmClient` is the seam: two prompts and a JSON schema in, a JSON object out, no knowledge
of golf. `AnthropicLlmClient` is the only file that knows a vendor exists, and it uses
Spring's `RestClient` with Jackson, so there is no new dependency. That seam is what lets
the tests exercise the prompt, the parsing and the validation against canned answers, and
therefore what lets CI run this feature on every push without calling a paid API.

## Tests

167 tests: 62 in the data layer, 103 in the domain layer, 2 over HTTP.

Repository tests run against a real `golf_trip_booker_test` database and call
`set_known_good_state()` before each method. `TestDataHelper` mirrors that
procedure — change one, change the other in the same commit. Service tests mock
the repositories with `@MockitoBean` and focus on the guards and validation.

104 of the 167 need a live MySQL: every data-layer test, and every domain test that
opens a transaction. That is why they are skipped during the Docker image build, which
has no database, and run in CI against a MySQL service container instead. Locally:

```bash
export DB_URL=jdbc:mysql://localhost:3306/golf_trip_booker_test
export DB_USERNAME=root
export DB_PASSWORD=...
mvn test
```

`AuthControllerTest` is the first test over the HTTP layer, a `@WebMvcTest` slice that
needs no database at all. It has to `@Import(SecurityConfig.class)`: a slice does not pick
up a plain `@Configuration`, and without it the test silently exercises Spring Boot's
default security instead of this project's, which answers 403 to everything. Any further
MockMvc security test needs the same import to be worth anything.

`JwtConverter` and the rest of the routes are still uncovered.
