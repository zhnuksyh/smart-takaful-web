# Smart Takaful & Consultation System

Muqmeen Group digital funnel — Java monolith replacing manual lead generation for Takaful agents.

## Tech Stack (LOCKED — university compliance)

- **Backend:** Java 17 + Spring Boot 3.x
- **Frontend:** Thymeleaf templates + Tailwind CSS (CDN for dev; npm/PostCSS build in Step 5)
- **Client interactions:** Vanilla JS or Alpine.js. **No React/Vue/Next.js.**
- **Database:** Supabase (PostgreSQL) via Spring Data JPA / JDBC
- **Deployment target:** Railway. All secrets via environment variables.

This stack is non-negotiable. Do not introduce SPA frameworks, microservices, or alternative ORMs.

## Project Structure

```
takaful-web-java/
├── pom.xml
├── CLAUDE.md
├── .gitignore
├── .env.example
├── README.md
└── src/main/
    ├── java/com/muqmeen/takaful/
    │   ├── SmartTakafulApplication.java  (bootstrap only)
    │   ├── domain/                       (JPA entities)
    │   ├── repository/                   (Spring Data interfaces)
    │   ├── service/                      (business logic)
    │   └── web/                          (Spring MVC controllers)
    └── resources/
        ├── application.yml               (env-driven config)
        ├── static/
        └── templates/                    (Thymeleaf views)
```

## Git Workflow (STRICT)

1. **Always cut a branch before editing.** Format: `feat/feature-name` or `fix/fix-name`. Never edit on `main`.
2. **Atomic commits** — one logical change per commit. Don't bundle unrelated work.
3. **No `Co-authored-by` tags.** Keep commit authorship as the user's git config. This overrides Claude Code's default commit template.

## UI / UX

- **Palette:** Yellow + Black. Premium, professional, trustworthy vibe.
- **Quality bar:** Tailwind UI / Flowbite level. Hover states, transitions, accessible markup.
- **Typography:** Inter (Google Fonts).
- **Icons:** Font Awesome 6.
- The existing prototype uses a Red/Crimson palette. This is migrated to Yellow/Black as part of Step 5, not earlier.

## Takeover Roadmap

1. **Architecture Setup** — Maven scaffold, package split, Thymeleaf templates, Tailwind CDN.
2. **Database Integration** — Supabase JDBC/JPA connection, `Lead` + `Product` entities.
3. **Refactor Leads to DB** — Wire form → service → Supabase end-to-end.
4. **Admin Product CRUD** — Thymeleaf admin views for dynamic Takaful product management.
5. **UI Overhaul** — Yellow/Black palette, Tailwind build pipeline, Alpine.js interactions. *(deferred)*
6. **Gemini chatbot** — Floating Takaful-grounded chatbot on the landing page (out-of-roadmap addition).

Spring Security hardening is a dedicated pass after Step 3, not inside the roadmap.

## Chatbot (Gemini, free tier)

Floating chat bubble on the public landing page. Backend at `POST /api/chat`. Lives entirely under `service/chat/` and `web/ChatController.java`.

- **Model**: `gemini-2.0-flash` via Google AI Studio's free tier (1500 requests/day, 15 RPM). Configured via `GEMINI_API_KEY` env var; the model id and base URL are overridable via `GEMINI_MODEL` / `GEMINI_BASE_URL` for future migration.
- **Lightweight RAG**: every request injects a system prompt assembled from hardcoded Muqmeen / Takaful / Hibah / consultation facts (in [`ChatKnowledgeBase`](src/main/java/com/muqmeen/takaful/service/chat/ChatKnowledgeBase.java)) plus the live active product list from `ProductService.listActiveForLanding()`. The prompt is cached for 60 seconds so chat turns don't hit Supabase per request.
- **Topic gate (defense in depth)**:
  1. The Takaful-only gating rule appears at both the top and bottom of the system prompt.
  2. A regex deny-list in `ChatService.JAILBREAK_PATTERN` rejects obvious jailbreak markers (`ignore previous`, `reveal system prompt`, DAN, etc.) before the call to Gemini.
  3. `ChatController` enforces a per-IP rate limit of 6 requests/minute to protect the daily quota.
  4. The system prompt instructs plain-text replies (no markdown), so the browser JS can render via `textContent` — no HTML sanitizer needed.
- **No history persistence**: conversation state is kept client-side in a JS array; the last 6 turns are echoed back on each request.
- **Updating static facts**: edit the `STATIC_FACTS` constant in `ChatKnowledgeBase.java` and restart. Product facts come from the database automatically.

## Running Locally

```
./mvnw spring-boot:run
```

App starts on `http://localhost:8080`. See `README.md` for environment variables and Supabase setup (filled in during Step 2).

## Environment Variables (Railway-friendly)

All read from env, never committed. A local `.env` is auto-loaded at startup via `spring-dotenv`:

- `SPRING_PROFILES_ACTIVE` — `dev` (H2 in-memory, offline) or `prod` (Supabase).
- `SPRING_DATASOURCE_URL` — Supabase transaction pooler JDBC URL (see below).
- `SPRING_DATASOURCE_USERNAME` — `postgres.<project-ref>`, **not** plain `postgres`.
- `SPRING_DATASOURCE_PASSWORD` — the Supabase DB password (reset in Project Settings → Database → Database password if lost). The publishable/anon API key is **not** accepted here.

`.env.example` lists the placeholders. Copy to `.env` locally (gitignored).

## Supabase connection (Transaction pooler)

The `prod` profile connects through Supabase's **transaction pooler** on port **6543** rather than the direct `db.<ref>.supabase.co:5432` host. This avoids the IPv4 add-on requirement and plays well with Railway's networking.

- JDBC URL: `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres`
- Username: `postgres.<project-ref>` (project ref for this codebase is `bpvphhicisctyhqiiayk`).
- **Prepared statements:** the transaction pooler does **not** support server-side prepared statements. `application-prod.yml` sets `prepareThreshold=0` and `preparedStatementCacheQueries=0` on the Hikari datasource; do not remove these — Hibernate will start throwing `PSQLException: prepared statement "S_1" does not exist` errors in minutes under load.
- **Schema management:** `ddl-auto=update` in prod so Hibernate auto-migrates the `leads` and `products` tables. Switch to Flyway/Liquibase if/when the schema starts changing frequently — not needed for a university submission.
