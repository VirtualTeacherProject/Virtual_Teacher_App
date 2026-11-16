# Backlog audit

Audit of every ticket in `docs/backlog.md` against the code on `main` at `ff442fd` (2026-11-16).

**How this was checked.** Controllers, services, repositories, entities, `SecurityConfig` and the Thymeleaf templates were read directly. The seven `*ServiceImplTest` classes were run with JaCoCo (`mvn test -Dtest='*ServiceImplTest' jacoco:report`): 125 tests, 0 failures. The full suite and the running app were not exercised, so anything described as "will fail at runtime" below is inferred from the mappings, not observed. Ticket wording comes only from `backlog.md`; Jira was not consulted.

Paths are relative to `src/main/java/com/MarianFinweFeanor/Virtual_Teacher/` unless they start with `templates/` (`src/main/resources/templates/`) or another top-level directory.

Sizes: **S** = up to a day, **M** = 2–4 days, **L** = a week or more, for one developer including tests.

## Summary

| Status | Count | Tickets |
|---|---|---|
| DONE | 9 | 5, 7, 8, 9, 11, 23, 25, 27, 42 |
| PARTIAL | 7 | 6, 12, 13, 19, 22, 31, 40 |
| NOT STARTED | 27 | 10, 15, 16, 17, 18, 20, 24, 26, 28, 29, 30, 32, 33, 34, 35, 36, 37, 39, 41, 43, 44, 45, 46, 47, 48, plus 14 and 38 (requirements unclear) |

Three tickets listed as completed in `backlog.md` are only partial: **SCRUM-12**, **SCRUM-13** and **SCRUM-22**.

## Tickets listed as completed

| Ticket | Verdict | Evidence | Gaps |
|---|---|---|---|
| SCRUM-5 Exception handling | DONE | `exceptions/GlobalMvcExceptionHandler.java` (404/400/500 pages, access-denied redirect), `exceptions/GlobalRestExceptionHandler.java` (JSON `ApiErrorResponse`), custom `EntityNotFoundException`, `EntityDuplicateException`, `FileStorageException`. | Several paths still throw bare `RuntimeException("User not found")` / `"Not enrolled"` / `"Enrollment not found"` (`Controller/AdminUserController.java`, `Controller/CourseRatingMvcController.java`, `Service/EnrollmentServiceImpl.java`, `UserServiceImpl.getUser`), which surface as a generic 500 instead of 404. |
| SCRUM-7 Swagger documentation | DONE | `springdoc-openapi-starter-webmvc-ui` in `pom.xml`, `Config/OpenApiConfig.java`, Swagger paths permitted in `Config/SecurityConfig.java`. | Only `WikiRestController` carries `@Operation`/`@Tag` annotations; the other REST controllers are documented by default introspection only. `Controller/RestController/SwaggerRestController.java` is an empty class. |
| SCRUM-8 Service coverage ≥ 80% | DONE | Measured in this audit: `Service` package **96.0% instruction, 87.0% branch** (backlog claims ~97% / 87%). Lowest class is `WikiServiceImpl` (87% instruction). | Nothing enforces the threshold: the JaCoCo plugin in `pom.xml` has `prepare-agent` and `report` but no `check` goal, so CI will not fail if coverage drops. |
| SCRUM-9 CI on main | DONE | `.github/workflows/main.yml` runs `mvn -B clean package` on push and pull request to `main`. | Whether recent runs are green was not checked. The full-context tests in CI boot against the committed H2 file `data/virtual_teacher_db.mv.db`. |
| SCRUM-11 Admins have teacher functionality | DONE | `UserServiceImpl.ensureApprovedTeacher` returns early for `ADMIN`; `hasAnyRole("TEACHER","ADMIN")` in `SecurityConfig` and on handlers; `Config/GlobalModelAttributes.java` sets `canManageCourses` for admins. | None found. |
| SCRUM-12 Admin manage courses/users, approve teachers/admins | **PARTIAL** | `Controller/AdminUserController.java` (list, edit, delete, make-teacher/admin/student, pending teachers, approve), `Controller/AdminCourseController.java` (list, edit, delete). | Deletion is only safe for trivial rows — see SCRUM-15: deleting a course that has lectures, or a user who teaches a course, has submissions or has comments, has no cascade and should fail on the foreign key. Also `UserServiceImpl.countByRole` returns a hard-coded `0`, so the admin dashboard teacher count is always zero. |
| SCRUM-13 Teachers create courses and upload video lectures | **PARTIAL** | Course creation and lecture add/edit exist (`Controller/EnrollmentMvcController.java`, `Controller/LectureMvcController.java`, `templates/add-course.html`, `templates/add-lecture.html`). | Video is a URL string (`Model/Lecture.videoUrl`, embedded as a YouTube iframe in `templates/lecture-detail.html`); there is no video file upload. There is no ownership check: any approved teacher can edit or publish any course and its lectures. |
| SCRUM-22 Draft/published; students cannot see drafts | **PARTIAL** | `Course.status`, `CourseServiceImpl.getVisibleCourses` / `searchVisibleCoursesByTitle` / `publishCourse`, `EnrollmentMvcController.canViewCourse`, enrolment refused for drafts. | The lecture routes do not check course status: `GET /courses/{id}/lectures` and `GET /courses/{id}/lectures/{lectureId}` in `LectureMvcController` are public and never call a visibility check, so a draft course's lectures are reachable by URL. `GET /api/courses` (`CourseRestController`) returns every course, drafts included, to any logged-in user. |
| SCRUM-23 Students comment on lectures | DONE | `Model/LectureComment.java`, `Service/LectureCommentServiceImpl.java`, `LectureMvcController.addComment` (students must be enrolled), comment list and form in `templates/lecture-detail.html`. | Comments are readable by anonymous visitors because the lecture page is public. No edit or delete. |
| SCRUM-25 Link to Swagger | DONE | `README.md` lines 7–8. | The link exists only in the README; no template links to Swagger. |
| SCRUM-27 Completion / rating eligibility | DONE | `EnrollmentServiceImpl.recalculateProgress` and `ensureCanRateCourse`, `Course.passingGrade` (default 50), called from `AssignmentServiceImpl.gradeAssignment` and `CourseRatingMvcController`. | The average is taken over every graded submission, so resubmissions for the same lecture all count. Git history files this work under SCRUM-21. |
| SCRUM-42 Lecture comments | DONE | Same implementation as SCRUM-23, plus `LectureCommentServiceImplTest` (7 tests). | Same as SCRUM-23. |

## Remaining — core functionality and security

| Ticket | Verdict | Evidence | Size | External setup |
|---|---|---|---|---|
| SCRUM-6 Approved teachers get course admin and student search | PARTIAL | Approval flow and course management exist; `GET /courses/{id}/students` lists enrolled students (`templates/course-students.html`). No student search anywhere. | S (on top of SCRUM-19) | None |
| SCRUM-10 Videos only after enrolment | NOT STARTED | `templates/lecture-detail.html` line 218 carries the comment "SCRUM-10 protection will be added later" and renders the video for everyone; `SecurityConfig` permits `GET /courses/**` to anonymous users. | S–M | None while videos are external URLs. Hiding a YouTube URL is not real protection; true protection needs self-hosted or signed-URL video storage. |
| SCRUM-14 Teacher access limited to administrative part of sessions | NOT STARTED (requirements unclear) | Nothing in the code maps to "sessions". | Unknown | Needs acceptance criteria first |
| SCRUM-15 Deletion policy for FK-connected entities | NOT STARTED | Inconsistent today: `User.enrollments` and `Course.enrollments` cascade with orphan removal, but nothing cascades to `Lecture`, `Assignment` or `LectureComment`, and `Course.teacher` is a non-null FK. `AdminCourseController.deleteCourse` has a comment deferring the decision. | M | None |
| SCRUM-16 Entrance exam with minimum grade | NOT STARTED | No exam, quiz or question entity. | L (with 17, 18, 33) | None |
| SCRUM-17 Exam start/end times | NOT STARTED | — | part of 16 | None |
| SCRUM-18 MCQ, MSQ, teacher-graded OEQ | NOT STARTED | — | part of 16 | None |
| SCRUM-19 Search users by first name, last name, email | PARTIAL | The pieces exist but are not connected: `util/UserSpecification.java` (exact-match only), `UserRepository` extends `JpaSpecificationExecutor`, and `GET /api/users` accepts the three parameters — but `UserServiceImpl.getAllUsers(firstName, lastName, email)` ignores them and returns `findAll()`. No search box in `templates/admin/users.html`. | S | None |
| SCRUM-31 Rating 1–5 with mandatory 75-character comment | PARTIAL | 1–5 validation in `CourseRatingMvcController`; `reviewText` is `required = false` and the textarea placeholder in `templates/course-detail.html` reads "Optional review...". No minimum length, no public display of reviews or an average. | S | None |
| SCRUM-33 Exam closes when time expires | NOT STARTED | — | S once 17 exists | None (check the deadline on submission; no scheduler needed) |
| SCRUM-43 Email verification before transactions | NOT STARTED | No mail dependency in `pom.xml`, no token entity, registration in `Controller/UserMvcController.java` activates the account immediately. | M | **SMTP/email provider, and a public base URL for the verification link** |
| SCRUM-48 Prerequisite courses on the same topic | NOT STARTED | No prerequisite relation on `Course`. | S–M | None |

## Remaining — learning experience

| Ticket | Verdict | Evidence | Size | External setup |
|---|---|---|---|---|
| SCRUM-20 Plain-text notes per lecture | NOT STARTED | No note entity, repository or template. | M (20 + 24 + 41 together) | None |
| SCRUM-24 Add/edit lecture notes | NOT STARTED | — | part of 20 | None |
| SCRUM-41 Plain-text notes per lecture | NOT STARTED | Duplicate of SCRUM-20. | — | None |
| SCRUM-28 Attendance tracker; teachers mark absences | NOT STARTED | No attendance entity or flag. | M (28 + 29 + 45 together) | None |
| SCRUM-29 Students see only their own attendance | NOT STARTED | — | part of 28 | None |
| SCRUM-45 Some courses require attendance | NOT STARTED | No flag on `Course`. | part of 28 | None |
| SCRUM-30 Groups of enrolled students | NOT STARTED | — | M | None |
| SCRUM-34 Extra resources only after enrolment | NOT STARTED | Only the Wikipedia search box exists; no resource entity. | M | None for links; file storage if resources are uploaded files (the local `uploads/` directory will not survive redeploys on most hosts) |
| SCRUM-47 PDF certificate on completion | NOT STARTED | No PDF library in `pom.xml`. Completion status already exists on `Enrollment`. | M | A PDF library dependency (no external service). The "receive" half needs the email provider from SCRUM-43. |

## Remaining — teacher and course tools

| Ticket | Verdict | Evidence | Size | External setup |
|---|---|---|---|---|
| SCRUM-32 Tutoring availability on teacher profile | NOT STARTED | `Controller/ProfileMvcController.java` edits only names, picture and status; there is no public teacher profile page. | M | None |
| SCRUM-35 Students request resources (≥ 100 characters) | NOT STARTED | — | S after 34 | None |
| SCRUM-36 Teachers manage resource requests | NOT STARTED | — | M after 35 | None |
| SCRUM-37 Student as assistant teacher | NOT STARTED | Roles are a single enum per user (`Model/UserRole.java`) and there is no per-course permission. | M–L | None |
| SCRUM-38 Course Lessons Availability | NOT STARTED (requirements unavailable) | — | Unknown | Needs requirements first |

## Remaining — documentation, engagement and email

| Ticket | Verdict | Evidence | Size | External setup |
|---|---|---|---|---|
| SCRUM-26 Database relationship diagrams | NOT STARTED | No image or diagram in `README.md` or `docs/`. `src/main/resources/db/database.sql` is a stale MySQL schema that no longer matches the entities. | S | None |
| SCRUM-39 Easter eggs | NOT STARTED | The 404 page has a robot image (`templates/errors/404.html`), which is a themed error page rather than a hidden feature. | S | None |
| SCRUM-40 Git history communicates order, features, contributors | PARTIAL | Recent work uses `SCRUM-<n>` commits and PRs, but only 5 of 116 non-merge commits start with a ticket key; messages include "edit code", "refactor", "bug fixes 15/12/2025". The two contributors appear under five author names. | Ongoing | None. History must not be rewritten (project rule), so this can only improve going forward. |
| SCRUM-44 Refer a friend by emailed link | NOT STARTED | — | M after 43 | **Email provider** |
| SCRUM-46 Enrolment confirmation email | NOT STARTED | — | S after 43 | **Email provider** |

## Groupings

**Confirmed**

- **SCRUM-20 / 24 / 41 (notes).** One feature. 41 is a word-for-word duplicate of 20 and should be closed as such; 24 is the edit half of the same screen.
- **SCRUM-28 / 29 / 45 (attendance).** One feature in three layers: 45 is the per-course flag, 28 is the tracker and teacher marking, 29 is the student's read-only view. Build in that order.
- **SCRUM-16 / 17 / 18 / 33 (entrance exams).** One epic. 33 is a small rule on top of 17 rather than a separate piece of work.

**Additional overlaps found**

- **SCRUM-10 / 34 / 38 (content gated by enrolment).** 10 and 34 need the same "is this user enrolled in this course, or allowed to manage it" check; build it once in 10 and reuse it in 34. If 38 turns out to mean scheduled release of lectures, it belongs in the same guard. The SCRUM-22 gap (draft lectures reachable by URL) is fixed by the same check.
- **SCRUM-34 → 35 → 36 (resources).** A strict chain: requests (35) are about resources (34), and request management (36) needs requests.
- **SCRUM-43 / 44 / 46, plus the emailed half of 47 (email).** All need the same mail configuration and sender service. 43 should own that setup; 44 can reuse its token mechanism.
- **SCRUM-6 / 19 (user search).** 6's student search is 19 restricted to students and exposed to teachers.
- **SCRUM-12 / 15 (deletion).** 12's delete buttons are only reliable once 15 defines the policy.
- **SCRUM-16 / 48 (enrolment preconditions).** Both add a rule that must pass before `EnrollmentServiceImpl.enroll` succeeds. Design one eligibility hook so the two rules do not end up in separate controllers.
- **SCRUM-27 / 31 (rating).** 31 extends the form and validation that 27 already gates.
- **SCRUM-13 / 14 / 37 (who may edit a course).** There is currently no notion of a course's owner having exclusive edit rights. 37 (assistant teachers) cannot be built sensibly until ownership is enforced, and 14 probably concerns the same boundary.

## Tickets needing external setup

| Need | Tickets | Notes |
|---|---|---|
| Email provider (SMTP credentials, sender address, public base URL) | 43, 44, 46, optional part of 47 | Nothing exists yet. A local mail catcher is enough for development; production needs real credentials as environment variables alongside the existing `PG*` ones. |
| File or video storage | 10 (only for real protection), 13 (if "upload" is taken literally), 34 (if resources are files) | Today videos are external URLs and uploads go to a local directory. |
| PDF library | 47 | A Maven dependency only. |

Nothing in the backlog requires a payment provider.

## Issues found outside the ticket list

These are not tracked by any ticket and several are security-relevant.

1. **Submission files are downloadable without login.** `AssignmentMvcController.download` has no authorisation check and its path falls under the public `GET /courses/**` matcher in `SecurityConfig`, so anyone who guesses an assignment id can download a student's file.
2. **Any logged-in user can use the write endpoints of the REST API.** `/api/**` only requires authentication, so a student can call `DELETE /api/users/{id}`, `DELETE /api/courses/{id}` and the lecture create/update/delete endpoints.
3. **No course ownership.** Any approved teacher can edit, publish and grade in any course (see SCRUM-13).
4. **Profile status mismatch.** `ProfileMvcController` accepts `ACTIVE` or `INACTIVE`, but `User.status` only validates `ACTIVE|PASSIVE`, and `CustomUserDetailsService` disables any account that is not `ACTIVE`.
5. **CSRF protection is disabled** in `SecurityConfig` for a session-based form application.
6. **`./mvnw` is not executable** in this checkout ("Permission denied"); system `mvn` works.

Items 1 and 2 fit naturally into SCRUM-10; item 3 into SCRUM-14 once its criteria are written.

## Proposed implementation order

Ordered by dependencies first, then by risk and size.

| Step | Tickets | Size | Why here |
|---|---|---|---|
| 1 | SCRUM-10, plus the SCRUM-22 lecture gap and issues 1–2 above | M | Closes the open access holes and produces the enrolment/visibility guard that 34 and 38 reuse. |
| 2 | SCRUM-19, then SCRUM-6 | S + S | Mostly wiring existing code; finishes two partial tickets. |
| 3 | SCRUM-31 | S | Small, finishes a partial ticket. |
| 4 | SCRUM-15, which completes SCRUM-12 | M | Decide the deletion policy before adding the many new entities below, so each new entity follows it from the start. |
| 5 | SCRUM-20 / 24 / 41 | M | Self-contained, no dependencies. |
| 6 | SCRUM-34 → 35 → 36 | M + S + M | Reuses the guard from step 1. |
| 7 | SCRUM-45 → 28 → 29 | M | Self-contained. |
| 8 | SCRUM-48 | S–M | Introduces the enrolment eligibility hook with the simpler of its two rules. |
| 9 | SCRUM-43 → 46 → 44 | M + S + M | Needs the email provider; 43 carries the setup. |
| 10 | SCRUM-47 | M | Completion logic already exists; email delivery is available after step 9. |
| 11 | SCRUM-16 / 17 / 18 / 33 | L | Largest item; plugs into the eligibility hook from step 8. Per the design note, model question types as an extensible hierarchy with a pluggable grader so coding and AI-graded questions can be added later. |
| 12 | SCRUM-30, SCRUM-32 | M + M | Independent; lower priority. |
| 13 | SCRUM-37 | M–L | Needs course ownership enforced first. |
| Any time | SCRUM-26, SCRUM-39 | S + S | No dependencies. Draw the diagram for 26 after step 4 or later so it is not immediately out of date. |
| Blocked | SCRUM-14, SCRUM-38 | — | Cannot be sized or ordered until requirements are written. |
| Ongoing | SCRUM-40 | — | A convention to keep, not a deliverable. |

Also worth doing alongside step 1: fix the SCRUM-13 wording (URL vs upload) so the ticket matches what is intended, and add a JaCoCo `check` rule so SCRUM-8 stays true.
