# Backlog audit

Audit of every ticket in `docs/backlog.md` against git history and the code on `main` at `85c4c7c` (2026-11-18), updated for SCRUM-22 (`773407b`), SCRUM-45 (`a9b4c43`), SCRUM-28 (`60e28f7`), SCRUM-51 (`57d852f`, `ff5b798`) SCRUM-52 (`ae6cf14`, plus the follow-up on `3e83cc9`) and SCRUM-29 with issue 11 (`d4901ee`) and SCRUM-48 (`feature/SCRUM-48-prerequisite-courses`). Previous audit: `ff442fd` (2026-11-16).

**How this was checked.** `git log` on `main` (all `SCRUM-<n>` commits, merges and PRs) and `git branch --no-merged main` (only the stale `origin/exception-handling` remains unmerged; nothing in it changes a status). Each claimed feature was then checked in the controllers, services, entities, migrations, `SecurityConfig` and templates. NOT STARTED tickets were confirmed by searching `src/main` for their entities and keywords (attendance, prerequisite, exam/quiz, mail/verification, PDF/certificate, group, tutoring, assistant). For the SCRUM-52 update the full suite was re-run on 2026-11-21 (`mvn -B verify`: 552 tests, 0 failures; 556 with the SCRUM-52 follow-up; `mvn test` on 2026-11-24 with SCRUM-29: 570 tests, 0 failures; `mvn -B verify` with SCRUM-48: 607 tests, 0 failures, `Service` package 97.8% instruction / 93.6% branch, `CoursePrerequisiteServiceImpl` 100% / 97.5%); the SCRUM-8 coverage figure is from that run. Ticket wording comes only from `backlog.md`; Jira was not consulted.

Paths are relative to `src/main/java/com/MarianFinweFeanor/Virtual_Teacher/` unless they start with `templates/` (`src/main/resources/templates/`), `db/migration/` (`src/main/resources/db/migration/`) or another top-level directory. Commit hashes are on `main`; a hash in brackets is the merge or PR that brought the work in.

Sizes: **S** = up to a day, **M** = 2–4 days, **L** = a week or more, for one developer including tests.

## Summary

| Status | Count | Tickets |
|---|---|---|
| DONE | 30 | 5, 6, 7, 8, 9, 10, 11, 12, 15, 19, 20, 22, 23, 24, 25, 27, 28, 29, 31, 34, 35, 36, 41 (duplicate of 20), 42, 45, 48, 49, 50, 51, 52 |
| PARTIAL | 2 | 13, 40 |
| NOT STARTED | 15 | 16, 17, 18, 26, 30, 32, 33, 37, 39, 43, 44, 46, 47, plus 14 and 38 (requirements unclear) |

### Status changes since the 2026-11-16 audit

| Ticket | Was | Now | Evidence |
|---|---|---|---|
| SCRUM-6 | PARTIAL | DONE | `0a7bd61` [`eca7feb`] |
| SCRUM-10 | NOT STARTED | DONE | `c300588` [PR #15 `eb3664c`] |
| SCRUM-12 | PARTIAL | DONE | `d8bf3d1`, `c1759c8` [`eca7feb`] |
| SCRUM-15 | NOT STARTED | DONE | `d8bf3d1`, `e695d8b` [`eca7feb`] |
| SCRUM-19 | PARTIAL | DONE | `cfe08f6` [`eca7feb`] |
| SCRUM-20 | NOT STARTED | DONE | `a0d77bc` [`1490a55`] |
| SCRUM-22 | PARTIAL | DONE | `773407b` |
| SCRUM-24 | NOT STARTED | DONE | `a0d77bc` [`1490a55`] |
| SCRUM-28 | NOT STARTED | DONE | `60e28f7`, fixed in SCRUM-51 part A |
| SCRUM-29 | NOT STARTED | DONE | `d4901ee` |
| SCRUM-31 | PARTIAL | DONE | `f540e6f` [`eca7feb`] |
| SCRUM-34 | NOT STARTED | DONE | `c69e3f8` [`4a50276`] |
| SCRUM-35 | NOT STARTED | DONE | `750b2e2` [`508cff2`] |
| SCRUM-36 | NOT STARTED | DONE | `caae73e` [`a3da031`] |
| SCRUM-41 | NOT STARTED | DONE (duplicate) | Closed as a duplicate of SCRUM-20 |
| SCRUM-45 | NOT STARTED | DONE | `a9b4c43` |
| SCRUM-48 | NOT STARTED | DONE | `feature/SCRUM-48-prerequisite-courses` |
| SCRUM-49 | not audited | DONE | `4ba7128` [PR #13 `5937900`] |
| SCRUM-50 | not audited | DONE | `eac9a74` [`7e46e27`] |
| SCRUM-51 | NOT STARTED | DONE | `feature/SCRUM-51-live-sessions-and-progression` (part A `57d852f`, part B) |
| SCRUM-52 | not listed | DONE | `ae6cf14`, follow-up `3e83cc9` (new placeholder ticket: course ownership) |

SCRUM-13 and SCRUM-40 stay PARTIAL. All other tickets keep their status.

## Done

| Ticket | Evidence | Remaining gaps (not blocking DONE) |
|---|---|---|
| SCRUM-5 Exception handling | `9d25b93`, `bf00898`, `1c155bb`. `exceptions/GlobalMvcExceptionHandler.java`, `exceptions/GlobalRestExceptionHandler.java` (`ApiErrorResponse`), custom `EntityNotFoundException`, `EntityDuplicateException`, `EntityInUseException`, `FileStorageException`. | Bare `RuntimeException` is still thrown in `Controller/AdminUserController.java` (6×, "User not found"), `Service/EnrollmentServiceImpl.java` (2×, "Enrollment not found") and `Service/UserServiceImpl.java` (1×), which surface as a 500 instead of a 404. |
| SCRUM-6 Approved teachers get course admin and student search | `0a7bd61` [`eca7feb`]. `Controller/StudentSearchMvcController.java` (`/students`), `templates/students.html`. | — |
| SCRUM-7 Swagger documentation | `c1b3cad`, `9a837b4`. `springdoc-openapi-starter-webmvc-ui` in `pom.xml`, `Config/OpenApiConfig.java`. | Only `WikiRestController` has `@Operation`/`@Tag`. `SwaggerRestController` is an empty class. |
| SCRUM-8 Service coverage ≥ 80% | `ff69e34` [PR #11 `909ef03`]. Measured 2026-11-21 (`mvn verify`, full suite, 552 tests): `Service` package 97.5% instruction, 93.7% branch; `CourseAccessServiceImpl` 100% / 98.0%; `CourseResourceServiceImpl` 100% / 96.9%; `ResourceRequestServiceImpl` 100% / 98.1%; `AttendanceServiceImpl` 96.9% / 96.4%. | The JaCoCo plugin in `pom.xml` has only `prepare-agent` and `report`, no `check` goal, so CI does not enforce the threshold. |
| SCRUM-9 CI on main | `9794fa0`, `b01c2a3` [PR #12 `d2519ae`]. `.github/workflows/main.yml` runs `mvn -B clean package` on push and PR to `main`. | Run results not checked. |
| SCRUM-10 Videos only after enrolment | `c300588` [PR #15 `eb3664c`]. `Service/CourseAccessServiceImpl.java` (`canViewCourse`, `canAccessCourseContent`, `ensureCanViewCourse`), used by `LectureMvcController`; `canAccessContent` gate in `templates/lecture-detail.html`. Also restricted REST writes in `SecurityConfig` and added an owner/manager check to `AssignmentMvcController.download`. | Video URLs are external (YouTube), so hiding them is not real protection. |
| SCRUM-11 Admins have teacher functionality | `64f6e3e` [PR #14 `2c3b95c`]. `UserServiceImpl.ensureApprovedTeacher` returns early for admins; `canManageCourses` in `Config/GlobalModelAttributes.java`. | — |
| SCRUM-12 Admin manage courses/users, approve teachers/admins | `d8bf3d1`, `c1759c8` [`eca7feb`]. `Controller/AdminUserController.java`, `Controller/AdminCourseController.java`; deletes go through `Service/ContentCleanupServiceImpl.java`; `UserServiceImpl.countByRole` now queries the repository. | — |
| SCRUM-15 Deletion policy | `d8bf3d1`, `e695d8b` [`eca7feb`]. `Service/ContentCleanupServiceImpl.java` (`deleteLectureContent`, `deleteCourseContent`, `deleteUserContent`); `EntityInUseException` when a user still teaches courses. | — |
| SCRUM-19 User search | `cfe08f6` [`eca7feb`]. `UserServiceImpl.getAllUsers(firstName, lastName, email)` builds a `UserSpecification`; search on `templates/admin/users.html` and `GET /api/users`. | — |
| SCRUM-20 Lecture notes | `a0d77bc` [`1490a55`]. `Model/LectureNote.java` (max 5000), `Service/LectureNoteServiceImpl.java`, notes panel in `templates/lecture-detail.html`. | — |
| SCRUM-22 Drafts hidden from students | `ff69e34`, `c300588`, `773407b`. `CourseServiceImpl.getVisibleCourses` / `searchVisibleCoursesByTitle`, `CourseAccessServiceImpl.ensureCanViewCourse` on the lecture routes, enrolment refused for drafts. `CourseRestController`: `GET /api/courses` returns `getVisibleCourses(email, canManageCourses)`, and `GET /api/courses/{id}` answers 404 when `canViewCourse` is false. `CourseRestControllerTest` (6 tests). | Any approved teacher can open any draft by id (same as the course page); the list shows only their own drafts. Kept on purpose in SCRUM-52 (viewing stays open; see issue 10). |
| SCRUM-23 Students comment on lectures | `10e2b28` [PR #10 `76dfd04`]. `Model/LectureComment.java`, `Service/LectureCommentServiceImpl.java`, form in `templates/lecture-detail.html`. | Comments are readable by anyone who can view the lecture page. No edit or delete. |
| SCRUM-24 Add/edit lecture notes | Same as SCRUM-20: saving overwrites, an empty save deletes. | — |
| SCRUM-25 Link to Swagger | `4ce7935` [PR #8 `eaa762f`]. `README.md` lines 7–8. | No template links to Swagger. |
| SCRUM-27 Completion / rating eligibility | `d99c93e`, `14b382e` [PR #9 `aa93b41`] (committed as SCRUM-21). `EnrollmentServiceImpl.recalculateProgress` / `ensureCanRateCourse`, `Course.passingGrade`. | The average counts every graded submission, resubmissions included. Since SCRUM-51 completion means every lecture is marked as finished (and graded when it has an assignment); a course without assignments can be rated once completed. |
| SCRUM-28 Attendance tracker | `60e28f7`, changed by SCRUM-51 part A. `Model/LectureAttendance.java` (`lecture_attendance`: lecture, student, `AttendanceStatus` PRESENT/ABSENT, `recorded_at`; unique per lecture and student), `db/migration/V6__lecture_attendance.sql`, `Repositories/LectureAttendanceRepository.java`, `Service/AttendanceServiceImpl.java`, `Controller/AttendanceMvcController.java` (GET/POST `/courses/{courseId}/lectures/{lectureId}/attendance`), `templates/lecture-attendance.html`, Attendance button in `templates/lecture-detail.html` for live lectures of courses with `attendanceRequired`. Since SCRUM-51 `AttendanceService.attendanceClosedReason` opens the roll call only when the course requires attendance, the lecture is live and its session has started; otherwise the page is read-only and says why. The roll call of a live lecture lists the students enrolled at its start (`EnrollmentRepository.findStudentsByCourseIdEnrolledBy`). The teacher ticks the absent students; every other student on the roll call is stored as present, and saving again overwrites. Guarded by the `SecurityConfig` matcher (above the public `GET /courses/**` rule), `@PreAuthorize` and `ensureApprovedTeacher`. Recording is refused when attendance is closed or an absent id is not on the roll call; a lecture from another course answers 404; two simultaneous saves give a flash error instead of a 500. `ContentCleanupServiceImpl` removes records with their lecture, course or student. Tests: `AttendanceServiceImplTest` (22), `AttendanceMvcControllerTest` (13), `LectureMvcControllerTest`, `SecurityRulesTest` (+2), `ContentCleanupServiceImplTest`, `DeletionPolicyIntegrationTest`, V6 in `FlywayMigrationTest` and `PostgresMigrationTest`. | Students see their own records since SCRUM-29. Only the course's own teacher or an admin takes attendance since SCRUM-52 (issue 3). Students who unenrol keep their old records but leave the roll call. No per-course attendance summary. Records taken before SCRUM-51 on non-live lectures stay readable but can no longer be changed. |
| SCRUM-29 Students see own attendance | `d4901ee`. `Model/StudentAttendanceEntry` (lecture, status or null, upcoming), `AttendanceService.getOwnAttendance(Enrollment)` in `Service/AttendanceServiceImpl.java`, reading only the enrolled student's records through `LectureAttendanceRepository.findByStudent_UserIdAndLecture_Course_CourseId`. `EnrollmentMvcController.courseDetail` passes the viewer's own enrolment, so nobody can ask for another student's records; `templates/course-detail.html` shows a "Your attendance" card (`#own-attendance`) with Present / Absent / Upcoming / Not recorded yet per lecture, in lecture order, and present/absent counts. Rows: every lecture with a record; when the course requires attendance, also live lectures still to come and those held since the student enrolled without a record. Sessions held before enrolment are left out. No migration, no new route. Tests: `AttendanceServiceImplTest` (+4), `CourseDetailPageTest` (+2). | No separate page or REST endpoint. Teachers see attendance per lecture only (no per-course summary). |
| SCRUM-31 Rating with 75-character review | `f540e6f` [`eca7feb`]. `Controller/CourseRatingMvcController.java`; reviews and average on `templates/course-detail.html`. | — |
| SCRUM-34 Resources after enrolment | `c69e3f8` [`4a50276`]. `Model/CourseResource.java`, `Service/CourseResourceServiceImpl.java`, `Controller/CourseResourceMvcController.java`, `db/migration/V2__course_resources.sql`. | Links only; no file upload. |
| SCRUM-35 Students request resources (≥ 100 characters) | `750b2e2` [`508cff2`]. `Model/ResourceRequest.java` (`MIN_MESSAGE_LENGTH = 100`), `Controller/ResourceRequestMvcController.java`, `db/migration/V3__resource_requests.sql`. | — |
| SCRUM-36 Teachers manage resource requests | `caae73e` [`a3da031`]. `ResourceRequestMvcController` `/status`, `/assign`, `/response`; `Service/ResourceRequestServiceImpl.java`; `db/migration/V4__resource_request_assignment.sql`. | — |
| SCRUM-41 Notes per lecture | Duplicate of SCRUM-20; closed. | — |
| SCRUM-42 Lecture comments | `10e2b28`, `0ecc6c3` [PR #10 `76dfd04`]. Same code as SCRUM-23, plus `LectureCommentServiceImplTest`. | Same as SCRUM-23. |
| SCRUM-45 Courses can require attendance | `a9b4c43`. `Model/Course.attendanceRequired` (default `false`), `db/migration/V5__course_attendance_required.sql` (`boolean default false not null`, so existing courses do not require attendance). Set by `EnrollmentMvcController.submitAddCourse` / `submitEditCourse` (checkbox in `templates/add-course.html`, `templates/edit-course.html`), `AdminCourseController` (`templates/admin/course-edit.html`) and `CourseServiceImpl.updateCourse`. Shown in `templates/courses.html` (badge) and `templates/course-detail.html`. Tests: `CourseAttendanceRequiredTest` (9), `AdminCourseControllerTest` (+2), `CourseServiceImplTest` (+1), V5 checked in `FlywayMigrationTest` and `PostgresMigrationTest`. | The tracker itself is SCRUM-28; roll call only for live lectures (SCRUM-51). The REST `PUT /api/courses/{id}` sets it to `false` when the field is left out of the JSON, like the other fields. |
| SCRUM-48 Prerequisite courses | `feature/SCRUM-48-prerequisite-courses`. Hybrid model: required (same topic, must be COMPLETED, grade ignored) and recommended (any topic, never blocks). `db/migration/V9__course_prerequisites.sql` (`course_prerequisites`: course, required course, `required`; unique per pair), `Model/CoursePrerequisite.java`, `Repositories/CoursePrerequisiteRepository.java`, `Service/CoursePrerequisiteServiceImpl.java` (`getPrerequisites`, `getCandidates`, `addPrerequisite`, `removePrerequisite`, `getMissingRequired`, `ensurePrerequisitesMet`; refuses self, drafts, duplicates, required with another topic and required cycles, found by a walk over required links). Depends only on repositories and `CourseAccessService`, so `EnrollmentServiceImpl.enroll` can call `ensurePrerequisitesMet` (the eligibility hook) without a dependency cycle. Web: `POST /courses/{id}/prerequisites` and `POST /courses/{id}/prerequisites/{prerequisiteCourseId}/delete` in `EnrollmentMvcController` (SecurityConfig matcher, `@PreAuthorize`, `ensureApprovedTeacher`, `ensureCanManageCourse`); Prerequisites card on `templates/course-detail.html` (`#course-prerequisites`, manager form `#add-prerequisite-form`, disabled Enroll `#enroll-blocked`); enrolling is refused with a flash error. `ContentCleanupServiceImpl.deleteCourseContent` removes links on both sides. Tests: `CoursePrerequisiteServiceImplTest` (21), `CoursePrerequisiteMvcTest` (11), `EnrollmentServiceImplTest` (+2), `ContentCleanupServiceImplTest`, `DeletionPolicyIntegrationTest` (+1), `SecurityRulesTest` (+2), V9 in `FlywayMigrationTest` and `PostgresMigrationTest`. | Requirements are checked only when enrolling; students already enrolled stay. A required course that is later moved to draft or given another topic stays linked (shown with a Draft badge) and can block enrolment until the teacher removes it. No REST endpoint for prerequisites. Teachers and admins enrolling are checked like students. |
| SCRUM-49 Pull request template | `4ba7128` [PR #13 `5937900`]. `.github/pull_request_template.md`. | Was not listed in `backlog.md` before this update. |
| SCRUM-50 Flyway, `ddl-auto=validate` | `eac9a74`, `589abb5` [`7e46e27`]. `flyway-core` in `pom.xml`, `db/migration/V1__baseline_schema.sql` to `V4`. | `courses.teacher_id` is still nullable in databases created before Flyway. |
| SCRUM-51 Live lecture sessions and self-paced lecture progression | `feature/SCRUM-51-live-sessions-and-progression`. Placeholder number, not yet in Jira. **Part A** (`57d852f`): `Model/Lecture` `live`, `liveStart`, `meetingUrl` (`db/migration/V7__lecture_live_sessions.sql`), set on `templates/add-lecture.html` / `edit-lecture.html`; live badge and start time on the lecture list and page, meeting link only for users with content access; SCRUM-28 roll call limited to live lectures that have started (see SCRUM-28). **Part B**: `db/migration/V8__lecture_progress.sql` adds `lectures.lecture_order`, `lectures.assignment_required`, `courses.lectures_in_order` and `lecture_progress` (backfilled from graded submissions). `Model/LectureProgress`, `Repositories/LectureProgressRepository` (`countCompleted`), `Service/LectureProgressServiceImpl` (`markFinished`, `isLocked`, `getLectureStates`, `getCourseProgress`). `EnrollmentServiceImpl.recalculateProgress` now counts completed lectures (finished, and graded when the lecture has an assignment); `ensureCanRateCourse` lets a completed course without assignments be rated. `AssignmentServiceImpl.submit` refuses locked lectures. `LectureServiceImpl` numbers new lectures and has `moveLecture`. Web: `POST /courses/{c}/lectures/{l}/finish`, `POST .../move?direction=up|down`, `GET /courses/{c}/progress` (`Controller/CourseProgressMvcController`, `templates/course-progress.html`); lock notice and Mark as finished on `templates/lecture-detail.html`; order, badges and move buttons on `templates/lectures.html`; "Lectures must be completed in order" on the course forms. New teacher actions call `CourseAccessService.ensureCanManageCourse`. Progress rows go with their lecture, course or student (`ContentCleanupServiceImpl`). Tests: `LectureProgressServiceImplTest` (17), `CourseProgressMvcControllerTest` (4), `LectureMvcControllerTest`, `LectureServiceImplTest`, `EnrollmentServiceImplTest`, `AssignmentServiceImplTest`, `CourseAccessServiceImplTest`, `SecurityRulesTest` (+3), `ContentCleanupServiceImplTest`, V7/V8 incl. backfill in `FlywayMigrationTest` and `PostgresMigrationTest`. | "Watched" is self-reported (a button), not verified. Reordering and the progress page are owner/admin only since SCRUM-52; the in-order lock is still bypassed by every approved teacher (viewing stays open, issue 10). Unmarking a lecture is not possible. The REST lecture API does not expose ordering. |
| SCRUM-52 Course ownership (placeholder number, not yet in Jira) | `ae6cf14`; follow-up `3e83cc9`: the course list and course page show the course's teacher with a "Your course" badge for the owner, and the Edit button on course cards (missed in `ae6cf14`, it still used the global flag) appears only on courses the teacher manages (`managedIds` from `EnrollmentMvcController`), `CourseOwnershipMvcTest` +4. `CourseAccessService.canManageCourse(course, email)`: an admin, or the approved teacher in `Course.teacher` (email compared ignoring case); `ensureCanManageCourse` throws `AccessDeniedException`. Used by: `EnrollmentMvcController` (edit GET/POST, publish, `/courses/{id}/students`, `canManageCourse` model attribute for the course page), `LectureMvcController` (add, edit, move; attribute for the lecture list and page), `AssignmentMvcController` (all submissions only for managers, download, grading, which now also checks approval and that the submission belongs to the lecture and course), `AttendanceServiceImpl`, `CourseProgressMvcController`, `CourseResourceServiceImpl` (add/delete), `ResourceRequestServiceImpl` (list all, status, assign), `CourseRestController` (PUT/DELETE) and `LecturesRestController` (POST/PUT/DELETE). `GlobalRestExceptionHandler` now has the highest precedence and maps `AccessDeniedException` to 403 JSON (before, the unscoped MVC handler could turn REST errors into a redirect). Templates `course-detail.html`, `lectures.html`, `lecture-detail.html`, `assignments.html` use the per-course attribute instead of the global `canManageCourses`. Unchanged on purpose: creating courses and student search (any approved teacher), and viewing. Tests: `CourseAccessServiceImplTest` (+6), `CourseOwnershipMvcTest` (8, new), `LecturesRestControllerTest` (5, new), `CourseRestControllerTest` (+4), `AssignmentMvcControllerTest` (+7), `LectureMvcControllerTest` (+4), `AttendanceServiceImplTest`, `CourseResourceServiceImplTest`, `ResourceRequestServiceImplTest` (refusals). | Related tickets: SCRUM-13 (its ownership gap is closed), SCRUM-14 (probably the same boundary; confirm in Jira and close), SCRUM-37 (assistant teachers would add a per-course permission inside `canManageCourse`). Follow-ups: issues 10 (viewing stays open) and 11 (REST course creation sets any teacher). |

## Partial

| Ticket | Evidence | What is missing | Size |
|---|---|---|---|
| SCRUM-13 Teachers create courses and upload video lectures | Course creation and lecture add/edit in `Controller/EnrollmentMvcController.java`, `Controller/LectureMvcController.java`, `templates/add-course.html`, `templates/add-lecture.html`. | Video is a URL string (`Model/Lecture.videoUrl`); no video upload. (Course ownership is enforced since SCRUM-52.) | S to reword, M–L for upload |
| SCRUM-40 Git history communicates order, features, contributors | 24 of 139 non-merge commits start with `SCRUM-<n>`; every commit since `c300588` does. | Older messages such as "edit code", "refactor", "bug fixes 15/12/2025". The two contributors appear under four author names (`Marian`, `marianmgm`, `umut yildirim`, `UMUT YILDIRIM`). History must not be rewritten, so this only improves going forward; a `.mailmap` would unify the names without rewriting. | Ongoing |

## Not started

None of these have a commit, entity, repository, migration or dependency on `main`.

| Ticket | Evidence | Size | External setup |
|---|---|---|---|
| SCRUM-14 Teacher access limited to administrative part of sessions | Nothing maps to "sessions". Requirements unclear; if it means "a teacher manages only their own courses", SCRUM-52 did it. | Unknown | Acceptance criteria first |
| SCRUM-16 Entrance exam with minimum grade | No exam, quiz or question entity. | L (with 17, 18, 33) | None |
| SCRUM-17 Exam start/end times | — | part of 16 | None |
| SCRUM-18 MCQ, MSQ, teacher-graded OEQ | — | part of 16 | None |
| SCRUM-26 Database relationship diagrams | No diagram in `README.md` or `docs/`. `db/migration/V1`–`V6` are now the accurate schema source (`db/database.sql` is stale). | S | None |
| SCRUM-30 Groups of enrolled students | No group entity. | M | None |
| SCRUM-32 Tutoring availability on teacher profile | `Controller/ProfileMvcController.java` edits only names, picture and status; no public teacher profile. | M | None |
| SCRUM-33 Exam closes when time expires | — | S once 17 exists | None |
| SCRUM-37 Student as assistant teacher | Single role per user (`Model/UserRole.java`), no per-course permission. Ownership exists since SCRUM-52; an assistant would be a per-course permission checked in `CourseAccessService.canManageCourse`. | M | None |
| SCRUM-38 Course Lessons Availability | Requirements unavailable. | Unknown | Requirements first |
| SCRUM-39 Easter eggs | Only the themed 404 page (`templates/errors/404.html`). | S | None |
| SCRUM-43 Email verification | No mail dependency in `pom.xml`, no token entity; `UserMvcController` activates accounts on registration. | M | **SMTP/email provider, public base URL** |
| SCRUM-44 Refer a friend | — | M after 43 | **Email provider** |
| SCRUM-46 Enrolment confirmation email | — | S after 43 | **Email provider** |
| SCRUM-47 PDF certificate | No PDF library in `pom.xml`. Completion status already exists on `Enrollment`. | M | PDF library; "receive" needs the email provider |

## Groupings

- **SCRUM-28 / 29 / 45 / 51 (attendance and progression).** 45 is the flag, 28 the roll call, 51 live lectures plus self-paced completion and in-order unlocking (all done); 29 is the student view of their own attendance. The group is finished. Attendance exists only for live lectures; self-paced courses use completion instead.
- **SCRUM-16 / 17 / 18 / 33 (entrance exams).** One epic; 33 is a small rule on top of 17.
- **SCRUM-16 / 48 (enrolment preconditions).** 48 is done and built the hook: `EnrollmentServiceImpl.enroll` calls `CoursePrerequisiteService.ensurePrerequisitesMet`; SCRUM-16's entrance-exam rule belongs next to it.
- **SCRUM-43 / 44 / 46, plus the emailed half of 47 (email).** Same mail configuration; 43 owns the setup and tokens.
- **SCRUM-13 / 14 / 37 / 52 (who may edit a course).** 52 enforces ownership through `CourseAccessService.canManageCourse` (done); 37 builds on it and 14 probably asks for it.
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
| 3 | No course ownership | Fixed by SCRUM-52: managing a course needs `CourseAccessService.canManageCourse`. |
| 4 | Profile status `INACTIVE` vs. `ACTIVE\|PASSIVE` mismatch | Fixed: `ProfileMvcController` now uses `UserService.STATUS_PASSIVE`. |
| 5 | CSRF disabled in `SecurityConfig` | Open. |
| 6 | `./mvnw` not executable | Open (file mode is still `-rw-rw-r--`). |
| 7 | Bare `RuntimeException` instead of `EntityNotFoundException` | Open (see SCRUM-5). |
| 8 | No JaCoCo `check` goal | Open (see SCRUM-8). |
| 9 | New lectures saved outside `LectureServiceImpl.createLectures` get `lectureOrder = 0` (SCRUM-51) | Open, harmless today. `lectures.lecture_order` is NOT NULL with no database default (V8). `Model/Lecture` defaults the field to 0 so such saves do not fail with `NULL not allowed for column "LECTURE_ORDER"`. Only `createLectures` (MVC form, REST API) puts a new lecture last (`maxLectureOrder + 1`). The unused `LectureServiceImpl.saveLecture` does not, so a lecture saved through it would sort first in its course (moving any lecture with ↑/↓ renumbers the course 1..n). Fix if it ever matters: (1) delete `saveLecture` and its test so `createLectures` is the only way to create a lecture, or (2) make `saveLecture` set `maxLectureOrder + 1` for new lectures. A V9 database default would not help: Hibernate always sends the column, so a default applies only to hand-written SQL inserts. |
| 10 | Viewing stays open to every approved teacher (SCRUM-52 decision, 2026-11-21) | Open, by decision. Any approved teacher can open other teachers' drafts by id, see their protected content (videos, resources, notes) and is never locked out of in-order lectures. To make viewing owner-only: switch `CourseAccessServiceImpl.canViewCourse` and `canAccessCourseContent`, the private `canViewCourse` in `EnrollmentMvcController`, and the lock bypass in `LectureProgressServiceImpl.getLectureStates` from `canManageCourses` to `canManageCourse`, and update the tests that expect the old behaviour. No migration or template change needed. |
| 11 | `POST /api/courses` lets a teacher create a course owned by another teacher (found in SCRUM-52) | Fixed with SCRUM-29 (`d4901ee`). `CourseRestController.createCourse` calls `ensureApprovedTeacher` (teachers awaiting approval get 403) and `CourseService.createCourseAs(course, callerEmail)`, which makes the caller the teacher; only an admin may name another teacher by `teacher.userId`, who must be an active admin or approved teacher (same rule as `assignTeacher`, now the shared `ensureMayTeach`). The old catch-all that turned every error into a 500 is gone, so errors go through `GlobalRestExceptionHandler`. Tests: `CourseServiceImplTest` (+6), `CourseRestControllerTest` (+2). |

## Proposed implementation order

| Step | Tickets | Size | Why here |
|---|---|---|---|
| ~~1~~ | ~~SCRUM-29, plus issue 11~~ | done | Done on `d4901ee`. |
| ~~2~~ | ~~SCRUM-48~~ | done | Done on `feature/SCRUM-48-prerequisite-courses`; built the enrolment eligibility hook. |
| 3 | SCRUM-43 → 46 → 44 | M + S + M | Needs the email provider; 43 carries the setup. |
| 4 | SCRUM-47 | M | Completion logic exists; email is available after step 3. |
| 5 | SCRUM-16 / 17 / 18 / 33 | L | Plugs into the hook from step 2. Model question types as an extensible hierarchy with a pluggable grader. |
| 6 | SCRUM-30, SCRUM-32 | M + M | Independent; lower priority. |
| 7 | SCRUM-37 | M | Builds on SCRUM-52 (done). |
| Any time | SCRUM-26, SCRUM-39 | S + S | No dependencies; the schema is stable after SCRUM-50. |
| Blocked | SCRUM-14, SCRUM-38 | — | Need requirements. |
| Ongoing | SCRUM-40 | — | Convention, not a deliverable. |

Also worth doing: reword SCRUM-13 (URL vs upload), add a JaCoCo `check` rule so SCRUM-8 stays true, and re-measure coverage now that four services were added.
