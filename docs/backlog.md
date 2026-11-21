>Note: statuses were re-verified against git history and the code on `main` at `85c4c7c` (2026-11-18), plus SCRUM-22 (merged as 773407b), SCRUM-45 (a9b4c43), SCRUM-28 (60e28f7) SCRUM-51 (merged as 57d852f, ff5b798) and SCRUM-52 (merged as ae6cf14, follow-up on feature/SCRUM-52-show-course-teacher); details are in docs/backlog-audit.md. Status is one of DONE / PARTIAL / NOT STARTED. Evidence is the commit that did the work (merge or PR in brackets) or, for NOT STARTED, what is missing.

## Completed — 28 tickets
# Ticket	Description	Status	Evidence	Notes
SCRUM-5	Proper exception handling and propagation	DONE	9d25b93, bf00898, 1c155bb	Global MVC/REST exception handlers and custom exceptions. A few paths still throw bare RuntimeException (see audit).
SCRUM-6	Approved teachers receive course administration and student-search functionality	DONE	0a7bd61 [merge eca7feb]	/students search page.
SCRUM-7	Swagger documentation	DONE	c1b3cad, 9a837b4	Springdoc/Swagger UI, Config/OpenApiConfig.
SCRUM-8	Service layer must have at least 80% unit-test coverage	DONE	ff69e34 [PR #11]	Measured 96% instruction / 87% branch on 2026-11-16. Not enforced in CI (no JaCoCo check goal).
SCRUM-9	Configure CI so tests run for commits to the main branch	DONE	9794fa0, b01c2a3 [PR #12 d2519ae]	.github/workflows/main.yml runs on push and PR to main.
SCRUM-10	Course videos are accessible only after enrollment	DONE	c300588 [PR #15 eb3664c]	Also closed the SCRUM-22 lecture-page gap, the unauthenticated submission download and the open REST write endpoints.
SCRUM-11	Administrators have all teacher functionality	DONE	64f6e3e [PR #14 2c3b95c]	
SCRUM-12	Admins can modify/delete courses and users and approve admins/teachers	DONE	d8bf3d1, c1759c8 [merge eca7feb]	Deletes work for rows with dependants, Deactivate/Activate on the user list, teacher select on the course edit page, dashboard teacher count fixed.
SCRUM-15	Decide how deletion works for entities connected through foreign keys	DONE	d8bf3d1, e695d8b [merge eca7feb]	Course -> lectures -> submissions/comments. Deleting a user removes only their own enrollments, submissions and comments, and is refused while they still teach courses. Deactivating is the normal way to remove a user.
SCRUM-19	Search users by first name, last name or email	DONE	cfe08f6 [merge eca7feb]	Admin user list search and GET /api/users filters.
SCRUM-20	Users can write plain-text notes for each lecture	DONE	a0d77bc [merge 1490a55]	One private note per user per lecture (max 5000 characters) for users with content access.
SCRUM-22	Courses can be drafts or published; students cannot see drafts	DONE	ff69e34, c300588, 773407b	Lecture pages fixed with SCRUM-10. GET /api/courses now returns the same visible list as /courses, and GET /api/courses/{id} answers 404 for a draft the caller cannot view (CourseRestController, CourseRestControllerTest).
SCRUM-23	Students can comment on lectures and ask questions	DONE	10e2b28 [PR #10 76dfd04]	Same implementation as SCRUM-42.
SCRUM-24	Add/edit a user’s lecture notes	DONE	a0d77bc [merge 1490a55]	Covered by SCRUM-20: saving again overwrites the note, saving an empty box deletes it.
SCRUM-25	Link to Swagger documentation	DONE	4ce7935 [PR #8 eaa762f]	Link is in README only.
SCRUM-27	Course completion/rating eligibility based on final grade, average and course passing grade	DONE	d99c93e, 14b382e [PR #9 aa93b41]	Git history files it under SCRUM-21.
SCRUM-28	Required attendance with a tracker for each lecture; teachers mark absent students	DONE	60e28f7, fixed in SCRUM-51 part A	Approved teachers and admins open /courses/{id}/lectures/{id}/attendance (Attendance button on the lecture page). Since SCRUM-51 the roll call is open only for live lectures of courses with attendanceRequired, once the session has started, and lists the students enrolled at the start time; the teacher ticks the absent ones, the rest are recorded present, saving again overwrites. Self-paced lectures have no attendance. Model/LectureAttendance (one row per lecture and student, PRESENT/ABSENT), AttendanceServiceImpl, AttendanceMvcController. Removed with its lecture, course or student (SCRUM-15). Migration V6. Student view is SCRUM-29.
SCRUM-31	Students rate courses 1–5 with a mandatory comment of at least 75 characters	DONE	f540e6f [merge eca7feb]	Reviews and average shown on the course page.
SCRUM-34	Teachers provide additional resources accessible only after enrollment	DONE	c69e3f8 [merge 4a50276]	Links (title, URL, optional description); no file uploads. Migration V2.
SCRUM-35	Enrolled students request additional resources using at least 100 characters	DONE	750b2e2 [merge 508cff2]	100–2000 characters; students see only their own requests. Migration V3.
SCRUM-36	Teachers manage resource requests as resolved, irrelevant or in progress and can assign them to another student	DONE	caae73e [merge a3da031]	Statuses OPEN, IN_PROGRESS, RESOLVED, IRRELEVANT; assign to another enrolled student who answers it. Migration V4.
SCRUM-41	Plain-text notes per lecture (duplicate)	DONE	a0d77bc	Closed as a duplicate of SCRUM-20; no separate work.
SCRUM-42	Lecture/course comments	DONE	10e2b28, 0ecc6c3 [PR #10 76dfd04]	
SCRUM-45	Some courses can require attendance	DONE	a9b4c43	Course.attendanceRequired (default false), set on the teacher add/edit forms and the admin course edit; shown as a badge on the course list and an Attendance field on the course page. Migration V5. The tracker itself is SCRUM-28; attendance is taken only for its live lectures (SCRUM-51).
SCRUM-49	Add a pull request template	DONE	4ba7128 [PR #13 5937900]	.github/pull_request_template.md. Was missing from this backlog.
SCRUM-50	Adopt Flyway and switch ddl-auto to validate	DONE	eac9a74 [7e46e27]	V1 baseline in src/main/resources/db/migration; both profiles validate. Not fixed: courses.teacher_id is still nullable in databases created before Flyway.
SCRUM-51	Live lecture sessions with attendance, and self-paced lecture progression	DONE	feature/SCRUM-51-live-sessions-and-progression (part A 57d852f, part B)	Number is a placeholder until the ticket exists in Jira. Part A: Lecture.live, liveStart, meetingUrl (V7); the SCRUM-28 roll call opens only for live lectures that have started and lists the students enrolled at the start. Part B: students mark lectures as finished (Model/LectureProgress, V8); a lecture with an assignment (Lecture.assignmentRequired) needs a submission before it can be marked and counts as completed once graded; the course is COMPLETED when every lecture is completed. Course.lecturesInOrder: a lecture opens only after every earlier one is finished (managers bypass). Lecture order (Lecture.lectureOrder) with up/down buttons for managers; /courses/{id}/progress shows each student's progress. Courses without assignments can be rated once completed. CourseAccessService.ensureCanManageCourse is the single place the course-ownership ticket will tighten.
SCRUM-52	Course ownership: only the course's own teacher (and admins) can manage a course	DONE	ae6cf14, follow-up feature/SCRUM-52-show-course-teacher	The course list and course page show each course's teacher, with a "Your course" badge for the owner; the Edit button on course cards appears only on courses the teacher manages. Placeholder number until the ticket exists in Jira. CourseAccessService.canManageCourse / ensureCanManageCourse: an admin, or the approved teacher in Course.teacher. Enforced on course edit/publish and the enrolled-students list, lecture add/edit/move, submission list (all submissions), download and grading, attendance, the progress page, course resources, resource-request list/status/assign, and the REST course PUT/DELETE and lecture POST/PUT/DELETE (403 JSON). Per-course buttons use a canManageCourse model attribute, so other teachers no longer see them. Creating courses and student search stay open to every approved teacher. Related: closes the ownership gap of SCRUM-13; is probably what SCRUM-14 asks for (confirm in Jira); is the prerequisite for SCRUM-37 (assistant teachers would extend canManageCourse). Follow-ups: (1) decision 2026-11-21: viewing stays open, so any approved teacher can still open other teachers' drafts and protected content and bypasses in-order locks; to make it owner-only, switch canViewCourse, canAccessCourseContent and the lock bypass in LectureProgressServiceImpl to canManageCourse. (2) POST /api/courses takes the teacher id from the request body, so a teacher can create a course owned by another teacher; it should use the caller unless an admin.


## Partially done
# Ticket	Description	Status	Evidence	What is missing
SCRUM-13	Teachers can create courses and upload video lectures	PARTIAL	Controller/EnrollmentMvcController, Controller/LectureMvcController	Video is a URL (Model/Lecture.videoUrl), not an upload — decide which is intended. Course ownership is enforced since SCRUM-52.
SCRUM-40	Git history should clearly communicate development order, features and contributors	PARTIAL	git log	24 of 139 non-merge commits start with a ticket key (all recent work does); contributors appear under four author names. Ongoing convention; history must not be rewritten.


## Remaining — grouped by feature, in proposed order
All tickets below are NOT STARTED: no code, entity, dependency or commit for them exists on main (checked 2026-11-18).

### 1. Access to course content (shared enrollment guard)
# Ticket	Description	Status	Evidence / notes
SCRUM-38	Course Lessons Availability — full requirements unavailable	NOT STARTED	Blocked on requirements. If it means scheduled release of lectures, it belongs to Service/CourseAccessServiceImpl.
New entities must follow the SCRUM-15 deletion policy (ContentCleanupService) and come with a Flyway migration (SCRUM-50).

### 2. User search — done (SCRUM-19, SCRUM-6)

### 3. Ratings — done (SCRUM-31)

### 4. Deletion policy — done (SCRUM-15)

### 5. Lecture notes — done (SCRUM-20, SCRUM-24; SCRUM-41 duplicate)

### 6. Attendance
# Ticket	Description	Status	Evidence / notes
SCRUM-29	Students can see only their own attendance records	NOT STARTED	No student view yet. The records exist since SCRUM-28 (Model/LectureAttendance, LectureAttendanceRepository); needs a query by student and course and a page or course-page section. Attendance exists only for live lectures (SCRUM-51 part A).
Order: SCRUM-45 (done) -> 28 (done) -> 51 (done) -> 29.

Design note (SCRUM-51, from the 2026-11-20 discussion after SCRUM-28, updated 2026-11-21; both parts are done):
The SCRUM-28 roll call had no basis: lectures had no date or time, so a teacher could
not know who was absent, and saving marked everyone present. Enrolment is not attendance.
Decision: attendance belongs only to live sessions. Self-paced courses have no
attendance; what matters there is completing the lectures, optionally in order.
- Part A, live sessions (DONE). A lecture can be marked live with a start time and an
  optional meeting link (V7). The roll call opens only when the course requires
  attendance (SCRUM-45 flag, kept as a manual setting), the lecture is live and the
  session has started; it lists the students enrolled at the start time, so later
  enrolments are not recorded as present. Later option: a check-in code students enter
  during the session.
- Part B, self-paced progression (DONE). Decisions of 2026-11-21: "watched" is a
  "Mark as finished" button (YouTube videos are placeholders, so the end of a video is
  not detected); a lecture with an assignment is completed only once it is graded, but in
  an in-order course the next lecture already opens once the assignment is submitted and
  the lecture marked, so slow grading does not block students; lecture order is a
  per-course setting (off by default); teachers and admins are never locked out. A live
  lecture is finished the same way as any other.
- Existing data (V8): lectures are numbered in id order, a lecture whose
  assignment_file_path is not blank has an assignment, and every lecture with a graded
  submission counts as finished, so students who had completed a course stay completed.
- Related: may overlap with SCRUM-38 (Course Lessons Availability) if that means lecture
  unlocking or scheduled release.

### 7. Enrollment preconditions (shared eligibility check before enrolling)
# Ticket	Description	Status	Evidence / notes
SCRUM-48	Courses can require completion of prerequisite courses on the same topic	NOT STARTED	No prerequisite relation on Course. Build the eligibility hook here, with the simpler rule.
SCRUM-16	Courses can require an entrance exam with a minimum enrollment grade; teachers create the quiz	NOT STARTED	No exam, quiz or question entity.
SCRUM-17	Teachers set entrance-exam start and end times	NOT STARTED	Part of SCRUM-16.
SCRUM-18	Entrance exams support MCQ, MSQ and teacher-graded OEQ questions	NOT STARTED	Part of SCRUM-16.
SCRUM-33	Entrance exam closes automatically when its time expires	NOT STARTED	A small rule on top of SCRUM-17.
Design note (SCRUM-16/17/18/33): design question types to be extensible
(future: coding questions, AI-graded open-ended answers). Don't implement
those now — just don't block them.
Order: SCRUM-48 first; the exam tickets (16/17/18/33) are one large epic and can wait until after the email group.

### 8. Email (needs an email provider)
# Ticket	Description	Status	Evidence / notes
SCRUM-43	Registration requires email verification before users can perform transactions	NOT STARTED	No mail dependency in pom.xml, no token entity. Owns the mail setup and the token mechanism.
SCRUM-46	Send an enrollment-confirmation email containing essential course information	NOT STARTED	Needs SCRUM-43's mail setup.
SCRUM-44	Refer a Friend through an emailed registration link	NOT STARTED	Reuses SCRUM-43's tokens.
SCRUM-47	Students can download and/or receive a PDF certificate after completing a course	NOT STARTED	No PDF library in pom.xml. Download needs only a PDF library; "receive" needs the mail setup.
Order: SCRUM-43 -> 46 -> 44, then SCRUM-47.

### 9. Course permissions (ownership done in SCRUM-52)
# Ticket	Description	Status	Evidence / notes
SCRUM-14	Teacher access should be limited to the administrative part of sessions	NOT STARTED	Blocked on acceptance criteria. If it means "a teacher manages only their own courses", SCRUM-52 has done it and this can be closed; confirm in Jira.
SCRUM-37	A student can become an assistant teacher and edit a teacher’s course content	NOT STARTED	Single role per user (Model/UserRole), no per-course permission. Course ownership exists since SCRUM-52: an assistant would be an extra per-course permission checked in CourseAccessService.canManageCourse.

### 10. Teacher and course tools (independent)
# Ticket	Description	Status	Evidence / notes
SCRUM-30	Courses can contain groups of enrolled students	NOT STARTED	No group entity.
SCRUM-32	Teachers show tutoring availability and available times on their profiles	NOT STARTED	No public teacher profile; ProfileMvcController edits only names, picture and status.

### 11. Technical debt — done (SCRUM-50)
Known gap (SCRUM-51): a lecture saved without LectureServiceImpl.createLectures (e.g. the unused saveLecture) gets lectureOrder 0 and sorts first instead of last; nothing in the app does this today. Fix and reasoning in docs/backlog-audit.md, "Issues found outside the ticket list", issue 9.

### 12. Documentation and engagement (any time)
# Ticket	Description	Status	Evidence / notes
SCRUM-26	Add images/diagrams of database relationships	NOT STARTED	No diagram in README.md or docs/. Can be drawn now that SCRUM-15 and SCRUM-50 are done.
SCRUM-39	Add creative Easter eggs	NOT STARTED	
SCRUM-40	Git history should clearly communicate development order, features and contributors	PARTIAL	See "Partially done".
