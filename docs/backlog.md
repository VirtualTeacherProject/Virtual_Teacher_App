>Note: statuses were re-verified against git history and the code on `main` at `85c4c7c` (2026-11-18), plus SCRUM-22 (merged as 773407b), SCRUM-45 (a9b4c43) and SCRUM-28 (finished on its feature branch); details are in docs/backlog-audit.md. Status is one of DONE / PARTIAL / NOT STARTED. Evidence is the commit that did the work (merge or PR in brackets) or, for NOT STARTED, what is missing.

## Completed — 26 tickets
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
SCRUM-28	Required attendance with a tracker for each lecture; teachers mark absent students	DONE	branch feature/SCRUM-28-attendance-tracker	Approved teachers and admins open /courses/{id}/lectures/{id}/attendance (Attendance button on the lecture page) for courses with attendanceRequired, tick the absent students and save; everyone else enrolled is recorded present, saving again overwrites. Model/LectureAttendance (one row per lecture and student, PRESENT/ABSENT), AttendanceServiceImpl, AttendanceMvcController. Removed with its lecture, course or student (SCRUM-15). Migration V6. Student view is SCRUM-29.
SCRUM-31	Students rate courses 1–5 with a mandatory comment of at least 75 characters	DONE	f540e6f [merge eca7feb]	Reviews and average shown on the course page.
SCRUM-34	Teachers provide additional resources accessible only after enrollment	DONE	c69e3f8 [merge 4a50276]	Links (title, URL, optional description); no file uploads. Migration V2.
SCRUM-35	Enrolled students request additional resources using at least 100 characters	DONE	750b2e2 [merge 508cff2]	100–2000 characters; students see only their own requests. Migration V3.
SCRUM-36	Teachers manage resource requests as resolved, irrelevant or in progress and can assign them to another student	DONE	caae73e [merge a3da031]	Statuses OPEN, IN_PROGRESS, RESOLVED, IRRELEVANT; assign to another enrolled student who answers it. Migration V4.
SCRUM-41	Plain-text notes per lecture (duplicate)	DONE	a0d77bc	Closed as a duplicate of SCRUM-20; no separate work.
SCRUM-42	Lecture/course comments	DONE	10e2b28, 0ecc6c3 [PR #10 76dfd04]	
SCRUM-45	Some courses can require attendance	DONE	branch feature/SCRUM-45-attendance-required	Course.attendanceRequired (default false), set on the teacher add/edit forms and the admin course edit; shown as a badge on the course list and an Attendance field on the course page. Migration V5. The tracker itself is SCRUM-28.
SCRUM-49	Add a pull request template	DONE	4ba7128 [PR #13 5937900]	.github/pull_request_template.md. Was missing from this backlog.
SCRUM-50	Adopt Flyway and switch ddl-auto to validate	DONE	eac9a74 [7e46e27]	V1 baseline in src/main/resources/db/migration; both profiles validate. Not fixed: courses.teacher_id is still nullable in databases created before Flyway.


## Partially done
# Ticket	Description	Status	Evidence	What is missing
SCRUM-13	Teachers can create courses and upload video lectures	PARTIAL	Controller/EnrollmentMvcController, Controller/LectureMvcController	Video is a URL (Model/Lecture.videoUrl), not an upload — decide which is intended. No course ownership check (see group 9).
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
SCRUM-29	Students can see only their own attendance records	NOT STARTED	No student view yet. The records exist since SCRUM-28 (Model/LectureAttendance, LectureAttendanceRepository); needs a query by student and course and a page or course-page section.
Order: SCRUM-45 (done) -> 28 (done) -> 29.

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

### 9. Course ownership and permissions
# Ticket	Description	Status	Evidence / notes
SCRUM-14	Teacher access should be limited to the administrative part of sessions	NOT STARTED	Blocked on acceptance criteria. Probably the "a teacher manages only their own courses" boundary, which does not exist today.
SCRUM-37	A student can become an assistant teacher and edit a teacher’s course content	NOT STARTED	Single role per user (Model/UserRole), no per-course permission. Needs course ownership enforced first.

### 10. Teacher and course tools (independent)
# Ticket	Description	Status	Evidence / notes
SCRUM-30	Courses can contain groups of enrolled students	NOT STARTED	No group entity.
SCRUM-32	Teachers show tutoring availability and available times on their profiles	NOT STARTED	No public teacher profile; ProfileMvcController edits only names, picture and status.

### 11. Technical debt — done (SCRUM-50)

### 12. Documentation and engagement (any time)
# Ticket	Description	Status	Evidence / notes
SCRUM-26	Add images/diagrams of database relationships	NOT STARTED	No diagram in README.md or docs/. Can be drawn now that SCRUM-15 and SCRUM-50 are done.
SCRUM-39	Add creative Easter eggs	NOT STARTED	
SCRUM-40	Git history should clearly communicate development order, features and contributors	PARTIAL	See "Partially done".
