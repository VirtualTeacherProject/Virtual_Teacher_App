# Backlog audit

Audit of every ticket in `docs/backlog.md` against git history and the code on `main` at `85c4c7c` (2026-11-18). Previous audit: `ff442fd` (2026-11-16).

**How this was checked.** `git log` on `main` (all `SCRUM-<n>` commits, merges and PRs) and `git branch --no-merged main` (only the stale `origin/exception-handling` remains unmerged; nothing in it changes a status). Each claimed feature was then checked in the controllers, services, entities, migrations, `SecurityConfig` and templates. NOT STARTED tickets were confirmed by searching `src/main` for their entities and keywords (attendance, prerequisite, exam/quiz, mail/verification, PDF/certificate, group, tutoring, assistant). Tests were not re-run for this update; the coverage figure for SCRUM-8 is from the 2026-11-16 run (`*ServiceImplTest` with JaCoCo, then 125 tests, 0 failures). Ticket wording comes only from `backlog.md`; Jira was not consulted.

Paths are relative to `src/main/java/com/MarianFinweFeanor/Virtual_Teacher/` unless they start with `templates/` (`src/main/resources/templates/`), `db/migration/` (`src/main/resources/db/migration/`) or another top-level directory. Commit hashes are on `main`; a hash in brackets is the merge or PR that brought the work in.

Sizes: **S** = up to a day, **M** = 2–4 days, **L** = a week or more, for one developer including tests.

## Summary

| Status | Count | Tickets |
|---|---|---|
| DONE | 23 | 5, 6, 7, 8, 9, 10, 11, 12, 15, 19, 20, 23, 24, 25, 27, 31, 34, 35, 36, 41 (duplicate of 20), 42, 49, 50 |
| PARTIAL | 3 | 13, 22, 40 |
| NOT STARTED | 19 | 16, 17, 18, 26, 28, 29, 30, 32, 33, 37, 39, 43, 44, 45, 46, 47, 48, plus 14 and 38 (requirements unclear) |

### Status changes since the 2026-11-16 audit

| Ticket | Was | Now | Evidence |
|---|---|---|---|
| SCRUM-6 | PARTIAL | DONE | `0a7bd61` [`eca7feb`] |
| SCRUM-10 | NOT STARTED | DONE | `c300588` [PR #15 `eb3664c`] |
| SCRUM-12 | PARTIAL | DONE | `d8bf3d1`, `c1759c8` [`eca7feb`] |
| SCRUM-15 | NOT STARTED | DONE | `d8bf3d1`, `e695d8b` [`eca7feb`] |
| SCRUM-19 | PARTIAL | DONE | `cfe08f6` [`eca7feb`] |
| SCRUM-20 | NOT STARTED | DONE | `a0d77bc` [`1490a55`] |
| SCRUM-24 | NOT STARTED | DONE | `a0d77bc` [`1490a55`] |
| SCRUM-31 | PARTIAL | DONE | `f540e6f` [`eca7feb`] |
| SCRUM-34 | NOT STARTED | DONE | `c69e3f8` [`4a50276`] |
| SCRUM-35 | NOT STARTED | DONE | `750b2e2` [`508cff2`] |
| SCRUM-36 | NOT STARTED | DONE | `caae73e` [`a3da031`] |
| SCRUM-41 | NOT STARTED | DONE (duplicate) | Closed as a duplicate of SCRUM-20 |
| SCRUM-49 | not audited | DONE | `4ba7128` [PR #13 `5937900`] |
| SCRUM-50 | not audited | DONE | `eac9a74` [`7e46e27`] |

SCRUM-13, SCRUM-22 and SCRUM-40 stay PARTIAL. All other tickets keep their status.

## Done

| Ticket | Evidence | Remaining gaps (not blocking DONE) |
|---|---|---|
| SCRUM-5 Exception handling | `9d25b93`, `bf00898`, `1c155bb`. `exceptions/GlobalMvcExceptionHandler.java`, `exceptions/GlobalRestExceptionHandler.java` (`ApiErrorResponse`), custom `EntityNotFoundException`, `EntityDuplicateException`, `EntityInUseException`, `FileStorageException`. | Bare `RuntimeException` is still thrown in `Controller/AdminUserController.java` (6×, "User not found"), `Service/EnrollmentServiceImpl.java` (2×, "Enrollment not found") and `Service/UserServiceImpl.java` (1×), which surface as a 500 instead of a 404. |
| SCRUM-6 Approved teachers get course admin and student search | `0a7bd61` [`eca7feb`]. `Controller/StudentSearchMvcController.java` (`/students`), `templates/students.html`. | — |
| SCRUM-7 Swagger documentation | `c1b3cad`, `9a837b4`. `springdoc-openapi-starter-webmvc-ui` in `pom.xml`, `Config/OpenApiConfig.java`. | Only `WikiRestController` has `@Operation`/`@Tag`. `SwaggerRestController` is an empty class. |
| SCRUM-8 Service coverage ≥ 80% | `ff69e34` [PR #11 `909ef03`]. Measured 2026-11-16: `Service` package 96.0% instruction, 87.0% branch. | Not re-measured after SCRUM-20/34/35/36 added services. The JaCoCo plugin in `pom.xml` has only `prepare-agent` and `report`, no `check` goal, so CI does not enforce the threshold. |
| SCRUM-9 CI on main | `9794fa0`, `b01c2a3` [PR #12 `d2519ae`]. `.github/workflows/main.yml` runs `mvn -B clean package` on push and PR to `main`. | Run results not checked. |
| SCRUM-10 Videos only after enrolment | `c300588` [PR #15 `eb3664c`]. `Service/CourseAccessServiceImpl.java` (`canViewCourse`, `canAccessCourseContent`, `ensureCanViewCourse`), used by `LectureMvcController`; `canAccessContent` gate in `templates/lecture-detail.html`. Also restricted REST writes in `SecurityConfig` and added an owner/manager check to `AssignmentMvcController.download`. | Video URLs are external (YouTube), so hiding them is not real protection. |
| SCRUM-11 Admins have teacher functionality | `64f6e3e` [PR #14 `2c3b95c`]. `UserServiceImpl.ensureApprovedTeacher` returns early for admins; `canManageCourses` in `Config/GlobalModelAttributes.java`. | — |
| SCRUM-12 Admin manage courses/users, approve teachers/admins | `d8bf3d1`, `c1759c8` [`eca7feb`]. `Controller/AdminUserController.java`, `Controller/AdminCourseController.java`; deletes go through `Service/ContentCleanupServiceImpl.java`; `UserServiceImpl.countByRole` now queries the repository. | — |
| SCRUM-15 Deletion policy | `d8bf3d1`, `e695d8b` [`eca7feb`]. `Service/ContentCleanupServiceImpl.java` (`deleteLectureContent`, `deleteCourseContent`, `deleteUserContent`); `EntityInUseException` when a user still teaches courses. | — |
| SCRUM-19 User search | `cfe08f6` [`eca7feb`]. `UserServiceImpl.getAllUsers(firstName, lastName, email)` builds a `UserSpecification`; search on `templates/admin/users.html` and `GET /api/users`. | — |
| SCRUM-20 Lecture notes | `a0d77bc` [`1490a55`]. `Model/LectureNote.java` (max 5000), `Service/LectureNoteServiceImpl.java`, notes panel in `templates/lecture-detail.html`. | — |
| SCRUM-23 Students comment on lectures | `10e2b28` [PR #10 `76dfd04`]. `Model/LectureComment.java`, `Service/LectureCommentServiceImpl.java`, form in `templates/lecture-detail.html`. | Comments are readable by anyone who can view the lecture page. No edit or delete. |
| SCRUM-24 Add/edit lecture notes | Same as SCRUM-20: saving overwrites, an empty save deletes. | — |
| SCRUM-25 Link to Swagger | `4ce7935` [PR #8 `eaa762f`]. `README.md` lines 7–8. | No template links to Swagger. |
| SCRUM-27 Completion / rating eligibility | `d99c93e`, `14b382e` [PR #9 `aa93b41`] (committed as SCRUM-21). `EnrollmentServiceImpl.recalculateProgress` / `ensureCanRateCourse`, `Course.passingGrade`. | The average counts every graded submission, resubmissions included. |
| SCRUM-31 Rating with 75-character review | `f540e6f` [`eca7feb`]. `Controller/CourseRatingMvcController.java`; reviews and average on `templates/course-detail.html`. | — |
| SCRUM-34 Resources after enrolment | `c69e3f8` [`4a50276`]. `Model/CourseResource.java`, `Service/CourseResourceServiceImpl.java`, `Controller/CourseResourceMvcController.java`, `db/migration/V2__course_resources.sql`. | Links only; no file upload. |
| SCRUM-35 Students request resources (≥ 100 characters) | `750b2e2` [`508cff2`]. `Model/ResourceRequest.java` (`MIN_MESSAGE_LENGTH = 100`), `Controller/ResourceRequestMvcController.java`, `db/migration/V3__resource_requests.sql`. | — |
| SCRUM-36 Teachers manage resource requests | `caae73e` [`a3da031`]. `ResourceRequestMvcController` `/status`, `/assign`, `/response`; `Service/ResourceRequestServiceImpl.java`; `db/migration/V4__resource_request_assignment.sql`. | — |
| SCRUM-41 Notes per lecture | Duplicate of SCRUM-20; closed. | — |
| SCRUM-42 Lecture comments | `10e2b28`, `0ecc6c3` [PR #10 `76dfd04`]. Same code as SCRUM-23, plus `LectureCommentServiceImplTest`. | Same as SCRUM-23. |
| SCRUM-49 Pull request template | `4ba7128` [PR #13 `5937900`]. `.github/pull_request_template.md`. | Was not listed in `backlog.md` before this update. |
| SCRUM-50 Flyway, `ddl-auto=validate` | `eac9a74`, `589abb5` [`7e46e27`]. `flyway-core` in `pom.xml`, `db/migration/V1__baseline_schema.sql` to `V4`. | `courses.teacher_id` is still nullable in databases created before Flyway. |

## Partial

| Ticket | Evidence | What is missing | Size |
|---|---|---|---|
| SCRUM-13 Teachers create courses and upload video lectures | Course creation and lecture add/edit in `Controller/EnrollmentMvcController.java`, `Controller/LectureMvcController.java`, `templates/add-course.html`, `templates/add-lecture.html`. | Video is a URL string (`Model/Lecture.videoUrl`); no video upload. No course ownership: any approved teacher can edit, publish and grade any course. | S to reword, M–L for upload or ownership |
| SCRUM-22 Drafts hidden from students | `ff69e34`, `c300588`. `CourseServiceImpl.getVisibleCourses` / `searchVisibleCoursesByTitle`, `CourseAccessServiceImpl.ensureCanViewCourse` on the lecture routes, enrolment refused for drafts. | `GET /api/courses` (`CourseRestController.getAllCourses` → `courseService.getAllCourses()`) returns every course, drafts included, to any logged-in user. | S |
| SCRUM-40 Git history communicates order, features, contributors | 24 of 139 non-merge commits start with `SCRUM-<n>`; every commit since `c300588` does. | Older messages such as "edit code", "refactor", "bug fixes 15/12/2025". The two contributors appear under four author names (`Marian`, `marianmgm`, `umut yildirim`, `UMUT YILDIRIM`). History must not be rewritten, so this only improves going forward; a `.mailmap` would unify the names without rewriting. | Ongoing |

## Not started

None of these have a commit, entity, repository, migration or dependency on `main`.

| Ticket | Evidence | Size | External setup |
|---|---|---|---|
| SCRUM-14 Teacher access limited to administrative part of sessions | Nothing maps to "sessions". Requirements unclear. | Unknown | Acceptance criteria first |
| SCRUM-16 Entrance exam with minimum grade | No exam, quiz or question entity. | L (with 17, 18, 33) | None |
| SCRUM-17 Exam start/end times | — | part of 16 | None |
| SCRUM-18 MCQ, MSQ, teacher-graded OEQ | — | part of 16 | None |
| SCRUM-26 Database relationship diagrams | No diagram in `README.md` or `docs/`. `db/migration/V1`–`V4` are now the accurate schema source (`db/database.sql` is stale). | S | None |
| SCRUM-28 Attendance tracker | No attendance entity. | M (with 29, 45) | None |
| SCRUM-29 Students see own attendance | — | part of 28 | None |
| SCRUM-30 Groups of enrolled students | No group entity. | M | None |
| SCRUM-32 Tutoring availability on teacher profile | `Controller/ProfileMvcController.java` edits only names, picture and status; no public teacher profile. | M | None |
| SCRUM-33 Exam closes when time expires | — | S once 17 exists | None |
| SCRUM-37 Student as assistant teacher | Single role per user (`Model/UserRole.java`), no per-course permission. | M–L | None |
| SCRUM-38 Course Lessons Availability | Requirements unavailable. | Unknown | Requirements first |
| SCRUM-39 Easter eggs | Only the themed 404 page (`templates/errors/404.html`). | S | None |
| SCRUM-43 Email verification | No mail dependency in `pom.xml`, no token entity; `UserMvcController` activates accounts on registration. | M | **SMTP/email provider, public base URL** |
| SCRUM-44 Refer a friend | — | M after 43 | **Email provider** |
| SCRUM-45 Courses can require attendance | No flag on `Model/Course.java`. | part of 28 | None |
| SCRUM-46 Enrolment confirmation email | — | S after 43 | **Email provider** |
| SCRUM-47 PDF certificate | No PDF library in `pom.xml`. Completion status already exists on `Enrollment`. | M | PDF library; "receive" needs the email provider |
| SCRUM-48 Prerequisite courses | No prerequisite relation on `Course`. | S–M | None |

## Groupings

- **SCRUM-28 / 29 / 45 (attendance).** One feature: 45 is the flag, 28 the tracker, 29 the student view. Build in that order.
- **SCRUM-16 / 17 / 18 / 33 (entrance exams).** One epic; 33 is a small rule on top of 17.
- **SCRUM-16 / 48 (enrolment preconditions).** Both add a rule before `EnrollmentServiceImpl.enroll`; design one eligibility hook.
- **SCRUM-43 / 44 / 46, plus the emailed half of 47 (email).** Same mail configuration; 43 owns the setup and tokens.
- **SCRUM-13 / 14 / 37 (who may edit a course).** No course ownership exists; 37 needs it and 14 probably concerns it.
- **SCRUM-38.** If it means scheduled release of lectures, it extends `CourseAccessServiceImpl` (built in SCRUM-10).
- Finished groups: content gating (10, 34, 35, 36), user search (6, 19), ratings (27, 31), deletion (12, 15), notes (20, 24, 41).

## Tickets needing external setup

| Need | Tickets | Notes |
|---|---|---|
| Email provider (SMTP credentials, sender address, public base URL) | 43, 44, 46, optional part of 47 | A local mail catcher is enough for development. |
| File or video storage | 13 (if "upload" is literal) | Videos are external URLs; submissions go to local `./uploads`. |
| PDF library | 47 | A Maven dependency only. |

## Issues found outside the ticket list

| # | Issue | Status at `85c4c7c` |
|---|---|---|
| 1 | Submission files downloadable without login | Fixed by SCRUM-10 (`c300588`): `AssignmentMvcController.download` checks owner or `canManageCourses`. |
| 2 | Any logged-in user can call REST write endpoints | Fixed by SCRUM-10 (`c300588`): `SecurityConfig` restricts `/api/lectures/**` and course/user writes by role. |
| 3 | No course ownership | Open (SCRUM-13 / 14 / 37). |
| 4 | Profile status `INACTIVE` vs. `ACTIVE\|PASSIVE` mismatch | Fixed: `ProfileMvcController` now uses `UserService.STATUS_PASSIVE`. |
| 5 | CSRF disabled in `SecurityConfig` | Open. |
| 6 | `./mvnw` not executable | Open (file mode is still `-rw-rw-r--`). |
| 7 | Bare `RuntimeException` instead of `EntityNotFoundException` | Open (see SCRUM-5). |
| 8 | No JaCoCo `check` goal | Open (see SCRUM-8). |

## Proposed implementation order

| Step | Tickets | Size | Why here |
|---|---|---|---|
| 1 | SCRUM-22 remainder (`GET /api/courses`) | S | Small leak; finishes a partial ticket. |
| 2 | SCRUM-45 → 28 → 29 | M | Self-contained. |
| 3 | SCRUM-48 | S–M | Introduces the enrolment eligibility hook. |
| 4 | SCRUM-43 → 46 → 44 | M + S + M | Needs the email provider; 43 carries the setup. |
| 5 | SCRUM-47 | M | Completion logic exists; email is available after step 4. |
| 6 | SCRUM-16 / 17 / 18 / 33 | L | Plugs into the hook from step 3. Model question types as an extensible hierarchy with a pluggable grader. |
| 7 | SCRUM-30, SCRUM-32 | M + M | Independent; lower priority. |
| 8 | Course ownership, then SCRUM-37 (and SCRUM-13's ownership gap) | M–L | 37 needs ownership. |
| Any time | SCRUM-26, SCRUM-39 | S + S | No dependencies; the schema is stable after SCRUM-50. |
| Blocked | SCRUM-14, SCRUM-38 | — | Need requirements. |
| Ongoing | SCRUM-40 | — | Convention, not a deliverable. |

Also worth doing: reword SCRUM-13 (URL vs upload), add a JaCoCo `check` rule so SCRUM-8 stays true, and re-measure coverage now that four services were added.
