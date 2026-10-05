# SwiftDeliver

A multi-tenant delivery marketplace. Vendors list products, customers order them, delivery companies bid for the jobs and assign their drivers, and everyone follows the order live as the driver reports positions. One platform, five kinds of user, each seeing only their own slice of the data.

**Spring Boot 4 · Java 21 · MySQL · JWT · STOMP/WebSocket · Angular 20 · Docker · Prometheus + Grafana**

| Super Admin dashboard | Partnerships between vendors and delivery companies |
| --- | --- |
| ![Admin dashboard](docs/screenshots/admin-dashboard.png) | ![Partnerships](docs/screenshots/admin-partnerships.png) |

| Customer: own orders | Driver portal |
| --- | --- |
| ![Customer orders](docs/screenshots/customer-orders.png) | ![Driver portal](docs/screenshots/driver-jobs.png) |

| Vendor: product catalogue | Grafana: API health |
| --- | --- |
| ![Vendor products](docs/screenshots/vendor-products.png) | ![Grafana dashboard](docs/screenshots/grafana-dashboard.png) |

## What it does

- **Five roles** with their own menus and permissions: Super Admin, Vendor Owner, Delivery Owner, Driver, Customer.
- **Orders** with a full lifecycle (pending → assigned → picked up → in transit → delivered / cancelled), priorities, ratings and reviews.
- **Marketplace and bidding**: open orders go to a pool, delivery companies and drivers bid, the vendor accepts one.
- **Partnerships** between vendor and delivery companies with commission rates and contract dates.
- **Live tracking**: a driver posts GPS positions, the customer and the people involved receive them instantly over WebSocket.
- **Catalogue** with search, stock levels, discounts and categories.
- **Operations**: Prometheus metrics, a provisioned Grafana dashboard, health checks, structured error responses.

## Run it

Needs Docker with Compose. Nothing else.

```bash
docker compose up --build
```

| Service | URL |
| --- | --- |
| Web app | http://localhost:4200 |
| API | http://localhost:8080/api |
| Grafana | http://localhost:3000 (admin / `CHANGE_ME`, see `.env.example`) |
| phpMyAdmin | http://localhost:8081 |

Everything is bound to `127.0.0.1`; the database is not published at all. The first start creates the schema and seeds demo data (3 customers, 3 vendor companies, 2 delivery companies, 4 drivers, 40 products, 7 orders, 3 partnerships).

### Demo accounts

Sign in with a username (or email) and password.

| Role | Username | Password |
| --- | --- | --- |
| Super Admin | `admin` | `admin123!` |
| Customer | `customer.amina`, `customer.karim`, `customer.leila` | `SwiftDeliver@2026!` |
| Vendor Owner | `vendor.sofia`, `vendor.youssef` | `SwiftDeliver@2026!` |
| Delivery Owner | `delivery.nadia`, `delivery.omar` | `SwiftDeliver@2026!` |
| Driver | `driver.ayoub`, `driver.salma`, `driver.hamza`, `driver.meriem` | `SwiftDeliver@2026!` |

Try this: sign in as `customer.karim` in one window and as `driver.hamza` in another. Hamza opens **My Jobs** and presses **Send Location**; Karim's browser receives the position without a refresh.

These credentials only exist for the demo seed. Set `ADMIN_USERNAME` / `ADMIN_PASSWORD` and a real `JWT_SECRET` for anything else (see [Configuration](#configuration)).

## Security

Authorization is where this kind of app usually goes wrong, so it is tested rather than assumed.

- **Deny by default**: every endpoint needs a valid access token unless explicitly public; roles are enforced with `@PreAuthorize` and tenant ownership is checked in the service layer (a vendor can only touch their own company and products, a driver only their own jobs).
- **JWT access + refresh tokens**, logout revokes a token by its `jti`, login lockout after repeated failures, per-user order creation rate limit.
- **No mass assignment**: request bodies bound to entities cannot choose their own `id`, and server-managed fields (status, total, rating, assignment, tracking number…) are reset on create, so a customer cannot overwrite someone else's order or forge a total.
- **Live location is private**: only people involved in an order can read or receive its driver position.
- **WebSocket**: the handshake requires a valid access token; a socket can follow only its own `/topic/users/{id}` and orders it may read; the global feeds are Super Admin only.
- **Refuses to start insecurely**: outside the `dev`/`test` profiles the backend will not boot with a missing, short (< 32 bytes) or published JWT secret.
- **Errors**: client mistakes return 4xx with a message, never a stack trace or a 500.

Every item above has a regression test (see below) that was checked by reintroducing the bug.

## Tests

```bash
cd swiftdeliver-backend
./mvnw test        # Windows: .\mvnw.cmd test
```

22 tests that start the whole application on an in-memory database and call it over real HTTP, so they need no services:

- `AuthorizationMatrixIntegrationTest`: who may read or change what, across roles and tenants, including the exact attacks listed above.
- `RealtimeSecurityIntegrationTest`: real STOMP clients; anonymous sockets refused, private topics, global feeds, per-order access, and the driver's position reaching the customer but not a stranger.
- `JwtSecretGuardTest`: the startup check on the signing secret.

Each fix was mutation-checked: reintroducing the bug makes the matching test fail. CI (`.github/workflows/ci.yml`) runs the backend tests, builds the web app and validates the compose file on every push.

## Monitoring

Grafana ships provisioned with a Prometheus datasource and a **SwiftDeliver API** dashboard (request rate by status, latency per endpoint, 5xx rate, JVM heap and threads, CPU, DB connection pool, log events). The metrics are served on a separate internal management port (`8081` inside the compose network), so `/actuator` is not exposed on the public API port.

## Realtime

- Endpoint: `ws://localhost:8080/ws` (SockJS/STOMP), connect with `?token=<access jwt>`.
- `/topic/users/{userId}`: your own private feed (order created, status changed, driver assigned, driver location).
- `/topic/orders/{orderId}`: one order, if you may read it.
- `/topic/orders`, `/topic/orders/location`: all orders and every driver's position, Super Admin only.

## Configuration

Copy `.env.example` to `.env`; every value is optional locally.

| Variable | Purpose |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | database password (not published outside Docker) |
| `JWT_SECRET` | signs tokens; empty locally uses a development secret and logs a warning; `openssl rand -hex 32` for real use |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | the Super Admin created on first start |
| `GRAFANA_PASSWORD` | Grafana admin password |

The backend's full list of settings is in `swiftdeliver-backend/.env.example`. The web app reads the API address at runtime from `swiftdeliver-frontend/public/config.js` (`window.__SWIFT_CONFIG__.apiUrl`), so one build can point at any API.

## Deploying

`render.yaml` describes the API as a Docker web service on Render's free plan with an external MySQL (TiDB Serverless works) and `prod` profile, and `.github/workflows/pages.yml` publishes the Angular app to GitHub Pages pointing at that API. Secrets (`DATABASE_*`, `ADMIN_*`) are set in the Render dashboard; `JWT_SECRET` is generated.

## Project layout

```
swiftdeliver-backend/    Spring Boot API: controller / service / repository / entity, security + realtime config
swiftdeliver-frontend/   Angular 20 web app
monitoring/              Prometheus config, Grafana provisioning and dashboard
nginx/                   optional reverse proxy (docker compose --profile deploy)
docs/screenshots/        images used in this README
```
