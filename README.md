# SwiftDeliver Delivery Platform

## Live Demo
- **Frontend:** [deployed on Vercel — link here]
- **Backend API:** [deployed on Render — link here]

## Local Development
```bash
cp .env.example .env
# Fill in values in .env
docker compose up --build
```
Frontend: http://localhost
Grafana: http://localhost:3000

Full-stack delivery platform: **Spring Boot backend** (JWT auth, orders, partnerships, assignment engine), **Angular web admin**, and **React Native (Expo) mobile** app.

## Quick start (Docker)

Requires Docker with the Compose plugin.

```bash
docker compose up --build
```

This starts:

| Service  | URL                                   |
| -------- | ------------------------------------- |
| MySQL    | `localhost:3306` (db `swiftdeliver_db_new`) |
| Backend  | `http://localhost:8080`               |
| Web      | `http://localhost:4200`               |

On first startup the backend auto-creates the schema and seeds a full demo dataset:

- **Super Admin** account (credentials come from `ADMIN_USERNAME` / `ADMIN_PASSWORD` in `.env`):

```
username: admin
password: <set ADMIN_PASSWORD in .env>
```

- **Demo business dataset** when empty: 7 customers, 3 vendor owners, 2 delivery owners, 5 vendor companies, 4 delivery companies, 8 drivers, 24 products (linked to the vendor companies), 36 orders, 6 partnerships.
- Only the **Super Admin** account exists (staff/admin sub-accounts are not seeded).
- Demo user passwords use `SwiftDeliver@2026!` (e.g. vendor owner `naimabarka`).

> Change `admin`'s credentials in production by setting `ADMIN_USERNAME` / `ADMIN_PASSWORD`.

Optional overrides (create a `.env` beside `docker-compose.yml` or export):

```bash
MYSQL_ROOT_PASSWORD=root
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change_me
JWT_SECRET=$(openssl rand -base64 32)   # REQUIRED — backend refuses to start without it
```

The web app is built with the API base baked in as `http://localhost:8080/api` (`swiftdeliver-frontend/src/app/config.ts`).

## Local development

### Backend (Java 17+, Maven wrapper included)

```bash
cd swiftdeliver-backend
cp .env.example .env          # set DB + ADMIN_* values
.\mvnw.cmd spring-boot:run    # *nix: ./mvnw spring-boot:run
```

Tests: `.\mvnw.cmd test`

### Web (Node 22+, Angular 20)

```bash
cd swiftdeliver-frontend
npm install
npm start                     # http://localhost:4200
```

Production build: `npm run build` (outputs to `dist/del/browser`).

### Mobile (Expo)

```bash
cd swiftdeliver-mobile
npm install
npx expo start
```

The API base defaults to `http://10.0.2.2:8080/api` (Android emulator loopback). On a physical device, point it at your machine's LAN IP:

```bash
cp .env.example .env          # then edit EXPO_PUBLIC_API_URL
EXPO_PUBLIC_API_URL=http://192.168.x.x:8080/api
```

## API

- Base URL: `http://localhost:8080/api`
- Auth: `POST /api/auth/login`
- Register (customer): `POST /api/users/register`
- Token-based auth via `Authorization: Bearer <jwt>`; tokens are keyed by JTI for revocation/blacklisting.
- Platform dashboard (Super Admin): `GET /api/dashboard/overview` returns live KPIs (customers, orders, revenue, per-status order counts, companies, owners, drivers, products, admins, partnerships).
- Per-domain stats: stats/count + statistics endpoints under `/api/orders`, `/api/customer-users`, `/api/driver-persons`, `/api/delivery-owners`, `/api/delivery-companies`, `/api/vendor-owners`, `/api/vendor-companies`, `/api/products`, `/api/admins`, `/api/super-admins` (Super Admin).

## Realtime (WebSocket / STOMP)

- Endpoint: `ws://localhost:8080/ws` (SockJS).
- Subscribe to the global feed `TOPIC /topic/orders` and per-order `TOPIC /topic/orders/{id}` (open, matches existing clients).
- Private per-user feed `TOPIC /topic/users/{userId}` — receiving only the events for orders you are involved in.
  - Authenticate the STOMP handshake by passing your access JWT as a query param: `/ws?token=<jwt>`.
  - Each session may only subscribe to its **own** user topic; subscribing to another user's topic is rejected with `403`.
- Event types: `ORDER_CREATED`, `ORDER_STATUS_CHANGED`, `DRIVER_ASSIGNED` (payload includes `involvedUserIds`), and `DRIVER_LOCATION_UPDATE` on `/topic/orders/location` and `/topic/orders/{id}`.