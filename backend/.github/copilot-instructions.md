# BloodConnect Backend

- Java 25, Spring Boot 3, Maven; base package `com.bloodconnect`.
- Keep controllers thin, validate input in DTOs, and put business logic in services.
- Never serialize password hashes or commit database/JWT secrets.
- Keep PostgreSQL schema changes disabled until the team agrees on migrations; currently `ddl-auto=validate`.
- Confirm the shared schema with Nikhil and frontend payloads/origins with Ranjitha before expanding donor/request APIs.
- Treat matching results as candidate suggestions only; medical eligibility must be confirmed by qualified clinicians/blood-bank procedures.

## Setup checklist

- [x] Requirements clarified: Spring Boot REST API with PostgreSQL and JWT authentication.
- [x] Maven project and source structure scaffolded.
- [x] Registration, login, `/api/auth/me`, BCrypt, and JWT bearer authentication implemented.
- [x] README and environment configuration documented.
- [x] Run Maven tests (`npm run test:backend`).
- [ ] Confirm database schema and matching contract with Nikhil.
- [ ] Confirm frontend origins and API payloads with Ranjitha.
