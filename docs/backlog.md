>Note: statuses were verified against the code in docs/backlog-audit.md (2026-11-16). Evidence and file references live there; this file only tracks grouping, status and order. Tickets finished after the audit are listed in Completed; one that is not merged yet names the branch it is on.

## Completed — 21 tickets
# Ticket	Description	Notes
SCRUM-5	Proper exception handling and propagation	Global MVC/REST exception handlers and custom exceptions implemented.
SCRUM-7	Swagger documentation	Springdoc/Swagger UI implemented.
SCRUM-8	Service layer must have at least 80% unit-test coverage	Measured 96% instruction / 87% branch. Not enforced in CI (no JaCoCo check rule).
SCRUM-9	Configure CI so tests run for commits to the main branch	GitHub Actions workflow runs on push and PR to main.
SCRUM-11	Administrators have all teacher functionality	Completed, tested and merged into main.
SCRUM-23	Students can comment on lectures and ask questions	Satisfied by SCRUM-42 (same implementation).
SCRUM-25	Link to Swagger documentation	Link is in README only.
SCRUM-27	Course completion/rating eligibility based on final grade, average and course passing grade	Implemented (git history files it under SCRUM-21).
SCRUM-42	Lecture/course comments	Implemented and merged.
SCRUM-10	Course videos are accessible only after enrollment	Merged. Also closed the SCRUM-22 lecture-page gap, the unauthenticated submission download and the open REST write endpoints.
SCRUM-19	Search users by first name, last name or email	Merged. Admin user list search and GET /api/users filters.
SCRUM-6	Approved teachers receive course administration and student-search functionality	Merged. /students search page.
SCRUM-31	Students rate courses 1–5 with a mandatory comment of at least 75 characters	Merged. Reviews and average shown on the course page.
SCRUM-15	Decide how deletion works for entities connected through foreign keys	Merged. Course -> lectures -> submissions/comments. Deleting a user removes only their own enrollments, submissions and comments, and is refused while they still teach courses (reassign first). Deactivating is the normal way to remove a user.
SCRUM-12	Admins can modify/delete courses and users and approve admins/teachers	Merged. Deletes work for rows with dependants, Deactivate/Activate on the user list, teacher select on the course edit page, dashboard teacher count fixed.
SCRUM-20	Users can write plain-text notes for each lecture	Merged. One private note per user per lecture (max 5000 characters) on the lecture page, for users with content access. Notes follow the SCRUM-15 deletion policy.
SCRUM-24	Add/edit a user’s lecture notes	Covered by SCRUM-20: saving again overwrites the note, saving an empty box deletes it. One note per lecture is enough for now.
SCRUM-50	Adopt Flyway and switch ddl-auto to validate	Merged. V1 baseline for H2 and PostgreSQL in src/main/resources/db/migration; both profiles validate. Existing databases are baselined at version 0 and V1 creates only missing tables. Every schema change now needs a new migration. Not fixed: courses.teacher_id is still nullable in databases created before Flyway.
SCRUM-34	Teachers provide additional resources accessible only after enrollment	Merged. Resources are links (title, URL, optional description) on the course page; approved teachers and admins add and remove them, enrolled students see them. Uploaded files are not supported. Migration V2.
SCRUM-35	Enrolled students request additional resources using at least 100 characters	Merged. Students enrolled in a published course send a request (100–2000 characters) from the course page and see only their own; approved teachers and admins see all requests of the course, read-only. Status is OPEN for now. Migration V3.
SCRUM-36	Teachers manage resource requests as resolved, irrelevant or in progress and can assign them to another student	Merged. Statuses OPEN, IN_PROGRESS, RESOLVED, IRRELEVANT. An approved teacher or admin assigns a request to another student enrolled in the course; that student (only) sees the request on the course page and answers it with text, links and notes (up to 2000 characters). The author sees the assignee and the answer. Assigning moves OPEN to IN_PROGRESS; changing or removing the assignee removes the previous answer; closed requests cannot be reassigned or answered. Deleting the assignee keeps the request but clears the assignment and answer. Migration V4.


## Partially done — previously listed as completed
# Ticket	Description	What is missing
SCRUM-13	Teachers can create courses and upload video lectures	Video is a URL, not an upload — decide which is intended. No course ownership check (see "Course ownership and permissions").
SCRUM-22	Courses can be drafts or published; students cannot see drafts	Lecture pages fixed with SCRUM-10. Remaining: GET /api/courses still returns drafts to any logged-in user.


## Remaining — grouped by feature, in proposed order

### 1. Access to course content (shared enrollment guard)
# Ticket	Description	Status
SCRUM-38	Course Lessons Availability — full requirements unavailable	Blocked on requirements. If it means scheduled release of lectures, it belongs to this guard.
Order: SCRUM-34, 35 and 36 done; only the blocked SCRUM-38 is left. New entities must follow the SCRUM-15 deletion policy (ContentCleanupService) and come with a Flyway migration (SCRUM-50).

### 2. User search — done (SCRUM-19, SCRUM-6)

### 3. Ratings — done (SCRUM-31)

### 4. Deletion policy — done (SCRUM-15)

### 5. Lecture notes — done (SCRUM-20, SCRUM-24). SCRUM-41 closed as a duplicate of SCRUM-20.

### 6. Attendance
# Ticket	Description	Status
SCRUM-45	Some courses can require attendance	Not started. The per-course flag.
SCRUM-28	Required attendance with a tracker for each lecture; teachers mark absent students	Not started. Needs SCRUM-45.
SCRUM-29	Students can see only their own attendance records	Not started. Needs SCRUM-28.
Order: SCRUM-45 -> 28 -> 29.

### 7. Enrollment preconditions (shared eligibility check before enrolling)
# Ticket	Description	Status
SCRUM-48	Courses can require completion of prerequisite courses on the same topic	Not started. Build the eligibility hook here, with the simpler rule.
SCRUM-16	Courses can require an entrance exam with a minimum enrollment grade; teachers create the quiz	Not started.
SCRUM-17	Teachers set entrance-exam start and end times	Not started.
SCRUM-18	Entrance exams support MCQ, MSQ and teacher-graded OEQ questions	Not started.
SCRUM-33	Entrance exam closes automatically when its time expires	Not started. A small rule on top of SCRUM-17.
Design note (SCRUM-16/17/18/33): design question types to be extensible
(future: coding questions, AI-graded open-ended answers). Don't implement
those now — just don't block them.
Order: SCRUM-48 first; the exam tickets (16/17/18/33) are one large epic and can wait until after the email group.

### 8. Email (needs an email provider)
# Ticket	Description	Status
SCRUM-43	Registration requires email verification before users can perform transactions	Not started. Owns the mail setup and the token mechanism.
SCRUM-46	Send an enrollment-confirmation email containing essential course information	Not started. Needs SCRUM-43's mail setup.
SCRUM-44	Refer a Friend through an emailed registration link	Not started. Reuses SCRUM-43's tokens.
SCRUM-47	Students can download and/or receive a PDF certificate after completing a course	Not started. Download needs only a PDF library; "receive" needs the mail setup.
Order: SCRUM-43 -> 46 -> 44, then SCRUM-47.

### 9. Course ownership and permissions
# Ticket	Description	Status
SCRUM-14	Teacher access should be limited to the administrative part of sessions	Blocked on acceptance criteria. Probably the "a teacher manages only their own courses" boundary, which does not exist today.
SCRUM-37	A student can become an assistant teacher and edit a teacher’s course content	Not started. Needs course ownership enforced first.

### 10. Teacher and course tools (independent)
# Ticket	Description	Status
SCRUM-30	Courses can contain groups of enrolled students	Not started.
SCRUM-32	Teachers show tutoring availability and available times on their profiles	Not started.

### 11. Technical debt — done (SCRUM-50)

### 12. Documentation and engagement (any time)
# Ticket	Description	Status
SCRUM-26	Add images/diagrams of database relationships	Not started. Best drawn after SCRUM-15.
SCRUM-39	Add creative Easter eggs	Not started.
SCRUM-40	Git history should clearly communicate development order, features and contributors	Ongoing convention, not a deliverable.
