>Note: "Current evidence/status" columns are my own notes — verify against the code.

## Completed — 12 tickets
# Ticket	Description	Current evidence
SCRUM-5	Proper exception handling and propagation	Global MVC/REST exception handlers and custom exceptions implemented.
SCRUM-7	Swagger documentation	Springdoc/Swagger UI implemented.
SCRUM-8	Service layer must have at least 80% unit-test coverage	Completed and merged: approximately 97% instruction and 87% branch coverage.
SCRUM-9	Configure CI so tests run for commits to the main branch	GitHub Actions workflow exists and PR checks run successfully.
SCRUM-11	Administrators have all teacher functionality	Completed, tested and merged into main.
SCRUM-12	Admins can modify/delete courses and users and approve admins/teachers	Existing admin functionality plus SCRUM-11 changes satisfy this.
SCRUM-13	Teachers can create courses and upload video lectures	Implemented.
SCRUM-22	Courses can be drafts or published; students cannot see drafts	Implemented, tested and merged.
SCRUM-23	Students can comment on lectures and ask questions	Satisfied through the completed lecture-comments implementation in SCRUM-42.
SCRUM-25	Link to Swagger documentation	Implemented.
SCRUM-27	Course completion/rating eligibility based on final grade, average and course passing grade	Completion and passing-grade validation implemented.
SCRUM-42	Lecture/course comments	Implemented and merged.


## Remaining — core functionality and security
# Ticket	Description	Current status
SCRUM-6	Approved teachers receive course administration and student-search functionality	Teacher approval and course management exist, but the general student-search requirement needs implementation or verification.
SCRUM-10	Course videos are accessible only after enrollment	Recommended next ticket.
SCRUM-14	Teacher access should be limited to the administrative part of sessions	Acceptance criteria remain unclear.
SCRUM-15	Decide how deletion works for entities connected through foreign keys	Requires a consistent deletion/cascade policy.
SCRUM-16	Courses can require an entrance exam with a minimum enrollment grade; teachers create the quiz	Not implemented.
SCRUM-17	Teachers set entrance-exam start and end times	Not implemented.
SCRUM-18	Entrance exams support MCQ, MSQ and teacher-graded OEQ questions	Not implemented.
SCRUM-19	Search users by first name, last name or email	Requires implementation or verification for all three fields.
SCRUM-31	Students rate courses 1–5 with a mandatory comment of at least 75 characters	Rating foundation exists, but mandatory 75-character validation and review display remain.
SCRUM-33	Entrance exam closes automatically when its time expires	Not implemented.
Design note (SCRUM-16/17/18/33): design question types to be extensible
(future: coding questions, AI-graded open-ended answers). Don't implement
those now — just don't block them.
SCRUM-43	Registration requires email verification before users can perform transactions	Not implemented.
SCRUM-48	Courses can require completion of prerequisite courses on the same topic	Not implemented.


## Remaining — learning experience
# Ticket	Description
SCRUM-20	Users can write plain-text notes for each lecture.
SCRUM-24	Add/edit a user’s lecture notes.
SCRUM-28	Required attendance with a tracker for each lecture; teachers mark absent students.
SCRUM-29	Students can see only their own attendance records.
SCRUM-30	Courses can contain groups of enrolled students.
SCRUM-34	Teachers provide additional resources accessible only after enrollment.
SCRUM-41	Users can take plain-text notes for each lecture.
SCRUM-45	Some courses can require attendance.
SCRUM-47	Students can download and/or receive a PDF certificate after completing a course.
SCRUM-20, 24 and 41 should be implemented together. SCRUM-28, 29 and 45 form one attendance feature.

## Remaining — teacher and course tools
# Ticket	Description
SCRUM-32	Teachers show tutoring availability and available times on their profiles.
SCRUM-35	Enrolled students request additional resources using at least 100 characters.
SCRUM-36	Teachers manage resource requests as resolved, irrelevant or in progress and can assign them to another student.
SCRUM-37	A student can become an assistant teacher and edit a teacher’s course content.
SCRUM-38	Course Lessons Availability — full requirements unavailable.


## Remaining — documentation, engagement and email
# Ticket	Description
SCRUM-26	Add images/diagrams of database relationships.
SCRUM-39	Add creative Easter eggs.
SCRUM-40	Git history should clearly communicate development order, features and contributors.
SCRUM-44	Refer a Friend through an emailed registration link.
SCRUM-46	Send an enrollment-confirmation email containing essential course information.