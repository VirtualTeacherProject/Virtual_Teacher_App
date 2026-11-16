package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureComment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureCommentRepository;
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

/**
 * SCRUM-15: the deletion policy against a real (in-memory) database, so
 * foreign keys and JPA cascades are exercised for real.
 *
 * Fixture: one course taught by "teacher" with two lectures. "anna" and
 * "boris" are enrolled; each has a submission and a comment on lecture 1.
 * A second course taught by the same teacher has one lecture with a
 * submission from anna.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({CourseServiceImpl.class, LectureServiceImpl.class, UserServiceImpl.class,
        EnrollmentServiceImpl.class, ContentCleanupServiceImpl.class})
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
    }

    // --- deleting a lecture --------------------------------------------------

    @Test
    void deletingALecture_shouldRemoveItsSubmissionsAndComments_only() {
        lectureService.delete(lecture1.getLectureId());
        flushAndClear();

        assertThat(lectureRepository.findById(lecture1.getLectureId())).isEmpty();
        assertThat(assignmentRepository.findByLecture_LectureId(lecture1.getLectureId())).isEmpty();
        assertThat(commentRepository.findByLecture_LectureId(lecture1.getLectureId())).isEmpty();

        // the course, its other lecture, the enrollments and the users stay
        assertThat(courseRepository.findById(course.getCourseId())).isPresent();
        assertThat(lectureRepository.findById(lecture2.getLectureId())).isPresent();
        assertThat(commentRepository.count()).isEqualTo(1);
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

        // nothing that belongs to a course or to someone else is touched
        assertThat(courseRepository.count()).isEqualTo(2);
        assertThat(lectureRepository.count()).isEqualTo(3);
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(enrollmentRepository.count()).isEqualTo(1);
        assertThat(assignmentRepository.count()).isEqualTo(1);
        assertThat(commentRepository.count()).isEqualTo(2);
    }

    @Test
    void deletingATeacher_shouldKeepTheirCoursesAndLectures_withoutATeacher() {
        userService.deleteUserById(teacher.getUserId());
        flushAndClear();

        assertThat(userRepository.findById(teacher.getUserId())).isEmpty();

        // courses and lectures survive, only the teacher link is cleared
        assertThat(courseRepository.count()).isEqualTo(2);
        assertThat(lectureRepository.count()).isEqualTo(3);
        assertThat(courseRepository.findById(course.getCourseId()).orElseThrow().getTeacher()).isNull();
        assertThat(courseRepository.findById(otherCourse.getCourseId()).orElseThrow().getTeacher()).isNull();

        // students and their work are untouched; only the teacher's own comment is gone
        assertThat(userRepository.count()).isEqualTo(2);
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
}
