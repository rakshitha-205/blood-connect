# BloodConnect

BloodConnect is a Java 25 / Spring Boot API with a static HTML frontend. It supports account registration and login, donor profiles and availability, emergency blood requests, compatible donor search, distance-based ranking, and request status updates.

## Project Structure

```text
backend/src/main/java/com/bloodconnect/
  user/       AppUser, UserRole, UserRepository
  auth/       register, login, JWT, email/SMS verification codes
  donor/      donor profile and availability
  request/    blood requests, status updates, donor accepts and declines
  matching/   compatible donor matching and AI ranking
  eligibility/ "Can I donate?" rules check with AI explanations
  ai/         shared OpenRouter client
  alert/      donor alerts, accept/decline, and alert emails
  review/     ratings and reviews between patients and donors
  bloodbank/  blood bank profiles and live blood stock
  admin/      admin dashboard
  config/     security, filters, error handling

frontend/
  pages/                 one HTML file per page, served at a clean URL
                         (pages/login.html -> /login, pages/index.html -> /)
  assets/css/style.css   shared styles; assets/css/pages/ holds page-only styles
  assets/js/core/        shared scripts, loaded in this order on every page:
                         config, session, http, ui, location, navigation
                         (connect.js adds WhatsApp/call buttons, ratings and
                         the review form on pages that need them)
  assets/js/pages/       one script per page
```

Vite (`frontend/vite.config.js`) maps each clean URL to its page and redirects old `.html` links (for example `/login.html` -> `/login`). Page paths used in scripts live in `assets/js/core/config.js`.

Each backend feature package holds its own controller, service, repository, entities and DTOs.

## Requirements

- JDK 25 or newer
- Apache Maven 3.9+
- PostgreSQL 14+ with two databases: `blood_connect` (the app) and `bloodconnect_test` (the tests)
- Node.js 20+ (runs the Vite frontend server)

## Configure

Copy `config/.env.example` to `config/.env` and set `DB_USERNAME` and `DB_PASSWORD` for your PostgreSQL server. Create the databases once:

```sql
CREATE DATABASE blood_connect;
CREATE DATABASE bloodconnect_test;
```

## Run Locally

The `local` profile connects to the `blood_connect` database and creates or updates tables automatically. From the project root, install the frontend tools once with `npm install`, then start the API and the frontend together:

```powershell
npm run dev
```

Open `http://localhost:5500`. Register a donor account and a requester account in separate browser sessions (or use a private window for the second account). Browser location access requires localhost or HTTPS; if declined, profiles still work but ranking cannot calculate a distance.

Run the tests with `npm run test:backend`. They recreate the tables in `bloodconnect_test` on every run, so never point them at the app database.

## Email And SMS Verification Codes

New accounts verify their email and phone with a 6-digit code. Copy `config/.env.example` to `config/.env` 
(git-ignored) and fill in the SMTP and Twilio values, then restart the backend; every profile reads that file. 
Any section left blank stays in testing mode, where the code is shown on the verify page and logged. In other 
profiles, set the same names (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM`, 
`TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`) as environment variables.

## Admin Accounts

Admins can't sign up. Register the account normally, then start the backend once with 
`--bloodconnect.admin.promote=<email>` to make it an admin (no accounts are ever created by this):

```powershell
mvn -f backend/pom.xml '-Dspring-boot.run.profiles=local' '-Dspring-boot.run.arguments=--bloodconnect.admin.promote=you@example.com' spring-boot:run
```

Log in with "Admin" selected to open `admin.html`. `GET /api/admin/overview`, `/hospitals`, `/donors`, and `/helped` 
(fulfilled requests) require the admin role.

## PostgreSQL Deployment

Set these environment variables before starting without the `local` profile:

- `DB_URL`: JDBC URL, default `jdbc:postgresql://localhost:5432/blood_connect`
- `DB_USERNAME`: PostgreSQL username, default `postgres`
- `DB_PASSWORD`: PostgreSQL password
- `JWT_SECRET`: Base64-encoded random secret of at least 256 bits
- `JWT_EXPIRATION`: token lifetime as a Spring duration, default `PT1H`
- `CORS_ALLOWED_ORIGINS`: comma-separated frontend origins, defaults to localhost ports 3000 and 5173

Generate a signing secret with PowerShell:

```powershell
$bytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Create the `blood_connect` database and apply [`src/main/resources/db/postgresql-schema.sql`](src/main/resources/db/postgresql-schema.sql) before startup. The default profile uses `spring.jpa.hibernate.ddl-auto=validate`; it will not alter a deployment schema. Keep `JWT_SECRET` private and replace the local-only signing key in deployed environments.

## Authentication API

`POST /api/auth/register`

```json
{"email":"donor@example.com","password":"a-long-password","role":"DONOR"}
```

Allowed roles are `DONOR` and `REQUESTER`. Registration returns `201 Created` with a bearer token and a password-free user object. Passwords are BCrypt hashed; passwords longer than 72 UTF-8 bytes are rejected. Send the token to protected endpoints using `Authorization: Bearer <accessToken>`.

`POST /api/auth/login`

```json
{"email":"donor@example.com","password":"a-long-password"}
```

Returns the same token response shape. Send the token to protected endpoints using `Authorization: Bearer <accessToken>`.

`GET /api/auth/me` returns the authenticated user's id, email, role, and creation time. Authentication is stateless; logout on the client by discarding the token.

## Donor And Matching API

All routes below require a bearer token and role authorization.

- `PUT /api/donors/me`: create or update the signed-in donor's name, phone, blood group, location, optional latitude/longitude, and availability.
- `GET /api/donors/me`: retrieve the signed-in donor profile.
- `PATCH /api/donors/me/availability`: set `{"available":false}` or `true`.
- `GET /api/donors/nearby?bloodGroup=A%2B&latitude=12.97&longitude=77.59`: list available compatible donors, sorted by distance when coordinates exist.
- `POST /api/requests`: create an emergency request as a `REQUESTER`; returns its UUID and `OPEN` status.
- `GET /api/requests/{id}`: retrieve a request owned by the signed-in requester.
- `GET /api/requests/{id}/matches`: list compatible, available donors ranked by distance and priority score. Donor phone numbers are not included.
- `PATCH /api/requests/{id}/status`: set `OPEN`, `FULFILLED`, or `CANCELLED`.

- `POST /api/donors/me/alerts/{id}/accept`: a donor accepts an open, compatible request; the requester is emailed the donor's phone and WhatsApp link.
- `GET /api/donors/me/accepted`: requests the signed-in donor accepted, with status and whether they already rated the requester.
- `GET /api/requests/{id}/acceptances`: donors who accepted, including their phone number, rating, and whether the requester rated them.

Compatibility is based on standard red-cell ABO/Rh donor-to-recipient rules. Distances use the Haversine formula on browser-provided coordinates. This matching list is informational only and is not medical advice; a qualified clinician or blood bank must confirm transfusion suitability.

## Reviews API

- `POST /api/reviews` with `{"requestId":"…","revieweeId":"…","rating":1-5,"comment":"…"}`: allowed once the request is `FULFILLED`. The requester rates a donor who accepted (`revieweeId` required); a donor who accepted rates the requester (`revieweeId` omitted). One review per person per request.
- `GET /api/reviews/me` and `GET /api/reviews/users/{userId}`: average rating, count, and the 20 latest reviews.

Donor matches (`/matches`, `/matches/ai`, `/api/donors/nearby`) include `rating` and `ratingCount`.

## Blood Bank API

Register with role `BLOOD_BANK`.

- `PUT /api/blood-banks/me`: name, phone, location, optional coordinates and opening hours.
- `GET /api/blood-banks/me`: the signed-in bank's profile and stock.
- `PUT /api/blood-banks/me/stock` with `{"stock":{"A+":12,"O-":2}}`: replaces the stock; groups left out count as 0.
- `GET /api/blood-banks?bloodGroup=O-&latitude=…&longitude=…`: public, no login needed. Lists banks with that group in stock, nearest first.

## Eligibility API

- `POST /api/eligibility/check`: public, limited to `bloodconnect.eligibility.max-checks-per-hour` (default 10) per IP address. Send age, weight, sex, optional dates (last donation, tattoo or piercing, surgery, fever recovery, last antibiotic dose, malaria recovery, dental work), `alcoholLast24Hours`, `pregnantOrBreastfeeding`, an optional free-text `note` (500 characters at most) and `language` (`en`, `hi`, `te`, `kn` or `ta`).
- Fixed NBTC rules in `EligibilityRules` make the decision. With an OpenRouter key, the AI only turns the note into known condition codes and writes the explanation in the chosen language. Without a key, the note is skipped and the explanation is in English.
- For a signed-in donor, the result is saved as `deferredUntil` on the donor profile. Until that date the donor is left out of matching and nearby search, and can't accept requests or switch availability on.
- This is guidance only, not medical advice. The blood bank's own screening is final.
