# HackTheSnackDB

REST API and database for [HackTheSnack](https://github.com/danilomonge/HackTheSnack), the web app that reverse-engineers the recipe of a supermarket product from its ingredient list.

The API stores the **products**, their **ingredients**, the **recipes** (which ingredients a product contains) and every saved **evaluation** (the estimated percentage of each ingredient). The HackTheSnack frontend uses it for *Search / autofill*, *Save*, *My Recipes* and *Statistics*. It also serves the **nutrition facts** (USDA) and **translations** (DeepL) of the nutrition dialog, so the API keys never reach the browser.

| | |
|---|---|
| Live API | `https://fakethesnackdb-api.onrender.com/user-management` |
| API docs (Swagger UI) | `https://fakethesnackdb-api.onrender.com/user-management/swagger-ui.html` |
| Health check | `https://fakethesnackdb-api.onrender.com/user-management/actuator/health` |
| Database | PostgreSQL 17 on **Supabase** (free plan), region `us-west-2` (Oregon), next to the API |
| Hosting | **Render** free web service (Docker), region Oregon |

> Render's free plan stops the service after 15 minutes without traffic; starting again takes about a minute. From 06:00 to 23:00 (Europe/Berlin) the API keeps itself awake by calling its own health URL every 10 minutes (`KeepAwakePinger`, env `KEEP_AWAKE_URL`), and an hourly [`keepalive`](.github/workflows/keepalive.yml) run wakes it in the morning. At night it sleeps; the first request then takes up to a minute and the frontend shows a progress bar while it waits.

---

## Architecture

```
Browser (GitHub Pages, Flet/Pyodide)
        │  HTTPS + CORS
        ▼
Render: Spring Boot API  (this repo, Docker) ──▶ USDA FoodData Central, DeepL
        │  JDBC over TLS, Supabase session pooler (IPv4)
        ▼
Supabase: PostgreSQL  (schema in src/main/resources/db/schema.sql)
```

* **Spring Boot 2.7 / Java 17**, Spring Data JPA (Hibernate 5.6), HikariCP, Log4j2, springdoc-openapi.
* The database schema is owned by [`schema.sql`](ms-java-user-management-pecus/src/main/resources/db/schema.sql). Hibernate runs with `ddl-auto=validate`: it never changes tables, it only checks that the entities match them on startup.
* `DataInitializer` makes sure the `DEFAULT` brand/category/subcategory rows (id 1) exist on every start.
* Rows are never physically deleted. `DN_ACTIVO = false` marks them as removed (soft delete).

### Repository layout

```
HackTheSnackDB/
├── Dockerfile                    # multi-stage build used by Render
├── render.yaml                   # Render Blueprint (service + env vars)
├── .github/workflows/
│   ├── ci.yml                    # build + integration tests on every push / PR
│   └── keepalive.yml             # hourly wake-up; Supabase never pauses
└── ms-java-user-management-pecus/
    ├── pom.xml
    └── src/
        ├── main/java/de/pecus/api/
        │   ├── controllers/      # REST endpoints
        │   ├── services/         # business rules (validation, upper-casing, duplicates)
        │   ├── repositories/     # Spring Data JPA queries
        │   ├── entities/         # JPA entities (tables)
        │   ├── vo/               # request / response objects
        │   ├── config/           # CORS, JPA, start-up data
        │   └── util/, error/, …  # helpers and error messages
        ├── main/resources/
        │   ├── application.yml   # configuration (all env-driven)
        │   └── db/schema.sql     # database schema + default rows
        └── test/java/…           # API integration tests (Testcontainers)
```

---

## API

Base URL: `…/user-management`. Every response has the shape

```json
{ "success": true, "errors": [], "data": …, "totalRows": 1 }
```

Errors return `success: false` and a list of `{ "code", "key", "message" }`, with status 400 (validation, duplicate, unknown id), 404 (not found) or 500.

### Products and ingredients

Names are stored in upper case and must be unique among active rows (max. 100 characters). `/ingredient` is an alias of `/substance`.

| Method | Path | Description |
|---|---|---|
| `POST` | `/product` | Create `{"name", "descripcion", "idBrand", "idCategory", "idSubCategory"}` (ids default to 1) → `201 {data: id}` |
| `GET` | `/product/detail?name=` or `?id=` | One product |
| `GET` | `/product/list?page=1&size=100&name=` | Products whose name contains `name` (empty = all) |
| `PUT` | `/product/{id}` | Update name / description (and optionally brand, category, subcategory) |
| `DELETE` | `/product/{id}` | Soft delete |
| `POST` | `/ingredient` | Create `{"name", "description"}` → `201 {data: id}` |
| `GET` | `/ingredient/detail?name=` or `?id=` | One ingredient |
| `GET` | `/ingredient/list?page=1&size=500&name=` | Ingredients |
| `PUT` / `DELETE` | `/ingredient/{id}` | Update / soft delete |

`/brand`, `/category` and `/subCategory` offer the same create / detail / list / update / delete operations.

### Recipes (product ↔ ingredient)

| Method | Path | Description |
|---|---|---|
| `POST` | `/recipe` | Associate `{"idProduct", "idIngredient"}` → `201 {data: idRecipe}`. `400 DUPLICATED_ERROR` if already associated; a removed association is re-activated with its old id |
| `GET` | `/recipe/list?id={idProduct}` | Active ingredients of a product in label order (as in the latest saved evaluation; ingredients added afterwards follow in insertion order): `idRecipe, idProduct, idIngredient, nameIngredient` |
| `GET` | `/recipe/detailRecipeByProduct?idProduct=&idIngredient=` | One association |
| `DELETE` | `/recipe/deleteByProduct?idProduct=&idIngredient=` | Remove an ingredient from a product (soft delete) |

### Evaluations (saved recipe estimations)

| Method | Path | Description |
|---|---|---|
| `POST` | `/evaluation` | Save one estimation run (see below) → `201 {data: evaluationId}` |
| `GET` | `/evaluation/list?idProduct=` | Evaluations of a product |
| `GET` | `/evaluation/detail?id=` | One evaluation with its `resultList` |
| `GET` | `/evaluation/listResult?idProduct=&idIngredient=` | Result lines filtered by product, ingredient or both, with `ingredientName`, `productName`, `evaluationDate` |

```json
POST /evaluation
{
  "idProduct": 1,
  "resultList": [
    { "recipeId": 1, "ingredientId": 1, "ingredientMeanPercentage": 85.71, "ingredientStdPercentage": 1.2 },
    { "recipeId": 2, "ingredientId": 2, "ingredientMeanPercentage": 14.29, "ingredientStdPercentage": 1.2 }
  ]
}
```

The evaluation and all its lines are written in one transaction.

### Nutrition dialog (USDA / DeepL)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/nutrition?ingredient=sugar` | `{"data": {name, kcal, carbs, fat, protein, sodium (mg), per: "100g"}}` or `{"data": null}`. Exact (case-insensitive) matches among USDA branded foods; every value is the median over all exact matches |
| `POST` | `/api/translate` | `{"text": "Zucker", "source_lang": "DE", "target_lang": "EN"}` (languages optional) → `{"text": "Sugar"}`; unchanged text if no DeepL key |
| `GET` | `/health` | `{"ok": true}` (the web app pings it on start-up) |

Errors are `{"detail": "..."}` with status 400 (invalid input), 503 (`USDA_API_KEY` missing) or 502 (USDA / DeepL failed). Neither URLs nor keys appear in them.

### Operations

| Path | Description |
|---|---|
| `/actuator/health` | `{"status":"UP"}` when the API and the database are reachable (used by Render) |
| `/swagger-ui.html`, `/v3/api-docs` | Interactive documentation / OpenAPI spec |

---

## Configuration

Everything is configured through environment variables (see [`application.yml`](ms-java-user-management-pecus/src/main/resources/application.yml)).

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | built from `PGHOST`/`PGPORT`/`PGDATABASE` (localhost:5432/postgres) | JDBC URL |
| `DB_USERNAME` | `PGUSER` or `postgres` | Database user |
| `DB_PASSWORD` | `PGPASSWORD` or `postgres` | Database password |
| `DB_POOL_SIZE` | `3` | Max. HikariCP connections |
| `CORS_ALLOWED_ORIGINS` | `https://danilomonge.github.io,http://localhost:*,http://127.0.0.1:*` | Origins allowed to call the API from a browser |
| `PORT` | `8087` | HTTP port (Render sets it automatically) |
| `JPA_DDL_AUTO` | `validate` | Leave as is in production |
| `USDA_API_KEY` | empty | USDA FoodData Central key for `/api/nutrition` (free at https://fdc.nal.usda.gov/api-key-signup) |
| `DEEPL_API_KEY` | empty | DeepL key for `/api/translate` (Free keys end in `:fx`; the right host is chosen automatically) |
| `KEEP_AWAKE_URL` | empty (off) | Public health URL the API pings every 10 min to stay awake on Render's free plan |
| `KEEP_AWAKE_ZONE`, `KEEP_AWAKE_FROM_HOUR`, `KEEP_AWAKE_UNTIL_HOUR` | `Europe/Berlin`, `6`, `23` | Daily window for those pings (about 17 h ≈ 520 of the 750 free Render hours a month) |

---

## Database on Supabase

1. Create a project in the [Supabase dashboard](https://supabase.com/dashboard) (free plan).
2. Open **SQL Editor**, paste the contents of [`schema.sql`](ms-java-user-management-pecus/src/main/resources/db/schema.sql) and run it. Re-running it is safe.
3. Open **Connect → Session pooler** and copy host, port, user and password. Use the *session* pooler: it supports IPv4 (Render has no IPv6) and prepared statements.
4. Set these variables on the Render service:

   ```
   DB_URL=jdbc:postgresql://aws-<n>-<region>.pooler.supabase.com:5432/postgres?sslmode=require
   DB_USERNAME=postgres.<project-ref>
   DB_PASSWORD=<database password>
   ```

   Copy the host exactly as the dashboard shows it (for example `aws-1-us-west-2.pooler.supabase.com`); the `aws-0` / `aws-1` prefix differs between projects.

The tables have row-level security enabled without policies, and `schema.sql` revokes every table privilege from the `anon` and `authenticated` roles. Supabase's public Data API therefore answers `permission denied` for these tables; only this API, which connects as the table owner, can read or write them.

Free Supabase projects pause after 7 days without activity. The [`keepalive`](.github/workflows/keepalive.yml) workflow prevents that: every run queries the database through the API. The API's daytime self-pings (~17 h/day ≈ 520 Render instance hours a month) stay inside Render's 750 free hours per workspace, leaving room for the nutrition proxy.

---

## Run locally

Requirements: Java 17, Maven 3.9, a PostgreSQL 14+ server (or Docker).

```bash
# 1. Database
docker run -d --name hackthesnack-db -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:15
psql postgresql://postgres:postgres@localhost:5432/postgres -f ms-java-user-management-pecus/src/main/resources/db/schema.sql

# 2. API on http://localhost:8087/user-management
cd ms-java-user-management-pecus
mvn spring-boot:run
```

Point the frontend at it with `USER_MANAGEMENT_BASE_URL=http://localhost:8087/user-management`.

With Docker only:

```bash
docker build -t hackthesnackdb-api .
docker run -p 8087:8087 -e DB_URL=jdbc:postgresql://host.docker.internal:5432/postgres hackthesnackdb-api
```

## Tests

```bash
cd ms-java-user-management-pecus
mvn verify
```

The integration tests start a disposable PostgreSQL container (Testcontainers, so Docker must be running), apply `schema.sql`, and exercise every endpoint the frontend uses. CI runs them on every push and pull request.

## Deployment

Every push to `main` runs CI; when the tests pass, its `deploy` job calls the service's Render **Deploy Hook** (repository secret `RENDER_DEPLOY_HOOK_URL`, copied from Render → *fakethesnackdb-api* → *Settings* → *Deploy Hook*). The secret is configured, so merging into `main` deploys the API; *Actions → CI → Run workflow* on `main` redeploys without a code change. Without the secret the job only prints a warning and the deploy is started from the Render dashboard (*Manual Deploy*). Render builds the Docker image and waits for `/user-management/actuator/health` before switching traffic to the new version.

## Authors

Emilio Ribelles, Danilo Monge, Jana Danner and Katja Greiffenhagen. University of Tübingen team project.
