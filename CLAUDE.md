# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Spring Boot 3.1.4 / Java 17 / Maven. The wrapper is script-only (`./mvnw` downloads Maven 3.9.9); CI uses plain `mvn`.

```bash
./mvnw spring-boot:run                                  # run on :8080 with the default (H2 file) profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod  # run against PostgreSQL (PG* env vars)
./mvnw test                                             # all tests
./mvnw test -Dtest=CourseServiceImplTest                # one test class
./mvnw test -Dtest=CourseServiceImplTest#methodName     # one test method
./mvnw -B clean package                                 # what CI runs (.github/workflows/main.yml)
./mvnw verify                                           # also writes the JaCoCo report to target/site/jacoco
```

## Rules
- Java 17, Spring Boot. Keep service-layer test coverage ≥80% (SCRUM-8).
- Never delete or reset data/virtual_teacher_db.mv.db.
- One branch per ticket: feature/SCRUM-<id>-<short-name>.
- Commit messages start with the ticket id: "SCRUM-41: add lecture notes entity".
- Never rewrite git history or force-push (shared repo).
- Work on a feature branch per ticket; I merge into main myself. Never force-push.

There is no linter or formatter configured.

Local URLs: app `http://localhost:8080/`, Swagger `/swagger-ui/index.html`, H2 console `/h2-console` (JDBC URL `jdbc:h2:file:./data/virtual_teacher_db`, user `sa`, password `password`).

A default admin (`admin1@example.com` / `Admin12345`) is seeded at startup by the `seedAdmin` `CommandLineRunner` in `VirtualTeacherApplication` (skipped under the `test` profile).

## The README is out of date

`README.md` describes JWT auth, `/api/auth/*` endpoints, and a PostgreSQL-only setup. None of that matches the code: auth is session-based form login, there are no `/api/auth` endpoints, and the default database is H2. Trust the code, not the README.

## Architecture

Server-rendered Thymeleaf MVC app with a small secondary REST API, under `src/main/java/com/MarianFinweFeanor/Virtual_Teacher/`. Layering is `Controller` → `Service/Interfaces` (+ `*ServiceImpl`) → `Repositories` (Spring Data JPA) → `Model` (JPA entities). Package names are capitalised (`Controller`, `Model`, `Service`, `Config`, `Repositories`) — match the existing casing.

### Two controller families

- **MVC controllers** (`Controller/*MvcController`, `Admin*Controller`) return template names from `src/main/resources/templates` and are the primary UI. Errors are handled by `GlobalMvcExceptionHandler`, which renders `errors/400|404|500` and turns `AccessDeniedException` into a redirect to `/courses` with a flash `error`.
- **REST controllers** (`Controller/RestController/*`, under `/api/**`) are handled by `GlobalRestExceptionHandler`, which is scoped to `@RestController` classes and returns `ApiErrorResponse` JSON.

Services signal failures with the custom `exceptions.EntityNotFoundException` / `EntityDuplicateException` (not the JPA ones); use those so both handlers map them correctly.

Note that `EnrollmentMvcController` is mapped to `/courses` and is effectively the main course controller (list, search, add/edit, detail, publish, enroll/unenroll, my-courses), not just enrollment. Lectures nest under `/courses/{courseId}/lectures`, assignments under `/courses/{courseId}/lectures/{lectureId}/assignments`.

### Security and roles

`Config/SecurityConfig` defines a single filter chain: form login at `/login` (username is the user's **email**), CSRF disabled, method security enabled. Authorisation is enforced in three layers that must be kept in sync when adding a route:

1. URL matchers in `SecurityConfig` (order matters — `GET /courses/**` is `permitAll` early in the chain).
2. `@PreAuthorize("hasAnyRole(...)")` on controller methods.
3. An explicit `userService.ensureApprovedTeacher(principal.getName())` call inside teacher-only handlers.

Layer 3 exists because `ROLE_TEACHER` alone is not sufficient: users who register as teachers get `teacherApproved = false` and must be approved by an admin (`/admin/users/pending-teachers`). Admins always pass `ensureApprovedTeacher` and can use all teacher functionality.

`CustomUserDetailsService` maps `UserRole` to a single `ROLE_<role>` authority and disables accounts whose `status` is not `ACTIVE`.

`Config/GlobalModelAttributes` injects `currentUserData` (email, names, role, `teacherApproved`, `canManageCourses`) into every Thymeleaf model; templates and `fragments/navbar.html` rely on it rather than on per-controller attributes.

Passwords use a `DelegatingPasswordEncoder` (bcrypt default, `{noop}` accepted for legacy rows — see `db/update_passwords_noop.sql`).

### Domain rules that span files

- **Course visibility**: `Course.status` is a plain string, `"DRAFT"` or `"PUBLISHED"` (the `CourseDraft` enum exists but the services compare strings). `CourseService.getVisibleCourses` / `searchVisibleCoursesByTitle` return published courses plus, for users who can manage courses, their own drafts. Enrolment is refused for unpublished courses.
- **Enrolment** is the `Enrollment` entity (student + course + `CompletionStatus` + `averageGrade` + rating fields). `User.getCourses()` is a convenience derived from the user's enrolments, not a separate join table.
- **Grading → progress → rating**: `AssignmentServiceImpl.gradeAssignment` calls `EnrollmentService.recalculateProgress`, which recomputes the enrolment's average grade and marks it `COMPLETED` once every lecture in the course has a graded submission. `ensureCanRateCourse` then only allows a rating when the enrolment is completed and the average is at least the course's `passingGrade` (default 50). Ratings are stored on the `Enrollment`; `Model/Rating` is not an entity.
- **Assignment uploads** are written to `app.upload.dir` (`./uploads`) as `<UUID>_<original filename>`; submitting requires being enrolled in the lecture's course.
- **Wikipedia search** (`/api/wiki/search`, `WikiServiceImpl`) calls the MediaWiki API through the `RestTemplate` bean from `RestTemplateConfig`.

### Persistence and profiles

- Default profile: H2 file database at `./data/virtual_teacher_db`, `ddl-auto=update`, SQL logging on. The schema comes from the entities; `src/main/resources/db/database.sql` is a stale MySQL-dialect reference and is not executed (`insert.sql` is a no-op `SELECT 1`).
- `prod` profile (`application-prod.properties`): PostgreSQL configured from `PORT`, `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`.
- `data/` and `uploads/` hold local runtime state. Both are gitignored now, but older files in them are still tracked — don't commit new ones.

## Tests

- `src/test/.../Service/*ServiceImplTest` are pure Mockito unit tests (`@ExtendWith(MockitoExtension.class)`) and make up most of the suite.
- `UserMvcControllerTest` / `UserRestControllerTest` are `@WebMvcTest` slices with `@ActiveProfiles("test")` and security filters disabled.
- `SmokeTest`, `HttpRequestTest`, and `VirtualTeacherApplicationTests` are full `@SpringBootTest`s with no test profile or test `application.properties`, so they boot against the real H2 file DB in `./data` (and run the admin seeder). They will fail if the app is already running locally and holding the H2 file lock.

## Workflow conventions

- Work is tracked in Jira as `SCRUM-<n>`; branches are named `feature/SCRUM-<n>-short-description` and commit messages start with the ticket key (e.g. `SCRUM-11 allow administrators to use teacher functionality`).
- PRs target `main` and use `.github/pull_request_template.md` (summary, related `SCRUM-XX` issue, type of change, testing, database changes).
