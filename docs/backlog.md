>Note: statuses were verified against the code in docs/backlog-audit.md (2026-11-16). Evidence and file references live there; this file only tracks grouping, status and order. Tickets finished after the audit are marked in the Completed list with the branch they are on.

## Completed — 15 tickets
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
SCRUM-19	Search users by first name, last name or email	Done on feature/SCRUM-19-user-search (not merged yet). Admin user list search and GET /api/users filters.
SCRUM-6	Approved teachers receive course administration and student-search functionality	Done on feature/SCRUM-6-teacher-student-search (not merged yet). /students search page.
SCRUM-31	Students rate courses 1–5 with a mandatory comment of at least 75 characters	Done on feature/SCRUM-31-mandatory-course-review (not merged yet). Reviews and average shown on the course page.
SCRUM-15	Decide how deletion works for entities connected through foreign keys	Done on feature/SCRUM-15-deletion-policy (not merged yet). Course -> lectures -> submissions/comments. Deleting a user removes only their own enrollments, submissions and comments, and is refused while they still teach courses (reassign first). Deactivating is the normal way to remove a user.
SCRUM-12	Admins can modify/delete courses and users and approve admins/teachers	Done on feature/SCRUM-15-deletion-policy (not merged yet). Deletes work for rows with dependants, Deactivate/Activate on the user list, teacher select on the course edit page, dashboard teacher count fixed.


## Partially done — previously listed as completed
# Ticket	Description	What is missing
SCRUM-13	Teachers can create courses and upload video lectures	Video is a URL, not an upload — decide which is intended. No course ownership check (see "Course ownership and permissions").
SCRUM-22	Courses can be drafts or published; students cannot see drafts	Lecture pages fixed with SCRUM-10. Remaining: GET /api/courses still returns drafts to any logged-in user.


## Remaining — grouped by feature, in proposed order

### 1. Access to course content (shared enrollment guard)
# Ticket	Description	Status
SCRUM-34	Teachers provide additional resources accessible only after enrollment	Not started. Next in this group. Reuses the SCRUM-10 guard (CourseAccessService).
SCRUM-35	Enrolled students request additional resources using at least 100 characters	Not started. Needs SCRUM-34.
SCRUM-36	Teachers manage resource requests as resolved, irrelevant or in progress and can assign them to another student	Not started. Needs SCRUM-35.
SCRUM-38	Course Lessons Availability — full requirements unavailable	Blocked on requirements. If it means scheduled release of lectures, it belongs to this guard.
Order: SCRUM-34 -> 35 -> 36. New entities must follow the SCRUM-15 deletion policy (ContentCleanupService).

### 2. User search — done (SCRUM-19, SCRUM-6)

### 3. Ratings — done (SCRUM-31)

### 4. Deletion policy — done (SCRUM-15)

### 5. Lecture notes
# Ticket	Description	Status
SCRUM-20	Users can write plain-text notes for each lecture	Not started.
SCRUM-24	Add/edit a user’s lecture notes	Not started. Same screen as SCRUM-20.
SCRUM-41	Users can take plain-text notes for each lecture	Duplicate of SCRUM-20 — close as duplicate.
Implement SCRUM-20 and 24 as one piece of work.

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

### 11. Technical debt
# Ticket	Description	Status
NEW (no Jira key yet)	Adopt Flyway and switch ddl-auto to validate	Not started. Today both profiles use ddl-auto=update and there is no migration tool, so a schema change that relaxes or removes something cannot reach existing databases. Scope: add Flyway, write a V1 baseline of the current schema that runs on H2 and PostgreSQL, set baseline-on-migrate for existing databases, switch ddl-auto to validate. Do this before the next ticket that adds tables (notes, resources, attendance, exams).

### 12. Documentation and engagement (any time)
# Ticket	Description	Status
SCRUM-26	Add images/diagrams of database relationships	Not started. Best drawn after SCRUM-15.
SCRUM-39	Add creative Easter eggs	Not started.
SCRUM-40	Git history should clearly communicate development order, features and contributors	Ongoing convention, not a deliverable.
