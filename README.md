# Smart Takaful & Consultation System

Muqmeen Group digital funnel — a Spring Boot monolith that replaces manual Takaful lead generation with a public consultation form and an admin dashboard.

## Stack

- Java 17, Spring Boot 3.5
- Thymeleaf server-side templates
- Tailwind CSS (CDN in dev; build pipeline added in Step 5)
- Spring Data JPA on Supabase (PostgreSQL) in prod; H2 in dev
- Deployment target: Railway

See [CLAUDE.md](./CLAUDE.md) for the full set of project rules, UI palette decisions, and the 5-step takeover roadmap.

## Requirements

- JDK 17 or newer (tested with OpenJDK 21)
- No global Maven install required — the Maven Wrapper (`./mvnw`) ships with the project.

## Running locally

```bash
# Windows (bash / PowerShell)
./mvnw spring-boot:run

# or
./mvnw.cmd spring-boot:run
```

App starts on `http://localhost:8080`. Key routes:

| Route | Purpose |
|---|---|
| `GET /` | Landing page with hero, 3 product cards, and consultation modal |
| `POST /submit-lead` | Form endpoint — saves the lead, branches to payment or success |
| `GET /payment/mock/{billCode}` | Simulated ToyyibPay gateway |
| `GET /payment/callback` | Callback endpoint (`status_id=1` marks the lead paid) |
| `GET /success` | Post-submission confirmation |
| `GET /admin/dashboard` | Leads table + total tips KPI (open in Step 1; locked down in the post-Step-3 hardening pass) |

The default profile is `dev`, which spins up an in-memory H2 database — no external credentials required.

## Environment

Environment variables are listed in [`.env.example`](./.env.example). Copy to a local `.env` (gitignored) and fill in real values once you need them:

- `SPRING_PROFILES_ACTIVE` — `dev` (default) or `prod`
- `SPRING_DATASOURCE_URL` — Supabase JDBC URL, e.g. `jdbc:postgresql://db.xxxx.supabase.co:5432/postgres`
- `SPRING_DATASOURCE_USERNAME` — Supabase DB user
- `SPRING_DATASOURCE_PASSWORD` — Supabase DB password
- `TOYYIBPAY_API_KEY` / `TOYYIBPAY_CATEGORY_CODE` — real ToyyibPay keys (after Step 4)

Spring Boot reads these env vars directly, so Railway's built-in env management works out of the box.

## Project layout

```
takaful-web-java/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/        Maven wrapper (no binary jar; downloads Maven on first use)
├── CLAUDE.md                    Project rules and takeover roadmap
├── .env.example                 Env var template
└── src/main/
    ├── java/com/muqmeen/takaful/
    │   ├── SmartTakafulApplication.java    Bootstrap only
    │   ├── domain/Lead.java                JPA entity
    │   ├── repository/LeadRepository.java  Spring Data repository
    │   ├── service/TakafulService.java     Lead processing + mock bill codes
    │   └── web/WebController.java          Public + admin routes
    └── resources/
        ├── application.yml         Base config (env-driven)
        ├── application-dev.yml     H2 in-memory for local dev
        ├── application-prod.yml    Postgres (Supabase) for Railway
        └── templates/              Thymeleaf views
            ├── index.html
            ├── payment_mock.html
            ├── success.html
            └── admin/dashboard.html
```

## Takeover roadmap

1. **Architecture Setup** *(current — branch `feat/architecture-setup`)* — Maven scaffold, package split, Thymeleaf templates, Tailwind CDN.
2. **Database Integration** — Supabase connection via env vars, schema created via `ddl-auto=update`.
3. **Refactor Leads to DB** — real end-to-end persistence; admin dashboard reads live data.
4. **Admin Product CRUD** — `Product` entity + admin views for dynamic product management.
5. **UI Overhaul** — Yellow/Black palette, Tailwind build pipeline, Alpine.js interactions.

Spring Security hardening is a dedicated pass after Step 3.
