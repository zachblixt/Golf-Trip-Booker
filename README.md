# Golf Trip Booker

A client asks for a golf trip: a place, a budget per player, a party size, rounds and
nights. A host either books it, counters with a proposal, or declines it with a reason.

A Spring Boot API with a React Native client, built as the capstone for the Dev10 program.

## Stack

| | |
|---|---|
| Server | Spring Boot 3.5.7, Java 17, MySQL 8, JdbcClient, stateless Spring Security with JWT |
| Client | Expo SDK 54, React Native 0.81, React 19.1, Expo Router 6 |
| Tests | 167 JUnit tests. 104 of them run against a real MySQL |

## Running it

Start the server first, so that if something fails you already know it was not the server.

```bash
# 1. In MySQL Workbench or the mysql CLI, run these in order
Server/database/production-schema.sql
Server/database/test-schema.sql
Server/database/initial-data.sql

# 2. Server
export DB_URL=jdbc:mysql://localhost:3306/golf_trip_booker
export DB_USERNAME=root
export DB_PASSWORD='your-password'
export JWT_SECRET="$(openssl rand -base64 48)"
cd Server && mvn spring-boot:run

# 3. Client, in a second terminal
cd Client && npx expo start
```

Open Expo Go on a phone on the same wifi and scan the QR code.

All seed accounts use the password `1234`. Sign in as `zach` for the client side or
`tourpro` for the host queue.

Full setup, including the IntelliJ run configurations and the separate test database,
is in [`Server/README.md`](Server/README.md). How the app finds the API on your LAN,
and the screen structure, are in [`Client/README.md`](Client/README.md).

## Layout

```
Server/         Spring Boot API. Controllers, domain services, JdbcClient repositories.
  database/     Schema, test schema with set_known_good_state(), and seed data.
  http/         Request files for hitting the API by hand.
Client/         Expo app. Only app/ is scanned for routes.
```

## Design notes

The two sub-READMEs cover the decisions worth defending: why the API authenticates with
a JWT instead of a session cookie, why the Expo SDK is pinned at 54, how the ownership
and role checks are enforced in more than one layer so a routing mistake cannot quietly
open a door, and why cancelling a request sets a status rather than deleting a row.
