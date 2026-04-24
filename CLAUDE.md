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

1. **Architecture Setup** — Maven scaffold, package split, Thymeleaf templates, Tailwind CDN. *(current step, branch `feat/architecture-setup`)*
2. **Database Integration** — Supabase JDBC/JPA connection, `Lead` + `Product` entities.
3. **Refactor Leads to DB** — Wire form → service → Supabase end-to-end.
4. **Admin Product CRUD** — Thymeleaf admin views for dynamic Takaful product management.
5. **UI Overhaul** — Yellow/Black palette, Tailwind build pipeline, Alpine.js interactions.

Spring Security hardening is a dedicated pass after Step 3, not inside the roadmap.

## Running Locally

```
./mvnw spring-boot:run
```

App starts on `http://localhost:8080`. See `README.md` for environment variables and Supabase setup (filled in during Step 2).

## Environment Variables (Railway-friendly)

All read from env, never committed:
- `SPRING_DATASOURCE_URL` — Supabase JDBC URL (e.g. `jdbc:postgresql://db.xxxx.supabase.co:5432/postgres`)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_PROFILES_ACTIVE` — `dev` locally, `prod` on Railway

`.env.example` lists the placeholders. Copy to `.env` locally (gitignored).
