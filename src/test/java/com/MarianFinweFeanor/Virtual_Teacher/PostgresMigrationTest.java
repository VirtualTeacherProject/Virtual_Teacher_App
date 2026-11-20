package com.MarianFinweFeanor.Virtual_Teacher;

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
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SCRUM-50: the same migrations on a real PostgreSQL, with the "prod" profile
 * (PostgreSQL dialect, ddl-auto=validate). The context only starts if V1 runs on
 * PostgreSQL and Hibernate accepts the resulting schema for every entity.
 */
@DataJpaTest
@ActiveProfiles({"prod", "test"})   // "test" only switches off the admin seeder
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgresMigrationTest {

    private static final EmbeddedPostgres POSTGRES = startPostgres();

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        POSTGRES.close();
    }

    @Autowired private Flyway flyway;
    @Autowired private TestEntityManager em;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void migrationsShouldRunOnPostgres() {
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).isNotEmpty();
    }

    @Test
    void everyEntityShouldBeStorableInTheMigratedSchema() {
        User teacher = new User();
        teacher.setEmail("teacher@test.com");
        teacher.setPassword("secret");
        teacher.setFirstName("First");
        teacher.setLastName("Last");
        teacher.setRole(UserRole.TEACHER);
        teacher.setStatus("ACTIVE");
        em.persist(teacher);

        Course course = new Course();
        course.setTitle("Java");
        course.setTopic("Programming");
        course.setStatus("PUBLISHED");
        course.setTeacher(teacher);
        course.setAttendanceRequired(true);   // SCRUM-45 column (V5)
        em.persist(course);

        Lecture lecture = new Lecture();
        lecture.setTitle("Intro");
        lecture.setDescription("Description");
        lecture.setVideoUrl("https://example.com/video");
        lecture.setAssignmentFilePath("task.txt");
        lecture.setCourse(course);
        em.persist(lecture);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(teacher);
        enrollment.setCourse(course);
        enrollment.setAverageGrade(75.5);
        em.persist(enrollment);

        com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment submission =
                new com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment();
        submission.setStudent(teacher);
        submission.setLecture(lecture);
        submission.setSubmissionFilePath("uploads/a.txt");
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setGrade(80.0);
        em.persist(submission);

        LectureComment comment = new LectureComment();
        comment.setAuthor(teacher);
        comment.setLecture(lecture);
        comment.setComment("A comment");
        comment.setCreatedAt(LocalDateTime.now());
        em.persist(comment);   // id comes from lecture_comments_seq

        LectureNote note = new LectureNote();
        note.setUser(teacher);
        note.setLecture(lecture);
        note.setContent("A note");
        note.setUpdatedAt(LocalDateTime.now());
        em.persist(note);

        CourseResource resource = new CourseResource();
        resource.setCourse(course);
        resource.setTitle("Slides");
        resource.setUrl("https://example.com/slides");
        resource.setCreatedAt(LocalDateTime.now());
        em.persist(resource);

        ResourceRequest request = new ResourceRequest();
        request.setCourse(course);
        request.setStudent(teacher);
        request.setMessage("Please share more exercises. ".repeat(4));
        request.setCreatedAt(LocalDateTime.now());
        // SCRUM-36 columns (V4)
        request.setAssignee(teacher);
        request.setStatus(ResourceRequest.STATUS_IN_PROGRESS);
        request.setResponse("See the docs.");
        request.setRespondedAt(LocalDateTime.now());
        em.persist(request);

        LectureAttendance attendance = new LectureAttendance();   // SCRUM-28 (V6)
        attendance.setLecture(lecture);
        attendance.setStudent(teacher);
        attendance.setStatus(AttendanceStatus.ABSENT);
        attendance.setRecordedAt(LocalDateTime.now());
        em.persist(attendance);

        em.flush();
        em.clear();

        assertThat(em.find(LectureComment.class, comment.getCommentId()).getComment())
                .isEqualTo("A comment");
        assertThat(em.find(LectureNote.class, note.getNoteId()).getContent()).isEqualTo("A note");
        ResourceRequest storedRequest = em.find(ResourceRequest.class, request.getRequestId());
        assertThat(storedRequest.getStatus()).isEqualTo(ResourceRequest.STATUS_IN_PROGRESS);
        assertThat(storedRequest.getAssignee().getUserId()).isEqualTo(teacher.getUserId());
        assertThat(storedRequest.getResponse()).isEqualTo("See the docs.");
        assertThat(em.find(Enrollment.class, enrollment.getEnrollmentId()).getAverageGrade())
                .isEqualTo(75.5);
        assertThat(em.find(Course.class, course.getCourseId()).isAttendanceRequired()).isTrue();
        assertThat(em.find(LectureAttendance.class, attendance.getAttendanceId()).getStatus())
                .isEqualTo(AttendanceStatus.ABSENT);
    }
}
