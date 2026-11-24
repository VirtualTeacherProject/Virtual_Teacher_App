package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceStatus;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.CourseResource;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureAttendance;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureComment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureNote;
import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseResourceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureAttendanceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureCommentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureNoteRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.ResourceRequestRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.ContentCleanupServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.Service.CourseServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.Service.EnrollmentServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.LectureServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.Service.UserServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityInUseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SCRUM-15: the deletion policy against a real (in-memory) database, so
 * foreign keys and JPA cascades are exercised for real.
 *
 * Fixture: one course taught by "teacher" with two lectures. "anna" and
 * "boris" are enrolled; each has a submission and a comment on lecture 1.
 * A second course taught by the same teacher has one lecture with a
 * submission from anna. Notes (SCRUM-20): anna and boris on lecture 1, anna on
 * lecture 2 and on the other course's lecture. Attendance (SCRUM-28): anna and
 * boris on lecture 1, anna on the other course's lecture.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({CourseServiceImpl.class, LectureServiceImpl.class, UserServiceImpl.class,
        EnrollmentServiceImpl.class, ContentCleanupServiceImpl.class,
        com.MarianFinweFeanor.Virtual_Teacher.Service.CoursePrerequisiteServiceImpl.class,   // SCRUM-48
        com.MarianFinweFeanor.Virtual_Teacher.Service.CourseAccessServiceImpl.class})
@TestPropertySource(properties = "app.upload.dir=target/test-uploads")
class DeletionPolicyIntegrationTest {

    @Autowired private TestEntityManager em;

    @Autowired private CourseService courseService;
    @Autowired private LectureService lectureService;
    @Autowired private UserService userService;

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private LectureRepository lectureRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private LectureCommentRepository commentRepository;
    @Autowired private LectureNoteRepository noteRepository;
    @Autowired private CourseResourceRepository resourceRepository;
    @Autowired private ResourceRequestRepository requestRepository;
    @Autowired private LectureAttendanceRepository attendanceRepository;

    @MockBean private PasswordEncoder passwordEncoder;

    private User teacher;
    private User anna;
    private User boris;
    private Course course;
    private Course otherCourse;
    private Lecture lecture1;
    private Lecture lecture2;
    private Lecture otherLecture;

    @BeforeEach
    void setUp() {
        teacher = user("teacher@test.com", UserRole.TEACHER);
        anna = user("anna@test.com", UserRole.STUDENT);
        boris = user("boris@test.com", UserRole.STUDENT);

        course = course("Java", teacher);
        otherCourse = course("Python", teacher);

        lecture1 = lecture("Intro", course);
        lecture2 = lecture("Loops", course);
        otherLecture = lecture("Basics", otherCourse);

        enroll(anna, course);
        enroll(boris, course);
        enroll(anna, otherCourse);

        submission(anna, lecture1);
        submission(boris, lecture1);
        submission(anna, otherLecture);

        comment(anna, lecture1);
        comment(boris, lecture1);
        comment(teacher, lecture2);

        note(anna, lecture1);
        note(boris, lecture1);
        note(anna, lecture2);
        note(anna, otherLecture);

        // SCRUM-34: two resources on the course, one on the other course
        resource(course);
        resource(course);
        resource(otherCourse);

        // SCRUM-35: anna and boris on the course, anna on the other course
        request(anna, course);
        request(boris, course);
        request(anna, otherCourse);

        attendance(anna, lecture1, AttendanceStatus.PRESENT);
        attendance(boris, lecture1, AttendanceStatus.ABSENT);
        attendance(anna, otherLecture, AttendanceStatus.PRESENT);

        flushAndClear();
    }

    // --- deleting a course ---------------------------------------------------

    @Test
    void deletingACourse_shouldRemoveItsLecturesAndTheirContent_butNoUsers() {
        courseService.deleteCourse(course.getCourseId());
        flushAndClear();

        assertThat(courseRepository.findById(course.getCourseId())).isEmpty();
        assertThat(lectureRepository.findAllByCourse_CourseId(course.getCourseId())).isEmpty();
        assertThat(enrollmentRepository.findByCourse_CourseId(course.getCourseId())).isEmpty();

        // every user is still there
        assertThat(userRepository.count()).isEqualTo(3);

        // the other course is untouched
        assertThat(courseRepository.findById(otherCourse.getCourseId())).isPresent();
        assertThat(lectureRepository.count()).isEqualTo(1);
        assertThat(assignmentRepository.count()).isEqualTo(1);
        assertThat(enrollmentRepository.count()).isEqualTo(1);
        assertThat(commentRepository.count()).isZero();
        assertThat(noteRepository.count()).isEqualTo(1);
        assertThat(resourceRepository.findByCourse_CourseId(course.getCourseId())).isEmpty();
        assertThat(resourceRepository.count()).isEqualTo(1);
        assertThat(requestRepository.findByCourse_CourseId(course.getCourseId())).isEmpty();
        assertThat(requestRepository.count()).isEqualTo(1);
        assertThat(attendanceRepository.findByLecture_Course_CourseId(course.getCourseId())).isEmpty();
        assertThat(attendanceRepository.count()).isEqualTo(1);
    }

    @Test
    void deletingACourse_shouldRemoveItsPrerequisiteLinks_onBothSides() {   // SCRUM-48
        Course advanced = course("Advanced Java", teacher);
        prerequisite(course, otherCourse, false);     // the deleted course has a prerequisite
        prerequisite(advanced, course, true);         // and is the prerequisite of another course
        prerequisite(advanced, otherCourse, false);   // unrelated link stays
        flushAndClear();

        courseService.deleteCourse(course.getCourseId());
        flushAndClear();

        assertThat(courseRepository.findById(advanced.getCourseId())).isPresent();
        assertThat(courseRepository.findById(otherCourse.getCourseId())).isPresent();
        assertThat(em.getEntityManager()
                .createQuery("select p.prerequisiteCourse.title from CoursePrerequisite p", String.class)
                .getResultList()).containsExactly("Python");
    }

    private void prerequisite(Course of, Course prerequisite, boolean required) {
        com.MarianFinweFeanor.Virtual_Teacher.Model.CoursePrerequisite link =
                new com.MarianFinweFeanor.Virtual_Teacher.Model.CoursePrerequisite();
        link.setCourse(em.find(Course.class, of.getCourseId()));
        link.setPrerequisiteCourse(em.find(Course.class, prerequisite.getCourseId()));
        link.setRequired(required);
        em.persist(link);
    }

    // --- deleting a lecture --------------------------------------------------

    @Test
    void deletingALecture_shouldRemoveItsSubmissionsAndComments_only() {
        lectureService.delete(lecture1.getLectureId());
        flushAndClear();

        assertThat(lectureRepository.findById(lecture1.getLectureId())).isEmpty();
        assertThat(assignmentRepository.findByLecture_LectureId(lecture1.getLectureId())).isEmpty();
        assertThat(commentRepository.findByLecture_LectureId(lecture1.getLectureId())).isEmpty();
        assertThat(noteRepository.findByLecture_LectureId(lecture1.getLectureId())).isEmpty();
        assertThat(attendanceRepository.findByLecture_LectureId(lecture1.getLectureId())).isEmpty();
        assertThat(attendanceRepository.count()).isEqualTo(1);

        // the course, its other lecture, the enrollments and the users stay
        assertThat(courseRepository.findById(course.getCourseId())).isPresent();
        assertThat(lectureRepository.findById(lecture2.getLectureId())).isPresent();
        assertThat(commentRepository.count()).isEqualTo(1);
        assertThat(noteRepository.count()).isEqualTo(2);
        assertThat(resourceRepository.count()).isEqualTo(3);
        assertThat(requestRepository.count()).isEqualTo(3);
        assertThat(assignmentRepository.count()).isEqualTo(1);
        assertThat(enrollmentRepository.count()).isEqualTo(3);
        assertThat(userRepository.count()).isEqualTo(3);
    }

    // --- deleting a user -----------------------------------------------------

    @Test
    void deletingAStudent_shouldRemoveOnlyTheirOwnEnrollmentsSubmissionsAndComments() {
        userService.deleteUserById(anna.getUserId());
        flushAndClear();

        assertThat(userRepository.findById(anna.getUserId())).isEmpty();
        assertThat(enrollmentRepository.findByStudent_Email("anna@test.com")).isEmpty();
        assertThat(assignmentRepository.findByStudent_UserId(anna.getUserId())).isEmpty();
        assertThat(commentRepository.findByAuthor_UserId(anna.getUserId())).isEmpty();
        assertThat(noteRepository.findByUser_UserId(anna.getUserId())).isEmpty();
        assertThat(requestRepository.findByStudent_UserId(anna.getUserId())).isEmpty();
        assertThat(attendanceRepository.findByStudent_UserId(anna.getUserId())).isEmpty();
        assertThat(attendanceRepository.count()).isEqualTo(1);   // boris's record stays

        // nothing that belongs to a course or to someone else is touched
        assertThat(courseRepository.count()).isEqualTo(2);
        assertThat(lectureRepository.count()).isEqualTo(3);
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(enrollmentRepository.count()).isEqualTo(1);
        assertThat(assignmentRepository.count()).isEqualTo(1);
        assertThat(commentRepository.count()).isEqualTo(2);
        assertThat(noteRepository.count()).isEqualTo(1);
        assertThat(resourceRepository.count()).isEqualTo(3);
        assertThat(requestRepository.count()).isEqualTo(1);
    }

    @Test
    void deletingAStudent_shouldKeepRequestsAssignedToThem_withoutAssignmentOrAnswer() {
        // SCRUM-36: boris was asked to answer anna's request
        ResourceRequest annas = requestRepository.findByStudent_UserId(anna.getUserId()).stream()
                .filter(r -> r.getCourse().getCourseId().equals(course.getCourseId()))
                .findFirst().orElseThrow();
        annas.setAssignee(boris);
        annas.setStatus(ResourceRequest.STATUS_IN_PROGRESS);
        annas.setResponse("See the official docs.");
        annas.setRespondedAt(LocalDateTime.now());
        requestRepository.save(annas);
        flushAndClear();

        userService.deleteUserById(boris.getUserId());
        flushAndClear();

        ResourceRequest kept = requestRepository.findById(annas.getRequestId()).orElseThrow();
        assertThat(kept.getAssignee()).isNull();
        assertThat(kept.getResponse()).isNull();
        assertThat(kept.getRespondedAt()).isNull();
        assertThat(kept.getStatus()).isEqualTo(ResourceRequest.STATUS_OPEN);
        // boris's own request is gone, anna's two stay
        assertThat(requestRepository.count()).isEqualTo(2);
    }

    @Test
    void deletingATeacher_shouldBeRefused_whileTheyStillTeachCourses() {
        assertThatThrownBy(() -> userService.deleteUserById(teacher.getUserId()))
                .isInstanceOf(EntityInUseException.class)
                .hasMessageContaining("2 courses")
                .hasMessageContaining("Reassign");
        flushAndClear();

        // nothing at all was removed
        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(courseRepository.count()).isEqualTo(2);
        assertThat(lectureRepository.count()).isEqualTo(3);
        assertThat(enrollmentRepository.count()).isEqualTo(3);
        assertThat(assignmentRepository.count()).isEqualTo(3);
        assertThat(commentRepository.count()).isEqualTo(3);
        assertThat(noteRepository.count()).isEqualTo(4);
        assertThat(requestRepository.count()).isEqualTo(3);
        assertThat(attendanceRepository.count()).isEqualTo(3);
    }

    @Test
    void deletingATeacher_shouldWork_onceTheirCoursesAreReassigned() {
        User replacement = user("replacement@test.com", UserRole.TEACHER);
        for (Course taught : courseRepository.findByTeacher_UserId(teacher.getUserId())) {
            taught.setTeacher(replacement);
        }
        flushAndClear();

        userService.deleteUserById(teacher.getUserId());
        flushAndClear();

        assertThat(userRepository.findById(teacher.getUserId())).isEmpty();

        // courses and lectures stay, now with the new teacher
        assertThat(courseRepository.count()).isEqualTo(2);
        assertThat(lectureRepository.count()).isEqualTo(3);
        assertThat(courseRepository.findById(course.getCourseId()).orElseThrow()
                .getTeacher().getEmail()).isEqualTo("replacement@test.com");

        // students and their work are untouched; only the teacher's own comment is gone
        assertThat(enrollmentRepository.count()).isEqualTo(3);
        assertThat(assignmentRepository.count()).isEqualTo(3);
        assertThat(commentRepository.count()).isEqualTo(2);
    }

    // --- fixture helpers -----------------------------------------------------

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private User user(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("secret");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setRole(role);
        user.setStatus("ACTIVE");
        return em.persist(user);
    }

    private Course course(String title, User teacher) {
        Course c = new Course();
        c.setTitle(title);
        c.setTopic("Programming");
        c.setDescription("Description");
        c.setStatus("PUBLISHED");
        c.setTeacher(teacher);
        return em.persist(c);
    }

    private Lecture lecture(String title, Course course) {
        Lecture l = new Lecture();
        l.setTitle(title);
        l.setDescription("Description");
        l.setVideoUrl("https://example.com/video");
        l.setAssignmentFilePath("task.txt");
        l.setCourse(course);
        return em.persist(l);
    }

    private void enroll(User student, Course course) {
        Enrollment e = new Enrollment();
        e.setStudent(student);
        e.setCourse(course);
        em.persist(e);
    }

    private void submission(User student, Lecture lecture) {
        Assignment a = new Assignment();
        a.setStudent(student);
        a.setLecture(lecture);
        a.setSubmissionFilePath("target/test-uploads/does-not-exist.txt");
        a.setSubmittedAt(LocalDateTime.now());
        em.persist(a);
    }

    private void comment(User author, Lecture lecture) {
        LectureComment c = new LectureComment();
        c.setAuthor(author);
        c.setLecture(lecture);
        c.setComment("A comment");
        c.setCreatedAt(LocalDateTime.now());
        em.persist(c);
    }

    private void note(User user, Lecture lecture) {
        LectureNote n = new LectureNote();
        n.setUser(user);
        n.setLecture(lecture);
        n.setContent("A note");
        n.setUpdatedAt(LocalDateTime.now());
        em.persist(n);
    }

    private void attendance(User student, Lecture lecture, AttendanceStatus status) {
        LectureAttendance a = new LectureAttendance();
        a.setStudent(student);
        a.setLecture(lecture);
        a.setStatus(status);
        a.setRecordedAt(LocalDateTime.now());
        em.persist(a);
    }

    private void resource(Course course) {
        CourseResource r = new CourseResource();
        r.setCourse(course);
        r.setTitle("Slides");
        r.setUrl("https://example.com/slides");
        r.setCreatedAt(LocalDateTime.now());
        em.persist(r);
    }

    private void request(User student, Course course) {
        ResourceRequest r = new ResourceRequest();
        r.setStudent(student);
        r.setCourse(course);
        r.setMessage("Please share more exercises. ".repeat(4));
        r.setStatus(ResourceRequest.STATUS_OPEN);
        r.setCreatedAt(LocalDateTime.now());
        em.persist(r);
    }
}
