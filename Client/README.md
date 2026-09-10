# Golf Trip Booker — Client

Expo SDK 54 · React Native 0.81 · React 19.1 · Expo Router 6 · plain JavaScript.

> **Why SDK 54 and not the latest?** Expo Go's App Store releases stalled after
> SDK 54 — Expo announced in May 2026 that newer builds were pending Apple approval
> with no timeline, and repositioned Expo Go as a beginner's tool. A project on a
> newer SDK simply cannot open in the App Store build of Expo Go. Going past 54
> means an EAS development build, which needs an Apple Developer account for a
> physical device. Pinning here keeps the project runnable with nothing but the
> free app on a phone.

## Running it

```bash
npx expo start
```

Then open **Expo Go** on your phone and pick the dev server from the list, or scan
the QR code the terminal prints. Phone and Mac have to be on the same wifi.

The Spring server needs to be running too — start it first, so that if something
fails you already know it was not the server.

## How it finds the API

`localhost` on a phone is the phone, so a hard-coded `localhost:8080` would never
reach your Mac. `services/api.js` reads the address the phone already used to reach
the Metro bundler:

```js
Constants.expoConfig?.hostUri?.split(':')[0]   // -> 192.168.x.x
```

That is your machine on this network, by definition. Nothing to configure, and
nothing to fix when DHCP hands you a new address.

Two cases need an override, via `EXPO_PUBLIC_API_URL` in `.env` (see `.env.example`):

- **`--tunnel` mode.** `hostUri` becomes an `exp.direct` domain, and the tunnel only
  carries the JavaScript bundle, not your API. The app would look for Spring at a
  hostname that has no Spring on it.
- **A deployed server.**

The **Me** tab prints the address the app actually resolved. When a request fails
on a phone, that is the first thing worth knowing.

## Structure

Only `app/` is scanned for routes. Everything else is ordinary imports.

```
app/
  _layout.jsx        AuthProvider + Stack.Protected guards
  index.jsx          decides where "/" means, by role
  (auth)/            login, register
  (tabs)/            new · trips · queue · booked · explore · me
components/          Screen, Field, Button, ErrorList, Placeholder
contexts/            AuthContext
services/            api, authService
storage/             tokenStorage
theme.js             colours, spacing, status pill styles
```

Imports are relative rather than using the `@/` alias that `tsconfig.json` defines,
so nothing depends on the bundler resolving tsconfig paths. `@/components/Button`
would also work if you prefer it.

Note that SDK 56 and later move all of this under `src/`. If this project is ever
upgraded, the whole tree moves — the relative imports survive the move unchanged,
because `app/` and its siblings keep the same relationship to each other.

The screens are `.jsx` in a TypeScript project, which is fine — Babel compiles them
either way, and Expo Router matches `.js`, `.jsx`, `.ts`, and `.tsx` as routes.
`tsconfig.json` only governs the editor's checking of `.ts`/`.tsx` files.

## Auth

The token lives in `expo-secure-store` — Keychain on iOS, EncryptedSharedPreferences
on Android. That is what makes *"I stay logged in when I reopen the app"* true; a
cookie or an in-memory value would not survive the process being killed. On web
(`expo start --web`) SecureStore is unavailable and it falls back to `localStorage`.

On launch, `AuthContext` takes the stored token, calls `GET /auth/refresh`, and
takes a fresh one back. The server is the source of truth — a token decodes happily
long after the account behind it changed. A 401 means the token expired, so it is
deleted and you land on login.

Routing is gated by `Stack.Protected` in `app/_layout.jsx`. Two guards, exact
opposites, so exactly one branch is live. A guard that evaluates false makes those
screens unreachable *including by deep link*, unlike a redirect inside a screen,
which only runs after the screen has already mounted.

**The tab bar is presentation, not security.** `href: null` hides the host tabs from
a client, but that is not what stops a client reaching host data — the server does,
and would still return 403 if someone typed the route by hand.

## What is built

| Screen | State |
|---|---|
| Login (01) | Done |
| Register | Done |
| Me | Done — user, role, resolved API address, log out |
| New Request (02) | Placeholder |
| My Trips (03) + detail (04, 05) | Placeholder |
| Explore (06) | Placeholder |
| Host queue (07) + booking form (08) | Placeholder |

Each placeholder names the wireframe it becomes and the endpoint it will call.

## Gotchas worth knowing

- `npx expo install`, not `npm install`, for new packages. It resolves the version
  matched to SDK 57 instead of the newest on npm.
- Do **not** run `npm audit fix --force`. The moderate advisories are transitive dev
  tooling; the force flag will bump past your SDK's pins and break the bundle.
  `npx expo install --check` is the dependency-health command that respects them.
- If the dev server does not appear in Expo Go, scan the QR instead. When neither
  works it is the network: guest wifi with client isolation, a VPN on the Mac, or
  the macOS firewall prompt getting dismissed rather than allowed.
